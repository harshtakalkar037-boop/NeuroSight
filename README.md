NeuroSight
See the World Differently. Feel What Matters.

[Hero image]

One short paragraph

Key Highlights
• Phone-first
• On-device AI
• Snapdragon / NPU ready
• Offline-first
• Haptic + Audio feedback

Problem

Solution

[System Overview]

How It Works

[Architecture]

AI Model
small table

Results
[Accuracy graph]

Dataset
[Dataset graph]

Phone + Office Kit

Tech Stack

Project Structure

Build & Run

Limitations

Roadmap

Hackathon

This will look much more like a serious GitHub project and less like a documentation report.

And importantly, we won't remove the technical substance—you'll just see the important information faster.

If you want, I can give you the entire shorter README as one single copy-paste block, exactly like you asked earlier.

no give proper no shorter nothing but guve proper i will directly chnage to the github see there their is looking better 

Yes bro — now I understand exactly what you mean. You don't want a shorter README; you want a properly designed GitHub README like AppliancePulse, with strong visual hierarchy, compact paragraphs, key-point lines, clean tables, badges, and visuals.

Below is the complete README in ONE single copy-paste block. I have kept the important technical depth while making the presentation much cleaner and more professional.

<div align="center">

# 🧠 NeuroSight

### See the World Differently. Feel What Matters.

**A phone-first assistive vision system that converts visual surroundings into haptic and audio feedback using on-device AI.**

<br>

**iQOO Hackathon 2026 · City Battles · Chennai**

`#PhoneFirst` · `#OnDeviceAI` · `#EdgeAI` · `#Accessibility`

<br>

**📱 Phone-First** &nbsp;·&nbsp; **🧠 On-Device AI** &nbsp;·&nbsp; **⚡ Snapdragon / NPU Ready** &nbsp;·&nbsp; **🔒 Offline-First**

</div>

---

## 🚀 What it is

**NeuroSight** is an Android-based assistive technology prototype that uses a smartphone camera and an on-device AI model to recognize basic surroundings and communicate the result through **distinct vibration and audio patterns**.

Instead of depending on a screen or cloud processing, NeuroSight explores a different interaction model:

> **Camera → AI → Haptics + Audio**

The current prototype recognizes:

- 🧱 **Wall**
- 🚪 **Door**
- 👤 **Person**

The smartphone becomes the sensing, computing, and feedback device — making the system **portable, privacy-aware, and designed for offline operation**.

<div align="center">

<img src="assets/neurosight-system-overview.png" alt="NeuroSight System Overview" width="900"/>

</div>

---

## 🎯 Why NeuroSight?

For blind and low-vision users, understanding what is immediately around them can be challenging.

Existing solutions may depend on:

- Continuous voice descriptions
- Internet connectivity
- Cloud processing
- Dedicated sensing hardware

NeuroSight explores whether the **smartphone already in the user's hand** can provide another sensory channel.

### The idea is simple:

**See with the camera.  
Understand with AI.  
Feel and hear the result.**

---

## 💡 Core Concept

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
              ┌──────────┼──────────┐
              │          │          │
              ▼          ▼          ▼
            WALL       DOOR      PERSON
              │          │          │
              └──────────┼──────────┘
                         │
                  ┌──────┴──────┐
                  ▼             ▼
              📳 HAPTICS     🔊 AUDIO
                  │             │
                  └──────┬──────┘
                         ▼
                       USER
📱 Phone-First Design

NeuroSight is designed around the smartphone as the primary product.

The phone provides the complete sensing and feedback pipeline:

Smartphone Capability	NeuroSight Usage
📷 Camera	Captures the environment
🧠 Mobile Compute	Runs image processing and inference
⚡ Snapdragon / NPU	Targeted mobile AI acceleration
📳 Vibration Motor	Communicates class-specific patterns
🔊 Speaker	Provides audio feedback
📱 Android	Integrates the complete experience

This avoids requiring a separate computer-vision device for the core prototype.

🧠 On-Device AI

The classification pipeline is built around TensorFlow Lite and local inference.

Key points
On-device inference
TensorFlow Lite
UINT8 quantized model
224 × 224 × 3 RGB input
~600 KB model
3-class classification
Designed for Snapdragon / Hexagon NPU acceleration where supported by the device and runtime
No mandatory cloud inference
Inference Pipeline
Camera Frame
     │
     ▼
Image Processing
     │
     ▼
224 × 224 RGB
     │
     ▼
TensorFlow Lite
     │
     ▼
┌───────────────┐
│ Wall          │
│ Door          │
│ Person        │
└───────┬───────┘
        │
        ▼
Haptic + Audio Feedback
🔒 Offline-First

The core NeuroSight classification pipeline is designed to work locally on the smartphone.

📷 Camera
    ↓
Local Image Processing
    ↓
On-Device TFLite Model
    ↓
Prediction
    ↓
📳 Haptics + 🔊 Audio
No mandatory cloud dependency
❌ No cloud inference
❌ No camera-image upload required
❌ No backend required for core classification
❌ No mandatory internet connection
Benefits

🔒 Privacy
Camera data can remain on the device.

📡 Availability
Core classification does not depend on network connectivity.

⚡ Edge Processing
Inference happens locally on the smartphone.

📱 Portability
The complete prototype fits into a device already carried by the user.

🏗️ Technical Architecture
<div align="center"> <img src="assets/neurosight-architecture.png" alt="NeuroSight Technical Architecture" width="900"/> </div>
Processing flow
CameraX captures camera frames.
The frame is converted and prepared for inference.
The image is resized to 224 × 224 RGB.
The TensorFlow Lite model performs local classification.
The predicted class is passed to the feedback engines.
HapticEngine generates the corresponding vibration pattern.
AudioEngine provides the corresponding audio feedback.
🤖 AI Model

The trained model is bundled directly with the Android application.

app/src/main/assets/neurosight_encoder.tflite
Model Specification
Property	Value
Framework	TensorFlow Lite
Input Shape	224 × 224 × 3
Input Type	UINT8
Output Shape	1 × 3
Output Type	UINT8
Classes	Wall / Door / Person
Model Size	~600 KB

The model is intentionally lightweight to make it practical for smartphone deployment.

📊 Development Results

The current prototype was developed and evaluated on a relatively small dataset.

<div align="center"> <img src="assets/neurosight-accuracy.png" alt="NeuroSight Accuracy Results" width="750"/> </div>
Evaluation	Result
Held-out validation	~82%
Full collected dataset evaluation	~91%

These are prototype development results, not production-level performance claims.

Real-world performance can vary with lighting, camera angle, distance, environment, and unseen objects.

📚 Dataset

The current development dataset contains approximately 65 images across the three supported classes.

<div align="center"> <img src="assets/neurosight-dataset-distribution.png" alt="NeuroSight Dataset Distribution" width="750"/> </div>
Class	Images
🧱 Wall	19
🚪 Door	23
👤 Person	23
Total	65

The current dataset demonstrates the feasibility of the prototype.

A production-ready system would require substantially larger and more diverse real-world datasets.

📳 Haptic Feedback

NeuroSight converts the model prediction into a recognizable vibration pattern.

Detection	Feedback
🧱 Wall	Steady / slower vibration
🚪 Door	Medium pulse pattern
👤 Person	Faster pulse pattern

Implementation:

app/src/main/java/com/neurosight/app/haptics/HapticEngine.kt

The goal is to create a simple sensory vocabulary that can be learned through repeated interaction.

🔊 Audio Feedback

Audio provides a second feedback channel.

Detection	Audio
🧱 Wall	Low-frequency tone
🚪 Door	Mid-frequency tone
👤 Person	Higher-frequency tone

Implementation:

app/src/main/java/com/neurosight/app/audio/AudioEngine.kt
Why both?
                 AI RESULT
                     │
             ┌───────┴───────┐
             ▼               ▼
          📳 TOUCH          🔊 SOUND
             │               │
             └───────┬───────┘
                     ▼
                   USER

Using two feedback channels allows NeuroSight to explore sensory substitution beyond a traditional visual interface.

🧩 Sensory Substitution

Most computer-vision applications follow:

Camera → AI → Screen

NeuroSight explores:

Camera → AI → Haptics + Audio

The user can potentially learn associations between patterns and environmental classes:

Pattern A → Wall
Pattern B → Door
Pattern C → Person

The current three classes are only the starting point.

Future versions can explore:

Stairs
Obstacles
Vehicles
Furniture
Crossings
Entrances
Indoor landmarks
🖥️ Office Kit

The phone remains the primary product.

The Office Kit provides a laptop-side environment for development, debugging, monitoring, screen mirroring, and demonstration.

                    OFFICE KIT
                        │
                        ▼
              ┌─────────────────┐
              │     LAPTOP      │
              │                 │
              │ Development     │
              │ Debugging       │
              │ Monitoring      │
              │ Mirroring       │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │   iQOO PHONE    │
              │                 │
              │ Camera          │
              │ On-device AI    │
              │ Haptics         │
              │ Audio           │
              └─────────────────┘
Phone

Handles the core experience:

Camera capture
Image processing
AI inference
Classification
Haptic feedback
Audio feedback
Laptop / Office Kit

Supports:

Development
Debugging
Device monitoring
Screen mirroring
Demonstration
🛠️ Technology Stack
Android
Kotlin
Jetpack Compose
CameraX
Android Camera APIs
Android Vibration APIs
Android Audio APIs
Kotlin Coroutines
Machine Learning
TensorFlow Lite
Lightweight CNN classifier
UINT8 quantization
Image augmentation
Mixup
Label smoothing
224 × 224 RGB input
Hardware
iQOO smartphone
Smartphone camera
Snapdragon mobile platform
Speaker
Vibration motor
📂 Project Structure
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
│       │   │   └── MainScreen.kt
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
└── gradlew.bat
🚀 Build & Run
Requirements
Android Studio
Compatible JDK
Android device
USB debugging enabled for physical-device testing
Clone
git clone https://github.com/harshtakalkar037-boop/NeuroSight.git
cd NeuroSight
Build
./gradlew assembleDebug
Run
Connect an Android device.
Enable Developer Options.
Enable USB Debugging.
Open the project in Android Studio.
Select the connected device.
Build and run the application.
Grant camera permission.
Start the NeuroSight experience.
🧪 Training

Training code and documentation are included in the repository.

training/train_neurosight.py

Training documentation:

docs/MODEL_TRAINING.md

The development pipeline includes:

Dataset preparation
Image augmentation
Mixup
Label smoothing
Lightweight CNN training
UINT8 TensorFlow Lite deployment
🌍 Potential Impact

NeuroSight explores how a device already carried by the user can become an assistive interface.

Potential areas
♿ Accessibility
🏠 Indoor awareness
🚪 Environmental awareness
🧠 Sensory substitution
📱 Accessible mobile computing
🔬 Assistive technology research

The long-term goal is to move from a small three-class prototype toward a more capable environmental-awareness system.

🛣️ Roadmap
01 — Better Data
Larger datasets
More environments
Different lighting conditions
Different distances
Different camera angles
Real-world smartphone images
02 — More Classes
Stairs
Obstacles
Vehicles
Furniture
Crossings
Indoor landmarks
03 — Personalization
Custom haptic patterns
Custom audio patterns
Adaptive feedback
User training mode
04 — Wearables
Haptic wristband
Multi-point vibration
Wireless audio integration
05 — Real-World Evaluation
Larger-scale testing
Accessibility organization collaboration
Blind / low-vision user studies
Usability evaluation
Safety evaluation

🏆 Why NeuroSight
📱 Phone-First

Uses the smartphone as the primary sensing, computing, and feedback platform.

🧠 On-Device AI

Runs the core classification pipeline locally using TensorFlow Lite.

⚡ Snapdragon / NPU

Designed for efficient mobile AI workloads and Snapdragon / Hexagon NPU acceleration where supported by the device and runtime.

🔒 Offline-First

Core classification does not require mandatory cloud processing.

📳 Haptics + Audio

Transforms AI predictions into non-visual feedback.

♿ Accessibility

Explores a new way of communicating environmental information to blind and low-vision users.

🛠️ Technical Depth

Combines:

Computer Vision · Model Training · TensorFlow Lite · Quantization · Android · CameraX · Haptics · Audio · Edge AI

📌 Current Prototype
Feature	Status
Android Application	✅
Camera Input	✅
On-device AI	✅
TensorFlow Lite	✅
Wall Detection	✅
Door Detection	✅
Person Detection	✅
Haptic Feedback	✅
Audio Feedback	✅
Offline Core Pipeline	✅
Lightweight Model	✅
Office Kit Workflow	✅
Larger Real-world Dataset	🔄
Additional Classes	🔄
User Testing	🔄
💭 Vision

NeuroSight is built around one question:

What if a smartphone could communicate the world without relying only on a screen?

Today:

Camera
  ↓
AI
  ↓
Wall / Door / Person
  ↓
Haptics + Audio

Tomorrow:

Smartphone
    ↓
Environmental Understanding
    ↓
Personalized Sensory Feedback
    ↓
Greater Independence

The current prototype is small by design.

The vision is much larger:

Turn the smartphone into a sensory bridge between people and the world around them.

<div align="center">
🧠 NeuroSight
See the World Differently. Feel What Matters.

Built for iQOO Hackathon 2026

Phone-First · On-Device AI · Offline-First · Accessibility

</div> ```
