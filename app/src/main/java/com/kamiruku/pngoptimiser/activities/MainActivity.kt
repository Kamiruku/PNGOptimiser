package com.kamiruku.pngoptimiser.activities

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.kamiruku.pngoptimiser.*
import com.kamiruku.pngoptimiser.databinding.ActivityMainBinding
import com.kamiruku.pngoptimiser.fragments.CompressionSelectionFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: ViewModel by viewModels()
    private var selectedUri: Uri? = null
    private var cachedConverted: File? = null

    init {
        //If put in MainActivity's onCreate, the screen would flash white then black if the device's default colour was light.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        checkPermissions()

        val aUtils = ActivityUtils()
        aUtils.hideDecor(window)
        aUtils.hideStatus(window)

        //Curved Corners
        binding.buttonBrowseImages.apply {
            setBackgroundResource(R.drawable.button_background)
            setBackgroundColor(Color.parseColor("#80512DA8"))
            text = getString(R.string.browse_images)
        }
        binding.buttonBrowseImages.setOnClickListener {
            val intent: Intent
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                //Android 13
                intent = Intent(MediaStore.ACTION_PICK_IMAGES)
            else {
                intent = Intent(Intent.ACTION_PICK)
                    .apply { type = "image/*" }
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
                    R.anim.slide_out_bottom,
                )
                //Shows the actual fragment
                ft.show(frag)
                    .commit()
                binding.viewDetectOptionExit.visibility = View.VISIBLE
                binding.textViewBeforeSize.background =
                    AppCompatResources.getDrawable(applicationContext, R.drawable.rounded_corner_open)
                //Allows fragment to be hidden
                settingsFragIsOpen = true
                println("Fragment popup opened.")
            }
        }

        binding.textViewAfterSize.setOnClickListener {
            if (cachedConverted != null && cachedConverted?.exists() !!) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    saveToExternalStorage(cachedConverted !!, cachedConverted?.extension ?: "")
                 else
                     saveToExternalStorage(cachedConverted !!)

                Toast.makeText(
                    applicationContext,
                  "File has been saved to Pictures/PNGOptimiser.",
                    Toast.LENGTH_SHORT
                ).show()

                cachedConverted?.delete()
                cachedConverted = null
            }
        }

        var compressType: String = "Original"
        var quality: Int = 0

        binding.viewDetectOptionExit.setOnClickListener {
            if (settingsFragIsOpen) {
                //Need new fragment transaction per.. transaction
                val ft = sfm.beginTransaction()
                ft.setCustomAnimations(
                    R.anim.slide_in_bottom,
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
                println("Fragment popup closed.")

                if (compressType != viewModel.selectedCompression.value || quality != viewModel.selectedQuality.value) {
                    //A check is done to see if the user has changed the compression type or the quality
                    compressType = viewModel.selectedCompression.value ?: ""
                    quality = viewModel.selectedQuality.value ?: 0

                    managesImage(selectedUri ?: Uri.EMPTY, compressType, quality)
                }
            }
        }
    }

    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        //Receiver for image picker
        if (it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data: Intent = it.data ?: return@registerForActivityResult

        val clipData: ClipData? = data.clipData

        if (clipData != null && clipData.itemCount == 1) {
            val imageUri: Uri = clipData.getItemAt(0).uri
            selectedUri = imageUri
            managesImage(
                imageUri,
                viewModel.selectedCompression.value ?: "",
                viewModel.selectedQuality.value ?: 0
            )
        } else {
            //For certain devices, clipData obtained when only 1 object is selected will be null
            val imageUri: Uri? = it.data?.data
            selectedUri = imageUri
            if (imageUri != null) {
                managesImage(
                    imageUri,
                    viewModel.selectedCompression.value ?: "",
                    viewModel.selectedQuality.value ?: 0
                )
            }
        }
    }

    private fun managesImage(imageUri: Uri, compressType: String, quality: Int) {
        if (imageUri == Uri.EMPTY) return

        if (cachedConverted?.exists() == true || cachedConverted != null)
            cachedConverted?.delete()

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
                val compressionType = when (compressType) {
                    "Original" -> OriginalFile()
                    "Default JPG" -> DefaultJPG()
                    "Default PNG" -> DefaultPNG()
                    "PNGQuant (Lossy)" -> PNGQuant()
                    "Luban" -> LubanCompress()
                    else -> null
                }

                val cachedFile = compressionType?.compress(file, quality, applicationContext)

                if (cachedFile == null || cachedFile.length() == 0L) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            applicationContext,
                            "An error has occurred. Please check the stack trace for more information.",
                            Toast.LENGTH_SHORT).show()
                    }
                    return@withContext
                }

                withContext(Dispatchers.Main) {
                    binding.imageViewer.setImage(ImageSource.uri(cachedFile.absolutePath))

                    binding.textViewAfterSize.text =
                        getString(
                            R.string.compressed_image_size,
                            formatBytes(cachedFile.length())
                        )
                }

                //Deletes file stored in root/data/data/com.kamiruku.pngoptimiser/files NOT original file
                file.delete()

                cachedConverted = cachedFile
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        return android.text.format.Formatter.formatFileSize(applicationContext, bytes)
    }

    private fun saveToExternalStorage(src: File) {
        //Check permissions again before saving because user can revoke permissions whilst app is open
        if (!checkPermissions()) {
            Toast.makeText(
                applicationContext,
                "You have not granted read - write access to your external storage.",
                Toast.LENGTH_LONG)
                .show()
            return
        }

        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
        val rootDir = File(root)
        if (!rootDir.exists()) rootDir.mkdir()

        val storageDir = File(root + File.separator + "pngoptimiser")
        if (!storageDir.exists()) storageDir.mkdir()

        val dst = File(storageDir, src.name)

        try {
            FileInputStream(src).use { `in` ->
                FileOutputStream(dst).use { out ->
                    // Transfer bytes from in to out
                    val buf = ByteArray(1024)
                    var len: Int
                    while (`in`.read(buf).also { len = it } > 0) {
                        out.write(buf, 0, len)
                    }
                }
            }
        }

        catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToExternalStorage(file: File, format: String) {
        //Check permissions again before saving because user can revoke permissions whilst app is open
        if (!checkPermissions()) {
            Toast.makeText(
                applicationContext,
                "You have not granted read - write access to your external storage.",
               Toast.LENGTH_LONG)
                .show()
            return
        }

        val values: ContentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "pngoptimiser")
        }

        val compressFormat = when (format) {
            "jpg" -> Bitmap.CompressFormat.JPEG
            "png" -> Bitmap.CompressFormat.PNG
            else -> Bitmap.CompressFormat.JPEG
        }

        val resolver = contentResolver
        var uri: Uri? = null

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)

        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Failed to create a new MediaStore record.")
            resolver.openOutputStream(uri)?.use {
                if (!bitmap.compress(compressFormat, 100, it))
                    throw IOException("Failed to save bitmap.")
            } ?: throw IOException("Failed to open output stream.")
        }

        catch (ex: Exception) {
            ex.printStackTrace()
            uri?.let {
                // Don't leave an orphan entry in the MediaStore
                resolver.delete(it, null, null)
            }
        }

    }

    private fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_GRANTED
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