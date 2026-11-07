package com.clearshot.ai.processing

import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.clearshot.ai.camera.CameraMode
import com.clearshot.ai.processing.analysis.ImageQualityAnalyzer
import com.clearshot.ai.processing.analysis.QualityAnalysisResult
import com.clearshot.ai.processing.color.TrueColorEngine
import com.clearshot.ai.processing.filters.ContrastEnhancer
import com.clearshot.ai.processing.filters.HorizonCorrector
import com.clearshot.ai.processing.filters.NoiseReducer
import com.clearshot.ai.processing.filters.SharpnessEnhancer
import com.clearshot.ai.processing.modes.DocumentProcessor
import com.clearshot.ai.processing.modes.MacroDetailProcessor
import com.clearshot.ai.processing.modes.NightSightProcessor
import com.clearshot.ai.processing.modes.PortraitBokehProcessor
import com.clearshot.ai.processing.multiframe.ExposureFusion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.imgcodecs.Imgcodecs
import java.io.File

/**
 * Result data holder representing the outcome of the computational pipeline.
 */
data class PipelineResult(
    val enhancedFile: File,
    val analysis: QualityAnalysisResult,
    val durationMs: Long,
    val originalWidth: Int,
    val originalHeight: Int,
    val burstFramesMerged: Int = 1,
    val cameraMode: CameraMode = CameraMode.PHOTO,
    val motionDetected: Boolean = false,
    val motionRatio: Float = 0.0f,
    val sharpestFrameIndex: Int = 0,
    val horizonCorrected: Boolean = false,
    val horizonCorrectionAngle: Float = 0f
)

/**
 * Orchestrator coordinating quality analysis, multi-frame burst fusion,
 * specialized scene mode processors (Night Sight, Document, Macro),
 * edge-preserving denoising, halo-controlled unsharp masking, and natural CLAHE contrast adjustment.
 */
class EnhancementPipeline(
    private val analyzer: ImageQualityAnalyzer = ImageQualityAnalyzer(),
    private val noiseReducer: NoiseReducer = NoiseReducer(),
    private val sharpnessEnhancer: SharpnessEnhancer = SharpnessEnhancer(),
    private val contrastEnhancer: ContrastEnhancer = ContrastEnhancer(),
    private val exposureFusion: ExposureFusion = ExposureFusion(),
    private val nightSightProcessor: NightSightProcessor = NightSightProcessor(),
    private val documentProcessor: DocumentProcessor = DocumentProcessor(),
    private val macroDetailProcessor: MacroDetailProcessor = MacroDetailProcessor(),
    private val portraitBokehProcessor: PortraitBokehProcessor = PortraitBokehProcessor(),
    private val trueColorEngine: TrueColorEngine = TrueColorEngine(),
    private val horizonCorrector: HorizonCorrector = HorizonCorrector()
) {

    /**
     * Executes single-frame or multi-frame computational clarity pipeline.
     */
    suspend fun execute(
        inputFile: File,
        outputFile: File,
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
    ): Result<PipelineResult> = executeMultiFrame(
        inputFiles = listOf(inputFile),
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

    /**
     * Executes computational fusion on multiple bracketed burst frames.
     */
    suspend fun executeMultiFrame(
        inputFiles: List<File>,
        outputFile: File,
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
    ): Result<PipelineResult> = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        var currentMat: Mat? = null
        val decodedMats = mutableListOf<Mat>()
        var motionDetected = false
        var motionRatio = 0f
        var sharpestIndex = 0
        var framesMerged = 1
        var isHorizonCorrected = false
        var appliedHorizonAngle = 0f

        try {
            if (inputFiles.isEmpty() || !inputFiles[0].exists()) {
                return@withContext Result.failure(IllegalArgumentException("Invalid input frames"))
            }

            val primaryFile = inputFiles[0]
            val exif = ExifInterface(primaryFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            if (inputFiles.size == 1) {
                onProgress(0.10f, "Decoding JPEG with full resolution...")
                val rawMat = Imgcodecs.imread(
                    primaryFile.absolutePath,
                    Imgcodecs.IMREAD_COLOR or Imgcodecs.IMREAD_IGNORE_ORIENTATION
                )
                if (rawMat.empty()) {
                    return@withContext Result.failure(IllegalStateException("Failed to decode image"))
                }
                currentMat = applyExifOrientation(rawMat, orientation)
                if (currentMat !== rawMat) {
                    rawMat.release()
                }
            } else {
                onProgress(0.12f, "Aligning and detecting motion across ${inputFiles.size} frames...")
                for (file in inputFiles) {
                    val frameExif = ExifInterface(file.absolutePath)
                    val frameOrientation = frameExif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    val mat = Imgcodecs.imread(
                        file.absolutePath,
                        Imgcodecs.IMREAD_COLOR or Imgcodecs.IMREAD_IGNORE_ORIENTATION
                    )
                    if (!mat.empty()) {
                        val oriented = applyExifOrientation(mat, frameOrientation)
                        if (oriented !== mat) {
                            mat.release()
                        }
                        decodedMats.add(oriented)
                    }
                }

                if (cameraMode == CameraMode.NIGHT) {
                    onProgress(0.15f, "Aligning, rejecting motion blur & temporal stacking ${inputFiles.size} night frames...")
                    val nightMat = Mat()
                    val nightResult = nightSightProcessor.processMultiFrame(decodedMats, nightMat)
                    currentMat = nightMat
                    framesMerged = nightResult.framesAligned
                    decodedMats.forEach { it.release() }
                    decodedMats.clear()
                } else {
                    val deghostResult = exposureFusion.fuseWithDeghost(decodedMats)
                    currentMat = deghostResult.fusedMat
                    motionDetected = deghostResult.motionDetected
                    motionRatio = deghostResult.motionRatio
                    sharpestIndex = deghostResult.sharpestFrameIndex
                    framesMerged = deghostResult.framesMerged
                    decodedMats.forEach { it.release() }
                    decodedMats.clear()
                }
            }

            val origWidth = currentMat.cols()
            val origHeight = currentMat.rows()
            Log.i(TAG, "Working frame: ${origWidth}x${origHeight} px across ${inputFiles.size} frame(s), mode: ${cameraMode.name}")

            // 1. True Color Engine: Calibrate white balance, neutralize tints, protect human skin tones & match lens profiles
            onProgress(0.20f, "Calibrating true colors & protecting skin tones...")
            val calibratedMat = Mat()
            val colorDiag = trueColorEngine.process(currentMat, calibratedMat, activeCameraId)
            currentMat.release()
            currentMat = calibratedMat

            // 2. Comprehensive Multi-Metric Image Quality Analysis (Tenengrad, LapVar, SNR, DR, Clipping, Entropy)
            onProgress(0.26f, "Analyzing multi-metric sharpness, SNR & dynamic range...")
            val analysis = analyzer.analyze(currentMat)

            // High Digital Zoom Crop (> 8.0X native limit):
            // Center crop on the clean, unsharpened frame first using Lanczos-4 resampling
            // to avoid magnifying noise grain and unsharp halos by up to 12.5x.
            if (digitalZoomRatio > 8.0f) {
                onProgress(0.30f, "Synthesizing %.1fX high-detail crop (Lanczos-4)...".format(digitalZoomRatio))
                val cropFactor = (digitalZoomRatio / 8.0f).coerceIn(1.0f, 12.5f)
                val curW = currentMat.cols()
                val curH = currentMat.rows()
                val cropW = (curW / cropFactor).toInt().coerceAtLeast(64)
                val cropH = (curH / cropFactor).toInt().coerceAtLeast(48)
                val maxShiftX = (curW - cropW).toFloat()
                val maxShiftY = (curH - cropH).toFloat()
                val startX = (((curW - cropW) / 2f) + (panOffsetX * maxShiftX)).toInt().coerceIn(0, curW - cropW)
                val startY = (((curH - cropH) / 2f) + (panOffsetY * maxShiftY)).toInt().coerceIn(0, curH - cropH)

                val roi = org.opencv.core.Rect(startX, startY, cropW, cropH)
                val croppedMat = Mat(currentMat, roi)
                val scaledMat = Mat()
                org.opencv.imgproc.Imgproc.resize(
                    croppedMat,
                    scaledMat,
                    org.opencv.core.Size(curW.toDouble(), curH.toDouble()),
                    0.0,
                    0.0,
                    org.opencv.imgproc.Imgproc.INTER_LANCZOS4
                )
                croppedMat.release()
                currentMat.release()
                currentMat = scaledMat
            }

            // 3. Mode-Specific Computational Photography Engine
            when (cameraMode) {
                CameraMode.PORTRAIT -> {
                    onProgress(0.50f, "Simulating optical disc bokeh & fine hair edge matting...")
                    val portraitMat = Mat()
                    portraitBokehProcessor.process(currentMat, portraitMat, blurStrength = 0.75f)
                    currentMat.release()
                    currentMat = portraitMat

                    // Refine subject micro-contrast and facial features
                    onProgress(0.78f, "Enhancing facial clarity & iris micro-contrast...")
                    val sharpMat = Mat()
                    sharpnessEnhancer.enhanceSharpness(currentMat, sharpMat, amount = 0.40)
                    currentMat.release()
                    currentMat = sharpMat
                }

                CameraMode.NIGHT -> {
                    // For single-frame capture, run single-frame Night Sight tone mapping & chroma denoise
                    if (inputFiles.size <= 1) {
                        onProgress(0.50f, "Applying Night Sight shadow synthesis & chroma denoising...")
                        val nightMat = Mat()
                        nightSightProcessor.process(currentMat, nightMat)
                        currentMat.release()
                        currentMat = nightMat
                    }

                    // Mild halo-controlled sharpening for night details
                    onProgress(0.75f, "Sharpening low-light textures...")
                    val sharpMat = Mat()
                    sharpnessEnhancer.enhanceSharpness(currentMat, sharpMat, amount = 0.40)
                    currentMat.release()
                    currentMat = sharpMat
                }

                CameraMode.DOCUMENT -> {
                    onProgress(0.50f, "Normalizing document lighting & enhancing text strokes...")
                    val docMat = Mat()
                    documentProcessor.process(currentMat, docMat)
                    currentMat.release()
                    currentMat = docMat
                }

                CameraMode.MACRO -> {
                    onProgress(0.50f, "Enhancing micro-textures and close-up detail...")
                    val macroMat = Mat()
                    macroDetailProcessor.process(currentMat, macroMat)
                    currentMat.release()
                    currentMat = macroMat
                }

                CameraMode.PHOTO, CameraMode.PRO -> {
                    // Adaptive Zoom Processing Strategy based on magnification:
                    // 0.5X - 2X: Preserve natural detail and colour (subtle denoise, gentle sharpen 0.35x, natural CLAHE 1.15)
                    // 2X - 5X: Optimize sensor crop and detail preservation (sigma 16, sharpen 0.45x, CLAHE 1.25)
                    // 5X - 10X: Moderate denoise (sigma 22), controlled sharpen (0.35x), no CLAHE
                    // 10X - 30X: Low noise, edge recovery (sigma 26, sharpen 0.22x), no CLAHE
                    // 30X - 100X: Stability and noise control, zero harsh ringing (sigma 32, sharpen 0.15x), no CLAHE
                    val (zoomSigma, zoomSharpen, zoomClahe) = when {
                        digitalZoomRatio <= 2.0f -> Triple(12.0, 0.35, 1.15)
                        digitalZoomRatio <= 5.0f -> Triple(16.0, 0.45, 1.25)
                        digitalZoomRatio <= 10.0f -> Triple(22.0, 0.35, null)
                        digitalZoomRatio <= 30.0f -> Triple(26.0, 0.22, null)
                        else -> Triple(32.0, 0.15, null) // 30x - 100x
                    }

                    val denoiseSigma = customDenoise ?: zoomSigma
                    val sharpenAmount = customSharpen ?: zoomSharpen
                    val claheClip = customClahe ?: zoomClahe

                    // Edge-preserving noise reduction
                    onProgress(0.45f, "Applying edge-preserving denoising (σ %.0f)...".format(denoiseSigma))
                    val denoisedMat = Mat()
                    noiseReducer.reduceNoise(
                        srcBgr = currentMat,
                        dstBgr = denoisedMat,
                        sigma = denoiseSigma
                    )
                    currentMat.release()
                    currentMat = denoisedMat

                    // Controlled sharpness enhancement
                    onProgress(0.68f, "Applying halo-controlled unsharp masking (%.2fx)...".format(sharpenAmount))
                    val sharpenedMat = Mat()
                    sharpnessEnhancer.enhanceSharpness(
                        srcBgr = currentMat,
                        dstBgr = sharpenedMat,
                        amount = sharpenAmount
                    )
                    currentMat.release()
                    currentMat = sharpenedMat

                    // Natural Contrast Enhancement (CLAHE on Lightness only)
                    if (claheClip != null) {
                        onProgress(0.85f, "Adjusting natural local contrast (CLAHE)...")
                        val contrastMat = Mat()
                        contrastEnhancer.enhanceContrast(
                            srcBgr = currentMat,
                            dstBgr = contrastMat,
                            clipLimit = claheClip
                        )
                        currentMat.release()
                        currentMat = contrastMat
                    }
                }

                CameraMode.VIDEO, CameraMode.PRO_VIDEO -> {
                    // Video frames are recorded directly via VideoCapture/MediaCodec
                }
            }

            // Auto-Horizon Straightening (if enabled and tilted between 0.4° and 15°)
            if (horizonRollDegrees != null && kotlin.math.abs(horizonRollDegrees) >= 0.4f) {
                onProgress(0.88f, "Auto-straightening horizon (%.1f°)...".format(-horizonRollDegrees))
                val straightenedMat = Mat()
                val applied = horizonCorrector.correct(currentMat, straightenedMat, horizonRollDegrees)
                if (applied) {
                    currentMat.release()
                    currentMat = straightenedMat
                    isHorizonCorrected = true
                    appliedHorizonAngle = horizonRollDegrees
                } else {
                    straightenedMat.release()
                }
            }

            // Aspect Ratio Cropping: FULL, 3:4 / 4:3, 1:1, 9:16 / 16:9
            if (!aspectRatio.equals("FULL", ignoreCase = true)) {
                onProgress(0.91f, "Framing photograph to $aspectRatio aspect ratio...")
                val croppedRatioMat = applyAspectRatioCrop(currentMat, aspectRatio)
                if (croppedRatioMat !== currentMat) {
                    currentMat.release()
                    currentMat = croppedRatioMat
                }
            }

            val finalWidth = currentMat.cols()
            val finalHeight = currentMat.rows()

            // High-efficiency, perceptually identical JPEG export:
            // Quality 90 + optimal Huffman entropy tree + progressive DCT scans
            // Reduces output file size by 50-60% while preserving visual clarity & textures
            onProgress(0.92f, "Encoding high-clarity photograph...")
            val params = MatOfInt(
                Imgcodecs.IMWRITE_JPEG_QUALITY, 90,
                Imgcodecs.IMWRITE_JPEG_OPTIMIZE, 1,
                Imgcodecs.IMWRITE_JPEG_PROGRESSIVE, 1
            )
            val written = Imgcodecs.imwrite(outputFile.absolutePath, currentMat, params)
            params.release()
            currentMat.release()
            currentMat = null

            if (!written || !outputFile.exists()) {
                return@withContext Result.failure(IllegalStateException("Failed to write enhanced image"))
            }

            // Copy EXIF metadata from sharpest reference source to enhanced output
            val bestSourceFile = inputFiles.getOrElse(sharpestIndex) { primaryFile }
            copyExifMetadata(bestSourceFile, outputFile, finalWidth, finalHeight)

            val duration = System.currentTimeMillis() - startTime
            onProgress(1.0f, "Complete (${duration}ms)")

            Result.success(
                PipelineResult(
                    enhancedFile = outputFile,
                    analysis = analysis,
                    durationMs = duration,
                    originalWidth = origWidth,
                    originalHeight = origHeight,
                    burstFramesMerged = framesMerged,
                    cameraMode = cameraMode,
                    motionDetected = motionDetected,
                    motionRatio = motionRatio,
                    sharpestFrameIndex = sharpestIndex,
                    horizonCorrected = isHorizonCorrected,
                    horizonCorrectionAngle = appliedHorizonAngle
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Computational pipeline failed: ${e.message}", e)
            currentMat?.release()
            decodedMats.forEach { it.release() }
            Result.failure(e)
        }
    }

    /**
     * Applies exact single-pass geometric transformation for all 8 standard EXIF orientations.
     * Guaranteed to produce upright pixel coordinates without double-rotation.
     */
    private fun applyExifOrientation(src: Mat, orientation: Int): Mat {
        val dst = Mat()
        return when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                Core.flip(src, dst, 1) // Horizontal mirror
                dst
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                Core.rotate(src, dst, Core.ROTATE_180)
                dst
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                Core.flip(src, dst, 0) // Vertical mirror
                dst
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                Core.transpose(src, dst)
                val flipped = Mat()
                Core.flip(dst, flipped, 1)
                dst.release()
                flipped
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                Core.rotate(src, dst, Core.ROTATE_90_CLOCKWISE)
                dst
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                Core.transpose(src, dst)
                val flipped = Mat()
                Core.flip(dst, flipped, 0)
                dst.release()
                flipped
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                Core.rotate(src, dst, Core.ROTATE_90_COUNTERCLOCKWISE)
                dst
            }
            else -> {
                // ORIENTATION_NORMAL or unspecified: return unchanged
                dst.release()
                src
            }
        }
    }

    private fun copyExifMetadata(sourceFile: File, targetFile: File, width: Int, height: Int) {
        try {
            val srcExif = ExifInterface(sourceFile.absolutePath)
            val dstExif = ExifInterface(targetFile.absolutePath)

            val tagsToCopy = arrayOf(
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_WHITE_BALANCE
            )

            for (tag in tagsToCopy) {
                srcExif.getAttribute(tag)?.let { value ->
                    dstExif.setAttribute(tag, value)
                }
            }

            // The image matrix has been physically oriented upright; tag orientation is NORMAL
            dstExif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL.toString()
            )
            // Ensure width & length reflect actual upright dimensions
            dstExif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
            dstExif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
            dstExif.saveAttributes()
        } catch (e: Exception) {
            Log.w(TAG, "Could not copy EXIF metadata: ${e.message}")
        }
    }

    /**
     * Center crops the Mat to the desired aspect ratio without stretching or distortion.
     */
    private fun applyAspectRatioCrop(mat: Mat, ratio: String): Mat {
        val w = mat.cols()
        val h = mat.rows()
        val isLandscape = w > h
        val targetRatio: Double = when (ratio.uppercase()) {
            "1:1" -> 1.0
            "3:4", "4:3" -> if (isLandscape) 4.0 / 3.0 else 3.0 / 4.0
            "9:16", "16:9" -> if (isLandscape) 16.0 / 9.0 else 9.0 / 16.0
            "FULL" -> return mat
            else -> return mat
        }

        val currentRatio = w.toDouble() / h.toDouble()
        if (kotlin.math.abs(currentRatio - targetRatio) < 0.02) {
            return mat
        }

        var cropW = w
        var cropH = h
        if (currentRatio > targetRatio) {
            // Frame is wider than target ratio: trim sides
            cropW = (h * targetRatio).toInt().coerceIn(1, w)
        } else {
            // Frame is taller than target ratio: trim top and bottom
            cropH = (w / targetRatio).toInt().coerceIn(1, h)
        }

        val startX = ((w - cropW) / 2).coerceIn(0, w - cropW)
        val startY = ((h - cropH) / 2).coerceIn(0, h - cropH)
        val roi = org.opencv.core.Rect(startX, startY, cropW, cropH)
        val roiMat = Mat(mat, roi)
        val dstMat = Mat()
        roiMat.copyTo(dstMat)
        roiMat.release()
        return dstMat
    }

    companion object {
        private const val TAG = "EnhancementPipeline"
    }
}
