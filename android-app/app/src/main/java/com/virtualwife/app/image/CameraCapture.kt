package com.virtualwife.app.image

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class CameraCapture(private val context: Context) {

    private var currentImageUri: Uri? = null

    fun createImageFile(): Pair<File, Uri> {
        val imageDir = File(context.cacheDir, "images")
        imageDir.mkdirs()
        val imageFile = File(imageDir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        currentImageUri = uri
        return imageFile to uri
    }

    fun getCurrentUri(): Uri? = currentImageUri

    fun clearCache() {
        val imageDir = File(context.cacheDir, "images")
        imageDir.listFiles()?.forEach { it.delete() }
    }
}
