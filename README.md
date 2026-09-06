# 🧠 NeuroSight

### A phone that sees the world and communicates it through sound + touch

**iQOO Hackathon · City Battles 2026 · Pune/Chennai**

> NeuroSight is a phone-first assistive vision system that uses the smartphone camera and on-device AI to recognize surrounding objects and convert visual information into distinct audio and haptic cues.

---

## 🚀 Overview

NeuroSight explores how a smartphone can become an **assistive sensory interface** for people who cannot reliably depend on vision.

The system continuously analyzes the camera feed using an **on-device TensorFlow Lite model** and recognizes common indoor objects.

Instead of presenting only a visual label, NeuroSight communicates the detected object through:

- 🔊 **Distinct audio cues**
- 📳 **Distinct vibration patterns**
- 📱 **Real-time Android feedback**
- ⚡ **Hardware-accelerated inference where supported**
- 🔒 **Offline-first processing**

The goal is simple:

> **Camera → AI → Object → Sound + Touch**

---

## 🎯 Problem

A smartphone camera can capture a huge amount of visual information, but that information is normally presented back to the user visually.

For a visually impaired or low-vision user, simply saying:

> "Chair detected"

is not always enough.

NeuroSight investigates a different interaction model:

> **What if the phone could translate visual information into a sensory language using touch and sound?**

Different objects therefore produce different combinations of:

- vibration rhythm
- vibration duration
- vibration intensity
- audio frequency
- audio rhythm

This allows the user to learn and distinguish objects without depending entirely on a screen.

---

## 💡 Core Idea

NeuroSight creates a lightweight sensory-substitution pipeline:

```text
                📷 Smartphone Camera
                         │
                         ▼
                    CameraX
                         │
                         ▼
              Image Preprocessing
                 224 × 224 RGB
                         │
                         ▼
              MobileNetV3-Small
                         │
                         ▼
             TensorFlow Lite Model
                         │
                 UINT8 Output
                         │
                         ▼
                  1 × 5 Classes
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
        📳 HapticEngine        🔊 AudioEngine
              │                     │
              └──────────┬──────────┘
                         ▼
                Sensory Feedback
🏗️ System Architecture

Processing Pipeline
CameraX captures frames from the smartphone camera.
Frames are resized and converted into the model's expected input format.
The image is processed at 224 × 224 RGB resolution.
MobileNetV3-Small performs lightweight image classification.
TensorFlow Lite executes the model on the Android device.
The model produces a 1 × 5 UINT8 output.
The highest-scoring class is selected.
HapticEngine generates the object's vibration pattern.
AudioEngine generates the object's audio pattern.
The user receives the result through touch + sound.
🤖 On-Device AI

NeuroSight is designed around edge inference rather than sending camera frames to a remote server.

Model
Property	NeuroSight
Architecture	MobileNetV3-Small
Framework	TensorFlow Lite
Input	224 × 224 × 3 RGB
Input Type	UINT8
Output	1 × 5
Output Type	UINT8
Quantization	Full-integer 8-bit
Classes	5
Model Size	~1.22 MB
Execution	Android device
Acceleration	Hardware-accelerated backend where supported
CPU fallback	Supported
Classes

The current model recognizes:

🚪 Door
🪟 Window
🪑 Chair
🟫 Table
🗄️ Cabinet
📊 Model Results

The model was trained and evaluated during development using the NeuroSight training pipeline.

Accuracy Results
Best validation accuracy: 99.59%
Validation accuracy: 98.63%

These numbers represent model accuracy, not the confidence score of an individual prediction.

Important Note

These results were obtained on the development dataset and should not be interpreted as production-world accuracy.

Real-world performance can vary with:

lighting
object distance
camera angle
occlusion
object appearance
background complexity
device camera characteristics
📱 Real Device Performance

NeuroSight was tested end-to-end on an iQOO smartphone.

Observed development measurements:

Metric	Observed
Average inference latency	~40–50 ms
Median latency	~50 ms
Throughput	~30 FPS
P95 latency	Not measured

The application was tested as a complete pipeline rather than only testing the model independently.

This includes:

Camera → preprocessing → TensorFlow Lite inference → classification → audio/haptic feedback

⚡ Edge / Hardware Acceleration

NeuroSight is designed to take advantage of Android's hardware acceleration capabilities where available.

The application uses a TensorFlow Lite inference path that can leverage supported device acceleration, with CPU execution available as a fallback.

The architecture is therefore suitable for modern smartphone SoCs containing dedicated AI acceleration hardware.

Exact accelerator routing can vary by device, Android version, TensorFlow Lite runtime, and delegate availability.

This keeps the system focused on the phone itself as the AI platform rather than requiring a cloud backend.

📊 Dataset

The model training pipeline uses an indoor-object detection dataset containing multiple object categories.

The original object-detection labels were consolidated into the five NeuroSight classes:

door
cabinetDoor
refrigeratorDoor
openedDoor
        ↓
      Door

The final classification categories are:

Door
Window
Chair
Table
Cabinet

The training pipeline converts object-detection annotations into classification crops before training the image classifier.

Dataset Distribution

Classification Crops
Class	Train	Validation	Test	Total
Door	900	180	100	1,180
Window	403	91	63	557
Chair	204	49	87	340
Table	228	40	47	315
Cabinet	179	32	52	263
Total	1,914	392	349	2,655

The source dataset contains:

1,012 training images
230 validation images
107 test images
1,349 source images total

The crop counts are larger because a single source image can contain multiple annotated objects.

📳 Haptic Sensory Language

NeuroSight does not use the same vibration for every object.

Each class has its own temporal vibration pattern.

Object	Pattern
Door	Long pulse → pause → repeat
Window	Short pulse → pause → repeat
Chair	Medium pulse → longer pause
Table	Short pulse → long pause
Cabinet	Long pulse → short pause

The implementation uses Android's VibratorManager / VibrationEffect APIs.

The patterns can therefore become a small tactile vocabulary that users can learn over time.

🔊 Audio Sensory Language

Audio is also class-specific.

NeuroSight generates lightweight tones locally using Android's audio stack.

Object	Frequency
Door	400 Hz
Window	520 Hz
Chair	300 Hz
Table	650 Hz
Cabinet	250 Hz

The audio engine uses:

AudioTrack
PCM 16-bit mono audio
44.1 kHz sample rate
generated sine-wave cues

This allows the application to communicate object identity without relying on speech synthesis or an internet connection.

🧠 Sensory Substitution

The central concept behind NeuroSight is sensory substitution.

Instead of:

Visual information
        ↓
       Eyes

the system explores:

Visual information
        ↓
 Smartphone Camera
        ↓
     AI Model
        ↓
 ┌──────┴──────┐
 ▼             ▼
Touch         Sound

Over time, users could potentially learn the association between:

object → vibration → sound

and build a compact sensory vocabulary.

📱 Why a Smartphone?

A smartphone already contains almost everything required:

📷 Camera
🧠 AI-capable processor
🔊 Speaker
📳 Vibration motor
🔋 Battery
📱 Display
⚡ Modern mobile SoC
📡 Connectivity when required

NeuroSight therefore focuses on transforming an existing device into an assistive interface rather than requiring dedicated hardware.

💻 Office Kit / Phone–Laptop Workflow

NeuroSight is also designed around the hackathon's phone-first development workflow.

The smartphone remains the actual sensing and inference device while the laptop can be used during development for:

application development
model training
APK generation
debugging
documentation
demonstration preparation

This keeps the phone inside the actual AI loop instead of treating it only as a display.

🛠️ Technology Stack
Android
Kotlin
Jetpack Compose
Android SDK
CameraX
Kotlin Coroutines
Computer Vision / AI
MobileNetV3-Small
TensorFlow Lite
Full-integer UINT8 quantization
On-device inference
Hardware acceleration where supported
Sensory Feedback
VibratorManager
VibrationEffect
AudioTrack
PCM audio
Generated sine-wave cues
Development
Python
TensorFlow / Keras
TensorFlow Lite Converter
GitHub Actions
Android Gradle Plugin
📂 Project Structure
NeuroSight/
│
├── app/
│   └── src/
│       └── main/
│           ├── java/com/neurosight/app/
│           │   ├── MainActivity.kt
│           │   │
│           │   ├── audio/
│           │   │   └── AudioEngine.kt
│           │   │
│           │   ├── camera/
│           │   │   └── CameraController.kt
│           │   │
│           │   ├── haptics/
│           │   │   └── HapticEngine.kt
│           │   │
│           │   ├── ml/
│           │   │   └── NeuroSightClassifier.kt
│           │   │
│           │   └── ...
│           │
│           └── assets/
│               ├── neurosight_encoder.tflite
│               ├── neurosight_labels.txt
│               └── PUT_MODEL_HERE.txt
│
├── training/
│   └── train_neurosight.py
│
├── docs/
│   └── MODEL_TRAINING.md
│
├── assets/
│   ├── neurosight-system-overview.png
│   ├── neurosight-architecture.png
│   ├── neurosight-accuracy.png
│   └── neurosight-dataset-distribution.png
│
└── .github/
    └── workflows/
🔧 Build & Run
Requirements
Android Studio
JDK
Android SDK
Android phone with camera
USB debugging enabled for device testing
Clone
git clone https://github.com/harshtakalkar037-boop/NeuroSight.git
cd NeuroSight
Open

Open the project in Android Studio.

Then:

Connect an Android device.
Enable USB debugging.
Build the application.
Install the debug APK.
Grant camera permission.
Point the camera toward an indoor object.
Observe the classification.
Experience the corresponding audio + haptic feedback.
🧪 Training

The training pipeline is available in:

training/train_neurosight.py

The pipeline performs:

Object Detection Dataset
          ↓
Class Consolidation
          ↓
Object Cropping
          ↓
Train / Validation / Test
          ↓
MobileNetV3-Small
          ↓
Transfer Learning
          ↓
Fine-Tuning
          ↓
TensorFlow Lite Conversion
          ↓
Full Integer UINT8 Model

The resulting model is placed in:

app/src/main/assets/neurosight_encoder.tflite

Class labels are stored in:

app/src/main/assets/neurosight_labels.txt
🔒 Privacy & Offline-First Design

The core recognition pipeline is designed to run directly on the smartphone.

The camera frame is processed locally for inference instead of requiring a remote inference server.

This architecture can reduce:

network dependency
cloud inference latency
recurring backend costs
transmission of camera frames

The project is therefore designed around:

Capture locally → infer locally → respond locally

🌍 Potential Impact

NeuroSight is intended as a prototype exploration of accessible computer vision.

Potential applications include:

👁️ Low-Vision Assistance

Providing an additional non-visual channel for identifying common indoor objects.

🏠 Indoor Navigation Support

Helping users build awareness of nearby objects.

♿ Accessibility Interfaces

Exploring touch and audio as alternative interfaces for computer vision systems.

📱 Smartphone-Based Assistive Technology

Demonstrating that modern phones can perform useful AI inference without requiring specialized external hardware.

🧪 Current Prototype

The current NeuroSight prototype provides:

Real-time camera input
On-device object classification
5 object classes
UINT8 TensorFlow Lite inference
Audio feedback
Haptic feedback
Android UI
Hardware-accelerated inference where supported
~40–50 ms observed inference latency
~30 FPS observed throughput
Current classes
Door
Window
Chair
Table
Cabinet
🚧 Limitations

NeuroSight is a hackathon prototype, not a medical or safety-certified navigation system.

Current limitations include:

Limited number of object categories
Development-scale dataset
Performance can vary with lighting and viewpoint
Occluded objects may be difficult to classify
Similar-looking objects can produce ambiguous predictions
P95 latency has not yet been measured
Accessibility testing with a large user population has not yet been performed
Real-world deployment would require substantially broader validation

The system should therefore be treated as an assistive experimental interface, not a replacement for established mobility or safety tools.

🔮 Roadmap

Future versions could explore:

More Objects

Expand beyond the initial five indoor categories.

Spatial Awareness

Combine object recognition with:

object location
distance estimation
depth sensing
directional audio
spatial vibration
Temporal Intelligence

Track objects across multiple frames rather than treating every frame independently.

Personalised Feedback

Allow users to customise:

vibration patterns
sound frequencies
intensity
timing
sensitivity
More Efficient Edge AI

Explore optimized architectures, delegates, and accelerator-specific execution for lower latency and power consumption.

Accessibility Testing

Conduct structured testing with visually impaired and low-vision users to validate whether the sensory vocabulary is actually learnable and useful.

🏆 Why NeuroSight?

NeuroSight combines several capabilities already present in a modern smartphone:

Camera
   +
On-device AI
   +
Hardware acceleration
   +
Haptics
   +
Audio
   =
Assistive sensory interface

The project is not only about recognizing an object.

It explores a different question:

Can a smartphone translate visual information into a language of touch and sound?

That is the core idea behind NeuroSight.

📈 Development Results

The current development prototype demonstrates:

Area	Result
Model architecture	MobileNetV3-Small
Model format	TensorFlow Lite
Quantization	Full-integer UINT8
Output classes	5
Best validation accuracy	99.59%
Validation accuracy	98.63%
Observed inference latency	~40–50 ms
Observed median latency	~50 ms
Observed throughput	~30 FPS
Audio feedback	✅
Haptic feedback	✅
Android integration	✅
On-device inference	✅
Hardware-accelerated backend	✅ Verified on target device
Cloud inference required	❌
⚠️ Accuracy vs Confidence

The model's accuracy and an individual prediction's confidence score are different measurements.

Accuracy

Measures how often the model predicts correctly across an evaluation set.

NeuroSight development results:

99.59%
98.63%

Confidence

Represents how strongly the model favors a particular class for an individual input.

For example:

Camera Frame
     ↓
Door       0.98
Window     0.01
Chair      0.00
Table      0.00
Cabinet    0.01

Here 0.98 is a prediction confidence score.

It is not the same thing as model accuracy.

👥 Team

NeuroSight — iQOO Hackathon 2026

Built as a phone-first Edge AI and accessibility prototype.

📜 Disclaimer

NeuroSight is an experimental hackathon project intended to demonstrate on-device computer vision and sensory substitution.

It is not a certified medical device, mobility aid, or guaranteed collision-avoidance system.

Users should not rely on the prototype as their sole source of environmental awareness.

❤️ Vision

We believe accessibility should not always require another device.

Sometimes, the technology people already carry can become something more.

NeuroSight explores a future where a smartphone doesn't just show the world — it helps communicate the world through sound and touch.
