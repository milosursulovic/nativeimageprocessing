package com.example.nativeimageprocessing.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nativeimageprocessing.Constants
import com.example.nativeimageprocessing.FilterOption
import com.example.nativeimageprocessing.R
import com.example.nativeimageprocessing.adapters.FilterOptionsAdapter
import com.example.nativeimageprocessing.databinding.ActivityEditBinding
import java.io.File
import java.io.FileOutputStream

class EditActivity : AppCompatActivity() {

    companion object {
        init {
            System.loadLibrary("nativeimageprocessing")
        }
    }

    private external fun convertToGrayscale(pixels: IntArray, width: Int, height: Int): IntArray
    private external fun invertColors(pixels: IntArray, width: Int, height: Int): IntArray
    private external fun sepia(pixels: IntArray, width: Int, height: Int): IntArray
    private external fun edgeDetect(pixels: IntArray, width: Int, height: Int): IntArray

    private lateinit var binding: ActivityEditBinding
    private lateinit var originalBitmap: Bitmap
    private lateinit var currentBitmap: Bitmap

    private lateinit var tempBitmap: Bitmap

    private lateinit var filterOptions: List<FilterOption>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filterOptions = listOf(
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
            binding.actionButtonsLayout.visibility = View.GONE
        }

        binding.btnCancel.setOnClickListener {
            binding.imageView.setImageBitmap(currentBitmap) // revert to currentBitmap (unchanged)
            binding.actionButtonsLayout.visibility = View.GONE
        }


        binding.btnExport.setOnClickListener {
            exportImageToGallery(currentBitmap)
        }
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

        val newPixels = when (filterId) {
            Constants.GRAYSCALE_ID -> convertToGrayscale(pixels, width, height)
            Constants.INVERT_ID -> invertColors(pixels, width, height)
            Constants.SEPIA_ID -> sepia(pixels, width, height)
            Constants.BRIGHTNESS_ID -> {
                openBrightnessActivity()
                return
            }
            Constants.CONTRAST_ID -> {
                openContrastActivity()
                return
            }
            Constants.BLUR_ID -> {
                openBlurActivity()
                return
            }
            Constants.EDGE_ID -> edgeDetect(pixels, width, height)
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

    private val brightnessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUriString = result.data?.getStringExtra(Constants.RESULT_IMAGE_URI)
            if (imageUriString != null) {
                val newUri = imageUriString.toUri()
                val bitmap = loadBitmapFromUri(newUri)
                currentBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                binding.imageView.setImageBitmap(currentBitmap)
            }
        }
    }

    private val contrastLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUriString = result.data?.getStringExtra(Constants.RESULT_IMAGE_URI)
            if (imageUriString != null) {
                val newUri = imageUriString.toUri()
                val bitmap = loadBitmapFromUri(newUri)
                currentBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                binding.imageView.setImageBitmap(currentBitmap)
            }
        }
    }

    private val blurLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUriString = result.data?.getStringExtra(Constants.RESULT_IMAGE_URI)
            if (imageUriString != null) {
                val newUri = imageUriString.toUri()
                val bitmap = loadBitmapFromUri(newUri)
                currentBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                binding.imageView.setImageBitmap(currentBitmap)
            }
        }
    }

    private fun openBrightnessActivity() {
        val intent = Intent(this, BrightnessActivity::class.java)
        // pass current image uri - you may want to cache currentBitmap to a file first
        val uri = saveBitmapToCache(currentBitmap)
        intent.putExtra(Constants.EXTRA_IMAGE_URI, uri.toString())
        brightnessLauncher.launch(intent)
    }

    private fun openContrastActivity() {
        val intent = Intent(this, ContrastActivity::class.java)
        val uri = saveBitmapToCache(currentBitmap)
        intent.putExtra(Constants.EXTRA_IMAGE_URI, uri.toString())
        contrastLauncher.launch(intent)
    }

    private fun openBlurActivity() {
        val intent = Intent(this, BlurActivity::class.java)
        val uri = saveBitmapToCache(currentBitmap)
        intent.putExtra(Constants.EXTRA_IMAGE_URI, uri.toString())
        blurLauncher.launch(intent)
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val cachePath = File(cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "temp_image.png")
        FileOutputStream(file).use { outStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        }
        return Uri.fromFile(file)
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
