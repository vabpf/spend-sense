package com.spendsense.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageCropUtil {

    /**
     * Decodes a Bitmap from a content Uri, automatically correcting for EXIF rotation.
     * Downsamples if image dimensions exceed maxDimension to prevent OutOfMemory errors.
     */
    fun decodeBitmapWithExif(context: Context, uri: Uri, maxDimension: Int = 2400): Bitmap? {
        val inputStream: InputStream = try {
            context.contentResolver.openInputStream(uri) ?: return null
        } catch (_: Exception) {
            return null
        }

        val orientation = try {
            val exif = ExifInterface(inputStream)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        } finally {
            try { inputStream.close() } catch (_: Exception) {}
        }

        // Measure bounds
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
        } catch (_: Exception) {
            return null
        }

        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight
        if (origWidth <= 0 || origHeight <= 0) return null

        var sampleSize = 1
        val maxSide = max(origWidth, origHeight)
        while (maxSide / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val rawBitmap = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (_: Exception) {
            null
        } ?: return null

        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        return if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (rotated != rawBitmap) {
                rawBitmap.recycle()
            }
            rotated
        } else {
            rawBitmap
        }
    }

    /**
     * Decodes a local file path, ensuring EXIF orientation is respected.
     */
    fun decodeFileWithExif(filePath: String, maxDimension: Int = 2400): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null

        val orientation = try {
            val exif = ExifInterface(filePath)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, boundsOptions)
        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight
        if (origWidth <= 0 || origHeight <= 0) return null

        var sampleSize = 1
        val maxSide = max(origWidth, origHeight)
        while (maxSide / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val rawBitmap = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return null

        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        return if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (rotated != rawBitmap) {
                rawBitmap.recycle()
            }
            rotated
        } else {
            rawBitmap
        }
    }

    /**
     * Crops the bitmap according to normalized crop coordinates (0f..1f),
     * applies additional manual rotation, and writes the JPEG to destFile.
     */
    fun cropAndSave(
        source: Bitmap,
        cropRectNorm: RectF,
        additionalRotation: Float,
        destFile: File
    ): Boolean {
        return try {
            var workingBitmap = source
            val normRotation = ((additionalRotation % 360f) + 360f) % 360f
            if (normRotation != 0f) {
                val matrix = Matrix().apply { postRotate(normRotation) }
                workingBitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            }

            val x = (cropRectNorm.left * workingBitmap.width).toInt().coerceIn(0, workingBitmap.width - 1)
            val y = (cropRectNorm.top * workingBitmap.height).toInt().coerceIn(0, workingBitmap.height - 1)
            val w = (cropRectNorm.width() * workingBitmap.width).toInt().coerceIn(1, workingBitmap.width - x)
            val h = (cropRectNorm.height() * workingBitmap.height).toInt().coerceIn(1, workingBitmap.height - y)

            val cropped = Bitmap.createBitmap(workingBitmap, x, y, w, h)
            if (workingBitmap != source && workingBitmap != cropped) {
                workingBitmap.recycle()
            }

            FileOutputStream(destFile).use { out ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            if (cropped != source) {
                cropped.recycle()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
