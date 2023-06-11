package com.kamiruku.pngoptimiser.compressor.constraint

import android.graphics.Bitmap
import id.zelory.compressor.compressFormat
import id.zelory.compressor.constraint.Compression
import id.zelory.compressor.constraint.Constraint
import id.zelory.compressor.loadBitmap
import id.zelory.compressor.overWrite
import java.io.File

/**
 * Created on : June 11, 2023
 * Author     : Kamiruku
 * Name       : Kamiruku
 * GitHub     : https://github.com/Kamiruku
 *
 * This is a fix for compressing PNG images to JPG.
 */
class FormatQualityConstraint(
    private val format: Bitmap.CompressFormat,
    private val quality: Int
) : Constraint {

    override fun isSatisfied(imageFile: File): Boolean {
        return format == imageFile.compressFormat()
    }

    override fun satisfy(imageFile: File): File {
        return overWrite(imageFile, loadBitmap(imageFile), format, quality = quality)
    }
}

fun Compression.qualityFormat(format: Bitmap.CompressFormat, quality: Int) {
    constraint(FormatQualityConstraint(format, quality))
}