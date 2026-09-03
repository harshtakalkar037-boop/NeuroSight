package com.neurosight.app.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.neurosight.app.util.ImageUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Wraps CameraX setup: a [Preview] use case bound to a [PreviewView] for the
 * on-screen camera feed, plus an [ImageAnalysis] use case that delivers
 * throttled, model-ready [Bitmap] frames to [onFrame].
 *
 * FPS / throttling:
 *   The camera sensor typically streams at 30 FPS. Running ML inference on
 *   every single frame is unnecessary and wastes battery, so we throttle to
 *   a target interval (see [minFrameIntervalMs]). Change [targetFps] below to
 *   tune this -- 15-20 FPS is a good default for a responsive but efficient
 *   demo. Frames arriving faster than the interval are dropped (not queued).
 */
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    /** Target frames-per-second delivered to [onFrame]. Tune here. */
    private val targetFps: Int = 18,
    /** Called on a background thread with a 224x224 RGB bitmap ready for inference. */
    private val onFrame: (Bitmap) -> Unit
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val minFrameIntervalMs: Long = 1000L / targetFps
    private var lastFrameTimestampMs = 0L

    /**
     * Binds camera Preview + ImageAnalysis to the given [previewView] and
     * [lifecycleOwner]. Safe to call once the activity/composable is in a
     * state to receive camera frames (i.e. after permission is granted).
     */
    fun start(previewView: PreviewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            // ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST automatically drops
            // frames that arrive while we're still processing the previous
            // one, which is exactly the backpressure behavior we want for a
            // real-time classification pipeline.
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                try {
                    val now = System.currentTimeMillis()
                    if (now - lastFrameTimestampMs < minFrameIntervalMs) {
                        // Drop this frame; we're ahead of our target FPS budget.
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    lastFrameTimestampMs = now

                    val bitmap = ImageUtils.imageProxyToBitmap(imageProxy)
                    if (bitmap != null) {
                        onFrame(bitmap)
                    }
                } finally {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                // On a hackathon device, binding can fail if e.g. the front
                // camera is the only one available, or the lifecycle is in a
                // bad state. Fail loudly in Logcat rather than silently.
                e.printStackTrace()
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
    }

    /** Unbinds all camera use cases. Call from onPause/onStop or when stopping the pipeline. */
    fun stop() {
        cameraProvider?.unbindAll()
    }

    /** Fully releases the analysis executor. Call from onDestroy. */
    fun shutdown() {
        stop()
        analysisExecutor.shutdown()
    }
}
