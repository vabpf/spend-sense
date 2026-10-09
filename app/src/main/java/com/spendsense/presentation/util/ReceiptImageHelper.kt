package com.spendsense.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ReceiptImageHelper {

    fun processAndEncodeImage(context: Context, uri: Uri, maxDimension: Int = 1600): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val rawBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (rawBitmap == null) return null

            // Read EXIF orientation
            val rotatedBitmap = try {
                context.contentResolver.openInputStream(uri)?.use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(rawBitmap, 90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(rawBitmap, 180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(rawBitmap, 270f)
                        else -> rawBitmap
                    }
                } ?: rawBitmap
            } catch (_: Exception) {
                rawBitmap
            }

            // Downscale if larger than maxDimension
            val scaledBitmap = if (rotatedBitmap.width > maxDimension || rotatedBitmap.height > maxDimension) {
                val ratio = minOf(
                    maxDimension.toFloat() / rotatedBitmap.width,
                    maxDimension.toFloat() / rotatedBitmap.height
                )
                val newW = (rotatedBitmap.width * ratio).toInt().coerceAtLeast(1)
                val newH = (rotatedBitmap.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(rotatedBitmap, newW, newH, true)
            } else {
                rotatedBitmap
            }

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
