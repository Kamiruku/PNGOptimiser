package com.kamiruku.pngoptimiser.activities

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.ImageSource
import com.kamiruku.pngoptimiser.*
import com.kamiruku.pngoptimiser.databinding.ActivityMainBinding
import com.kamiruku.pngoptimiser.fragments.CompressionSelectionFragment
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val aUtils = ActivityUtils()
        aUtils.hideDecor(window)
        aUtils.hideStatus(window)

        //Night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        //Curved Corners
        binding.buttonBrowseImages.setBackgroundResource(R.drawable.button_background)
        binding.buttonBrowseImages.setBackgroundColor(Color.parseColor("#80512DA8"))
        binding.buttonBrowseImages.text = getString(R.string.browse_images)

        binding.buttonBrowseImages.setOnClickListener {
            val intent: Intent
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                //Android 13
                intent = Intent(MediaStore.ACTION_PICK_IMAGES)
            else {
                intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
            }
            //Allows > 1 images to be selected
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            getResult.launch(intent)
        }

        val sfm = supportFragmentManager
        var settingsFragIsOpen: Boolean = false
        val frag: CompressionSelectionFragment = CompressionSelectionFragment()

        sfm.beginTransaction()
            .add(R.id.fragment_container_view, frag)
            .hide(frag)
            .commit()

        //View is gone from layout - i.e does not have a clickable event
        binding.viewDetectOptionExit.visibility = View.GONE
        //Centers text inside the image size textbox vertically
        binding.textViewBeforeSize.gravity = Gravity.CENTER_VERTICAL
        binding.textViewAfterSize.gravity = Gravity.CENTER_VERTICAL

        binding.textViewBeforeSize.setOnClickListener {
            if (!settingsFragIsOpen) {
                //Need new fragment transaction per.. transaction
                val ft = sfm.beginTransaction()
                //Sliding animation
                ft.setCustomAnimations(
                    R.anim.slide_in_bottom,
                    R.anim.slide_in_bottom,
                )
                //Shows the actual fragment
                ft.show(frag)
                    .commit()
                binding.viewDetectOptionExit.visibility = View.VISIBLE
                binding.textViewBeforeSize.background =
                    AppCompatResources.getDrawable(applicationContext, R.drawable.rounded_corner_open)
                //Allows fragment to be hidden
                settingsFragIsOpen = true
                println("Fragment popup.")
            }
        }

        binding.viewDetectOptionExit.setOnClickListener {
            if (settingsFragIsOpen) {
                //Need new fragment transaction per.. transaction
                val ft = sfm.beginTransaction()
                ft.setCustomAnimations(
                    R.anim.slide_out_bottom,
                    R.anim.slide_out_bottom,
                )
                //Hides the actual fragment
                ft.hide(frag)
                    .commit()
                binding.viewDetectOptionExit.visibility = View.GONE
                binding.textViewBeforeSize.background =
                    AppCompatResources.getDrawable(applicationContext, R.drawable.rounded_corner)
                //Allows fragment to be shown again
                settingsFragIsOpen = false
                println("Fragment popup close.")
            }
        }
    }

    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        //Receiver for image picker
        if (it.resultCode == Activity.RESULT_OK) {
            val data: Intent? = it.data
            if (data != null) {
                val clipData: ClipData? = data.clipData
                if (clipData != null) {
                    if (clipData.itemCount == 1) {
                        managesImage(clipData.getItemAt(0).uri)
                    }
                } else {
                    //For certain devices, clipData obtained when only 1 object is selected will be null
                    val imageUri: Uri? = it.data?.data
                    if (imageUri != null) {
                        managesImage(imageUri)
                    }
                }
            }
        }
    }

    private fun managesImage(imageUri: Uri) {
        //Displays uncompressed image & uncompressed image size
        val file = getFile(applicationContext, imageUri)
        binding.textViewBeforeSize.text =
            getString(
                R.string.actual_image_size,
                formatBytes(file.length())
            )
        //Display uncompressed image on image viewer
        binding.imageViewer.setImage(ImageSource.uri(imageUri))
        //Compressing techniques should not be run on UI thread
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                var newFile: File? = null
                when (viewModel.selectedCompression.value) {
                    "Original" -> {
                        newFile = File(cacheDir, file.name)
                        file.copyTo(newFile)
                    }
                    "Default JPG" -> {
                        newFile = File(cacheDir, "${file.nameWithoutExtension}.jpg")
                        val compressedImage = Compressor.compress(applicationContext, file) {
                            quality(viewModel.selectedQuality.value ?: 80)
                            format(Bitmap.CompressFormat.JPEG)
                        }
                        compressedImage.copyTo(newFile)
                    }
                    "Default PNG" -> {
                        newFile = File(cacheDir, "${file.nameWithoutExtension}.png")
                        val compressedImage = Compressor.compress(applicationContext, file) {
                            quality(viewModel.selectedQuality.value ?: 80)
                            format(Bitmap.CompressFormat.PNG)
                        }
                        compressedImage.copyTo(newFile)
                    }
                    "PNGQuant (Lossy)" -> {
                        if (file.extension.lowercase() != "png") {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    applicationContext,
                                    "Compressing non PNG is not allowed!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@withContext
                        }
                        newFile = File(cacheDir, "${file.nameWithoutExtension}.png")
                        val pngquant = LibPngQuant()
                        pngquant.pngQuantFile(
                            file,
                            newFile,
                            viewModel.selectedQuality.value ?: 80,
                            viewModel.selectedQuality.value ?: 80
                        )
                    }
                }
                binding.textViewAfterSize.text =
                    getString(
                        R.string.compressed_image_size,
                        formatBytes(newFile?.length() ?: 0)
                    )
                newFile?.delete()
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        return android.text.format.Formatter.formatFileSize(applicationContext, bytes)
    }

    private fun Int.toPixels(): Int {
        val scale = applicationContext.resources.displayMetrics.density
        //Converts from dp/sp to pixels
        return (this * scale + 0.5).toInt()
    }

    @Throws(IOException::class)
    private fun getFile(context: Context, uri: Uri): File {
        //Creates File object from Uri
        val destinationFile: File =
            File(context.filesDir.path + File.separatorChar + queryName(context, uri))
        try {
            //Opens input stream for uri
            context.contentResolver.openInputStream(uri).use {
                //Writes data from input stream to destination file
                createFileFromStream(
                    it !!,
                    destinationFile
                )
            }
        } catch (ex: Exception) {
            Log.d("Save File", ex.message.toString())
            ex.printStackTrace()
        }
        //Returns destination file
        return destinationFile
    }

    private fun createFileFromStream(ins: InputStream, destination: File?) {
        //Reads data from input stream and writes it to destination file
        try {
            //Creates FOS object using destination file
            FileOutputStream(destination).use {
                val buffer = ByteArray(4096)
                var length: Int
                while (ins.read(buffer).also { length = it } > 0) {
                    it.write(buffer, 0, length)
                }
                it.flush()
            }
        } catch (ex: Exception) {
            Log.e("Save File", ex.message!!)
            ex.printStackTrace()
        }
    }

    private fun queryName(context: Context, uri: Uri): String {
        //Creates cursor by querying content resolver using 'uri'
        val returnCursor: Cursor = context.contentResolver.query(uri, null, null, null, null)!!
        //Retrieves index of the display name column
        val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        //Ensures cursor is positioned at the beginning of result set
        returnCursor.moveToFirst()
        //Retrieves display name from cursor
        val name: String = returnCursor.getString(nameIndex)
        //Closes cursor
        returnCursor.close()
        return name
    }
}

class ViewModel: androidx.lifecycle.ViewModel() {
    private val mutableSelectedCompression = MutableLiveData<String>()
    val selectedCompression: LiveData<String> get() = mutableSelectedCompression

    fun changeCompression(selected: String) {
        mutableSelectedCompression.value = selected
    }

    private val mutableSelectedQuality = MutableLiveData<Int>()
    val selectedQuality: LiveData<Int> get() = mutableSelectedQuality

    fun changeQuality(selected: Int) {
        mutableSelectedQuality.value = selected
    }
}