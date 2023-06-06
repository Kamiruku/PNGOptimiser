package com.kamiruku.pngoptimiser.activities

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kamiruku.pngoptimiser.ActivityUtils
import com.kamiruku.pngoptimiser.R
import com.kamiruku.pngoptimiser.databinding.ActivityMainBinding
import com.kamiruku.pngoptimiser.fragments.CompressionSelectionFragment
import kotlinx.coroutines.launch
import java.io.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val aUtils = ActivityUtils()
        aUtils.hideDecor(window)
        aUtils.hideStatus(window)

        //Night mode
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        val scale = applicationContext.resources.displayMetrics.density

        //Curved Corners
        binding.browseImages.setBackgroundResource(R.drawable.button_background)
        binding.browseImages.setBackgroundColor(Color.parseColor("#80512DA8"))
        binding.browseImages.text = getString(R.string.browseImages)

        binding.browseImages.setOnClickListener {
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

        binding.textViewImageSize.setOnClickListener {
            if (!settingsFragIsOpen) {
                val ft = sfm.beginTransaction()
                ft.setCustomAnimations(
                    R.anim.slide_in_bottom,
                    R.anim.slide_out_top,
                    R.anim.slide_in_top,
                    R.anim.slide_out_bottom
                )
                val frag = CompressionSelectionFragment()
                ft.replace(R.id.fragmentContainerView, frag)
                    .show(frag)
                    .commit()
                settingsFragIsOpen = true
                println("Fragment popup")
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
        binding.textViewImageSize.text = "Actual Size ${formatBytes(file.length())}"
        //Display uncompressed image on image viewer
        binding.imageViewer.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
        //Compressing techniques should not be run on UI thread
        lifecycleScope.launch {
        }
    }

    private fun formatBytes(bytes: Long): String {
        return android.text.format.Formatter.formatFileSize(applicationContext, bytes)
    }

    private fun Int.toPixels(scale: Float): Int {
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