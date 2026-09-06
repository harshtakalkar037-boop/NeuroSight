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

data class ClassificationResult(val className: String, val confidence: Float)

/**
 * On-device 5-class classifier.
 *
 * Model contract:
 *   UINT8 224x224x3 input -> UINT8 1x5 output
 *   labels: door, window, chair, table, cabinet
 *
 * The model is quantized end-to-end. Raw camera pixels are passed directly;
 * the trained model contains its own input rescaling.
 */
class NeuroSightClassifier(private val context: Context) {

    companion object {
        private const val TAG = "NeuroSightClassifier"
        private const val MODEL_FILE = "neurosight_encoder.tflite"
        private const val INPUT_SIZE = 224
        private val LABELS = listOf("door", "window", "chair", "table", "cabinet")
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
            builder.add(NormalizeOp(0f, 255f))
        }
        builder.build()
    }

    fun initialize() {
        val modelBuffer = loadModelFile()

        try {
            val nnApiOptions = NnApiDelegate.Options().apply {
                setExecutionPreference(NnApiDelegate.Options.EXECUTION_PREFERENCE_SUSTAINED_SPEED)
            }
            val delegate = NnApiDelegate(nnApiOptions)
            interpreter = Interpreter(modelBuffer, Interpreter.Options().addDelegate(delegate))
            nnApiDelegate = delegate
            activeBackend = "NNAPI (device-routed)"
            Log.i(TAG, "Initialized TFLite interpreter with NNAPI delegate.")
            return
        } catch (e: Exception) {
            Log.w(TAG, "NNAPI delegate init failed, falling back to GPU.", e)
            nnApiDelegate?.close()
            nnApiDelegate = null
        }

        try {
            val compatList = CompatibilityList()
            if (compatList.isDelegateSupportedOnThisDevice) {
                val delegate = GpuDelegate()
                interpreter = Interpreter(modelBuffer, Interpreter.Options().addDelegate(delegate))
                gpuDelegate = delegate
                activeBackend = "GPU"
                Log.i(TAG, "Initialized TFLite interpreter with GPU delegate.")
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "GPU delegate init failed, falling back to CPU.", e)
            gpuDelegate?.close()
            gpuDelegate = null
        }

        val cpuOptions = Interpreter.Options().apply {
            setNumThreads(Runtime.getRuntime().availableProcessors().coerceAtMost(4))
            setUseXNNPACK(true)
        }
        interpreter = Interpreter(modelBuffer, cpuOptions)
        activeBackend = "CPU (XNNPACK)"
        Log.i(TAG, "Initialized TFLite interpreter with CPU backend.")
    }

    fun activeBackendName(): String = activeBackend

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
            val quantParams = interp.getOutputTensor(0).quantizationParams()
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

        return ClassificationResult(
            className = LABELS.getOrElse(bestIdx) { "unknown" },
            confidence = bestScore
        )
    }

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
            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength
            )
        }
    }
}
