package com.clearshot.ai.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File

/**
 * Phase 3 Edge-Guided Clarity Enhancer.
 * Uses gradient-guided high-frequency texture synthesis to refine structural edges
 * (e.g. text, architectural contours, fine foliage) while strictly preserving smooth backgrounds.
 */
class EdgeClarityEnhancer : AIEnhancer {

    override suspend fun enhance(inputFile: File): Result<File> = withContext(Dispatchers.Default) {
        var srcMat: Mat? = null
        try {
            if (!inputFile.exists() || inputFile.length() == 0L) {
                return@withContext Result.failure(IllegalArgumentException("Input file is empty"))
            }

            srcMat = Imgcodecs.imread(inputFile.absolutePath, Imgcodecs.IMREAD_COLOR)
            if (srcMat.empty()) {
                return@withContext Result.failure(IllegalStateException("Could not read image for AI clarity enhancement"))
            }

            // Convert to YCrCb to operate strictly on Luminance
            val ycrcb = Mat()
            Imgproc.cvtColor(srcMat, ycrcb, Imgproc.COLOR_BGR2YCrCb)
            val channels = ArrayList<Mat>(3)
            Core.split(ycrcb, channels)
            val y = channels[0]

            // 1. Compute multi-directional Sobel gradient magnitude (Edge Energy Mask)
            val gradX = Mat()
            val gradY = Mat()
            Imgproc.Sobel(y, gradX, CvType.CV_16S, 1, 0, 3, 1.0, 0.0)
            Imgproc.Sobel(y, gradY, CvType.CV_16S, 0, 1, 3, 1.0, 0.0)

            val absGradX = Mat()
            val absGradY = Mat()
            Core.convertScaleAbs(gradX, absGradX)
            Core.convertScaleAbs(gradY, absGradY)

            val edgeMask = Mat()
            Core.addWeighted(absGradX, 0.5, absGradY, 0.5, 0.0, edgeMask)

            // Normalize edge mask to [0.0, 1.0] float weights
            val edgeWeights = Mat()
            edgeMask.convertTo(edgeWeights, CvType.CV_32F, 1.0 / 255.0)

            // 2. High-frequency micro-texture extraction
            val blurredY = Mat()
            Imgproc.GaussianBlur(y, blurredY, Size(3.0, 3.0), 0.7)

            val y32f = Mat()
            val blurred32f = Mat()
            y.convertTo(y32f, CvType.CV_32F)
            blurredY.convertTo(blurred32f, CvType.CV_32F)

            val microDetail = Mat()
            Core.subtract(y32f, blurred32f, microDetail)

            // Modulate micro-detail with edge mask so enhancement is concentrated strictly on structural contours
            val edgeGuidedDetail = Mat()
            Core.multiply(microDetail, edgeWeights, edgeGuidedDetail)

            // 3. Synthesize enhanced luminance: Y + 0.35 * EdgeGuidedDetail
            val boostedY32 = Mat()
            Core.scaleAdd(edgeGuidedDetail, 0.35, y32f, boostedY32)

            val finalY = Mat()
            boostedY32.convertTo(finalY, CvType.CV_8U)

            channels[0] = finalY
            val merged = Mat()
            Core.merge(channels, merged)

            val enhancedBgr = Mat()
            Imgproc.cvtColor(merged, enhancedBgr, Imgproc.COLOR_YCrCb2BGR)

            // Overwrite enhanced temp cache file
            val params = MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 96)
            Imgcodecs.imwrite(inputFile.absolutePath, enhancedBgr, params)

            // Cleanup
            srcMat.release()
            ycrcb.release()
            y.release()
            gradX.release()
            gradY.release()
            absGradX.release()
            absGradY.release()
            edgeMask.release()
            edgeWeights.release()
            blurredY.release()
            y32f.release()
            blurred32f.release()
            microDetail.release()
            edgeGuidedDetail.release()
            boostedY32.release()
            finalY.release()
            channels[1].release()
            channels[2].release()
            merged.release()
            enhancedBgr.release()
            params.release()

            Log.i(TAG, "Edge clarity enhancement applied to ${inputFile.name}")
            Result.success(inputFile)
        } catch (e: Exception) {
            srcMat?.release()
            Log.e(TAG, "Edge clarity enhancement failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * In-memory Mat processing method for real-time pipeline integration.
     */
    fun enhanceMat(srcBgr: Mat, dstBgr: Mat, boostFactor: Double = 0.35) {
        val ycrcb = Mat()
        Imgproc.cvtColor(srcBgr, ycrcb, Imgproc.COLOR_BGR2YCrCb)
        val channels = ArrayList<Mat>(3)
        Core.split(ycrcb, channels)
        val y = channels[0]

        val gradX = Mat()
        val gradY = Mat()
        Imgproc.Sobel(y, gradX, CvType.CV_16S, 1, 0, 3, 1.0, 0.0)
        Imgproc.Sobel(y, gradY, CvType.CV_16S, 0, 1, 3, 1.0, 0.0)

        val absGradX = Mat()
        val absGradY = Mat()
        Core.convertScaleAbs(gradX, absGradX)
        Core.convertScaleAbs(gradY, absGradY)

        val edgeMask = Mat()
        Core.addWeighted(absGradX, 0.5, absGradY, 0.5, 0.0, edgeMask)

        val edgeWeights = Mat()
        edgeMask.convertTo(edgeWeights, CvType.CV_32F, 1.0 / 255.0)

        val blurredY = Mat()
        Imgproc.GaussianBlur(y, blurredY, Size(3.0, 3.0), 0.7)

        val y32f = Mat()
        val blurred32f = Mat()
        y.convertTo(y32f, CvType.CV_32F)
        blurredY.convertTo(blurred32f, CvType.CV_32F)

        val microDetail = Mat()
        Core.subtract(y32f, blurred32f, microDetail)

        val edgeGuidedDetail = Mat()
        Core.multiply(microDetail, edgeWeights, edgeGuidedDetail)

        val boostedY32 = Mat()
        Core.scaleAdd(edgeGuidedDetail, boostFactor, y32f, boostedY32)

        val finalY = Mat()
        boostedY32.convertTo(finalY, CvType.CV_8U)

        channels[0] = finalY
        val merged = Mat()
        Core.merge(channels, merged)

        Imgproc.cvtColor(merged, dstBgr, Imgproc.COLOR_YCrCb2BGR)

        ycrcb.release()
        y.release()
        gradX.release()
        gradY.release()
        absGradX.release()
        absGradY.release()
        edgeMask.release()
        edgeWeights.release()
        blurredY.release()
        y32f.release()
        blurred32f.release()
        microDetail.release()
        edgeGuidedDetail.release()
        boostedY32.release()
        finalY.release()
        channels[1].release()
        channels[2].release()
        merged.release()
    }

    companion object {
        private const val TAG = "EdgeClarityEnhancer"
    }
}
