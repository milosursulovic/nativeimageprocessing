package com.example.nativeimageprocessing.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nativeimageprocessing.FilterOption
import com.example.nativeimageprocessing.adapters.FilterOptionsAdapter
import com.example.nativeimageprocessing.databinding.ActivityEditBinding

class EditActivity : AppCompatActivity() {

    companion object {
        init {
            System.loadLibrary("nativeimageprocessing")
        }
    }

    private external fun convertToGrayscale(pixels: IntArray, width: Int, height: Int): IntArray
    private external fun invertColors(pixels: IntArray, width: Int, height: Int): IntArray
    private external fun sepia(pixels: IntArray, width: Int, height: Int): IntArray
    private external fun brightness(
        pixels: IntArray,
        width: Int,
        height: Int,
        brightnessValue: Int
    ): IntArray
    private external fun contrast(
        pixels: IntArray,
        width: Int,
        height: Int,
        contrastValue: Int
    ): IntArray


    private lateinit var binding: ActivityEditBinding
    private lateinit var originalBitmap: Bitmap
    private lateinit var currentBitmap: Bitmap

    private lateinit var tempBitmap: Bitmap

    private val filterOptions = listOf(
        FilterOption("grayscale", "Grayscale"),
        FilterOption("invert", "Invert Colors"),
        FilterOption("sepia", "Sepia Tone"),
        FilterOption("brightness", "Brightness"),
        FilterOption("contrast", "Contrast"),
        FilterOption("blur", "Gaussian Blur"),
        FilterOption("edge", "Edge Detection"),
        FilterOption("rotate90", "Rotate 90°"),
        FilterOption("rotate180", "Rotate 180°"),
        FilterOption("rotate270", "Rotate 270°"),
        FilterOption("crop", "Crop")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra("imageUri")
        if (imageUriString == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imageUri = imageUriString.toUri()
        originalBitmap = loadBitmapFromUri(imageUri)
        currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        binding.imageView.setImageBitmap(currentBitmap)

        // Setup RecyclerView
        binding.optionsRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        binding.optionsRecyclerView.adapter = FilterOptionsAdapter(filterOptions) { option ->
            applyFilter(option.id)
        }

        binding.btnSave.setOnClickListener {
            currentBitmap = tempBitmap.copy(Bitmap.Config.ARGB_8888, true)
            binding.imageView.setImageBitmap(currentBitmap)
            resetUIAfterFilterApplied()
        }

        binding.btnCancel.setOnClickListener {
            binding.imageView.setImageBitmap(currentBitmap) // revert to currentBitmap (unchanged)
            resetUIAfterFilterApplied()
        }


        binding.btnExport.setOnClickListener {
            exportImageToGallery(currentBitmap)
        }
    }

    private fun resetUIAfterFilterApplied() {
        binding.actionButtonsLayout.visibility = android.view.View.GONE
        binding.brightnessSeekBar.visibility =
            android.view.View.GONE // Hide the brightness slider
        binding.brightnessSeekBar.setOnSeekBarChangeListener(null) // Remove listener
        binding.brightnessSeekBar.progress = 100 // Reset brightness to default
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val stream = contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(stream) ?: error("Failed to decode image")
    }

    private fun applyFilter(filterId: String) {
        val width = currentBitmap.width
        val height = currentBitmap.height
        val pixels = IntArray(width * height)
        currentBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        binding.brightnessSeekBar.visibility =
            if (filterId == "brightness") android.view.View.VISIBLE else android.view.View.GONE

        if (filterId == "brightness") {
            // Initially apply brightness at 100 (normal)
            val brightnessValue = binding.brightnessSeekBar.progress
            val newPixels = brightness(pixels, width, height, brightnessValue)
            tempBitmap = createBitmap(width, height)
            tempBitmap.setPixels(newPixels, 0, width, 0, 0, width, height)
            binding.imageView.setImageBitmap(tempBitmap)
            binding.actionButtonsLayout.visibility = android.view.View.VISIBLE

            // Add listener for SeekBar changes
            binding.brightnessSeekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val updatedPixels = brightness(pixels, width, height, progress)
                    tempBitmap.setPixels(updatedPixels, 0, width, 0, 0, width, height)
                    binding.imageView.setImageBitmap(tempBitmap)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        } else {
            // Remove listener if previously brightness
            binding.brightnessSeekBar.setOnSeekBarChangeListener(null)

            val newPixels = when (filterId) {
                "grayscale" -> convertToGrayscale(pixels, width, height)
                "invert" -> invertColors(pixels, width, height)
                "sepia" -> sepia(pixels, width, height)
                // other filters ...
                else -> {
                    Toast.makeText(this, "Filter not implemented: $filterId", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
            }

            tempBitmap = createBitmap(width, height)
            tempBitmap.setPixels(newPixels, 0, width, 0, 0, width, height)

            binding.imageView.setImageBitmap(tempBitmap)
            binding.actionButtonsLayout.visibility = android.view.View.VISIBLE
        }
    }


    private fun exportImageToGallery(bitmap: Bitmap) {
        val filename = "edited_image_${System.currentTimeMillis()}.png"
        val mimeType = "image/png"
        val relativeLocation = "Pictures/NativeImageProcessing"

        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
        }

        val resolver = contentResolver
        val uri = resolver.insert(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        if (uri != null) {
            resolver.openOutputStream(uri).use { outStream ->
                if (outStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                    Toast.makeText(this, "Image exported to gallery.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to open output stream.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Failed to create image file.", Toast.LENGTH_SHORT).show()
        }
    }

}
