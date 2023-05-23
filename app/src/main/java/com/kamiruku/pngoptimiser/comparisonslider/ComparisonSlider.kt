package com.kamiruku.pngoptimiser.comparisonslider

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.kamiruku.pngoptimiser.R
import com.kamiruku.pngoptimiser.databinding.SliderLayoutBinding
import com.kamiruku.pngoptimiser.loadImage
import com.kamiruku.pngoptimiser.stayVisibleOrGone

/**
 * Created by Jemo on 12/5/16.
 */

class ComparisonSlider : RelativeLayout, ClipDrawableProcessorTask.OnAfterImageLoaded {
    private lateinit var binding: SliderLayoutBinding

    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val attr = context.theme.obtainStyledAttributes(attrs, R.styleable.BeforeAfterSlider,0,0)
        try {
            val thumbDrawable = attr.getDrawable(R.styleable.BeforeAfterSlider_slider_thumb)

            val beforeImage = attr.getDrawable(R.styleable.BeforeAfterSlider_before_image)
            val afterImageUrl = attr.getDrawable(R.styleable.BeforeAfterSlider_after_image)

            setSliderThumb(thumbDrawable)
            setBeforeImage(beforeImage)
            setAfterImage(afterImageUrl)
        }finally {
            attr.recycle()
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.slider_layout, this)
        binding = SliderLayoutBinding.inflate(
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        )
    }

    /**
     * set original image
     */
    fun setBeforeImage(imageUri: String): ComparisonSlider {
        binding.beforeImageViewId.loadImage(imageUri)
        return this
    }

    fun setBeforeImage(imgDrawable: Drawable?): ComparisonSlider {
        binding.beforeImageViewId.loadImage(imgDrawable)
        return this
    }

    /**
     * set changed image
     */
    fun setAfterImage(imageUri: String) {
        ClipDrawableProcessorTask<String>(
            binding.afterImageViewId,
            binding.seekbarId,
            context,
            this).
        execute(imageUri)
    }

    /**
     * set changed image
     */
    fun setAfterImage(imageDrawable: Drawable?) {
        ClipDrawableProcessorTask<Drawable>(
            binding.afterImageViewId,
            binding.seekbarId,
            context, this).
        execute(imageDrawable)
    }

    /**
     * set thumb
     */
    fun setSliderThumb(thumb: Drawable?){
        thumb?.let {
            binding.seekbarId.thumb = thumb
        }
    }

    /**
     * fired up after second image loading will be finished
     */
    override fun onLoadedFinished(loadedSuccess: Boolean) {
        binding.seekbarId.stayVisibleOrGone(loadedSuccess)
    }

}