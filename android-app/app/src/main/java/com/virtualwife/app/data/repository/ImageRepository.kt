package com.virtualwife.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.virtualwife.app.util.Constants
import java.io.ByteArrayOutputStream

class ImageRepository(private val context: Context) {

    fun uriToBase64(uri: Uri): Result<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("无法读取图片"))

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val resizedBitmap = resizeBitmap(originalBitmap)
            val compressedBytes = compressToJpeg(resizedBitmap)
            val base64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)

            if (originalBitmap != resizedBitmap) {
                resizedBitmap.recycle()
            }
            originalBitmap.recycle()

            Result.success(base64)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): Result<String> {
        return try {
            val resizedBitmap = resizeBitmap(bitmap)
            val compressedBytes = compressToJpeg(resizedBitmap)
            val base64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)

            if (bitmap != resizedBitmap) {
                resizedBitmap.recycle()
            }

            Result.success(base64)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val maxWidth = Constants.IMAGE_MAX_WIDTH
        val maxHeight = Constants.IMAGE_MAX_HEIGHT
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun compressToJpeg(bitmap: Bitmap): ByteArray {
        var quality = Constants.IMAGE_QUALITY
        val maxSize = Constants.IMAGE_MAX_SIZE_KB * 1024
        var output: ByteArray

        do {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            output = stream.toByteArray()
            quality -= 10
        } while (output.size > maxSize && quality > 10)

        return output
    }
}
