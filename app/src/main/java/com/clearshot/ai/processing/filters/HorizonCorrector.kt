package com.clearshot.ai.processing.filters

import android.util.Log
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Precision optical horizon corrector.
 * Automatically straightens tilted photographs by rotating the image counter-tilt
 * and applying an inscribed crop to eliminate corner black border wedges while preserving aspect ratio and resolution.
 */
class HorizonCorrector {

    /**
     * Straightens horizon tilt on source BGR Mat.
     * @param srcBgr Input Mat.
     * @param dstBgr Output Mat (straightened and rescaled to original dimensions).
     * @param rollDegrees Tilt angle in degrees.
     * @return true if correction was applied, false if angle was negligible (<0.4°) or extreme (>15°).
     */
    fun correct(srcBgr: Mat, dstBgr: Mat, rollDegrees: Float): Boolean {
        val absRoll = abs(rollDegrees)
        if (absRoll < 0.4f || absRoll > 15.0f) {
            srcBgr.copyTo(dstBgr)
            return false
        }

        val w = srcBgr.cols()
        val h = srcBgr.rows()
        val center = Point(w / 2.0, h / 2.0)

        // Rotate counter-tilt (-rollDegrees) around image center
        val rotMatrix = Imgproc.getRotationMatrix2D(center, -rollDegrees.toDouble(), 1.0)
        val rotated = Mat()
        Imgproc.warpAffine(
            srcBgr,
            rotated,
            rotMatrix,
            Size(w.toDouble(), h.toDouble()),
            Imgproc.INTER_CUBIC,
            Core.BORDER_REPLICATE
        )
        rotMatrix.release()

        // Calculate maximum inscribed unclipped rectangle preserving aspect ratio
        val alphaRad = Math.toRadians(absRoll.toDouble())
        val aspect = w.toDouble() / h.toDouble()
        val dominantAspect = max(aspect, 1.0 / aspect)
        val scale = 1.0 / (cos(alphaRad) + dominantAspect * sin(alphaRad))

        val cropW = (w * scale).roundToInt().coerceIn(1, w)
        val cropH = (h * scale).roundToInt().coerceIn(1, h)
        val x0 = ((w - cropW) / 2).coerceIn(0, w - cropW)
        val y0 = ((h - cropH) / 2).coerceIn(0, h - cropH)

        val cropRect = Rect(x0, y0, cropW, cropH)
        val cropped = Mat(rotated, cropRect)

        // Rescale back to pristine original sensor resolution using bicubic interpolation
        Imgproc.resize(cropped, dstBgr, Size(w.toDouble(), h.toDouble()), 0.0, 0.0, Imgproc.INTER_CUBIC)

        cropped.release()
        rotated.release()

        Log.i(TAG, "Auto horizon correction applied: %.2f° tilt removed (inscribed crop: %dx%d -> %dx%d)"
            .format(-rollDegrees, cropW, cropH, w, h))
        return true
    }

    companion object {
        private const val TAG = "HorizonCorrector"
    }
}
