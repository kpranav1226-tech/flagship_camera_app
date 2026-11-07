package com.clearshot.ai.processing.multiframe

import android.util.Log
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.photo.Photo
import kotlin.math.abs
import kotlin.math.pow

/**
 * Result data holder for motion-aware de-ghosting fusion.
 */
data class DeghostResult(
    val fusedMat: Mat,
    val motionDetected: Boolean,
    val motionRatio: Float,
    val sharpestFrameIndex: Int,
    val framesMerged: Int
)

/**
 * Computational motion detection and anti-ghosting fusion engine.
 * Eliminates motion blur, double-edges, and ghost silhouettes when photographing moving subjects.
 *
 * Algorithm:
 * 1. Lucky Imaging Reference Selection: Selects the sharpest, well-exposed frame as the anchor.
 * 2. 2D Phase Correlation Micro-Alignment: Sub-pixel translation compensation for hand tremor.
 * 3. Exposure-Normalized Pixel Motion Detection: Absolute difference thresholding in luminance space.
 * 4. Morphological Edge-Dilation & Soft Blending: Fully encapsulates moving contours.
 * 5. Anti-Ghosting Frame Replacement: Substitutes moving regions in secondary frames with the sharp anchor.
 * 6. Global Motion Fallback: Automatically falls back to single-frame clarity if scene motion > 35%.
 */
class MotionDeghostingFusion(
    private val frameAligner: FrameAligner = FrameAligner()
) {

    /**
     * Fuses multi-frame burst images with full anti-ghosting and moving subject freezing.
     * @param frames List of decoded BGR Mats.
     * @return DeghostResult containing fused Mat and motion diagnostic metrics.
     */
    fun fuse(frames: List<Mat>): DeghostResult {
        require(frames.isNotEmpty()) { "Frames list must not be empty" }

        if (frames.size == 1) {
            val single = Mat()
            frames[0].copyTo(single)
            return DeghostResult(
                fusedMat = single,
                motionDetected = false,
                motionRatio = 0f,
                sharpestFrameIndex = 0,
                framesMerged = 1
            )
        }

        // 1. Lucky Imaging: Select the sharpest, well-exposed frame as anchor
        val refIndex = selectSharpestReference(frames)
        val refFrame = frames[refIndex]
        Log.i(TAG, "Lucky imaging: Selected frame #$refIndex as sharpest reference anchor")

        // 2. Prepare reference luminance for motion comparison
        val refGray = Mat()
        Imgproc.cvtColor(refFrame, refGray, Imgproc.COLOR_BGR2GRAY)
        val refMeanLuma = Core.mean(refGray).`val`[0]
        val refEq = Mat()
        Imgproc.equalizeHist(refGray, refEq)
        val totalPixels = (refFrame.rows() * refFrame.cols()).toDouble()

        val alignedFrames = mutableListOf<Mat>()
        var globalMaxMotionRatio = 0f
        var anyMotionDetected = false

        try {
            // Structuring element for morphological dilation (encapsulates motion contours)
            val dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(7.0, 7.0))

            for (i in frames.indices) {
                if (i == refIndex) {
                    alignedFrames.add(refFrame)
                    continue
                }

                // Sub-pixel translation alignment against reference anchor
                val aligned = frameAligner.align(refFrame, frames[i])

                // Dense motion detection in exposure-invariant rank-equalized space
                val targetGray = Mat()
                Imgproc.cvtColor(aligned, targetGray, Imgproc.COLOR_BGR2GRAY)
                val targetMeanLuma = Core.mean(targetGray).`val`[0]

                val targetEq = Mat()
                Imgproc.equalizeHist(targetGray, targetEq)

                // Absolute difference map on histogram-equalized space eliminates false motion from EV bracketing
                val diff = Mat()
                Core.absdiff(refEq, targetEq, diff)
                targetEq.release()

                // 5x5 Gaussian blur to eliminate sensor shot grain
                val blurredDiff = Mat()
                Imgproc.GaussianBlur(diff, blurredDiff, Size(5.0, 5.0), 1.5)

                // Binary threshold: pixels with difference > 28 indicate true scene motion
                val binaryMotion = Mat()
                Imgproc.threshold(blurredDiff, binaryMotion, 28.0, 255.0, Imgproc.THRESH_BINARY)

                // Morphological dilation to cover subject motion blur tails and boundary fringes
                val dilatedMotion = Mat()
                Imgproc.dilate(binaryMotion, dilatedMotion, dilateKernel)

                val motionCount = Core.countNonZero(dilatedMotion)
                val frameMotionRatio = (motionCount / totalPixels).toFloat()

                if (frameMotionRatio > globalMaxMotionRatio) {
                    globalMaxMotionRatio = frameMotionRatio
                }

                if (frameMotionRatio > 0.008f) { // More than 0.8% of scene moved
                    anyMotionDetected = true
                    Log.d(TAG, "Frame #$i: Subject motion detected across ${(frameMotionRatio * 100).toInt()}% of frame")

                    // Soft feathering blur for seamless transition between moving subject & static background
                    val softMask = Mat()
                    Imgproc.GaussianBlur(dilatedMotion, softMask, Size(7.0, 7.0), 2.2)

                    // Normalized 3-channel alpha blending: aligned * (1 - alpha) + scaledRefFrame * alpha
                    val softMask3C = Mat()
                    Imgproc.cvtColor(softMask, softMask3C, Imgproc.COLOR_GRAY2BGR)
                    softMask.release()

                    val alpha32 = Mat()
                    softMask3C.convertTo(alpha32, CvType.CV_32FC3, 1.0 / 255.0)
                    softMask3C.release()

                    val expRatio = targetMeanLuma / (refMeanLuma + 1e-5)
                    val aligned32 = Mat()
                    val scaledRef32 = Mat()
                    aligned.convertTo(aligned32, CvType.CV_32FC3)
                    refFrame.convertTo(scaledRef32, CvType.CV_32FC3, expRatio)

                    val diff32 = Mat()
                    Core.subtract(scaledRef32, aligned32, diff32)
                    val scaledDiff = Mat()
                    Core.multiply(diff32, alpha32, scaledDiff)
                    val blended32 = Mat()
                    Core.add(aligned32, scaledDiff, blended32)

                    val deghosted = Mat()
                    blended32.convertTo(deghosted, CvType.CV_8U)

                    alignedFrames.add(deghosted)

                    // Cleanup intermediate blending Mats
                    alpha32.release()
                    aligned32.release()
                    scaledRef32.release()
                    diff32.release()
                    scaledDiff.release()
                    blended32.release()
                    aligned.release()
                } else {
                    alignedFrames.add(aligned)
                }

                // Cleanup per-frame detection Mats
                targetGray.release()
                diff.release()
                blurredDiff.release()
                binaryMotion.release()
                dilatedMotion.release()
            }

            dilateKernel.release()
            refGray.release()
            refEq.release()

            // 3. Global Motion Fallback Check
            if (globalMaxMotionRatio > 0.35f) {
                Log.w(
                    TAG,
                    "Excessive scene motion detected (%.1f%% > 35%%). Safely falling back to sharpest single frame anchor to guarantee zero blur."
                        .format(globalMaxMotionRatio * 100)
                )

                // Cleanup intermediate aligned frames
                for (i in alignedFrames.indices) {
                    if (alignedFrames[i] !== refFrame) {
                        alignedFrames[i].release()
                    }
                }

                val fallbackMat = Mat()
                refFrame.copyTo(fallbackMat)
                return DeghostResult(
                    fusedMat = fallbackMat,
                    motionDetected = true,
                    motionRatio = globalMaxMotionRatio,
                    sharpestFrameIndex = refIndex,
                    framesMerged = 1
                )
            }

            // 4. Mertens Multi-Exposure Fusion on De-ghosted Stack
            val merger = Photo.createMergeMertens()
            // Configure natural, halo-free computational HDR weights
            merger.setContrastWeight(1.0f)
            merger.setExposureWeight(1.0f)     // Ensures proper highlight & shadow weighting
            merger.setSaturationWeight(0.35f)  // Prevents synthetic oversaturated cartoon look

            val fused32F = Mat()
            merger.process(alignedFrames, fused32F)

            val rawFused8U = Mat()
            fused32F.convertTo(rawFused8U, CvType.CV_8U, 255.0)
            fused32F.release()

            // 5. Edge-Preserving Halo Suppression via Bilateral Base-Detail Layering
            val ycrcb = Mat()
            Imgproc.cvtColor(rawFused8U, ycrcb, Imgproc.COLOR_BGR2YCrCb)
            rawFused8U.release()

            val ycrcbChannels = mutableListOf<Mat>()
            Core.split(ycrcb, ycrcbChannels)
            val yChan = ycrcbChannels[0]

            // Bilateral filter extracts smooth illumination base layer while isolating edge details
            val baseLayer = Mat()
            Imgproc.bilateralFilter(yChan, baseLayer, 7, 25.0, 7.0)

            // Detail layer = Original Y - Base Layer
            val y32F = Mat()
            val base32F = Mat()
            yChan.convertTo(y32F, CvType.CV_32F)
            baseLayer.convertTo(base32F, CvType.CV_32F)
            baseLayer.release()

            val detail32F = Mat()
            Core.subtract(y32F, base32F, detail32F)
            y32F.release()

            // Natural tone compression on base illumination layer (protects highlights & prevents edge halos)
            val compressedBase = Mat()
            val baseLut = Mat(1, 256, CvType.CV_8U)
            val baseLutData = ByteArray(256)
            for (v in 0..255) {
                val n = v / 255.0
                // Gentle S-curve: shadow lift with soft highlight shoulder roll-off
                val sCurve = (n.pow(0.85) * (1.0 - n) + (1.0 - (1.0 - n).pow(1.15)) * n)
                baseLutData[v] = (sCurve * 255.0).coerceIn(0.0, 255.0).toInt().toByte()
            }
            baseLut.put(0, 0, baseLutData)
            val base8U = Mat()
            base32F.convertTo(base8U, CvType.CV_8U)
            base32F.release()
            Core.LUT(base8U, baseLut, compressedBase)
            base8U.release()
            baseLut.release()

            val compBase32F = Mat()
            compressedBase.convertTo(compBase32F, CvType.CV_32F)
            compressedBase.release()

            // Recombine: Final Y = Compressed Base + Detail Layer (Halos eliminated)
            val finalY32F = Mat()
            Core.add(compBase32F, detail32F, finalY32F)
            compBase32F.release()
            detail32F.release()

            val finalY8U = Mat()
            finalY32F.convertTo(finalY8U, CvType.CV_8U)
            finalY32F.release()
            yChan.release()

            ycrcbChannels[0] = finalY8U
            val cleanYCrCb = Mat()
            Core.merge(ycrcbChannels, cleanYCrCb)
            finalY8U.release()
            ycrcbChannels.forEach { it.release() }
            ycrcbChannels.clear()

            val fused8U = Mat()
            Imgproc.cvtColor(cleanYCrCb, fused8U, Imgproc.COLOR_YCrCb2BGR)
            cleanYCrCb.release()
            ycrcb.release()

            // Cleanup intermediate aligned frames
            for (i in alignedFrames.indices) {
                if (alignedFrames[i] !== refFrame) {
                    alignedFrames[i].release()
                }
            }

            Log.i(
                TAG,
                "Anti-ghosting fusion successful: merged ${alignedFrames.size} frames, motionDetected=$anyMotionDetected (maxRatio=%.1f%%)"
                    .format(globalMaxMotionRatio * 100)
            )

            return DeghostResult(
                fusedMat = fused8U,
                motionDetected = anyMotionDetected,
                motionRatio = globalMaxMotionRatio,
                sharpestFrameIndex = refIndex,
                framesMerged = alignedFrames.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Anti-ghosting fusion failed: ${e.message}", e)
            refGray.release()
            for (m in alignedFrames) {
                if (m !== refFrame) m.release()
            }
            val fallback = Mat()
            refFrame.copyTo(fallback)
            return DeghostResult(
                fusedMat = fallback,
                motionDetected = false,
                motionRatio = 0f,
                sharpestFrameIndex = refIndex,
                framesMerged = 1
            )
        }
    }

    /**
     * Evaluates Laplacian variance and exposure acceptability to select the sharpest frame.
     */
    fun selectSharpestReference(frames: List<Mat>): Int {
        var bestIndex = if (frames.size > 1) frames.size / 2 else 0
        var bestSharpness = -1.0

        for (i in frames.indices) {
            val score = computeLaplacianSharpness(frames[i])

            // Check exposure clipping
            val gray = Mat()
            Imgproc.cvtColor(frames[i], gray, Imgproc.COLOR_BGR2GRAY)
            val meanVal = MatOfDouble()
            val stdVal = MatOfDouble()
            Core.meanStdDev(gray, meanVal, stdVal)
            val meanLuma = meanVal.toArray().firstOrNull() ?: 128.0
            gray.release()
            meanVal.release()
            stdVal.release()

            // Normalize sharpness by mean luminance squared to prevent brighter EV frames from dominating
            val normalizedScore = score / (meanLuma * meanLuma + 100.0)

            // Penalty for clipped frames (underexposed or washed out)
            val exposureFactor = when {
                meanLuma < 25.0 -> 0.3
                meanLuma > 230.0 -> 0.3
                else -> 1.0
            }

            val weightedScore = normalizedScore * exposureFactor
            if (weightedScore > bestSharpness) {
                bestSharpness = weightedScore
                bestIndex = i
            }
        }
        return bestIndex
    }

    /**
     * Computes the variance of the Laplacian as a real metric of optical edge sharpness.
     */
    fun computeLaplacianSharpness(frameBgr: Mat): Double {
        val gray = Mat()
        Imgproc.cvtColor(frameBgr, gray, Imgproc.COLOR_BGR2GRAY)

        val laplacian = Mat()
        Imgproc.Laplacian(gray, laplacian, CvType.CV_32F)

        val meanLap = MatOfDouble()
        val stdDevLap = MatOfDouble()
        Core.meanStdDev(laplacian, meanLap, stdDevLap)

        val std = stdDevLap.toArray().firstOrNull() ?: 0.0
        val variance = std * std

        gray.release()
        laplacian.release()
        meanLap.release()
        stdDevLap.release()

        return variance
    }

    companion object {
        private const val TAG = "MotionDeghosting"
    }
}
