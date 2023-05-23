package com.kamiruku.pngoptimiser

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.constraintlayout.widget.ConstraintSet
import com.kamiruku.pngoptimiser.comparisonslider.ComparisonSlider
import com.kamiruku.pngoptimiser.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //Night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        val scale = applicationContext.resources.displayMetrics.density
        val browseImages: Button = Button(applicationContext)
        //150 dp width, 150 dp height
        browseImages.layoutParams = ViewGroup.LayoutParams(
            150.toPixels(scale),
            ConstraintSet.WRAP_CONTENT
        )
        browseImages.id = View.generateViewId()
        binding.constraintLayout.addView(browseImages)

        //Establishes constraints to the spinner
        val set: ConstraintSet = ConstraintSet()
        set.clone(binding.constraintLayout)
        set.connect(browseImages.id, ConstraintSet.TOP,
            binding.constraintLayout.id, ConstraintSet.TOP)
        set.connect(browseImages.id, ConstraintSet.BOTTOM,
            binding.constraintLayout.id, ConstraintSet.BOTTOM)
        set.connect(browseImages.id, ConstraintSet.START,
            binding.constraintLayout.id, ConstraintSet.START)
        set.connect(browseImages.id, ConstraintSet.END,
            binding.constraintLayout.id, ConstraintSet.END)

        set.setVerticalBias(browseImages.id, 0.85f)
        set.applyTo(binding.constraintLayout)

        //Curved Corners
        browseImages.setBackgroundResource(R.drawable.button_background)
        browseImages.setBackgroundColor(Color.parseColor("#80512DA8"))
        browseImages.text = getString(R.string.browseImages)

        browseImages.setOnClickListener {
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

        val comparisonSlider: ComparisonSlider = ComparisonSlider(applicationContext)
        comparisonSlider.layoutParams = ViewGroup.LayoutParams(
            400.toPixels(scale),
            400.toPixels(scale)
        )
        comparisonSlider.id = View.generateViewId()
        binding.constraintLayout.addView(comparisonSlider)

        set.clone(binding.constraintLayout)
        set.connect(comparisonSlider.id, ConstraintSet.TOP,
            binding.constraintLayout.id, ConstraintSet.TOP)
        set.connect(comparisonSlider.id, ConstraintSet.BOTTOM,
            binding.constraintLayout.id, ConstraintSet.BOTTOM)
        set.connect(comparisonSlider.id, ConstraintSet.START,
            binding.constraintLayout.id, ConstraintSet.START)
        set.connect(comparisonSlider.id, ConstraintSet.END,
            binding.constraintLayout.id, ConstraintSet.END)
        set.applyTo(binding.constraintLayout)

        comparisonSlider.setBeforeImage(
            getDrawable(this, R.drawable.ic_launcher_background))
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