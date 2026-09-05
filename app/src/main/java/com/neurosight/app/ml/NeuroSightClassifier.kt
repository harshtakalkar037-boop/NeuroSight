package com.neurosight.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/** Result of a single classification pass. */
data class ClassificationResult(val className: String, val confidence: Float)

/**
 * On-device classifier that loads `neurosight_encoder.tflite` from assets and
 * runs INT8-quantized inference, preferring the Hexagon/NPU-capable delegate
 * path where available and falling back gracefully otherwise.
 *
 * ---- WHERE TO PUT THE MODEL ----
 * Place your trained, INT8-quantized TFLite model at:
 *     app/src/main/assets/neurosight_encoder.tflite
 * (see PUT_MODEL_HERE.txt in that folder for the exact I/O contract.)
 *
 * ---- DELEGATE / NPU NOTES (read before the demo!) ----
 * True "Hexagon delegate" (org.tensorflow:tensorflow-lite-hexagon) requires:
 *   1. Adding the `tensorflow-lite-hexagon` AAR dependency (commented out in
 *      app/build.gradle.kts) alongside the Qualcomm Hexagon NN libraries
 *      (libhexagon_nn_skel*.so) that must be present on-device or bundled in
 *      jniLibs. This is finicky to set up in a hackathon timebox and is
 *      largely superseded on modern Snapdragon SoCs by NNAPI or Qualcomm's
 *      QNN delegate.
 *   2. On Android 8.1+ (minSdk 26 here), NNAPI (android.nnapi) is the
 *      standard, always-available path that Android will itself route to
 *      whatever accelerator (DSP/NPU/GPU) the OEM has exposed -- on iQOO /
 *      Snapdragon devices this commonly includes the Hexagon DSP. That's why
 *      this class tries NNAPI FIRST as the practical "NPU" path.
 *   3. TODO(dev): If you have access to Qualcomm's QNN TFLite delegate for
 *      the specific iQOO chipset, initialize it here before the NNAPI
 *      attempt for the most direct Hexagon NPU access.
 *
 * Fallback order implemented below: NNAPI -> GPU -> CPU (XNNPACK, multi-thread).
 */
class NeuroSightClassifier(private val context: Context) {

    companion object {
        private const val TAG = "NeuroSightClassifier"
        private const val MODEL_FILE = "neurosight_encoder.tflite"
        private const val INPUT_SIZE = 224

        // Must match the label order your model was trained/exported with.
        private val LABELS = listOf("wall", "door", "person")

        // TODO(dev): Set this to false if you export a FLOAT32 model instead
        // of an INT8-quantized one. When true, we treat the model as
        // fully-quantized (UINT8 in, UINT8 out) and skip the normalization
        // step; when false we normalize pixels to [0,1] float before inference.
        private const val MODEL_IS_INT8_QUANTIZED = true
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var nnApiDelegate: NnApiDelegate? = null
    private var activeBackend: String = "uninitialized"

    private val imageProcessor: ImageProcessor by lazy {
        val builder = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        if (!MODEL_IS_INT8_QUANTIZED) {
            // Normalize 0-255 -> 0-1 for float models. Quantized (uint8) models
            // consume raw 0-255 pixel values directly, so no normalization there.
            builder.add(NormalizeOp(0f, 255f))
        }
        builder.build()
    }

    /** Loads the model and initializes the best available delegate. Call once, e.g. in onCreate. */
    fun initialize() {
        val modelBuffer = loadModelFile()

        // --- 1) Try NNAPI first (routes to DSP/NPU/GPU per-device at the OS level). ---
        try {
            val nnApiOptions = NnApiDelegate.Options().apply {
                // Prefer sustained throughput over single-shot latency for a
                // continuous live-camera pipeline.
                setExecutionPreference(NnApiDelegate.Options.EXECUTION_PREFERENCE_SUSTAINED_SPEED)
            }
            val delegate = NnApiDelegate(nnApiOptions)
            val options = Interpreter.Options().addDelegate(delegate)
            interpreter = Interpreter(modelBuffer, options)
            nnApiDelegate = delegate
            activeBackend = "NNAPI (device-routed, may include Hexagon DSP/NPU)"
            Log.i(TAG, "Initialized TFLite interpreter with NNAPI delegate.")
            return
        } catch (e: Exception) {
            Log.w(TAG, "NNAPI delegate init failed, falling back to GPU.", e)
            nnApiDelegate?.close()
            nnApiDelegate = null
        }

        // --- 2) Try GPU delegate. ---
        try {
            val compatList = CompatibilityList()
            if (compatList.isDelegateSupportedOnThisDevice) {
                // NOTE: We intentionally use GpuDelegate() with default options
                // instead of CompatibilityList.bestOptionsForThisDevice /
                // GpuDelegate.Options. With the tflite:2.16.1 + tflite-gpu:2.16.1
                // combination on the classpath, GpuDelegate.Options' supertype
                // (GpuDelegateFactory.Options) cannot be resolved, which fails
                // compilation. Default options are plenty for this demo.
                val delegate = GpuDelegate()
                val options = Interpreter.Options().addDelegate(delegate)
                interpreter = Interpreter(modelBuffer, options)
                gpuDelegate = delegate
                activeBackend = "GPU"
                Log.i(TAG, "Initialized TFLite interpreter with GPU delegate.")
                return
            } else {
                Log.w(TAG, "GPU delegate not supported on this device, falling back to CPU.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "GPU delegate init failed, falling back to CPU.", e)
            gpuDelegate?.close()
            gpuDelegate = null
        }

        // --- 3) CPU fallback (always works). XNNPACK + multi-threading for speed. ---
        val cpuOptions = Interpreter.Options().apply {
            setNumThreads(Runtime.getRuntime().availableProcessors().coerceAtMost(4))
            setUseXNNPACK(true)
        }
        interpreter = Interpreter(modelBuffer, cpuOptions)
        activeBackend = "CPU (XNNPACK)"
        Log.i(TAG, "Initialized TFLite interpreter with CPU backend.")
    }

    /** Human-readable name of whichever backend ended up active, useful for demo debug overlay. */
    fun activeBackendName(): String = activeBackend

    /**
     * Runs one classification pass on [bitmap] (expected ~224x224, but the
     * internal [ImageProcessor] will resize if needed) and returns the
     * top predicted class + confidence.
     *
     * Reuses the interpreter and pre-allocated output buffer across calls --
     * no per-frame interpreter creation or large allocations, so this is
     * safe to call at 15-20 FPS from the camera analyzer thread.
     */
    fun classify(bitmap: Bitmap): ClassificationResult {
        val interp = interpreter
            ?: throw IllegalStateException("NeuroSightClassifier.initialize() must be called before classify().")

        var tensorImage = TensorImage(if (MODEL_IS_INT8_QUANTIZED) DataType.UINT8 else DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        val outputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, LABELS.size),
            if (MODEL_IS_INT8_QUANTIZED) DataType.UINT8 else DataType.FLOAT32
        )

        interp.run(tensorImage.buffer, outputBuffer.buffer.rewind())

        val scores: FloatArray = if (MODEL_IS_INT8_QUANTIZED) {
            // Dequantize using the output tensor's quantization params.
            val outTensor = interp.getOutputTensor(0)
            val quantParams = outTensor.quantizationParams()
            val scale = quantParams.scale
            val zeroPoint = quantParams.zeroPoint
            val rawBytes = outputBuffer.buffer
            rawBytes.rewind()
            FloatArray(LABELS.size) { i ->
                val raw = rawBytes.get(i).toInt() and 0xFF
                (raw - zeroPoint) * scale
            }
        } else {
            outputBuffer.floatArray
        }

        var bestIdx = 0
        var bestScore = scores.getOrElse(0) { 0f }
        for (i in scores.indices) {
            if (scores[i] > bestScore) {
                bestScore = scores[i]
                bestIdx = i
            }
        }

        val className = LABELS.getOrElse(bestIdx) { "unknown" }
        return ClassificationResult(className = className, confidence = bestScore)
    }

    /** Releases the interpreter and any active delegates. Call from onDestroy. */
    fun close() {
        interpreter?.close()
        interpreter = null
        gpuDelegate?.close()
        gpuDelegate = null
        nnApiDelegate?.close()
        nnApiDelegate = null
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        FileInputStream(assetFileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }
}
