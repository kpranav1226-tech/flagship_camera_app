package com.clearshot.ai.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Baseline AI enhancer ready to be hooked to TensorFlow Lite / PyTorch Mobile / ONNX Runtime.
 * Kept lightweight in v1.0 to guarantee immediate stability on Samsung Galaxy M15 5G.
 */
class DefaultAIEnhancer : AIEnhancer {

    override suspend fun enhance(inputFile: File): Result<File> = withContext(Dispatchers.Default) {
        Log.d(TAG, "Default AI enhancer bypassed in v1.0 baseline.")
        Result.success(inputFile)
    }

    companion object {
        private const val TAG = "DefaultAIEnhancer"
    }
}
