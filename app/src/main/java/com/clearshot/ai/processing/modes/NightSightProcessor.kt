package com.clearshot.ai.processing.modes

import android.util.Log
import com.clearshot.ai.processing.multiframe.FrameAligner
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.pow

/**
 * Diagnostics outcome from Advanced Night Photography processing.
 */
data class NightProcessingResult(
    val framesReceived: Int,
    val framesAligned: Int,
    val framesRejected: Int,
    val noiseReductionDb: Double,
    val isMultiFrame: Boolean
)

/**
 * Advanced Computational Night Photography Engine.
 *
 * Capabilities:
 * 1. Multi-Frame Burst Stacking & Alignment: Sub-pixel translation registration aligns micro-movements.
 * 2. Motion Estimation & Bad Frame Rejection: Analyzes handshake blur and rejects frames with excessive motion.
 * 3. Temporal Super-Sampling Denoising: Averages aligned frames to reduce sensor photon noise by 1/sqrt(N).
 * 4. Realistic Night Tone Curve: Avoids brightening dark scenes unnaturally. Preserves deep rich blacks (Y < 12)
 *    and prevents flat milky haze while gently lifting shadow details and protecting specular streetlight highlights.
 * 5. Chroma Speckle Denoising: Eliminates purple/green color noise in Cr/Cb planes via bilateral filtering.
 * 6. Single-Frame Fallback: Seamlessly falls back to single-frame clarity if motion is too extreme.
 */
class NightSightProcessor(
    private val frameAligner: FrameAligner = FrameAligner()
) {

    /**
     * Multi-frame processing for Night mode.
     * @param frames List of decoded BGR Mats.
     * @param dstBgr Output enhanced Mat.
     * @return NightProcessingResult diagnostic metrics.
     */
    fun processMultiFrame(
        frames: List<Mat>,
        dstBgr: Mat
    ): NightProcessingResult {
        if (frames.isEmpty()) {
            return NightProcessingResult(0, 0, 0, 0.0, false)
        }

        if (frames.size == 1) {
            process(frames[0], dstBgr)
            return NightProcessingResult(1, 1, 0, 0.0, false)
        }

        // 1. Select the sharpest, least motion-blurred frame as reference anchor
        var bestIndex = 0
        var bestSharpness = -1.0
        val sharpnessScores = mutableListOf<Double>()

        for (i in frames.indices) {
            val score = computeLaplacianSharpness(frames[i])
            sharpnessScores.add(score)
            if (score > bestSharpness) {
                bestSharpness = score
                bestIndex = i
            }
        }
        val refFrame = frames[bestIndex]
        Log.i(TAG, "Night mode: Frame #$bestIndex selected as sharpest reference (score: %.1f)".format(bestSharpness))

        // 2. Align frames and reject blurry / unstable frames
        val acceptedFrames = mutableListOf<Mat>()
        acceptedFrames.add(refFrame)
        var rejectedCount = 0

        for (i in frames.indices) {
            if (i == bestIndex) continue

            val score = sharpnessScores[i]
            // Frame rejection rule: If frame lost > 55% sharpness compared to anchor, reject as motion blur
            if (score < bestSharpness * 0.45) {
                Log.w(TAG, "Night frame #$i rejected: Excessive motion blur (score %.1f vs anchor %.1f)".format(score, bestSharpness))
                rejectedCount++
                continue
            }

            try {
                val aligned = frameAligner.align(refFrame, frames[i])
                acceptedFrames.add(aligned)
            } catch (e: Exception) {
                Log.w(TAG, "Night frame #$i alignment failed: ${e.message}")
                rejectedCount++
            }
        }

        // 3. Temporal Super-Sampling Stack Averaging
        val stackedBgr = Mat()
        if (acceptedFrames.size > 1) {
            val sumFloat = Mat.zeros(refFrame.rows(), refFrame.cols(), CvType.CV_32FC3)
            for (frame in acceptedFrames) {
                val f32 = Mat()
                frame.convertTo(f32, CvType.CV_32FC3)
                Core.add(sumFloat, f32, sumFloat)
                f32.release()
            }
            val invCount = 1.0 / acceptedFrames.size
            val avgFloat = Mat()
            sumFloat.convertTo(avgFloat, CvType.CV_32FC3, invCount)
            sumFloat.release()

            avgFloat.convertTo(stackedBgr, CvType.CV_8U)
            avgFloat.release()

            // Release intermediate aligned frames
            for (i in 1 until acceptedFrames.size) {
                acceptedFrames[i].release()
            }
        } else {
            // Single-frame fallback
            refFrame.copyTo(stackedBgr)
        }

        // 4. Realistic Night Tone Mapping & Chroma Denoising on Stacked Result
        process(stackedBgr, dstBgr)
        stackedBgr.release()

        val theoreticalNoiseReductionDb = if (acceptedFrames.size > 1) {
            10.0 * kotlin.math.log10(acceptedFrames.size.toDouble())
        } else 0.0

        Log.i(TAG, "Night synthesis complete: %d/%d frames stacked (%d rejected, +%.1fdB SNR gain)".format(
            acceptedFrames.size, frames.size, rejectedCount, theoreticalNoiseReductionDb
        ))

        return NightProcessingResult(
            framesReceived = frames.size,
            framesAligned = acceptedFrames.size,
            framesRejected = rejectedCount,
            noiseReductionDb = theoreticalNoiseReductionDb,
            isMultiFrame = acceptedFrames.size > 1
        )
    }

    /**
     * Single-frame computational low-light engine.
     * Preserves natural night atmosphere without milky flat blacks.
     */
    fun process(srcBgr: Mat, dstBgr: Mat) {
        val ycrcb = Mat()
        Imgproc.cvtColor(srcBgr, ycrcb, Imgproc.COLOR_BGR2YCrCb)

        val channels = mutableListOf<Mat>()
        Core.split(ycrcb, channels)

        val yChannel = channels[0]
        val crChannel = channels[1]
        val cbChannel = channels[2]

        // 1. Natural Night Tone Curve on Y channel
        // CRITICAL: Protects deep blacks (Y < 12) from being lifted into washed-out haze.
        // Gently decompresses mid-shadows (15 <= Y <= 100) while leaving specular highlights (Y > 210) untouched.
        val lut = Mat(1, 256, CvType.CV_8U)
        val lutData = ByteArray(256)
        for (i in 0..255) {
            val norm = i / 255.0
            val valOut = when {
                // Deep true black preservation: 0 to 12 stays true black
                i < 12 -> i.toDouble()
                // Mid-shadow shadow lift: Smooth logarithmic toe curve
                i < 160 -> {
                    val shadowNorm = (i - 12) / 148.0
                    val liftedShadow = shadowNorm.pow(0.72) * 148.0 + 12.0
                    // Blend smoothly back toward identity as luminance rises
                    val weight = (1.0 - shadowNorm).coerceIn(0.0, 1.0)
                    liftedShadow * weight + i.toDouble() * (1.0 - weight)
                }
                // High tones and specular highlights (neon, streetlights): Strict identity preservation
                else -> i.toDouble()
            }
            lutData[i] = valOut.coerceIn(0.0, 255.0).toInt().toByte()
        }
        lut.put(0, 0, lutData)

        val liftedY = Mat()
        Core.LUT(yChannel, lut, liftedY)
        lut.release()
        yChannel.release()

        // 2. Chroma speckle noise reduction in Cr and Cb channels (bilateral filtering)
        // Eliminates dark purple/green color noise without bleeding color across sharp edges
        val cleanCr = Mat()
        val cleanCb = Mat()
        Imgproc.bilateralFilter(crChannel, cleanCr, 7, 28.0, 7.0)
        Imgproc.bilateralFilter(cbChannel, cleanCb, 7, 28.0, 7.0)
        crChannel.release()
        cbChannel.release()

        // Merge back into YCrCb
        val cleanChannels = listOf(liftedY, cleanCr, cleanCb)
        val cleanYCrCb = Mat()
        Core.merge(cleanChannels, cleanYCrCb)

        liftedY.release()
        cleanCr.release()
        cleanCb.release()

        // Convert back to BGR
        Imgproc.cvtColor(cleanYCrCb, dstBgr, Imgproc.COLOR_YCrCb2BGR)
        cleanYCrCb.release()
        ycrcb.release()
    }

    private fun computeLaplacianSharpness(mat: Mat): Double {
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        val laplacian = Mat()
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)
        val mean = MatOfDouble()
        val stdDev = MatOfDouble()
        Core.meanStdDev(laplacian, mean, stdDev)
        val std = stdDev.toArray().firstOrNull() ?: 0.0
        gray.release()
        laplacian.release()
        mean.release()
        stdDev.release()
        return std * std
    }

    companion object {
        private const val TAG = "NightSightProcessor"
    }
}
