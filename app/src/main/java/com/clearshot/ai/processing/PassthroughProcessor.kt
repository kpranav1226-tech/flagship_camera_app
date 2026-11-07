package com.clearshot.ai.processing

import android.util.Log
import com.clearshot.ai.camera.CameraMode
import com.clearshot.ai.processing.analysis.QualityAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lightweight baseline processor.
 */
class PassthroughProcessor : ImageProcessor {

    override suspend fun processImage(
        inputFile: File,
        onProgress: (progress: Float, message: String) -> Unit
    ): Result<File> = withContext(Dispatchers.Default) {
        if (!inputFile.exists() || inputFile.length() == 0L) {
            return@withContext Result.failure(
                IllegalArgumentException("Input photo file does not exist or is empty")
            )
        }
        Log.d(TAG, "Image passed through processor: ${inputFile.name}")
        Result.success(inputFile)
    }

    override suspend fun processWithDetails(
        inputFile: File,
        cameraMode: CameraMode,
        customSharpen: Double?,
        customDenoise: Double?,
        customClahe: Double?,
        horizonRollDegrees: Float?,
        digitalZoomRatio: Float,
        aspectRatio: String,
        panOffsetX: Float,
        panOffsetY: Float,
        activeCameraId: String,
        onProgress: (progress: Float, message: String) -> Unit
    ): Result<PipelineResult> = withContext(Dispatchers.Default) {
        val analysis = QualityAnalysisResult(
            sharpnessScore = 500.0,
            noiseScore = 3.0,
            underexposureRatio = 0.0f,
            overexposureRatio = 0.0f,
            meanLuminance = 128.0,
            recommendedDenoiseSigma = 20.0,
            recommendedSharpenAmount = 0.6,
            recommendedClaheClip = 1.4,
            shouldApplyClahe = false
        )
        Result.success(
            PipelineResult(
                enhancedFile = inputFile,
                analysis = analysis,
                durationMs = 0L,
                originalWidth = 1080,
                originalHeight = 1920,
                burstFramesMerged = 1,
                cameraMode = cameraMode
            )
        )
    }

    override suspend fun processMultiFrame(
        inputFiles: List<File>,
        cameraMode: CameraMode,
        customSharpen: Double?,
        customDenoise: Double?,
        customClahe: Double?,
        horizonRollDegrees: Float?,
        digitalZoomRatio: Float,
        aspectRatio: String,
        panOffsetX: Float,
        panOffsetY: Float,
        activeCameraId: String,
        onProgress: (progress: Float, message: String) -> Unit
    ): Result<PipelineResult> = processWithDetails(
        inputFile = inputFiles.first(),
        cameraMode = cameraMode,
        customSharpen = customSharpen,
        customDenoise = customDenoise,
        customClahe = customClahe,
        horizonRollDegrees = horizonRollDegrees,
        digitalZoomRatio = digitalZoomRatio,
        aspectRatio = aspectRatio,
        panOffsetX = panOffsetX,
        panOffsetY = panOffsetY,
        activeCameraId = activeCameraId,
        onProgress = onProgress
    )

    companion object {
        private const val TAG = "PassthroughProcessor"
    }
}
