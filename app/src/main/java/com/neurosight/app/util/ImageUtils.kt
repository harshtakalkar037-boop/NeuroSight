package com.neurosight.app.util

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Small helper object for turning a CameraX [ImageProxy] (YUV_420_888) into an
 * RGB [Bitmap] that's cropped/resized to the square input the TFLite model expects.
 *
 * NOTE: This does a JPEG round-trip (YUV -> JPEG -> Bitmap) for simplicity and
 * broad-device compatibility. It's not the fastest possible path (a hand-rolled
 * YUV->RGB conversion would be faster) but it's simple, correct, and fast enough
 * for the 15-20 FPS target described in the spec. If you need more headroom,
 * replace [imageProxyToBitmap] with a RenderScript-free manual YUV_420_888 ->
 * ARGB_8888 conversion.
 */
object ImageUtils {

    /**
     * The model input size. Change this in ONE place if you retrain with a
     * different resolution -- both the camera pipeline and the classifier
     * read this constant.
     */
    const val MODEL_INPUT_SIZE = 224

    /** Converts a YUV_420_888 [ImageProxy] frame into an ARGB_8888 [Bitmap]. */
    fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        if (image.format != ImageFormat.YUV_420_888) return null

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
        val jpegBytes = out.toByteArray()
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            ?: return null

        return rotateAndCropToSquare(bitmap, image.imageInfo.rotationDegrees)
    }

    /**
     * Rotates the bitmap to account for sensor orientation, then center-crops
     * to a square and scales to [MODEL_INPUT_SIZE] x [MODEL_INPUT_SIZE].
     */
    fun rotateAndCropToSquare(source: Bitmap, rotationDegrees: Int): Bitmap {
        val rotated = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        val size = minOf(rotated.width, rotated.height)
        val xOffset = (rotated.width - size) / 2
        val yOffset = (rotated.height - size) / 2
        val cropped = Bitmap.createBitmap(rotated, xOffset, yOffset, size, size)

        return if (cropped.width != MODEL_INPUT_SIZE || cropped.height != MODEL_INPUT_SIZE) {
            Bitmap.createScaledBitmap(cropped, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true)
        } else {
            cropped
        }
    }
}
