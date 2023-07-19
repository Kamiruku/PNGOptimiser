package com.kamiruku.pngoptimiser

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.kamiruku.pngoptimiser.compressor.constraint.qualityFormat
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import java.io.File
import java.lang.Integer.max
import java.lang.Integer.min


interface CompressionLogic {
    suspend fun compress(file: File, quality: Int, context: Context): File?
}

class OriginalFile: CompressionLogic {
    override suspend fun compress(file: File, quality: Int, context: Context): File {
        val deferred = CompletableDeferred<File>()

        val newFile = File(context.cacheDir, file.name)
        if (newFile.exists())
            newFile.delete()
        file.copyTo(newFile)

        deferred.complete(newFile)
        return deferred.await()
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

        Log.i("PNGQuant", "File: ${file.name} \t Quality: $quality")

        val pngQuant = LibPngQuant()
        val newFile = File(context.cacheDir, file.name)
        pngQuant.pngQuantFile(file, newFile, max(quality - 10, 0), min(quality + 10, 100))

        return newFile
    }
}

class LubanCompress: CompressionLogic {
    override suspend fun compress(file: File, quality: Int, context: Context): File? {
        val deferred = CompletableDeferred<File?>()

        val gear = when (quality) {
            0 -> Luban.FIRST_GEAR
            1-> Luban.THIRD_GEAR
            else -> Luban.THIRD_GEAR
        }

        Log.i("Luban", "File: ${file.name} \t Quality: $quality")

        Luban.compress(file, context.cacheDir)
            .putGear(gear)
            .setCompressFormat(Bitmap.CompressFormat.JPEG)
            .launch(
                object: OnCompressListener {
                    override fun onStart() {
                        Log.i("Luban", "${file.name} has started.")
                    }

                    override fun onSuccess(file: File?) {
                        deferred.complete(file)
                    }

                    override fun onError(e: Throwable?) {
                        e?.printStackTrace()
                        Toast.makeText(context, "An error has occurred. Check stack trace for more information.", Toast.LENGTH_SHORT).show()
                        deferred.complete(null)
                    }
                }
            )
        return deferred.await()
    }
}