package com.clearshot.ai.processing

import com.clearshot.ai.camera.CameraMode
import java.io.File

/**
 * Contract for computational image processing pipelines.
 */
interface ImageProcessor {
    /**
     * Processes an input photograph file and produces an enhanced output file.
     * Guaranteed never to overwrite the input file.
     */
    suspend fun processImage(
        inputFile: File,
        onProgress: (progress: Float, message: String) -> Unit = { _, _ -> }
    ): Result<File>

    /**
     * Extended processing returning detailed analysis metrics, duration and custom tuning.
     */
    suspend fun processWithDetails(
        inputFile: File,
        cameraMode: CameraMode = CameraMode.PHOTO,
        customSharpen: Double? = null,
        customDenoise: Double? = null,
        customClahe: Double? = null,
        horizonRollDegrees: Float? = null,
        digitalZoomRatio: Float = 1.0f,
        aspectRatio: String = "FULL",
        panOffsetX: Float = 0f,
        panOffsetY: Float = 0f,
        activeCameraId: String = "0",
        onProgress: (progress: Float, message: String) -> Unit = { _, _ -> }
    ): Result<PipelineResult>

    /**
     * Computational multi-frame burst fusion pipeline.
     */
    suspend fun processMultiFrame(
        inputFiles: List<File>,
        cameraMode: CameraMode = CameraMode.PHOTO,
        customSharpen: Double? = null,
        customDenoise: Double? = null,
        customClahe: Double? = null,
        horizonRollDegrees: Float? = null,
        digitalZoomRatio: Float = 1.0f,
        aspectRatio: String = "FULL",
        panOffsetX: Float = 0f,
        panOffsetY: Float = 0f,
        activeCameraId: String = "0",
        onProgress: (progress: Float, message: String) -> Unit = { _, _ -> }
    ): Result<PipelineResult>
}
