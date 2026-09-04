#!/usr/bin/env python3
"""
Train the NeuroSight 3-class classifier (wall / door / person) and export
an INT8-quantized TFLite model that matches the app's contract
(UINT8 224x224x3 input -> UINT8 1x3 scores, order wall, door, person).

Usage:
    python train_neurosight.py --data-dir ./data \
        --out app/src/main/assets/neurosight_encoder.tflite

data-dir layout (one subfolder per class):
    data/wall/*.jpg  data/door/*.jpg  data/person/*.jpg

For much better accuracy, swap `build_model()` to use a pre-trained
backbone (e.g. tf.keras.applications.MobileNetV3Small weights="imagenet")
and train on a few hundred real frames per class.
"""
import os, glob, json, math, argparse
import numpy as np
import tensorflow as tf
from PIL import Image

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data-dir", required=True)
    ap.add_argument("--out", default="app/src/main/assets/neurosight_encoder.tflite")
    ap.add_argument("--classes", nargs="+", default=["wall", "door", "person"])
    ap.add_argument("--epochs", type=int, default=60)
    ap.add_argument("--batch", type=int, default=8)
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--val-frac", type=float, default=0.22)
    args = ap.parse_args()

    IMG = 224
    CLASSES = args.classes          # order must match app's LABELS
    tf.random.set_seed(args.seed); np.random.seed(args.seed)

    def read_jpg(path, label):
        img = tf.io.read_file(path)
        img = tf.io.decode_jpeg(img, channels=3)
        img = tf.image.resize(img, [IMG, IMG])
        return tf.cast(img, tf.float32), label

    def augment(img, label):
        img = tf.image.random_flip_left_right(img)
        img = tf.image.random_brightness(img, 0.22)
        img = tf.image.random_contrast(img, 0.75, 1.25)
        img = tf.image.random_saturation(img, 0.75, 1.25)
        img = tf.image.random_crop(img, [int(IMG*0.88), int(IMG*0.88), 3])
        img = tf.image.resize(img, [IMG, IMG])
        return img, label

    def make_ds(files, labels, train):
        ds = tf.data.Dataset.from_tensor_slices((files, labels))
        ds = ds.map(read_jpg, num_parallel_calls=tf.data.AUTOTUNE)
        if train:
            ds = ds.map(augment, num_parallel_calls=tf.data.AUTOTUNE)
            ds = ds.repeat(30).shuffle(1024).batch(args.batch)
        else:
            ds = ds.batch(args.batch)
        return ds.prefetch(tf.data.AUTOTUNE)

    def build_model(reg_lambda=1e-4):
        reg = tf.keras.regularizers.l2(reg_lambda)
        i = tf.keras.Input(shape=(IMG, IMG, 3))
        x = tf.keras.layers.Rescaling(1/255.0)(i)
        for f in [32, 64, 128]:
            x = tf.keras.layers.Conv2D(f, 3, strides=2, padding="same",
                                       use_bias=False, kernel_regularizer=reg)(x)
            x = tf.keras.layers.BatchNormalization()(x); x = tf.keras.layers.ReLU()(x)
            x = tf.keras.layers.Conv2D(f, 3, padding="same",
                                       use_bias=False, kernel_regularizer=reg)(x)
            x = tf.keras.layers.BatchNormalization()(x); x = tf.keras.layers.ReLU()(x)
            x = tf.keras.layers.MaxPooling2D(2)(x)
        x = tf.keras.layers.GlobalAveragePooling2D()(x)
        x = tf.keras.layers.Dropout(0.4)(x)
        x = tf.keras.layers.Dense(128, activation="relu", kernel_regularizer=reg)(x)
        x = tf.keras.layers.Dropout(0.4)(x)
        out = tf.keras.layers.Dense(len(CLASSES), activation="softmax", name="probs")(x)
        return tf.keras.Model(i, out)

    # ---- stratified split ----
    train_files, tl, val_files, vl = [], [], [], []
    for ci, c in enumerate(CLASSES):
        files = sorted(glob.glob(os.path.join(args.data_dir, c, "*.jpg")))
        np.random.shuffle(files)
        n_val = max(1, int(round(len(files) * args.val_frac)))
        for i, f in enumerate(files):
            (val_files if i < n_val else train_files).append(f)
            (vl if i < n_val else tl).append(ci)
    train_y = np.array(tl); val_y = np.array(vl)
    print("train:", len(train_files), np.bincount(train_y, minlength=len(CLASSES)))
    print("val:  ", len(val_files),   np.bincount(val_y, minlength=len(CLASSES)))

    model = build_model()
    train_ds = make_ds(train_files, train_y, True)
    val_ds = make_ds(val_files, val_y, False)
    model.compile(optimizer=tf.keras.optimizers.Adam(1e-3),
                  loss="sparse_categorical_crossentropy", metrics=["accuracy"])
    steps = math.ceil(len(train_files)*30/args.batch)
    hist = model.fit(
        train_ds, steps_per_epoch=steps, epochs=args.epochs,
        validation_data=val_ds,
        validation_steps=math.ceil(len(val_files)/args.batch),
        callbacks=[
            tf.keras.callbacks.EarlyStopping(monitor="val_loss", patience=12,
                                             restore_best_weights=True),
            tf.keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5,
                                                 patience=5, min_lr=1e-5),
        ], verbose=2)
    print("best val acc:", round(max(hist.history["val_accuracy"]), 3))

    # ---- INT8 quantize (UINT8 in/out) ----
    def rep():
        for i in range(len(train_files)):
            yield [read_jpg(train_files[i], 0)[0].numpy()[None,...].astype(np.float32)]
    conv = tf.lite.TFLiteConverter.from_keras_model(model)
    conv.optimizations = [tf.lite.Optimize.DEFAULT]
    conv.representative_dataset = rep
    conv.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
    conv.inference_input_type = tf.uint8
    conv.inference_output_type = tf.uint8
    tfl = conv.convert()
    os.makedirs(os.path.dirname(args.out) or ".", exist_ok=True)
    open(args.out, "wb").write(tfl)
    print("saved:", args.out, os.path.getsize(args.out), "bytes")

if __name__ == "__main__":
    main()
