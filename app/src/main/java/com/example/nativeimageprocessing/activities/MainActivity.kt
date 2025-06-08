@file:Suppress("DEPRECATION")

package com.example.nativeimageprocessing.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.nativeimageprocessing.R
import com.example.nativeimageprocessing.databinding.ActivityMainBinding
import com.example.nativeimageprocessing.utils.ImageUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var photoUri: Uri? = null  // Holds URI for full-res photo

    // Permission launcher to request multiple permissions
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val grantedCamera = permissions[Manifest.permission.CAMERA] ?: false
        val grantedStorage =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            } else {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            }

        if (grantedCamera && grantedStorage) {
            Toast.makeText(this, getString(R.string.permissions_granted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                getString(R.string.permissions_denied_cannot_take_or_pick_photos),
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // Launcher for picking image from gallery
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            launchEditActivity(it)
        }
    }

    // Launcher for taking photo via camera
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                // Load bitmap from the full-size photo URI
                launchEditActivity(uri)
            } ?: run {
                Toast.makeText(
                    this,
                    getString(R.string.failed_to_capture_image),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.camera_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request permissions on startup
        checkAndRequestPermissions()

        binding.pickImageButton.setOnClickListener {
            if (hasPermissions()) {
                pickImageLauncher.launch("image/*")
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.please_grant_permissions_first), Toast.LENGTH_SHORT
                ).show()
                checkAndRequestPermissions()
            }
        }

        binding.takePhotoButton.setOnClickListener {
            if (hasPermissions()) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                // Create a file and URI for full resolution image
                photoUri = createImageUri()
                photoUri?.let { uri ->
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    takePhotoLauncher.launch(intent)
                } ?: Toast.makeText(
                    this,
                    getString(R.string.failed_to_create_image_file),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.please_grant_permissions_first),
                    Toast.LENGTH_SHORT
                ).show()
                checkAndRequestPermissions()
            }
        }
    }

    private fun createImageUri(): Uri? {
        return try {
            val imageFile = ImageUtils.createImageFile(this)
            FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        val storagePermission =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        if (ContextCompat.checkSelfPermission(
                this,
                storagePermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(storagePermission)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun hasPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        val storagePermission =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                storagePermission == PackageManager.PERMISSION_GRANTED
    }

    private fun launchEditActivity(uri: Uri) {
        val intent = Intent(this, EditActivity::class.java)
        intent.putExtra("imageUri", uri.toString())
        startActivity(intent)
    }
}
