````markdown
# NeuroSight 🧠

### See the World Differently. Feel What Matters.

**Phone-first sensory substitution for blind and low-vision users**

**iQOO Hackathon 2026 · City Battles · Chennai**

> 📷 Camera → 🧠 On-Device AI → 📳 Haptics + 🔊 Audio

---

## What is NeuroSight?

NeuroSight is an Android application that explores a simple idea:

**Can a smartphone convert visual information into something a blind or low-vision user can learn through touch and sound?**

The phone camera observes the environment, an on-device TensorFlow Lite model classifies the scene, and the result is converted into different **vibration and audio patterns**.

The current prototype recognizes:

- **Wall**
- **Door**
- **Person**

The goal is not to replace vision.  
The goal is to explore an alternative way of communicating environmental information through a device that users already carry.

---

## The Core Idea

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
                  ▼
       ┌─────────────────────┐
       │  Wall / Door /       │
       │  Person              │
       └──────────┬──────────┘
                  │
             ┌────┴────┐
             ▼         ▼
          📳 HAPTICS  🔊 AUDIO
             │         │
             └────┬────┘
                  ▼
                USER
````

<div align="center">

<img src="assets/neurosight-system-overview.png"
  alt="NeuroSight System Overview"
  width="950"/>

</div>

---

# Why NeuroSight?

For a blind or low-vision user, understanding what is immediately around them can be important.

Many assistive solutions can depend heavily on:

* Continuous spoken descriptions
* Internet connectivity
* Cloud-based processing
* Additional hardware

A modern smartphone already provides the basic building blocks needed for an assistive system:

* 📷 Camera
* 🧠 Mobile compute
* ⚡ AI acceleration
* 📳 Vibration motor
* 🔊 Speaker

### NeuroSight brings these capabilities together in one phone-first system.

---

# 📱 Phone-First

## The phone is the product.

NeuroSight is designed around the iQOO smartphone rather than treating the phone as only a display or controller.

### The phone provides:

* 📷 **Camera** — captures the environment
* 🧠 **On-device compute** — runs the AI pipeline
* ⚡ **Snapdragon / NPU acceleration** — designed for mobile AI workloads
* 📳 **Haptics** — communicates through vibration
* 🔊 **Audio** — provides a second sensory channel

```text
                    iQOO PHONE
                         │
          ┌──────────────┼──────────────┐
          │              │              │
          ▼              ▼              ▼
       Camera       On-device        Haptics
                        AI
                         │
                         ▼
                       Audio
```

The phone-first approach keeps the system portable and avoids requiring a separate sensing computer.

---

# 🧠 On-Device AI

The core classification pipeline uses **TensorFlow Lite** and is designed for local inference.

### Key points

* **On-device inference**
* **TensorFlow Lite**
* **UINT8 quantized model**
* **224 × 224 RGB input**
* **~600 KB model**
* **3-class classification**
* Designed for **Snapdragon / Hexagon NPU acceleration where supported by the device and runtime**

```text
Camera Frame
     ↓
Resize / RGB Conversion
     ↓
224 × 224 Input
     ↓
TensorFlow Lite Model
     ↓
Wall / Door / Person
     ↓
Haptics + Audio
```

### Why on-device?

**⚡ Lower dependency on network connectivity**

**🔒 Better privacy for camera processing**

**📡 Core inference can work without cloud processing**

**📱 Designed for mobile deployment**

---

# 🔒 Offline-First

NeuroSight is designed so that the core AI pipeline does not require a cloud server.

### No mandatory:

* Cloud inference
* Backend server
* User account
* Camera upload
* Internet connection for the core classification pipeline

The camera frame can be processed locally on the smartphone.

```text
             📷 CAMERA
                  │
                  ▼
          LOCAL PROCESSING
                  │
                  ▼
            TFLITE MODEL
                  │
                  ▼
           LOCAL FEEDBACK
             ↙       ↘
        📳 HAPTICS   🔊 AUDIO
```

For an assistive application, this architecture is particularly useful because **privacy, portability and availability** matter.

---

# 🏗️ Technical Architecture

<div align="center">

<img src="assets/neurosight-architecture.png"
  alt="NeuroSight Technical Architecture"
  width="950"/>

</div>

### Processing pipeline

1. **CameraX** captures camera frames.
2. Frames are prepared for model inference.
3. Images are resized to **224 × 224 RGB**.
4. The TensorFlow Lite model performs classification.
5. The predicted class is passed to the sensory engines.
6. Haptic and audio patterns communicate the result.

---

# 🤖 AI Model

The trained model is included directly in the Android application:

```text
app/src/main/assets/neurosight_encoder.tflite
```

### Model details

| Property    | Value                |
| ----------- | -------------------- |
| Framework   | TensorFlow Lite      |
| Input       | `224 × 224 × 3`      |
| Input type  | `UINT8`              |
| Output      | `1 × 3`              |
| Output type | `UINT8`              |
| Classes     | Wall / Door / Person |
| Model size  | ~600 KB              |

The lightweight model is suitable for experimentation with smartphone-based inference and keeps the deployed model footprint small.

---

# 📳 Haptic Feedback

NeuroSight maps each detected class to a distinguishable vibration pattern.

### Wall

**Slow / steady vibration pattern**

### Door

**Medium pulse pattern**

### Person

**Faster pulse pattern**

Implementation:

```text
app/src/main/java/com/neurosight/app/haptics/HapticEngine.kt
```

The patterns are designed to create a simple sensory vocabulary that a user could potentially learn through repeated interaction.

---

# 🔊 Audio Feedback

Audio provides a second channel of information.

### Wall

**Low-frequency tone**

### Door

**Mid-frequency tone**

### Person

**Higher-frequency tone**

Implementation:

```text
app/src/main/java/com/neurosight/app/audio/AudioEngine.kt
```

Using **touch + sound** gives NeuroSight two complementary ways to communicate environmental information.

---

# 🧩 Sensory Substitution

This is the central interaction concept behind NeuroSight.

Instead of:

```text
Camera → AI → Screen
```

NeuroSight explores:

```text
Camera → AI → Haptics + Audio
```

A user could gradually learn associations such as:

```text
Pattern A → Wall
Pattern B → Door
Pattern C → Person
```

The current three classes are only the starting point.

Future versions could extend the sensory vocabulary to:

* Stairs
* Obstacles
* Vehicles
* Furniture
* Crossings
* Entrances
* Indoor landmarks

---

# 📊 Model Results

The current model is a **hackathon prototype** trained on a relatively small dataset.

We therefore present these as development results rather than production-level accuracy.

<div align="center">

<img src="assets/neurosight-accuracy.png"
  alt="NeuroSight Accuracy Results"
  width="800"/>

</div>

### Development results

| Evaluation                        |   Result |
| --------------------------------- | -------: |
| Held-out validation               | **~82%** |
| Full collected dataset evaluation | **~91%** |

These results should be interpreted in the context of the current dataset size and prototype stage.

---

# 📚 Dataset

The current development dataset contains approximately **65 images**.

<div align="center">

<img src="assets/neurosight-dataset-distribution.png"
  alt="NeuroSight Dataset Distribution"
  width="800"/>

</div>

| Class     | Images |
| --------- | -----: |
| Wall      |     19 |
| Door      |     23 |
| Person    |     23 |
| **Total** | **65** |

The dataset demonstrates the feasibility of the current prototype.

For a production system, significantly more real-world data would be required.

---

# 🧪 Training

The training code is included in the repository.

```text
training/train_neurosight.py
```

Training documentation:

```text
docs/MODEL_TRAINING.md
```

The development process includes:

* Dataset balancing
* Image augmentation
* Mixup
* Label smoothing
* Lightweight CNN architecture
* Quantized TensorFlow Lite deployment

---

# 🖥️ Office Kit

The smartphone remains the **primary product**.

The Office Kit provides a laptop-side environment for development, monitoring and demonstration.

```text
┌─────────────────────────┐
│          LAPTOP         │
│                         │
│  Android Studio         │
│  Debugging              │
│  Monitoring             │
│  Screen Mirroring       │
└────────────┬────────────┘
             │
          Office Kit
             │
             ▼
┌─────────────────────────┐
│       iQOO PHONE        │
│                         │
│  Camera                 │
│  On-device AI           │
│  Haptics                │
│  Audio                  │
└─────────────────────────┘
```

### Phone

Handles the main workflow:

* Camera capture
* Image processing
* AI inference
* Classification
* Haptics
* Audio

### Laptop

Supports:

* Development
* Debugging
* Device monitoring
* Screen mirroring
* Demonstration

---

# 🏆 Why This Fits the iQOO Hackathon

NeuroSight is built around the core strengths of a modern smartphone.

### 📱 Creative Phone Use

The phone is not just a screen.

Its:

* Camera
* Processor
* AI acceleration
* Speaker
* Vibration motor

are combined into an assistive interface.

### 🧠 Technical Depth

The project combines:

* Computer vision
* Model training
* TensorFlow Lite
* Quantization
* Android development
* CameraX
* Haptic feedback
* Audio feedback
* On-device inference

### ⚡ Mobile AI / NPU

The model is designed for efficient smartphone inference and **Snapdragon / Hexagon NPU acceleration where supported by the device and runtime**.

### 🔒 Offline

The core inference pipeline is local rather than dependent on a cloud server.

### ♿ Impact

The project explores how a device already carried by millions of people can become a more accessible interface for understanding the surrounding environment.

---

# 🛠️ Technology Stack

### Android

* Kotlin
* Jetpack Compose
* CameraX
* Android Camera APIs
* Android Vibration APIs
* Android Audio APIs
* Kotlin Coroutines

### Machine Learning

* TensorFlow Lite
* Lightweight CNN classifier
* UINT8 quantization
* 224 × 224 RGB input
* Three-class classification

### Hardware

* iQOO smartphone
* Smartphone camera
* Snapdragon mobile platform
* Speaker
* Vibration motor

---

# 📂 Project Structure

```text
NeuroSight/
│
├── app/
│   └── src/main/
│       ├── assets/
│       │   └── neurosight_encoder.tflite
│       │
│       ├── java/com/neurosight/app/
│       │   ├── MainActivity.kt
│       │   │
│       │   ├── audio/
│       │   │   └── AudioEngine.kt
│       │   │
│       │   ├── camera/
│       │   │   └── CameraController.kt
│       │   │
│       │   ├── haptics/
│       │   │   └── HapticEngine.kt
│       │   │
│       │   ├── ml/
│       │   │   └── NeuroSightClassifier.kt
│       │   │
│       │   ├── ui/
│       │   │   ├── MainScreen.kt
│       │   │   └── theme/
│       │   │
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
│       └── build-apk.yml
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

---

# 🚀 Build

## Requirements

* Android Studio
* Compatible JDK
* Android device
* USB debugging enabled for physical-device testing

### Clone

```bash
git clone https://github.com/harshtakalkar037-boop/NeuroSight.git
cd NeuroSight
```

### Build on macOS / Linux

```bash
./gradlew assembleDebug
```

### Build on Windows

```bash
gradlew.bat assembleDebug
```

---

# 📱 Run on an Android Device

1. Enable Developer Options.
2. Enable USB Debugging.
3. Connect the Android device.
4. Open the project in Android Studio.
5. Select the connected device.
6. Build and run.
7. Grant camera permission.
8. Start the NeuroSight pipeline.

---

# 🌍 Potential Impact

NeuroSight is designed around an everyday device rather than requiring a dedicated computer-vision computer.

### Potential beneficiaries

* Blind users
* Low-vision users
* Accessibility organizations
* Rehabilitation centers
* Assistive technology researchers

### Potential applications

**Indoor awareness**

Walls, doors, corridors and obstacles.

**Environmental awareness**

Objects and people in unfamiliar surroundings.

**Sensory substitution**

Learning associations between environmental classes and alternative sensory signals.

**Accessible mobile computing**

Exploring how smartphone hardware can become an assistive interface.

---

# 🛣️ Roadmap

## 1. Better Data

* Larger datasets
* Real smartphone-camera images
* More environments
* Different lighting conditions
* Different distances
* Different camera angles

## 2. More Classes

* Stairs
* Obstacles
* Vehicles
* Furniture
* Crossings
* Indoor landmarks

## 3. Personalization

* User-specific haptic patterns
* User-specific audio patterns
* Adaptive feedback
* Training mode

## 4. Wearables

* Haptic wristband
* Multi-point vibration
* Wireless earbuds

## 5. Real-World Evaluation

* Accessibility organization partnerships
* Blind / low-vision user testing
* Usability studies
* Safety evaluation
* Larger real-world datasets

---

# ⚠️ Limitations

NeuroSight is currently a **hackathon prototype**.

Current limitations include:

* Only three classes are supported.
* The training dataset is relatively small.
* Accuracy may vary in unseen environments.
* Lighting can affect predictions.
* Camera angle and distance can affect predictions.
* Similar-looking objects may be misclassified.
* Extensive real-world accessibility testing has not yet been completed.

### Safety

NeuroSight should **not** currently replace:

* White canes
* Guide dogs
* Human assistance
* Established mobility aids
* Professional accessibility systems

The prototype demonstrates the concept and requires substantially more testing before safety-critical use.

---

# 🔐 Design Principles

### 📱 Phone-First

The smartphone is the primary sensing, computing and feedback platform.

### 🧠 On-Device

The core AI pipeline is designed for local inference.

### 🔒 Offline-First

The core classification pipeline does not require mandatory cloud inference.

### ⚡ Mobile AI

The model is intentionally lightweight and designed for smartphone deployment.

### ♿ Human-Centered

The output is designed around how people can learn and interpret sensory patterns.

---

# 💡 The Bigger Vision

Most computer-vision systems communicate their result through a screen.

NeuroSight explores a different interaction model:

```text
TRADITIONAL

Camera
   ↓
AI
   ↓
Screen
   ↓
User
```

```text
NEUROSIGHT

Camera
   ↓
On-device AI
   ↓
┌───────────────┐
│               │
▼               ▼
Haptics        Audio
│               │
└───────┬───────┘
        ▼
       User
```

The long-term vision is to make the smartphone a **sensory bridge between the physical world and the user**.

---

# 📌 Current Prototype

**Classes:** Wall · Door · Person
**Model:** TensorFlow Lite
**Input:** 224 × 224 RGB
**Model size:** ~600 KB
**Inference:** On-device
**Deployment:** Android
**Feedback:** Haptics + Audio
**Connectivity:** Core pipeline designed for offline operation
**Hardware:** iQOO smartphone / Snapdragon platform

---

## Built for iQOO Hackathon 2026

**NeuroSight — See the World Differently. Feel What Matters.**

```
```
