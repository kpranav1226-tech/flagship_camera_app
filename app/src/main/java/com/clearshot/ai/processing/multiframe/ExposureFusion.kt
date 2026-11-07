package com.clearshot.ai.processing.multiframe

import org.opencv.core.Mat

/**
 * Computational exposure fusion using the Mertens algorithm with motion de-ghosting.
 * Blends bracketed exposures into an expanded dynamic range photograph
 * with natural contrast, highlight detail preservation, shadow recovery,
 * and complete moving subject stabilization with zero ghosting.
 */
class ExposureFusion(
    frameAligner: FrameAligner = FrameAligner(),
    private val deghostingFusion: MotionDeghostingFusion = MotionDeghostingFusion(frameAligner)
) {

    /**
     * Aligns and fuses bracketed burst frames with anti-ghosting motion suppression.
     * @param frames List of decoded BGR Mats.
     * @return Single merged BGR Mat (CV_8U).
     */
    fun fuse(frames: List<Mat>): Mat {
        return fuseWithDeghost(frames).fusedMat
    }

    /**
     * Performs exposure fusion and returns detailed motion analysis diagnostics.
     */
    fun fuseWithDeghost(frames: List<Mat>): DeghostResult {
        return deghostingFusion.fuse(frames)
    }

    companion object {
        private const val TAG = "ExposureFusion"
    }
}
