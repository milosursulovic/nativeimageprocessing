package com.example.nativeimageprocessing.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "temp_image.png")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.flush()
        }
        return Uri.fromFile(file)
    }
}
