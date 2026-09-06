# Training the NeuroSight v2 classifier

NeuroSight v2 uses a **5-class** visual classifier:

- `door`
- `window`
- `chair`
- `table`
- `cabinet`

The Android model contract is `1x224x224x3 UINT8 -> 1x5 UINT8` in the exact
label order above.

## Dataset used for v2

The current training pipeline accepts the Kaggle **Indoor Object Detection**
YOLO dataset. Its source labels include `door`, `cabinetDoor`,
`refrigeratorDoor`, `window`, `chair`, `table`, `cabinet`, `couch`,
`openedDoor`, and `pole`.

For NeuroSight v2, the four door-related source labels are merged into the
single `door` class. The five selected output classes are therefore:

```text
door <- door + cabinetDoor + refrigeratorDoor + openedDoor
window
chair
table
cabinet
```

The training script crops annotated bounding boxes into object images before
classification. Train/validation/test remain separated according to the
source dataset splits, and per-class crop limits are used to reduce the large
class imbalance in the source labels.

## Train and export

Install dependencies:

```bash
python3 -m venv venv
. venv/bin/activate
pip install tensorflow numpy pillow pyyaml
```

Then run:

```bash
python training/train_neurosight.py \
  --yolo-dir /path/to/kagglehub/datasets/thepbordin/indoor-object-detection/versions/1 \
  --out app/src/main/assets/neurosight_encoder.tflite
```

The script:

1. Reads YOLO bounding-box annotations.
2. Crops the five selected object classes.
3. Uses MobileNetV3-Small with ImageNet transfer learning when the pretrained
   weights are available.
4. Fine-tunes with a low learning rate.
5. Evaluates the held-out test split.
6. Converts the model to full-integer TFLite with a representative dataset.
7. Verifies the resulting TFLite input/output tensors.
8. Writes `neurosight_labels.txt` beside the model.

TensorFlow's documented full-integer conversion requires a representative
dataset for activation calibration and supports explicit UINT8 input/output.
See the official TensorFlow quantization documentation for details.

## Model contract

| Field | v2 value |
|---|---|
| Model | `app/src/main/assets/neurosight_encoder.tflite` |
| Input | `1x224x224x3`, **UINT8**, raw 0-255 |
| Output | `1x5`, **UINT8** |
| Label order | `door, window, chair, table, cabinet` |
| Quantization | Full integer, UINT8 input/output |

`NeuroSightClassifier.kt` must use the same label order. Do not change the
order in only one place.

## Important limitation

This v2 dataset does **not contain a person or wall class**. Therefore the
5-class v2 model should not be described as a wall/person detector. A future
v3 model can add those classes after collecting or licensing suitable data.

Also, the dataset is an object-detection source dataset and the v2 classifier
is trained on annotated object crops. Live camera accuracy can differ from
crop-level test accuracy because a phone camera frame contains background,
scale, occlusion, blur, and multiple objects.
