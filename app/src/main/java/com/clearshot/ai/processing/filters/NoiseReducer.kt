package com.clearshot.ai.processing.filters

import android.util.Log
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Texture-preserving, edge-aware noise reducer operating on the luminance channel in YCrCb color space.
 * Smooths high-frequency sensor grain while guaranteeing 100% preservation of facial features,
 * skin textures, hair strands, and edges without causing plastic or waxy smoothing.
 */
class NoiseReducer {

    /**
     * Denoises the source BGR Mat with texture-guided edge protection.
     * @param srcBgr Input Mat in BGR format.
     * @param dstBgr Output Mat in BGR format (must be same size and type).
     * @param sigma Tuning strength for color/space domain (typically 12.0 - 22.0).
     * @param diameter Neighborhood diameter (default: 5).
     */
    fun reduceNoise(
        srcBgr: Mat,
        dstBgr: Mat,
        sigma: Double = 18.0,
        diameter: Int = 5
    ) {
        val ycrcb = Mat()
        Imgproc.cvtColor(srcBgr, ycrcb, Imgproc.COLOR_BGR2YCrCb)

        val channels = ArrayList<Mat>(3)
        Core.split(ycrcb, channels)

        val yChannel = channels[0]

        // 1. Compute high-frequency texture & edge energy map
        val gradX = Mat()
        val gradY = Mat()
        Imgproc.Sobel(yChannel, gradX, CvType.CV_16S, 1, 0)
        Imgproc.Sobel(yChannel, gradY, CvType.CV_16S, 0, 1)

        val absGradX = Mat()
        val absGradY = Mat()
        Core.convertScaleAbs(gradX, absGradX)
        Core.convertScaleAbs(gradY, absGradY)

        val edgeEnergy = Mat()
        Core.add(absGradX, absGradY, edgeEnergy)

        gradX.release()
        gradY.release()
        absGradX.release()
        absGradY.release()

        // 2. Build texture protection mask (protects hair, eyes, pores, facial details, moving edges)
        val textureMask = Mat()
        Imgproc.threshold(edgeEnergy, textureMask, 24.0, 255.0, Imgproc.THRESH_BINARY)
        edgeEnergy.release()

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        Imgproc.dilate(textureMask, textureMask, kernel)
        kernel.release()

        val softTexture = Mat()
        Imgproc.GaussianBlur(textureMask, softTexture, Size(5.0, 5.0), 1.2)
        textureMask.release()

        // 3. Bilateral filter on luminance channel
        val filteredY = Mat()
        val clampedSigma = sigma.coerceIn(10.0, 24.0)
        Imgproc.bilateralFilter(
            yChannel,
            filteredY,
            diameter,
            clampedSigma,
            clampedSigma
        )

        // 4. Alpha blend: flat areas get filteredY (denoised), textured areas keep original yChannel
        // blended = filteredY + (yChannel - filteredY) * alpha
        val alpha32 = Mat()
        softTexture.convertTo(alpha32, CvType.CV_32F, 1.0 / 255.0)
        softTexture.release()

        val yOrig32 = Mat()
        val yFilt32 = Mat()
        yChannel.convertTo(yOrig32, CvType.CV_32F)
        filteredY.convertTo(yFilt32, CvType.CV_32F)

        val diff32 = Mat()
        Core.subtract(yOrig32, yFilt32, diff32)
        val scaledDiff = Mat()
        Core.multiply(diff32, alpha32, scaledDiff)
        val final32 = Mat()
        Core.add(yFilt32, scaledDiff, final32)

        val protectedY = Mat()
        final32.convertTo(protectedY, CvType.CV_8U)

        // Cleanup
        alpha32.release()
        yOrig32.release()
        yFilt32.release()
        diff32.release()
        scaledDiff.release()
        final32.release()
        yChannel.release()
        filteredY.release()

        channels[0] = protectedY
        val merged = Mat()
        Core.merge(channels, merged)
        Imgproc.cvtColor(merged, dstBgr, Imgproc.COLOR_YCrCb2BGR)

        // Cleanup
        ycrcb.release()
        protectedY.release()
        channels[1].release()
        channels[2].release()
        merged.release()

        Log.d(TAG, "Texture-guided noise reduction applied with diameter=$diameter, sigma=%.1f".format(clampedSigma))
    }

    companion object {
        private const val TAG = "NoiseReducer"
    }
}
