package com.kamiruku.pngoptimiser

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.kamiruku.pngoptimiser.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedImageUri: Uri? = null

    private var mScaleGestureDetector: ScaleGestureDetector? = null
    private var mScaleFactor = 1.0f

    // this redirects all touch events in the activity to the gesture detector
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return mScaleGestureDetector!!.onTouchEvent(event!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

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

        mScaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
    }

    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        //Receiver for image picker
        if (it.resultCode == 1) {
            val imageUri: Uri? = it.data?.data
            if (imageUri != null) {
                selectedImageUri = imageUri
            }
        }
    }

    private fun Int.toPixels(scale: Float): Int {
        //Converts from dp/sp to pixels
        return (this * scale + 0.5).toInt()
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        // when a scale gesture is detected, use it to resize the image
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            mScaleFactor *= scaleGestureDetector.scaleFactor
            //mScaleFactor = 0.1f.coerceAtLeast(scaleGestureDetector.scaleFactor.coerceAtMost(10.0f))
            binding.imageBefore.scaleX = mScaleFactor
            binding.imageBefore.scaleY = mScaleFactor
            return true
        }
    }


    /**
     * A native method that is implemented by the 'pngoptimiser' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'pngoptimiser' library on application startup.
        init {
            System.loadLibrary("pngoptimiser")
        }
    }
}