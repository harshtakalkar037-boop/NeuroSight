#!/usr/bin/env python3
"""
Train NeuroSight v2 from the Indoor Object Detection YOLO dataset.

The source dataset is object detection data. This script converts annotated
bounding boxes into object crops, merges door variants, trains a 5-class
image classifier, evaluates it on the held-out test split, and exports a
UINT8 full-integer TFLite model compatible with the Android app.

Five output classes (exact Android order):
    door, window, chair, table, cabinet

Usage:
    python training/train_neurosight.py \
      --yolo-dir /path/to/indoor-object-detection/versions/1 \
      --out app/src/main/assets/neurosight_encoder.tflite

The script uses a MobileNetV3-Small ImageNet backbone with transfer learning
when the pretrained weights are available. If the weights cannot be
 downloaded, use --no-pretrained to train the same head with a randomly
initialized backbone.
"""

import argparse
import os
import random
from pathlib import Path

import numpy as np
from PIL import Image
import tensorflow as tf

IMG = 224
CLASS_NAMES = ["door", "window", "chair", "table", "cabinet"]
DOOR_SOURCE_NAMES = {"door", "cabinetDoor", "refrigeratorDoor", "openedDoor"}


def parse_args():
    ap = argparse.ArgumentParser()
    ap.add_argument("--yolo-dir", required=True)
    ap.add_argument("--out", default="app/src/main/assets/neurosight_encoder.tflite")
    ap.add_argument("--work-dir", default="training/neurosight5_data")
    ap.add_argument("--epochs", type=int, default=18)
    ap.add_argument("--fine-tune-epochs", type=int, default=6)
    ap.add_argument("--batch", type=int, default=32)
    ap.add_argument("--max-train", type=int, default=900)
    ap.add_argument("--max-val", type=int, default=180)
    ap.add_argument("--max-test", type=int, default=100)
    ap.add_argument("--no-pretrained", action="store_true")
    ap.add_argument("--seed", type=int, default=42)
    return ap.parse_args()


def set_seed(seed):
    random.seed(seed)
    np.random.seed(seed)
    tf.random.set_seed(seed)


def source_to_target(source_name):
    if source_name in DOOR_SOURCE_NAMES:
        return "door"
    if source_name in CLASS_NAMES:
        return source_name
    return None


def read_yaml_names(yolo_dir):
    import yaml
    with open(Path(yolo_dir) / "data.yaml", "r") as f:
        data = yaml.safe_load(f)
    return list(data["names"])


def make_crops(yolo_dir, work_dir, seed, limits):
    """Create balanced object crops from YOLO labels without crossing splits."""
    yolo_dir = Path(yolo_dir)
    work_dir = Path(work_dir)
    class_names = read_yaml_names(yolo_dir)
    rng = random.Random(seed)

    for split in ["train", "valid", "test"]:
        src_images = yolo_dir / split / "images"
        src_labels = yolo_dir / split / "labels"
        dst_split = "val" if split == "valid" else split
        for c in CLASS_NAMES:
            (work_dir / dst_split / c).mkdir(parents=True, exist_ok=True)

        candidates = {c: [] for c in CLASS_NAMES}
        for label_file in sorted(src_labels.glob("*.txt")):
            image_path = None
            for ext in [".jpg", ".jpeg", ".png", ".webp"]:
                p = src_images / (label_file.stem + ext)
                if p.exists():
                    image_path = p
                    break
            if image_path is None:
                continue

            try:
                with open(label_file, "r") as f:
                    lines = [x.strip().split() for x in f if x.strip()]
                with Image.open(image_path) as im:
                    w, h = im.size
                    for j, parts in enumerate(lines):
                        if len(parts) < 5:
                            continue
                        cls_id = int(parts[0])
                        if cls_id < 0 or cls_id >= len(class_names):
                            continue
                        target = source_to_target(class_names[cls_id])
                        if target is None:
                            continue
                        xc, yc, bw, bh = map(float, parts[1:5])
                        x1 = max(0, int((xc - bw / 2) * w))
                        y1 = max(0, int((yc - bh / 2) * h))
                        x2 = min(w, int((xc + bw / 2) * w))
                        y2 = min(h, int((yc + bh / 2) * h))
                        if x2 <= x1 or y2 <= y1:
                            continue
                        # Add a small context margin; useful for recognizing furniture.
                        mx = max(2, int((x2 - x1) * 0.08))
                        my = max(2, int((y2 - y1) * 0.08))
                        box = (max(0, x1 - mx), max(0, y1 - my),
                               min(w, x2 + mx), min(h, y2 + my))
                        candidates[target].append((image_path, box, j))
            except Exception as exc:
                print("skip", label_file, exc)

        limit = limits[dst_split]
        print(f"\n{dst_split.upper()}")
        for target in CLASS_NAMES:
            items = candidates[target]
            rng.shuffle(items)
            items = items[:limit]
            out_dir = work_dir / dst_split / target
            for k, (image_path, box, obj_idx) in enumerate(items):
                out_path = out_dir / f"{image_path.stem}_{obj_idx}_{k}.jpg"
                try:
                    with Image.open(image_path).convert("RGB") as im:
                        crop = im.crop(box)
                        crop.save(out_path, quality=95)
                except Exception as exc:
                    print("crop skip", image_path, exc)
            print(f"  {target:8s}: {len(items)} crops")

    return work_dir


def make_ds(root, split, batch, training):
    ds = tf.keras.utils.image_dataset_from_directory(
        Path(root) / split,
        class_names=CLASS_NAMES,
        image_size=(IMG, IMG),
        batch_size=batch,
        shuffle=training,
        seed=42,
    )
    if training:
        aug = tf.keras.Sequential([
            tf.keras.layers.RandomFlip("horizontal"),
            tf.keras.layers.RandomRotation(0.06),
            tf.keras.layers.RandomZoom(0.12),
            tf.keras.layers.RandomContrast(0.18),
        ])
        ds = ds.map(lambda x, y: (aug(x, training=True), y),
                    num_parallel_calls=tf.data.AUTOTUNE)
    return ds.prefetch(tf.data.AUTOTUNE)


def build_model(pretrained=True):
    inputs = tf.keras.Input(shape=(IMG, IMG, 3), dtype=tf.uint8, name="image")
    # Keep raw 0-255 at the model boundary so TFLite can expose a UINT8 input.
    x = tf.keras.layers.Rescaling(1.0 / 127.5, offset=-1.0)(inputs)
    weights = "imagenet" if pretrained else None
    base = tf.keras.applications.MobileNetV3Small(
        input_shape=(IMG, IMG, 3),
        include_top=False,
        weights=weights,
        include_preprocessing=False,
    )
    base.trainable = False
    x = base(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.25)(x)
    outputs = tf.keras.layers.Dense(len(CLASS_NAMES), activation="softmax", name="probs")(x)
    model = tf.keras.Model(inputs, outputs)
    return model, base


def representative_dataset(ds, count=300):
    seen = 0
    for images, _ in ds.unbatch().batch(1):
        yield [tf.cast(images, tf.float32).numpy()]
        seen += 1
        if seen >= count:
            break


def evaluate_tflite(model_path, test_ds):
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()
    inp = interpreter.get_input_details()[0]
    out = interpreter.get_output_details()[0]
    correct = total = 0
    for images, labels in test_ds:
        for image, label in zip(images.numpy(), labels.numpy()):
            x = image[None, ...].astype(inp["dtype"])
            interpreter.set_tensor(inp["index"], x)
            interpreter.invoke()
            pred = int(np.argmax(interpreter.get_tensor(out["index"])[0]))
            correct += int(pred == int(label))
            total += 1
    print(f"TFLite test accuracy: {correct / max(total, 1):.4f} ({correct}/{total})")
    print("TFLite input:", inp["shape"], inp["dtype"])
    print("TFLite output:", out["shape"], out["dtype"])


def main():
    args = parse_args()
    set_seed(args.seed)

    work_dir = make_crops(
        args.yolo_dir,
        args.work_dir,
        args.seed,
        {"train": args.max_train, "val": args.max_val, "test": args.max_test},
    )

    train_ds = make_ds(work_dir, "train", args.batch, True)
    val_ds = make_ds(work_dir, "val", args.batch, False)
    test_ds = make_ds(work_dir, "test", args.batch, False)

    try:
        model, base = build_model(pretrained=not args.no_pretrained)
    except Exception as exc:
        if args.no_pretrained:
            raise
        print("Could not load ImageNet weights:", exc)
        print("Falling back to randomly initialized MobileNetV3-Small.")
        model, base = build_model(pretrained=False)

    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )

    callbacks = [
        tf.keras.callbacks.EarlyStopping(monitor="val_accuracy", patience=5,
                                         restore_best_weights=True, mode="max"),
        tf.keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.35,
                                             patience=2, min_lr=1e-6),
    ]

    print("\n=== CLASSIFIER HEAD TRAINING ===")
    model.fit(train_ds, validation_data=val_ds, epochs=args.epochs, callbacks=callbacks)

    print("\n=== LIGHT FINE-TUNING ===")
    base.trainable = True
    # Keep BatchNorm frozen for stable small-dataset fine-tuning.
    for layer in base.layers:
        if isinstance(layer, tf.keras.layers.BatchNormalization):
            layer.trainable = False
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-5),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    model.fit(train_ds, validation_data=val_ds, epochs=args.fine_tune_epochs, callbacks=callbacks)

    print("\n=== FLOAT TEST ===")
    _, float_acc = model.evaluate(test_ds, verbose=0)
    print("Float TFLite-source test accuracy:", round(float_acc, 4))

    print("\n=== UINT8 TFLITE CONVERSION ===")
    def rep():
        yield from representative_dataset(train_ds, count=300)

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.representative_dataset = rep
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
    converter.inference_input_type = tf.uint8
    converter.inference_output_type = tf.uint8
    tflite_model = converter.convert()

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_bytes(tflite_model)
    print("Saved:", out_path)
    print("Size:", out_path.stat().st_size, "bytes")

    labels_path = out_path.with_name("neurosight_labels.txt")
    labels_path.write_text("\n".join(CLASS_NAMES) + "\n")
    print("Saved labels:", labels_path)

    evaluate_tflite(str(out_path), test_ds)


if __name__ == "__main__":
    main()
