package com.kamiruku.pngoptimiser

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import com.kamiruku.pngoptimiser.compressor.constraint.qualityFormat
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Integer.max
import java.lang.Integer.min

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
            qualityFormat(Bitmap.CompressFormat.JPEG, quality)
            destination(newFile)
        }
        return newFile
    }
}

class DefaultPNG: CompressionLogic {
    override suspend fun compress(file: File, quality: Int, context: Context): File {
        val newFile = File(context.cacheDir, "${file.nameWithoutExtension}.png")
        Compressor.compress(context, file) {
            qualityFormat(Bitmap.CompressFormat.PNG, quality)
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
        pngQuant.pngQuantFile(file, newFile, max(quality - 10, 0), min(quality + 10, 100))
        return newFile
    }
}