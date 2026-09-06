<div align="center">

# 🧠 NeuroSight

### See the World Differently. Feel What Matters.

**A phone-first assistive vision system that converts visual surroundings into haptic and audio feedback using on-device AI.**

**iQOO Hackathon 2026 · City Battles · Chennai**

`Phone-First` · `On-Device AI` · `Edge AI` · `Accessibility`

<br>

</div>

---

## 🚀 What is NeuroSight?

**NeuroSight** is an Android prototype that uses the smartphone camera to recognize basic surroundings and communicate the result through **distinct vibration and audio patterns**.

Instead of sending camera data to a cloud service or relying only on a visual screen, NeuroSight explores a simple sensory-substitution pipeline:

> **Camera → On-Device AI → Haptics + Audio**

The current prototype recognizes:

- 🚪 **Door**
- 🪟 **Window**
- 🪑 **Chair**
- 🟫 **Table**
- 🗄️ **Cabinet**

The smartphone acts as the sensing, computing, and feedback device.

<div align="center">
<img src="assets/neurosight-system-overview.png" alt="NeuroSight system overview" width="900">
</div>

---

## 🎯 The Problem

For blind and low-vision users, understanding nearby surroundings can be difficult, especially in unfamiliar indoor environments.

Many assistive approaches can involve continuous voice descriptions, internet connectivity, cloud processing, or additional hardware.

### NeuroSight asks:

> **Can the smartphone already in a user's hand become a real-time sensory bridge to the surrounding environment?**

The prototype focuses on three building blocks:

**Visual input → Local intelligence → Non-visual feedback**

---

## 💡 The Core Idea

```text
                    REAL WORLD
                         │
                         ▼
                    📷 CAMERA
                         │
                         ▼
                IMAGE PROCESSING
                         │
                         ▼
                 🧠 ON-DEVICE AI
                         │
            ┌────────────┼────────────┐
            ▼            ▼            ▼
          DOOR        WINDOW        CHAIR
            │            │            │
            ├──────── TABLE ──────────┤
            │         CABINET         │
            └────────────┬────────────┘
                         │
                  ┌──────┴──────┐
                  ▼             ▼
              📳 HAPTICS     🔊 AUDIO
                  │             │
                  └──────┬──────┘
                         ▼
                       USER
```

---

# 📱 Phone-First Architecture

NeuroSight is designed around the **smartphone as the primary product**.

| Phone Capability | Role in NeuroSight |
|---|---|
| 📷 Camera | Captures the surrounding scene |
| 🧠 Mobile Compute | Runs image processing and inference |
| ⚡ Snapdragon / NPU | Target for efficient mobile AI acceleration where supported |
| 📳 Vibration Motor | Communicates class-specific patterns |
| 🔊 Speaker | Provides audio feedback |
| 📱 Android | Integrates the complete experience |

The core workflow is designed to run locally rather than requiring a separate computer-vision device.

---

# 🧠 On-Device AI

The classification pipeline uses **MobileNetV3-Small + TensorFlow Lite** and is designed for local smartphone inference.

### Key points

- **On-device inference**
- **TensorFlow Lite deployment**
- **Full-integer UINT8 quantized model**
- **224 × 224 × 3 RGB input**
- **~1.22 MB model**
- **5-class classification**
- **Snapdragon / Hexagon NPU acceleration where supported by the device and runtime**
- **No mandatory cloud inference**

### Inference Pipeline

```text
Camera Frame
     ↓
Image Processing
     ↓
224 × 224 RGB
     ↓
MobileNetV3-Small
     ↓
TensorFlow Lite
     ↓
Door / Window / Chair / Table / Cabinet
     ↓
Haptics + Audio
```

---

# ⚡ Why Edge / NPU?

The model is intentionally lightweight and quantized for mobile deployment.

The architecture is suitable for **edge AI execution** and is designed to take advantage of **Snapdragon / Hexagon NPU acceleration where supported by the device and runtime**.

This keeps the intelligence close to the sensor:

**Camera → Mobile AI → Feedback**

rather than:

**Camera → Cloud → Server → Phone**

### Benefits

- ⚡ Efficient mobile inference
- 🔒 Local camera processing
- 📡 Reduced dependence on connectivity
- 📱 No separate AI computer required for the core prototype

---

# 🔒 Offline-First

The core classification pipeline is designed to work locally on the smartphone.

```text
📷 Camera
   ↓
Local Image Processing
   ↓
On-Device TFLite Model
   ↓
Prediction
   ↓
📳 Haptics + 🔊 Audio
```

### Core pipeline does not require

- ❌ Cloud inference
- ❌ Camera-image upload
- ❌ Backend server
- ❌ Mandatory internet connection

### Why this matters

**Privacy** — camera data can remain on the device.

**Availability** — the core inference path does not depend on network access.

**Portability** — the complete prototype is centered around one smartphone.

---

# 🏗️ Technical Architecture

<div align="center">
<img src="assets/neurosight-architecture.png" alt="NeuroSight technical architecture" width="900">
</div>

### Processing flow

1. **CameraX** captures camera frames.
2. The frame is prepared for inference.
3. The image is resized to **224 × 224 RGB**.
4. **MobileNetV3-Small** performs lightweight image classification.
5. TensorFlow Lite performs local inference using the **UINT8** model.
6. The predicted class is passed to the feedback layer.
7. **HapticEngine** generates the corresponding vibration pattern.
8. **AudioEngine** provides the corresponding audio feedback.

---

# 🤖 AI Model

The trained model is bundled directly with the Android application:

```text
app/src/main/assets/neurosight_encoder.tflite
```

Class labels are stored in:

```text
app/src/main/assets/neurosight_labels.txt
```

| Property | Value |
|---|---|
| Architecture | MobileNetV3-Small |
| Framework | TensorFlow Lite |
| Input | `224 × 224 × 3` |
| Input Type | `UINT8` |
| Output | `1 × 5` |
| Output Type | `UINT8` |
| Classes | Door / Window / Chair / Table / Cabinet |
| Quantization | Full-integer 8-bit |
| Model Size | ~1.22 MB |

The model is packaged as a compact mobile inference asset for real-time smartphone experimentation.

---

# 📊 Development Results

The current model was developed and evaluated as a **hackathon prototype** using the training pipeline and development dataset.

<div align="center">
<img src="assets/neurosight-accuracy.png" alt="NeuroSight accuracy results" width="750">
</div>

| Evaluation | Result |
|---|---:|
| Best validation accuracy | **99.59%** |
| Validation accuracy | **98.63%** |

> **These are model accuracy results, not individual prediction confidence scores.**

These are development results, not production-level performance claims. Real-world performance can vary with lighting, camera angle, distance, environment, and unseen scenes.

---

# 📱 Real Device Performance

The current application was tested end-to-end on an **iQOO smartphone**.

| Metric | Observed |
|---|---:|
| Average inference latency | **~40–50 ms** |
| Median latency | **~50 ms** |
| Throughput | **~30 FPS** |
| P95 latency | Not measured |

The measurements reflect the working Android pipeline from camera processing through model inference and feedback.

---

# 📚 Dataset

The training pipeline uses an indoor-object detection dataset and consolidates related door categories into a single **Door** class.

```text
Door
├── door
├── cabinetDoor
├── refrigeratorDoor
└── openedDoor
```

The final classification categories are:

```text
Door
Window
Chair
Table
Cabinet
```

The training pipeline converts object-detection annotations into classification crops before training the image classifier.

<div align="center">
<img src="assets/neurosight-dataset-distribution.png" alt="NeuroSight dataset distribution" width="750">
</div>

### Classification Crops

| Class | Train | Validation | Test | Total |
|---|---:|---:|---:|---:|
| 🚪 Door | 900 | 180 | 100 | 1,180 |
| 🪟 Window | 403 | 91 | 63 | 557 |
| 🪑 Chair | 204 | 49 | 87 | 340 |
| 🟫 Table | 228 | 40 | 47 | 315 |
| 🗄️ Cabinet | 179 | 32 | 52 | 263 |
| **Total** | **1,914** | **392** | **349** | **2,655** |

### Source Dataset Images

- **1,012** training images
- **230** validation images
- **107** test images
- **1,349** source images total

The crop counts are larger because a single source image can contain multiple annotated objects.

---

# 📳 Haptic + 🔊 Audio Feedback

NeuroSight maps each prediction to a distinguishable sensory pattern.

| Detection | Haptic Feedback | Audio Feedback |
|---|---|---|
| 🚪 Door | Long pulse pattern | 400 Hz tone |
| 🪟 Window | Short pulse pattern | 520 Hz tone |
| 🪑 Chair | Medium pulse + longer pause | 300 Hz tone |
| 🟫 Table | Short pulse + long pause | 650 Hz tone |
| 🗄️ Cabinet | Long pulse + short pause | 250 Hz tone |

### Implementation

```text
app/src/main/java/com/neurosight/app/haptics/HapticEngine.kt
app/src/main/java/com/neurosight/app/audio/AudioEngine.kt
```

The purpose is to create a simple **sensory vocabulary** that can be learned through repeated interaction.

---

# 🧩 Sensory Substitution

Traditional computer-vision applications often follow:

```text
Camera → AI → Screen
```

NeuroSight explores:

```text
Camera → AI → Haptics + Audio
```

The current prototype provides five basic object associations:

```text
Pattern A → Door
Pattern B → Window
Pattern C → Chair
Pattern D → Table
Pattern E → Cabinet
```

The same interaction model can later be extended to more environmental classes.

---

# 🖥️ Office Kit

The **phone remains the primary product**.

The Office Kit provides a laptop-side environment for development, debugging, monitoring, screen mirroring, and demonstration.

```text
                 OFFICE KIT
                     │
                     ▼
              ┌──────────────┐
              │    LAPTOP    │
              │              │
              │ Development  │
              │ Debugging    │
              │ Monitoring   │
              │ Mirroring    │
              └──────┬───────┘
                     │
                     ▼
              ┌──────────────┐
              │  iQOO PHONE  │
              │              │
              │ Camera       │
              │ On-device AI │
              │ Haptics      │
              │ Audio        │
              └──────────────┘
```

### Phone

- Camera capture
- Image processing
- AI inference
- Classification
- Haptic feedback
- Audio feedback

### Office Kit

- Development
- Debugging
- Device monitoring
- Screen mirroring
- Demonstration

---

# 🛠️ Technology Stack

| Area | Technologies |
|---|---|
| Android | Kotlin, Jetpack Compose, Android SDK |
| Camera | CameraX / Image Analysis |
| AI | MobileNetV3-Small, TensorFlow Lite |
| Model | Full-integer UINT8 quantization |
| Acceleration | Snapdragon / NPU acceleration where supported |
| Feedback | VibratorManager, VibrationEffect, AudioTrack |
| Audio | PCM 16-bit mono, generated sine-wave cues |
| Concurrency | Kotlin Coroutines |
| Training | Python, TensorFlow / Keras |
| CI/CD | GitHub Actions |

---

# 📂 Project Structure

```text
NeuroSight/
│
├── app/
│   └── src/main/
│       ├── assets/
│       │   ├── neurosight_encoder.tflite
│       │   └── neurosight_labels.txt
│       │
│       ├── java/com/neurosight/app/
│       │   ├── MainActivity.kt
│       │   ├── audio/
│       │   │   └── AudioEngine.kt
│       │   ├── camera/
│       │   │   └── CameraController.kt
│       │   ├── haptics/
│       │   │   └── HapticEngine.kt
│       │   ├── ml/
│       │   │   └── NeuroSightClassifier.kt
│       │   ├── ui/
│       │   │   └── MainScreen.kt
│       │   └── util/
│       │       └── ImageUtils.kt
│       │
│       └── AndroidManifest.xml
│
├── assets/
│   ├── neurosight-system-overview.png
│   ├── neurosight-architecture.png
│   ├── neurosight-accuracy.png
│   └── neurosight-dataset-distribution.png
│
├── docs/
│   └── MODEL_TRAINING.md
│
├── training/
│   └── train_neurosight.py
│
├── .github/
│   └── workflows/
│
└── Gradle build files
```

---

# 🚀 Build & Run

### Requirements

- Android Studio
- Compatible JDK
- Android SDK
- Android device for physical testing
- USB debugging enabled

### Clone

```bash
git clone https://github.com/harshtakalkar037-boop/NeuroSight.git
cd NeuroSight
```

### Build

```bash
./gradlew assembleDebug
```

### Run

1. Connect the Android device.
2. Enable Developer Options and USB Debugging.
3. Open the project in Android Studio.
4. Select the connected device.
5. Build and run the application.
6. Grant camera permission.
7. Start the NeuroSight experience.

---

# 🧪 Training

Training code and documentation are included in the repository.

```text
training/train_neurosight.py
docs/MODEL_TRAINING.md
```

The development pipeline includes:

- Dataset preparation
- Door-category consolidation
- Object cropping
- Image augmentation
- MobileNetV3-Small transfer learning
- Fine-tuning
- TensorFlow Lite conversion
- Full-integer UINT8 deployment

---

# 🌍 Potential Impact

NeuroSight explores how a **device already carried by the user** can become an assistive interface.

### Potential applications

- ♿ Accessibility
- 🏠 Indoor awareness
- 🚪 Environmental awareness
- 🧠 Sensory substitution
- 📱 Accessible mobile computing
- 🔬 Assistive technology research

The current five-class prototype demonstrates the core interaction model and provides a foundation for richer environmental awareness.

---

# 🛣️ Roadmap

### 01 · Better Data

Larger datasets, more environments, different lighting conditions, distances, camera angles, and real-world smartphone images.

### 02 · More Classes

Stairs, obstacles, vehicles, furniture, crossings, entrances, and indoor landmarks.

### 03 · Personalization

Custom haptic patterns, custom audio patterns, adaptive feedback, and user training.

### 04 · Wearables

Explore haptic wristbands, multi-point vibration, and wireless audio integration.

### 05 · Real-World Evaluation

Larger-scale testing, accessibility collaboration, usability studies, and safety evaluation.

---

# 🏆 Why NeuroSight?

| Hackathon Focus | NeuroSight |
|---|---|
| 📱 Phone-First | Camera, compute, haptics, and audio are integrated into the phone |
| 🧠 On-Device AI | MobileNetV3-Small + TensorFlow Lite inference runs locally |
| ⚡ NPU / Edge AI | Designed for Snapdragon / Hexagon NPU acceleration where supported |
| 🔒 Offline | Core classification does not require cloud inference |
| 📳 Creative Hardware Use | AI predictions become vibration and audio patterns |
| 🛠️ Technical Depth | Computer vision + ML + Android + edge deployment |
| 🖥️ Office Kit | Laptop supports development, monitoring, mirroring, and demonstration |
| ♿ Impact | Explores accessible environmental awareness |

---

# 📌 Current Prototype

**Classes:** Door · Window · Chair · Table · Cabinet  
**Architecture:** MobileNetV3-Small  
**Model:** TensorFlow Lite  
**Input:** 224 × 224 RGB / UINT8  
**Output:** 1 × 5 / UINT8  
**Model size:** ~1.22 MB  
**Inference:** On-device  
**Feedback:** Haptics + Audio  
**Observed latency:** ~40–50 ms  
**Observed throughput:** ~30 FPS  
**Connectivity:** Core pipeline designed for offline operation  
**Platform:** Android / iQOO smartphone  
**AI acceleration:** Snapdragon / Hexagon NPU where supported by device and runtime

---

# 💭 Vision

NeuroSight is built around one question:

> **What if a smartphone could communicate the world without relying only on a screen?**

Today:

```text
Camera
  ↓
On-Device AI
  ↓
Door / Window / Chair / Table / Cabinet
  ↓
Haptics + Audio
```

Tomorrow:

```text
Smartphone
    ↓
Environmental Understanding
    ↓
Personalized Sensory Feedback
    ↓
Greater Independence
```

The current prototype is small. **The vision is to turn the smartphone into a sensory bridge between people and the world around them.**

---

<div align="center">

### ❤️ Built for iQOO Hackathon 2026 · City Battles · Chennai

**NeuroSight · Phone-First · On-Device AI · Edge AI · Accessibility**

</div>
