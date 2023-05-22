package com.kamiruku.pngoptimiser

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.kamiruku.pngoptimiser.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val scale = applicationContext.resources.displayMetrics.density
        val imageSelector: ShapeableImageView = ShapeableImageView(applicationContext)
        //150 dp width, 150 dp height
        imageSelector.layoutParams = ViewGroup.LayoutParams(
            150.toPixels(scale),
            150.toPixels(scale)
        )
        imageSelector.id = View.generateViewId()
        binding.constraintLayout.addView(imageSelector)

        //Establishes constraints to the spinner
        val set: ConstraintSet = ConstraintSet()
        set.clone(binding.constraintLayout)
        set.connect(imageSelector.id, ConstraintSet.TOP,
            binding.constraintLayout.id, ConstraintSet.TOP)
        set.connect(imageSelector.id, ConstraintSet.BOTTOM,
            binding.constraintLayout.id, ConstraintSet.BOTTOM)
        set.connect(imageSelector.id, ConstraintSet.START,
            binding.constraintLayout.id, ConstraintSet.START)
        set.connect(imageSelector.id, ConstraintSet.END,
            binding.constraintLayout.id, ConstraintSet.END)
        set.applyTo(binding.constraintLayout)

        imageSelector.setBackgroundColor(Color.parseColor("#80512DA8"))
        //Curved corners
        imageSelector.shapeAppearanceModel =
            imageSelector.shapeAppearanceModel
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, 50f)
                .build()

        imageSelector.setOnClickListener {
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
    }

    private val getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        //Receiver for image picker
        if (it.resultCode == 1) {
            val selectedImageUri: Uri? = it.data?.data
            if (selectedImageUri != null) {
                //TODO display selected image
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