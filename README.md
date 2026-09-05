# NeuroSight — iQOO Hackathon 2026

Offline, on-device navigation-assistance app: the camera classifies each live
frame as **wall**, **door**, or **person**, and the phone responds with a
distinct **vibration pattern** and **tone** for each class — no network calls,
no cloud, no accounts.

## Getting an APK without installing anything (GitHub Actions)

This project ships with `.github/workflows/build-apk.yml`, which builds a
debug APK in the cloud on every push and lets you download it — no local
Android SDK/Gradle/Android Studio required.

1. Create a new GitHub repo and push this project to it:
   ```bash
   cd NeuroSight
   git init
   git add .
   git commit -m "Initial NeuroSight project"
   git branch -M main
   git remote add origin https://github.com/<you>/<repo>.git
   git push -u origin main
   ```
2. (Optional but recommended) Add your trained model at
   `app/src/main/assets/neurosight_encoder.tflite` *before* pushing, so the
   build produces a fully working classifier. If you skip this, the APK
   still builds and installs — the UI, camera preview, and controls all
   work — but classification stays off until you add the model and push
   again (see the "graceful missing-model" note below).
3. On GitHub, open the **Actions** tab → you should see the "Build APK"
   workflow run automatically. Wait for it to go green (a few minutes).
4. Click into the finished run → scroll to **Artifacts** → download
   `neurosight-debug-apk` (a zip containing `app-debug.apk`).
5. Transfer that APK to your Android phone (email, Drive, `adb install
   app-debug.apk`, etc.) and install it. You'll need to allow "install from
   unknown sources" for a debug build since it isn't from the Play Store.

You can also trigger a build manually any time from **Actions → Build APK →
Run workflow**, without needing a new push.

### Missing-model behavior
If `neurosight_encoder.tflite` isn't present, `MainActivity` catches the load
failure and shows `Model: not loaded` in the Demo Mode overlay instead of
crashing — camera preview, haptics/audio wiring, and UI are still fully
demoable; only the classification step is inactive.

### Signed release build (optional)
The workflow includes a commented-out section for producing a signed release
APK via repo secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`). Uncomment it and add those secrets under repo Settings →
Secrets and variables → Actions if you want a release build instead of a
debug one.

## Opening the project

1. Open this folder (`NeuroSight/`) directly in Android Studio (Koala/Ladybug
   or newer recommended).
2. Let Android Studio sync Gradle. If it prompts to regenerate the Gradle
   wrapper jar, accept — the wrapper *scripts* are here but the binary jar
   isn't committed (kept out of this offline handoff); Android Studio will
   fetch/regenerate it automatically on first sync.
3. **Add your trained model** before building a working APK:
   `app/src/main/assets/neurosight_encoder.tflite`
   See `app/src/main/assets/PUT_MODEL_HERE.txt` for the exact input/output
   contract the app expects (1x224x224x3 UINT8 in, 1x3 scores out, ordered
   `wall, door, person`). The project *will not build a working classifier*
   until this file is added — the UI and camera preview will still run.
4. Build & run on a physical device (camera + vibration won't work on most
   emulators). `minSdk 26`.

## Project layout

```
app/src/main/java/com/neurosight/app/
├── MainActivity.kt        # wires everything together, owns lifecycle
├── camera/                # CameraX preview + throttled frame capture
├── ml/                    # NeuroSightClassifier (TFLite + delegate fallback)
├── haptics/                # HapticEngine (VibrationEffect waveforms)
├── audio/                  # AudioEngine (synthesized tones via AudioTrack)
├── ui/                     # Jetpack Compose single-screen UI
└── util/                   # ImageUtils (YUV -> square RGB bitmap)
```

## Key tuning points

| What                          | Where                                                        |
|-------------------------------|---------------------------------------------------------------|
| Camera FPS                    | `CameraController(targetFps = 18)` in `MainActivity.kt`       |
| Model input resolution         | `ImageUtils.MODEL_INPUT_SIZE` (keep in sync with your model)  |
| Haptic patterns per class      | `HapticEngine.patternFor()`                                   |
| Audio tone/pitch per class     | `AudioEngine.specFor()`                                       |
| Confidence threshold for feedback | `MainActivity.onFrameCaptured()` (`confidenceThreshold`)   |
| Quantized vs float model switch | `NeuroSightClassifier.MODEL_IS_INT8_QUANTIZED`               |

## NPU / Hexagon delegate notes

True Hexagon-delegate wiring (`tensorflow-lite-hexagon` AAR + Qualcomm
`libhexagon_nn_skel*.so`) is finicky to set up within a hackathon timebox and
is largely superseded on modern Snapdragon SoCs by **NNAPI**, which Android
itself routes to whatever accelerator (DSP/NPU/GPU) the OEM exposes. This app
tries delegates in this order:

1. **NNAPI** (`EXECUTION_PREFERENCE_SUSTAINED_SPEED`) — typically reaches the
   Hexagon DSP/NPU on Snapdragon/iQOO devices without extra native libs.
2. **GPU delegate** — if NNAPI init fails or isn't supported.
3. **CPU (XNNPACK, multi-threaded)** — always-available fallback.

The active backend is shown in the "Demo Mode" debug overlay so you can
confirm which path is live during the demo. See the header comment in
`ml/NeuroSightClassifier.kt` for a TODO on wiring a true Hexagon/QNN delegate
if you have access to the vendor libraries.

## Offline guarantee

No `INTERNET` permission is requested, no analytics/crash-reporting SDKs are
included, and all inference runs locally via TFLite. See the comment block in
`AndroidManifest.xml`.
