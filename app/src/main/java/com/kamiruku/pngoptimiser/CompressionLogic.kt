package com.kamiruku.pngoptimiser

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.destination
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface CompressionLogic {
    suspend fun compress(file: File, quality: Int, context: Context): File?
}

class OriginalFile: CompressionLogic {
    override suspend fun compress(file: File, quality: Int, context: Context): File {
        val newFile = File(context.cacheDir, file.name)
        file.copyTo(newFile)
        return newFile
    }
}

class DefaultJPG: CompressionLogic {
    override suspend fun compress(file: File, quality: Int, context: Context): File {
        val newFile = File(context.cacheDir, "${file.nameWithoutExtension}.jpg")
        Compressor.compress(context, file) {
            quality(quality)
            format(Bitmap.CompressFormat.JPEG)
            destination(newFile)
        }
        return newFile
    }
}

class DefaultPNG: CompressionLogic {
    override suspend fun compress(file: File, quality: Int, context: Context): File {
        val newFile = File(context.cacheDir, "${file.nameWithoutExtension}.png")
        Compressor.compress(context, file) {
            quality(quality)
            format(Bitmap.CompressFormat.PNG)
            destination(newFile)
        }
        return newFile
    }
}

class PNGQuant: CompressionLogic {
    override suspend fun compress(file: File, quality: Int, context: Context): File? {
        if (file.extension != "png") {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Compressing non-PNG files is not allowed!", Toast.LENGTH_SHORT).show()
            }
            return null
        }
        val pngQuant = LibPngQuant()
        val newFile = File(context.cacheDir, file.name)
        pngQuant.pngQuantFile(file, newFile, quality - 10, quality + 10)
        return newFile
    }
}