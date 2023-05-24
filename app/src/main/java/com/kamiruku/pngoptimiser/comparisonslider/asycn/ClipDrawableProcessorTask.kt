package com.kamiruku.pngoptimiser.comparisonslider.asycn

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ClipDrawable
import android.os.AsyncTask
import android.os.Looper
import android.view.Gravity
import android.widget.ImageView
import android.widget.SeekBar

import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.lang.ref.WeakReference

/**
 * Created by Jemo on 12/5/16.
 */

class ClipDrawableProcessorTask<T>(
    private val imageView: ImageView,
    private val seekBar: SeekBar,
    private val context: Context,
    private val loadedFinishedListener: OnAfterImageLoaded? = null
) {
    suspend fun processImage(vararg args: T): ClipDrawable? {
        withContext(Dispatchers.IO) {
            var theBitmap: Bitmap?
            if (args[0] is String) {
                theBitmap = Glide.with(context)
                    .asBitmap()
                    .load(args[0])
                    .submit()
                    .get()
            } else {
                theBitmap = (args[0] as BitmapDrawable).bitmap
            }
            val tmpBitmap = getScaledBitmap(theBitmap)
            if (tmpBitmap != null)
                theBitmap = tmpBitmap

            val bitmapDrawable = BitmapDrawable(context.resources, theBitmap)
            val clipDrawable = ClipDrawable(bitmapDrawable, Gravity.START, ClipDrawable.HORIZONTAL)
            execute(clipDrawable)
            return@withContext clipDrawable
        }
        return null
    }

    private fun getScaledBitmap(bitmap: Bitmap): Bitmap? {
        try {
            val imageWidth = imageView.width
            val imageHeight = imageView.height

            if (imageWidth > 0 && imageHeight > 0)
                return Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun execute(clipDrawable: ClipDrawable?) {
        if (clipDrawable != null) {
            initSeekBar(clipDrawable)
            imageView.setImageDrawable(clipDrawable)
            if (clipDrawable.level != 0) {
                val progressNum = 5000
                clipDrawable.level = progressNum
            } else
                clipDrawable.level = seekBar.progress
            loadedFinishedListener?.onLoadedFinished(true)
        } else {
            loadedFinishedListener?.onLoadedFinished(false)
        }
    }

    private fun initSeekBar(clipDrawable: ClipDrawable) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                clipDrawable.level = i
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
    }

    interface OnAfterImageLoaded {
        fun onLoadedFinished(loadedSuccess: Boolean)
    }
}