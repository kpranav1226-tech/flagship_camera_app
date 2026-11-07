package com.clearshot.ai.processing.filters

import android.util.Log
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Natural contrast enhancer using Contrast Limited Adaptive Histogram Equalization (CLAHE).
 * Operates strictly on the perceptual lightness (L) channel in Lab color space to prevent
 * chromatic distortion or unnatural HDR artifacts.
 */
class ContrastEnhancer {

    /**
     * Applies adaptive contrast enhancement.
     * @param srcBgr Input Mat in BGR format.
     * @param dstBgr Output Mat in BGR format.
     * @param clipLimit CLAHE contrast clipping threshold (default: 1.6, conservative).
     * @param tileGridSize Grid tile distribution (default: 8x8).
     */
    fun enhanceContrast(
        srcBgr: Mat,
        dstBgr: Mat,
        clipLimit: Double = 1.6,
        tileGridSize: Size = Size(8.0, 8.0)
    ) {
        val lab = Mat()
        Imgproc.cvtColor(srcBgr, lab, Imgproc.COLOR_BGR2Lab)

        val channels = ArrayList<Mat>(3)
        Core.split(lab, channels)
        val lChannel = channels[0]

        val clahe = Imgproc.createCLAHE(clipLimit, tileGridSize)
        val enhancedL = Mat()
        clahe.apply(lChannel, enhancedL)

        channels[0] = enhancedL
        val merged = Mat()
        Core.merge(channels, merged)
        Imgproc.cvtColor(merged, dstBgr, Imgproc.COLOR_Lab2BGR)

        // Memory cleanup
        lab.release()
        lChannel.release()
        enhancedL.release()
        clahe.collectGarbage()
        channels[1].release()
        channels[2].release()
        merged.release()

        Log.d(TAG, "Contrast enhancement applied: clipLimit=%.2f, tileGrid=${tileGridSize.width}x${tileGridSize.height}".format(clipLimit))
    }

    companion object {
        private const val TAG = "ContrastEnhancer"
    }
}
