package com.example.nativeimageprocessing.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.nativeimageprocessing.Constants
import com.example.nativeimageprocessing.R
import com.example.nativeimageprocessing.databinding.ActivityBlurBinding
import java.io.File
import java.io.FileOutputStream

class BlurActivity : AppCompatActivity() {
    companion object {
        init {
            System.loadLibrary("nativeimageprocessing")
        }
    }

    private external fun blur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray

    private lateinit var bitmap: Bitmap
    private lateinit var tempBitmap: Bitmap
    private lateinit var binding: ActivityBlurBinding
    private lateinit var imageUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBlurBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriString = intent.getStringExtra(Constants.EXTRA_IMAGE_URI)
        if (uriString == null) {
            finish()
            return
        }

        imageUri = uriString.toUri()
        bitmap = loadBitmapFromUri(imageUri)
        tempBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        binding.imageView.setImageBitmap(tempBitmap)

        binding.seekBar.max = 25
        binding.seekBar.progress = 0

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val radius = progress
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                val newPixels = blur(pixels, width, height, radius)
                tempBitmap.setPixels(newPixels, 0, width, 0, 0, width, height)
                binding.imageView.setImageBitmap(tempBitmap)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnSave.setOnClickListener {
            val resultUri = saveBitmapToCache(tempBitmap)
            val resultIntent = intent.apply {
                putExtra(Constants.RESULT_IMAGE_URI, resultUri.toString())
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        binding.btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val stream = contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(stream)
            ?: error(getString(R.string.failed_to_decode_image))
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val cachePath = File(cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "blurred_image.png")
        FileOutputStream(file).use { outStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        }
        return Uri.fromFile(file)
    }
}