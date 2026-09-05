package com.neurosight.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.neurosight.app.audio.AudioEngine
import com.neurosight.app.camera.CameraController
import com.neurosight.app.haptics.HapticEngine
import com.neurosight.app.ml.NeuroSightClassifier
import com.neurosight.app.ui.MainScreen
import com.neurosight.app.ui.NeuroSightUiState
import com.neurosight.app.ui.theme.NeuroSightTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Single-screen entry point. Owns the lifecycle of every module:
 *   - CameraController (camera package): live frames.
 *   - NeuroSightClassifier (ml package): TFLite inference.
 *   - HapticEngine (haptics package): vibration feedback.
 *   - AudioEngine (audio package): tone feedback.
 *
 * The pipeline (camera -> ML -> haptics/audio) only runs while the user has
 * pressed "Start" AND the activity is in the foreground; it's fully torn
 * down on stop/pause to save battery and release the camera for other apps.
 */
class MainActivity : ComponentActivity() {

    private lateinit var classifier: NeuroSightClassifier
    private lateinit var hapticEngine: HapticEngine
    private lateinit var audioEngine: AudioEngine
    private var cameraController: CameraController? = null
    private var previewView: PreviewView? = null

    // Background scope for running inference off the camera analyzer thread's
    // caller and off the main thread. Cancelled in onDestroy.
    private val mlScope = CoroutineScope(Dispatchers.Default)

    // ---- Compose-observable UI state ----
    private var isRunning by mutableStateOf(false)
    private var currentClass by mutableStateOf("-")
    private var confidence by mutableStateOf(0f)
    private var demoModeEnabled by mutableStateOf(false)
    private var activeBackendName by mutableStateOf("-")
    private var hasCameraPermission by mutableStateOf(false)
    private var modelLoaded by mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCameraPermission = granted
            if (granted) {
                previewView?.let { attachCamera(it) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        classifier = NeuroSightClassifier(applicationContext)
        hapticEngine = HapticEngine(applicationContext)
        audioEngine = AudioEngine()

        // Model loading + delegate init can take a moment; do it off the main thread.
        // Wrapped in try/catch so a missing/invalid neurosight_encoder.tflite
        // (e.g. a CI build made before the real model is added) surfaces as a
        // visible "Model: not loaded" state instead of crashing the app.
        mlScope.launch {
            try {
                classifier.initialize()
                activeBackendName = classifier.activeBackendName()
                modelLoaded = true
            } catch (e: Exception) {
                activeBackendName = "not loaded (add neurosight_encoder.tflite to assets/)"
                modelLoaded = false
            }
        }

        hasCameraPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            NeuroSightTheme {
                MainScreen(
                    uiState = NeuroSightUiState(
                        isRunning = isRunning,
                        currentClass = currentClass,
                        confidence = confidence,
                        demoModeEnabled = demoModeEnabled,
                        activeBackendName = activeBackendName,
                        hasCameraPermission = hasCameraPermission
                    ),
                    onPreviewViewCreated = { pv ->
                        previewView = pv
                        if (hasCameraPermission) attachCamera(pv)
                    },
                    onToggleRunning = { toggleRunning() },
                    onToggleDemoMode = { demoModeEnabled = it },
                    onRequestPermission = {
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }
    }

    /** Binds the CameraX pipeline to the given [PreviewView] once permission is granted. */
    private fun attachCamera(previewView: PreviewView) {
        if (cameraController == null) {
            cameraController = CameraController(
                context = applicationContext,
                lifecycleOwner = this,
                targetFps = 18, // TODO: tune FPS here (15-20 recommended).
                onFrame = { bitmap -> onFrameCaptured(bitmap) }
            )
        }
        // Camera only actually starts streaming frames once isRunning is true;
        // see toggleRunning(). Binding here just prepares the preview surface.
        if (isRunning) {
            cameraController?.start(previewView)
        }
    }

    /** Starts or stops the full pipeline: camera streaming + inference + feedback. */
    private fun toggleRunning() {
        isRunning = !isRunning
        if (isRunning) {
            previewView?.let { cameraController?.start(it) }
        } else {
            cameraController?.stop()
            hapticEngine.stop()
            audioEngine.stop()
            currentClass = "-"
            confidence = 0f
        }
    }

    /**
     * Called on a background thread (the camera analyzer executor) with a
     * ready-to-classify 224x224 bitmap. Runs inference, then drives haptics
     * + audio based on the predicted class.
     */
    private fun onFrameCaptured(bitmap: Bitmap) {
        if (!isRunning || !modelLoaded) return

        val result = try {
            classifier.classify(bitmap)
        } catch (e: Exception) {
            // Model not loaded yet, or a transient inference error -- skip this frame.
            return
        }

        runOnUiThread {
            currentClass = result.className
            confidence = result.confidence
        }

        // Only trigger feedback for confident predictions to avoid flickering
        // between classes on noisy/ambiguous frames.
        val confidenceThreshold = 0.6f
        if (result.confidence >= confidenceThreshold) {
            hapticEngine.startForClass(result.className)
            audioEngine.startForClass(result.className)
        } else {
            hapticEngine.stop()
            audioEngine.stop()
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop the camera + feedback when the app leaves the foreground, but
        // keep isRunning's logical state so the UI reflects the user's intent
        // and camera resources are released for other apps.
        cameraController?.stop()
        hapticEngine.stop()
        audioEngine.stop()
    }

    override fun onResume() {
        super.onResume()
        if (isRunning) {
            previewView?.let { cameraController?.start(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraController?.shutdown()
        hapticEngine.stop()
        audioEngine.stop()
        classifier.close()
    }
}
