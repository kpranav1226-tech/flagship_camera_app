package com.clearshot.ai.processing

import android.content.Context
import com.clearshot.ai.camera.CameraMode
import java.io.File

/**
 * Concrete ImageProcessor leveraging OpenCV-powered multi-frame EnhancementPipeline.
 */
class OpenCVImageProcessor(
    private val context: Context,
    private val pipeline: EnhancementPipeline = EnhancementPipeline()
) : ImageProcessor {

    override suspend fun processImage(
        inputFile: File,
        onProgress: (progress: Float, message: String) -> Unit
    ): Result<File> {
        val result = processWithDetails(inputFile, onProgress = onProgress)
        return result.map { it.enhancedFile }
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
    ): Result<PipelineResult> {
        val outputFile = File.createTempFile("clearshot_enhanced_", ".jpg", context.cacheDir)
        return pipeline.execute(
            inputFile = inputFile,
            outputFile = outputFile,
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
    ): Result<PipelineResult> {
        val outputFile = File.createTempFile("clearshot_hdr_enhanced_", ".jpg", context.cacheDir)
        return pipeline.executeMultiFrame(
            inputFiles = inputFiles,
            outputFile = outputFile,
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
    }
}
