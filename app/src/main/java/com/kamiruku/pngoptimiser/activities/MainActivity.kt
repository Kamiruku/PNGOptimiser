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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.kamiruku.pngoptimiser.*
import com.kamiruku.pngoptimiser.databinding.ActivityMainBinding
import com.kamiruku.pngoptimiser.fragments.CompressionSelectionFragment
import com.kamiruku.pngoptimiser.fragments.DisplayImagesFragment
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
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                //Android 13
                Intent(MediaStore.ACTION_PICK_IMAGES)
            else {
                Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            }
            //Allows > 1 images to be selected
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            getResult.launch(intent)
        }
        binding.progressBar.visibility = View.INVISIBLE

        val sfm = supportFragmentManager
        var settingsFragIsOpen = false
        val frag = CompressionSelectionFragment()

        sfm.beginTransaction()
            .add(R.id.fragment_container_view, frag)
            .hide(frag)
            .commit()

        //View is gone from layout - i.e does not have a clickable event
        binding.viewDetectOptionExit.visibility = View.GONE
        //Centers text inside the image size text box vertically
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
                val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    saveToExternalStorage(cachedConverted !!, cachedConverted?.extension ?: "")
                else
                    saveToExternalStorage(cachedConverted !!)

                if (success) {
                    Toast.makeText(
                        applicationContext,
                        "File has been saved to Pictures/PNGOptimiser.",
                        Toast.LENGTH_SHORT
                    ).show()

                    cachedConverted?.delete()
                    cachedConverted = null
                } else {
                    Toast.makeText(
                        applicationContext,
                        "An error has occurred when saving the file. Please check the stacktrace for more information.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        var compressType = "Original"
        var quality  = 0

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

        val fragments = arrayOf(DisplayImagesFragment(), DisplayImagesFragment())
        val adapter = ViewStateAdapter(sfm, fragments, lifecycle)
        binding.viewPager2.adapter = adapter

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Before"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("After"))

        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.viewPager2.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position))
            }
        })
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
                viewModel.selectedCompression.value ?: "Original",
                viewModel.selectedQuality.value ?: 0
            )
        } else {
            //For certain devices, clipData obtained when only 1 object is selected will be null
            val imageUri: Uri? = it.data?.data
            selectedUri = imageUri
            if (imageUri != null) {
                managesImage(
                    imageUri,
                    viewModel.selectedCompression.value ?: "Original",
                    viewModel.selectedQuality.value ?: 0
                )
            }
        }
    }

    private fun managesImage(imageUri: Uri, compressType: String, quality: Int) {
        if (imageUri == Uri.EMPTY) return
        //Check cached file existence and deletes it then set to null
        if (cachedConverted?.exists() == true || cachedConverted != null)
            cachedConverted?.delete().also { cachedConverted = null }

        val file = getFile(applicationContext, imageUri)

        //Sets uncompressed image file path & uncompressed image size
        viewModel.beforePath.value = imageUri.toString()
        binding.textViewBeforeSize.text =
            getString(
                R.string.actual_image_size,
                formatBytes(file.length())
            )
        //Force slide to go to first tab
        binding.tabLayout.getTabAt(0)?.select()

        val compressionType = when (compressType) {
            "Original" -> OriginalFile()
            "Default JPG" -> DefaultJPG()
            "Default PNG" -> DefaultPNG()
            "PNGQuant (Lossy)" -> PNGQuant()
            "Luban" -> LubanCompress()
            else -> null
        }

        var cachedFile: File? = null

        val compressJob = lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.VISIBLE
            }
            cachedFile = compressionType?.compress(file, quality, applicationContext)

            //Handles pngquant non-png error
            if (cachedFile == file) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.INVISIBLE
                }
                return@launch
            }
            //Display error message & force return
            if ((cachedFile == null) || (cachedFile?.length() == 0L)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "An error has occurred. Please check the stack trace for more information or retry with a lower quality setting.",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.INVISIBLE
                }
                return@launch
            }
        }

        //Launch on main thread for UI changes
        lifecycleScope.launch(Dispatchers.Main) {
            //Wait for compress job to finish
            compressJob.join()

            binding.progressBar.visibility = View.INVISIBLE
            //Shows compressed file size
            binding.textViewAfterSize.text =
                getString(
                    R.string.compressed_image_size,
                    formatBytes(cachedFile?.length() ?: 0L)
                )
            viewModel.afterPath.value = cachedFile?.path
            //Deletes file stored in root/data/data/com.kamiruku.pngoptimiser/files NOT original file since we no longer need it
            file.delete()
            //CachedFile is not deleted because user can choose to save it
            cachedConverted = cachedFile
            binding.tabLayout.getTabAt(1)?.select()
        }
    }

    private fun formatBytes(bytes: Long): String {
        return android.text.format.Formatter.formatFileSize(applicationContext, bytes)
    }

    private fun saveToExternalStorage(src: File): Boolean {
        //Check permissions again before saving because user can revoke permissions whilst app is open
        if (!checkPermissions()) {
            Toast.makeText(
                applicationContext,
                "You have not granted read - write access to your external storage.",
                Toast.LENGTH_LONG)
                .show()
            return false
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
            return false
        }

        return true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToExternalStorage(file: File, format: String): Boolean {
        //Check permissions again before saving because user can revoke permissions whilst app is open
        if (!checkPermissions()) {
            Toast.makeText(
                applicationContext,
                "You have not granted read - write access to your external storage.",
               Toast.LENGTH_LONG)
                .show()
            return false
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
            return false
        }
        return true
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

    val beforePath = MutableLiveData<String>()
    val afterPath = MutableLiveData<String>()
}