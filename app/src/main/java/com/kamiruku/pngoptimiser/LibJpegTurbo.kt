package com.kamiruku.pngoptimiser

class LibJpegTurbo {
    companion object {
        init {
            System.loadLibrary("turbo-jpegandroid")
        }
    }
}