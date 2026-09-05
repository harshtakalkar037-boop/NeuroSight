# Training the NeuroSight classifier

This project ships with a trained 3-class model at
`app/src/main/assets/neurosight_encoder.tflite`. This document explains
what that model is and how to train a better one on your own data.

## What the committed model is

- A 3-class CNN (`wall`, `door`, `person`), input `224x224x3`.
- Trained **from scratch** (no pre-trained ImageNet backbone) on ~65
  images (real web photos + a few synthetic ones) that were captured/
  scraped for this task. A pre-trained backbone was not used because the
  ImageNet weights host (`storage.googleapis.com`) is unreachable from the
  build environment.
- Squeeze-and-Excitation + L2 regularized architecture with strong data
  augmentation and mixup.
- INT8-quantized TFLite (UINT8 input, UINT8 output) so it matches
  `NeuroSightClassifier.kt` with `MODEL_IS_INT8_QUANTIZED = true`.

### Measured performance (from training)

- Best held-out validation accuracy: **~0.82**
- Full-dataset (train+val, UINT8 TFLite) accuracy: **~0.91**
- Model size: ~611 KB

These numbers come from the exact quantized model the app loads, evaluated
with the app's read/dequantize logic.

**It is a DEMO model.** It makes the full pipeline (camera → class →
haptics → audio) work and is reasonably accurate on scenes similar to its
training data, but it is not "100% accurate" on real, unconstrained
camera footage.

## Why it won't be "100% accurate" out of the box

Real-world accuracy requires:
1. Training on frames captured with *your* phone's camera (same lens,
   lighting, Indian indoor/crowded scenes, 224x224 crops).
2. A pre-trained backbone (MobileNetV3-Small / V2 with ImageNet weights)
   + fine-tuning, which transfers knowledge from millions of images.

The master document's Day-2 plan already describes this exact flow.

## Reproducing training (Linux / Mac)

1. Install dependencies:
   ```bash
   python3 -m venv venv && . venv/bin/activate
   pip install tensorflow-cpu numpy pillow
   ```
2. Create a dataset folder with one subfolder per class, each containing
   `.jpg` images (224x224 is fine, any size works — the loader resizes):
   ```
   data/
     wall/   .jpg ...
     door/   .jpg ...
     person/ .jpg ...
   ```
3. Point the script at it and run:
   ```bash
   python training/train_neurosight.py --data-dir ./data \
       --out app/src/main/assets/neurosight_encoder.tflite
   ```
   The CLI script is in `training/train_neurosight.py`. The cross-validated
   improvement script (with mixup + squeeze-excite) used for the current
   model is available in this document's history on the branch; the CLI
   script exposes the core options (`--epochs`, `--batch`, `--classes`).
4. To improve accuracy a lot, use a pre-trained backbone (e.g.
   `tf.keras.applications.MobileNetV3Small` with `weights="imagenet"`)
   instead of the from-scratch CNN, and train on a few hundred real frames
   per class.

## The model contract (what your replacement must satisfy)

| Field | Value |
|---|---|
| Place | `app/src/main/assets/neurosight_encoder.tflite` |
| Input | `1x224x224x3`, **UINT8** (raw 0-255), quantized |
| Output | `1x3` scores, order `wall, door, person` |
| Quantization | INT8 full-integer (input & output UINT8) |

The app's `NeuroSightClassifier` feeds raw 0-255 UINT8 pixels and reads
dequantized scores. A model built with the standard TensorFlow full-integer
conversion (input/output type `uint8`, representative dataset in `[0,255]`)
matches this automatically. If you switch to a float model, set
`MODEL_IS_INT8_QUANTIZED = false` in `NeuroSightClassifier.kt`.
