package com.clearshot.ai.processing.multiframe

import android.util.Log
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

/**
 * High-speed sub-pixel frame aligner using 2D phase correlation.
 * Compensates for micro-handshake between handheld burst frames to prevent ghosting.
 */
class FrameAligner {

    /**
     * Aligns a target frame to a reference frame.
     * @param referenceBgr Base reference frame.
     * @param targetBgr Target frame to align.
     * @return Newly allocated aligned Mat matching targetBgr.
     */
    fun align(referenceBgr: Mat, targetBgr: Mat): Mat {
        val refGray = Mat()
        val targetGray = Mat()
        Imgproc.cvtColor(referenceBgr, refGray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.cvtColor(targetBgr, targetGray, Imgproc.COLOR_BGR2GRAY)

        val ref32f = Mat()
        val target32f = Mat()
        refGray.convertTo(ref32f, CvType.CV_32F)
        targetGray.convertTo(target32f, CvType.CV_32F)

        // Phase correlation to calculate translation offset (dx, dy)
        val shift = Imgproc.phaseCorrelate(ref32f, target32f)
        val dx = shift.x
        val dy = shift.y

        refGray.release()
        targetGray.release()
        ref32f.release()
        target32f.release()

        // If shift is negligible (< 0.2 px) or excessive (> 50 px, camera panned away)
        if ((abs(dx) < 0.2 && abs(dy) < 0.2) || abs(dx) > 60.0 || abs(dy) > 60.0) {
            val copy = Mat()
            targetBgr.copyTo(copy)
            return copy
        }

        // Apply affine translation matrix
        val transform = Mat(2, 3, CvType.CV_64F)
        transform.put(0, 0, 1.0, 0.0, -dx)
        transform.put(1, 0, 0.0, 1.0, -dy)

        val aligned = Mat()
        Imgproc.warpAffine(
            targetBgr,
            aligned,
            transform,
            Size(targetBgr.cols().toDouble(), targetBgr.rows().toDouble()),
            Imgproc.INTER_LINEAR,
            org.opencv.core.Core.BORDER_REPLICATE
        )

        transform.release()
        Log.d(TAG, "Frame aligned with shift dx=%.2f, dy=%.2f".format(dx, dy))
        return aligned
    }

    companion object {
        private const val TAG = "FrameAligner"
    }
}
