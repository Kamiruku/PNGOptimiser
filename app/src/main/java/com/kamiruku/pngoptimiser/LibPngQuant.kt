package com.kamiruku.pngoptimiser

import java.io.File

class LibPngQuant {
    fun pngQuantFile(inputFile: File?, outputFile: File?): Boolean {
        //Use default quality in the windows batch file bundled with pngQuant
        return pngQuantFile(inputFile, outputFile, 50, 100)
    }

    fun pngQuantFile(
        inputFile: File?,
        outputFile: File?,
        minQuality: Int,
        maxQuality: Int
    ): Boolean {
        //Use lowest speed to get the best quality
        return pngQuantFile(inputFile, outputFile, minQuality, maxQuality, 1)
    }

    fun pngQuantFile(
        inputFile: File?,
        outputFile: File?,
        minQuality: Int,
        maxQuality: Int,
        speed: Int
    ): Boolean {
        //Use the default dither value.
        return pngQuantFile(inputFile, outputFile, minQuality, maxQuality, speed, 1f)
    }

    private fun pngQuantFile(
        inputFile: File?,
        outputFile: File?,
        minQuality: Int = 50,
        maxQuality: Int = 100,
        speed: Int = 1,
        floydDitherAmount: Float = 1f
    ): Boolean {
        if (inputFile == null) throw NullPointerException()
        require(inputFile.exists())
        if (outputFile == null) throw NullPointerException()
        require(outputFile.length() == 0L)
        require(!(maxQuality < 0 || maxQuality > 100))
        require(!(minQuality < 0 || minQuality > 100))
        require(maxQuality >= minQuality)
        require(!(speed < 1 || speed > 11))
        require(!(floydDitherAmount < 0f || floydDitherAmount > 1f))
        val inputFilename = inputFile.absolutePath
        val outputFilename = outputFile.absolutePath
        return nativePngQuantFile(
            inputFilename,
            outputFilename,
            minQuality,
            maxQuality,
            speed,
            floydDitherAmount
        )
    }

    companion object {
        init {
            System.loadLibrary("pngquantandroid")
        }

        private external fun nativePngQuantFile(
            inputFilename: String,
            outputFilename: String,
            minQuality: Int,
            maxQuality: Int,
            speed: Int,
            floydDitherAmount: Float
        ): Boolean
    }
}