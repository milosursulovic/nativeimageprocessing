package com.example.nativeimageprocessing.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nativeimageprocessing.Constants
import com.example.nativeimageprocessing.FilterOption
import com.example.nativeimageprocessing.R
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

    private var activeSeekBarFilter: String? = null

    private lateinit var binding: ActivityEditBinding
    private lateinit var originalBitmap: Bitmap
    private lateinit var currentBitmap: Bitmap

    private lateinit var tempBitmap: Bitmap

    private val filterOptions = listOf(
        FilterOption(Constants.GRAYSCALE_ID, getString(R.string.grayscale)),
        FilterOption(Constants.INVERT_ID, getString(R.string.invert_colors)),
        FilterOption(Constants.SEPIA_ID, getString(R.string.sepia)),
        FilterOption(Constants.BRIGHTNESS_ID, getString(R.string.brightness)),
        FilterOption(Constants.CONTRAST_ID, getString(R.string.contrast)),
        FilterOption(Constants.BLUR_ID, getString(R.string.blur)),
        FilterOption(Constants.EDGE_ID, getString(R.string.edge_detection)),
        FilterOption(Constants.ROTATE_90_ID, getString(R.string.rotate_90)),
        FilterOption(Constants.ROTATE_180_ID, getString(R.string.rotate_180)),
        FilterOption(Constants.ROTATE_270_ID, getString(R.string.rotate_270)),
        FilterOption(Constants.CROP_ID, getString(R.string.crop))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra("imageUri")
        if (imageUriString == null) {
            Toast.makeText(this, getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show()
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
        binding.actionButtonsLayout.visibility = View.GONE

        when (activeSeekBarFilter) {
            Constants.BRIGHTNESS_ID -> {
                binding.seekBar.visibility = View.GONE
                binding.seekBar.setOnSeekBarChangeListener(null)
                binding.seekBar.progress = 100
            }

            Constants.CONTRAST_ID -> {
                binding.seekBar.visibility = View.GONE
                binding.seekBar.setOnSeekBarChangeListener(null)
                binding.seekBar.max = 510
                binding.seekBar.progress = 255
            }

            else -> {
                binding.seekBar.visibility = View.GONE
                binding.seekBar.setOnSeekBarChangeListener(null)
                binding.seekBar.progress = 100
            }
        }

        activeSeekBarFilter = null
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val stream = contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(stream)
            ?: error(getString(R.string.failed_to_decode_image))
    }

    private fun applyFilter(filterId: String) {
        val width = currentBitmap.width
        val height = currentBitmap.height
        val pixels = IntArray(width * height)
        currentBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        when (filterId) {
            Constants.BRIGHTNESS_ID -> {

                activeSeekBarFilter = Constants.BRIGHTNESS_ID

                binding.seekBar.visibility = View.VISIBLE

                val brightnessValue = binding.seekBar.progress
                val newPixels = brightness(pixels, width, height, brightnessValue)
                tempBitmap = createBitmap(width, height)
                tempBitmap.setPixels(newPixels, 0, width, 0, 0, width, height)
                binding.imageView.setImageBitmap(tempBitmap)
                binding.actionButtonsLayout.visibility = View.VISIBLE

                // Add listener for SeekBar changes
                binding.seekBar.setOnSeekBarChangeListener(object :
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
            }

            Constants.CONTRAST_ID -> {

                activeSeekBarFilter = Constants.CONTRAST_ID

                binding.seekBar.visibility = View.VISIBLE

                binding.seekBar.max = 510 // to map from -255 to +255, for example
                binding.seekBar.progress = 255 // default (no change)

                val contrastValue = binding.seekBar.progress - 255 // map 0-510 to -255 to +255
                val newPixels = contrast(pixels, width, height, contrastValue)
                tempBitmap = createBitmap(width, height)
                tempBitmap.setPixels(newPixels, 0, width, 0, 0, width, height)
                binding.imageView.setImageBitmap(tempBitmap)
                binding.actionButtonsLayout.visibility = View.VISIBLE

                binding.seekBar.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        val contrastVal = progress - 255
                        val updatedPixels = contrast(pixels, width, height, contrastVal)
                        tempBitmap.setPixels(updatedPixels, 0, width, 0, 0, width, height)
                        binding.imageView.setImageBitmap(tempBitmap)
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            else -> {

                binding.seekBar.visibility = View.GONE

                binding.seekBar.setOnSeekBarChangeListener(null)

                val newPixels = when (filterId) {
                    Constants.GRAYSCALE_ID -> convertToGrayscale(pixels, width, height)
                    Constants.INVERT_ID -> invertColors(pixels, width, height)
                    Constants.SEPIA_ID -> sepia(pixels, width, height)
                    // other filters ...
                    else -> {
                        Toast.makeText(
                            this,
                            getString(R.string.filter_not_implemented, filterId), Toast.LENGTH_SHORT
                        )
                            .show()
                        return
                    }
                }

                tempBitmap = createBitmap(width, height)
                tempBitmap.setPixels(newPixels, 0, width, 0, 0, width, height)

                binding.imageView.setImageBitmap(tempBitmap)
                binding.actionButtonsLayout.visibility = View.VISIBLE
            }
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
                    Toast.makeText(
                        this,
                        getString(R.string.image_exported_to_gallery), Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.failed_to_open_output_stream), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                this,
                getString(R.string.failed_to_create_image_file),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}
