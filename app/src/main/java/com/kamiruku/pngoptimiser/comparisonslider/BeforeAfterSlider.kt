package com.kamiruku.pngoptimiser.comparisonslider

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.kamiruku.pngoptimiser.comparisonslider.asycn.ClipDrawableProcessorTask
import com.github.developer__.extensions.loadImage
import com.github.developer__.extensions.stayVisibleOrGone
import com.kamiruku.pngoptimiser.R
import com.kamiruku.pngoptimiser.databinding.SliderLayoutBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by Jemo on 12/5/16.
 */

class BeforeAfterSlider : RelativeLayout, ClipDrawableProcessorTask.OnAfterImageLoaded {
    private lateinit var binding: SliderLayoutBinding
    private lateinit var clipDrawableProcessorTask: ClipDrawableProcessorTask<Any>

    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val attr = context.theme.obtainStyledAttributes(attrs, R.styleable.BeforeAfterSlider,0,0)
        try {
            val thumbDrawable = attr.getDrawable(R.styleable.BeforeAfterSlider_slider_thumb)

            val beforeImage = attr.getDrawable(R.styleable.BeforeAfterSlider_before_image)
            val afterImageUrl = attr.getDrawable(R.styleable.BeforeAfterSlider_after_image)

            binding = SliderLayoutBinding.inflate(
                LayoutInflater.from(context)
            )

            setSliderThumb(thumbDrawable)
            setBeforeImage(beforeImage)
            setAfterImage(afterImageUrl)
        } finally {
            attr.recycle()
        }
        clipDrawableProcessorTask = ClipDrawableProcessorTask(
            binding.afterImageViewId, binding.seekbarId, context
        )
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.slider_layout, this)
    }

    /**
     * set original image
     */
    fun setBeforeImage(imageUri: String): BeforeAfterSlider {
        binding.beforeImageViewId.loadImage(imageUri)
        return this
    }

    fun setBeforeImage(imgDrawable: Drawable?): BeforeAfterSlider {
        binding.beforeImageViewId.loadImage(imgDrawable)
        return this
    }

    /**
     * set changed image
     */
    fun setAfterImage(imageUri: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val clipDrawable = clipDrawableProcessorTask.processImage(imageUri)
            binding.afterImageViewId.setImageDrawable(clipDrawable)
            binding.seekbarId.stayVisibleOrGone(clipDrawable != null)
        }
    }

    /**
     * set changed image
     */
    fun setAfterImage(imageDrawable: Drawable?) {
        CoroutineScope(Dispatchers.Main).launch {
            val clipDrawable = imageDrawable?.let { clipDrawableProcessorTask.processImage(it) }
            binding.afterImageViewId.setImageDrawable(clipDrawable)
            binding.seekbarId.stayVisibleOrGone(clipDrawable != null)
        }
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
