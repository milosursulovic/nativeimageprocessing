package com.example.nativeimageprocessing.utils

import android.content.Context
import java.io.File

object ImageUtils {
    fun createImageFile(context: Context): File {
        val storageDir = context.cacheDir
        return File.createTempFile("photo_", ".jpg", storageDir)
    }
}
