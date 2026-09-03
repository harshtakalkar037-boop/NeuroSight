package com.example.neurosight.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

/**
 * NeuroSightClassifier
 *
 * - Loads TFLite model from assets: "neurosight_encoder.tflite"
 * - Input: 224x224 RGB Bitmap
 * - Output: ClassificationResult(className, confidence)
 * - Classes: ["wall", "door", "person"]
 *
 * If the model file is missing, it will not crash; it will return
 * className = "UNKNOWN", confidence = 0f and log a warning.
 */
class NeuroSightClassifier(context: Context) {

    private val interpreter: Interpreter?
    private val labels = listOf("wall", "door", "person")

    data class ClassificationResult(
        val className: String,
        val confidence: Float
    )

    init {
        interpreter = try {
            val modelBuffer = loadModelFile(context, "neurosight_encoder.tflite")

            // Try NPU / NNAPI first, then GPU, then CPU
            val options = Interpreter.Options().apply {
                setNumThreads(2)
                // NNAPI (often uses NPU/DSP on Qualcomm)
                try {
                    val nnApiDelegate = NnApiDelegate()
                    addDelegate(nnApiDelegate)
                    Log.i(TAG, "Using NNAPI delegate (possible NPU/DSP).")
                } catch (e: Exception) {
                    Log.w(TAG, "NNAPI delegate not available: ${e.message}")
                }

                // Fallback GPU delegate
                try {
                    val gpuDelegate = GpuDelegate()
                    addDelegate(gpuDelegate)
                    Log.i(TAG, "Using GPU delegate.")
                } catch (e: Exception) {
                    Log.w(TAG, "GPU delegate not available: ${e.message}")
                }
            }

            Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TFLite model: ${e.message}")
            null
        }
    }

    /**
     * Classify a 224x224 RGB bitmap.
     * If interpreter is null (model missing), returns UNKNOWN with 0 confidence.
     */
    fun classify(bitmap: Bitmap): ClassificationResult {
        if (interpreter == null) {
            Log.w(TAG, "TFLite interpreter not initialized (model missing?). Returning UNKNOWN.")
            return ClassificationResult("UNKNOWN", 0f)
        }

        require(bitmap.width == 224 && bitmap.height == 224) {
            "Bitmap must be 224x224, got ${bitmap.width}x${bitmap.height}"
        }

        val inputBuffer = bitmapToByteBuffer(bitmap)

        // Output: [1, 3] logits or probabilities
        val output = Array(1) { FloatArray(labels.size) }

        interpreter.run(inputBuffer, output)

        val logits = output[0]

        // Softmax to get probabilities (if model outputs logits)
        val probs = softmax(logits)

        val maxIndex = probs.indices.maxByOrNull { probs[it] } ?: 0
        val className = labels.getOrElse(maxIndex) { "UNKNOWN" }
        val confidence = probs.getOrElse(maxIndex) { 0f }

        Log.d(
            TAG,
            "Classification: wall=${probs.getOrNull(0) ?: 0f}, " +
                "door=${probs.getOrNull(1) ?: 0f}, " +
                "person=${probs.getOrNull(2) ?: 0f} -> $className ($confidence)"
        )

        return ClassificationResult(className, confidence)
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3) // float32
        buffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(224 * 224)
        bitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)

        for (pixel in intValues) {
            // Extract RGB
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f

            buffer.putFloat(r)
            buffer.putFloat(g)
            buffer.putFloat(b)
        }

        buffer.rewind()
        return buffer
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val exps = logits.map { exp(it - max) }
        val sum = exps.sum()
        return exps.map { it / sum }.toFloatArray()
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val assetManager = context.assets
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    companion object {
        private const val TAG = "NeuroSightClassifier"
    }
}
