package com.clearshot.ai.processing.multiframe

import android.util.Log
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Multi-frame temporal stack denoiser with motion de-ghosting.
 * Uses temporal frame averaging on aligned burst frames to eliminate random sensor shot noise
 * while actively freezing moving subjects and preventing ghosting/smear.
 */
class MultiFrameDenoiser(
    private val frameAligner: FrameAligner = FrameAligner(),
    private val motionFusion: MotionDeghostingFusion = MotionDeghostingFusion(frameAligner)
) {

    /**
     * Stacks and denoises multiple aligned frames with motion-safe pixel accumulation.
     */
    fun denoiseStack(frames: List<Mat>): Mat {
        if (frames.size <= 1) {
            val copy = Mat()
            frames.first().copyTo(copy)
            return copy
        }

        // 1. Pick the sharpest frame as anchor
        val refIndex = motionFusion.selectSharpestReference(frames)
        val refFrame = frames[refIndex]

        val refGray = Mat()
        Imgproc.cvtColor(refFrame, refGray, Imgproc.COLOR_BGR2GRAY)
        val totalPixels = (refFrame.rows() * refFrame.cols()).toDouble()
        val dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(7.0, 7.0))

        val accumulator = Mat.zeros(refFrame.size(), CvType.CV_32FC3)

        // Add reference frame to accumulator
        val ref32 = Mat()
        refFrame.convertTo(ref32, CvType.CV_32FC3)
        Core.add(accumulator, ref32, accumulator)
        ref32.release()

        var maxMotionRatio = 0f

        for (i in frames.indices) {
            if (i == refIndex) continue

            val aligned = frameAligner.align(refFrame, frames[i])

            // Motion detection
            val targetGray = Mat()
            Imgproc.cvtColor(aligned, targetGray, Imgproc.COLOR_BGR2GRAY)

            val diff = Mat()
            Core.absdiff(refGray, targetGray, diff)

            val blurredDiff = Mat()
            Imgproc.GaussianBlur(diff, blurredDiff, Size(5.0, 5.0), 1.5)

            val binaryMotion = Mat()
            Imgproc.threshold(blurredDiff, binaryMotion, 18.0, 255.0, Imgproc.THRESH_BINARY)

            val dilatedMotion = Mat()
            Imgproc.dilate(binaryMotion, dilatedMotion, dilateKernel)

            val motionCount = Core.countNonZero(dilatedMotion)
            val frameMotionRatio = (motionCount / totalPixels).toFloat()
            if (frameMotionRatio > maxMotionRatio) {
                maxMotionRatio = frameMotionRatio
            }

            val frameToAdd = if (frameMotionRatio > 0.008f) {
                // Moving pixels replaced with refFrame so moving subjects freeze without ghosting
                val deghosted = Mat()
                aligned.copyTo(deghosted)
                refFrame.copyTo(deghosted, dilatedMotion)
                aligned.release()
                deghosted
            } else {
                aligned
            }

            val frameToAdd32 = Mat()
            frameToAdd.convertTo(frameToAdd32, CvType.CV_32FC3)
            Core.add(accumulator, frameToAdd32, accumulator)

            frameToAdd.release()
            frameToAdd32.release()
            targetGray.release()
            diff.release()
            blurredDiff.release()
            binaryMotion.release()
            dilatedMotion.release()
        }

        dilateKernel.release()
        refGray.release()

        // Global motion fallback
        if (maxMotionRatio > 0.35f) {
            Log.w(TAG, "Global motion in temporal stack (%.1f%%). Returning sharpest reference directly.".format(maxMotionRatio * 100))
            accumulator.release()
            val fallback = Mat()
            refFrame.copyTo(fallback)
            return fallback
        }

        val scale = 1.0 / frames.size.toDouble()
        val averaged32 = Mat()
        Core.multiply(accumulator, org.opencv.core.Scalar(scale, scale, scale), averaged32)

        val result8U = Mat()
        averaged32.convertTo(result8U, CvType.CV_8UC3)

        accumulator.release()
        averaged32.release()

        Log.i(TAG, "Multi-frame temporal stack denoised across ${frames.size} frames (maxMotion=%.1f%%, SNR boost ~%.1fx)"
            .format(maxMotionRatio * 100, kotlin.math.sqrt(frames.size.toDouble())))
        return result8U
    }

    companion object {
        private const val TAG = "MultiFrameDenoiser"
    }
}
