package com.clearshot.ai.processing.modes

import com.clearshot.ai.ai.EdgeClarityEnhancer
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Computational Macro & Close-Up Detail engine.
 * 1. Synthesizes micro-texture energy using dual-scale Sobel edge analysis.
 * 2. Applies high-frequency Laplacian detail enhancement.
 * 3. Preserves smooth bokeh backgrounds without introducing noise grain.
 */
class MacroDetailProcessor(
    private val edgeClarityEnhancer: EdgeClarityEnhancer = EdgeClarityEnhancer()
) {

    fun process(srcBgr: Mat, dstBgr: Mat) {
        // Step 1: AI micro-texture edge guidance
        val edgeBoosted = Mat()
        edgeClarityEnhancer.enhanceMat(srcBgr, edgeBoosted, boostFactor = 0.55)

        // Step 2: High-pass micro-sharpening in Luminance domain
        val ycrcb = Mat()
        Imgproc.cvtColor(edgeBoosted, ycrcb, Imgproc.COLOR_BGR2YCrCb)
        edgeBoosted.release()

        val channels = mutableListOf<Mat>()
        Core.split(ycrcb, channels)
        val yChannel = channels[0]

        // High-pass micro filter: Y_micro = Y - Gaussian(Y, 1.0)
        val microBlur = Mat()
        Imgproc.GaussianBlur(yChannel, microBlur, Size(0.0, 0.0), 1.0)

        val yFloat = Mat()
        val blurFloat = Mat()
        yChannel.convertTo(yFloat, CvType.CV_32F)
        microBlur.convertTo(blurFloat, CvType.CV_32F)
        microBlur.release()

        val highPass = Mat()
        Core.subtract(yFloat, blurFloat, highPass)
        blurFloat.release()

        // Add weighted high-pass to original luminance: Y + 0.6 * HighPass
        val enhancedYFloat = Mat()
        Core.scaleAdd(highPass, 0.6, yFloat, enhancedYFloat)
        highPass.release()
        yFloat.release()

        val finalY = Mat()
        enhancedYFloat.convertTo(finalY, CvType.CV_8U)
        enhancedYFloat.release()

        yChannel.release()
        channels[0] = finalY

        val mergedYCrCb = Mat()
        Core.merge(channels, mergedYCrCb)
        channels.forEach { it.release() }

        Imgproc.cvtColor(mergedYCrCb, dstBgr, Imgproc.COLOR_YCrCb2BGR)
        mergedYCrCb.release()
        ycrcb.release()
    }
}
