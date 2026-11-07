package com.clearshot.ai.processing.filters

import android.util.Log
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Controlled unsharp masking sharpness enhancer.
 * Amplifies fine textures while suppressing edge halos via dynamic detail thresholding.
 */
class SharpnessEnhancer {

    /**
     * Enhances high-frequency edge clarity and micro-contrast.
     * @param srcBgr Input Mat in BGR format.
     * @param dstBgr Output Mat in BGR format.
     * @param amount Sharpening strength multiplier (typically 0.4 - 1.0).
     * @param sigma Gaussian blur standard deviation for unsharp mask (default: 1.2).
     * @param haloLimit Threshold to clamp high-contrast boundaries to prevent halos (default: 28.0).
     */
    fun enhanceSharpness(
        srcBgr: Mat,
        dstBgr: Mat,
        amount: Double = 0.65,
        sigma: Double = 1.2,
        haloLimit: Double = 28.0
    ) {
        // Work in luminance space to eliminate chromatic fringing
        val ycrcb = Mat()
        Imgproc.cvtColor(srcBgr, ycrcb, Imgproc.COLOR_BGR2YCrCb)

        val channels = ArrayList<Mat>(3)
        Core.split(ycrcb, channels)
        val yChannel = channels[0]

        // 1. Compute Gaussian smoothed baseline
        val blurredY = Mat()
        Imgproc.GaussianBlur(yChannel, blurredY, Size(0.0, 0.0), sigma)

        // 2. Compute signed difference in 16-bit signed format to avoid byte underflow/overflow
        val y16 = Mat()
        val blurred16 = Mat()
        yChannel.convertTo(y16, CvType.CV_16S)
        blurredY.convertTo(blurred16, CvType.CV_16S)

        val detail16 = Mat()
        Core.subtract(y16, blurred16, detail16)

        // 3. Halo suppression: Clamp high-contrast detail transitions
        val upperLimit = Mat(detail16.size(), CvType.CV_16S, Scalar(haloLimit))
        val lowerLimit = Mat(detail16.size(), CvType.CV_16S, Scalar(-haloLimit))
        val clampedDetail = Mat()
        Core.min(detail16, upperLimit, clampedDetail)
        Core.max(clampedDetail, lowerLimit, clampedDetail)

        // 4. Scale clamped micro-detail by amount and recombine
        val scaledDetail = Mat()
        clampedDetail.convertTo(scaledDetail, CvType.CV_32F, amount)

        val y32 = Mat()
        yChannel.convertTo(y32, CvType.CV_32F)

        val sharpened32 = Mat()
        Core.add(y32, scaledDetail, sharpened32)

        val finalY = Mat()
        sharpened32.convertTo(finalY, CvType.CV_8U)

        // 5. Merge back and convert to BGR
        channels[0] = finalY
        val merged = Mat()
        Core.merge(channels, merged)
        Imgproc.cvtColor(merged, dstBgr, Imgproc.COLOR_YCrCb2BGR)

        // Cleanup
        ycrcb.release()
        yChannel.release()
        blurredY.release()
        y16.release()
        blurred16.release()
        detail16.release()
        upperLimit.release()
        lowerLimit.release()
        clampedDetail.release()
        scaledDetail.release()
        y32.release()
        sharpened32.release()
        finalY.release()
        channels[1].release()
        channels[2].release()
        merged.release()

        Log.d(TAG, "Sharpness enhancement applied: amount=%.2f, haloLimit=%.1f".format(amount, haloLimit))
    }

    companion object {
        private const val TAG = "SharpnessEnhancer"
    }
}
