package com.clearshot.ai

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class ClearShotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("c++_shared")
            Log.i(TAG, "libc++_shared loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Note: Explicit c++_shared load: ${e.message}")
        }

        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV 4.10.0 initialized successfully via OpenCVLoader.initLocal().")
        } else {
            Log.e(TAG, "Failed to initialize OpenCV 4.10.0.")
        }
    }

    companion object {
        const val TAG = "ClearShotAI"
    }
}
