package com.clearshot.ai.processing.modes

import android.graphics.PointF
import android.util.Log
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import kotlin.math.hypot
import kotlin.math.max

/**
 * 4 detected corners of a document in normalized coordinates (0.0 to 1.0)
 * relative to image width and height.
 */
data class DocumentCorners(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF,
    val isAutoDetected: Boolean = true
) {
    fun toAbsolute(width: Int, height: Int): List<Point> = listOf(
        Point((topLeft.x * width).toDouble(), (topLeft.y * height).toDouble()),
        Point((topRight.x * width).toDouble(), (topRight.y * height).toDouble()),
        Point((bottomRight.x * width).toDouble(), (bottomRight.y * height).toDouble()),
        Point((bottomLeft.x * width).toDouble(), (bottomLeft.y * height).toDouble())
    )

    companion object {
        fun defaultInset(): DocumentCorners = DocumentCorners(
            topLeft = PointF(0.08f, 0.08f),
            topRight = PointF(0.92f, 0.08f),
            bottomRight = PointF(0.92f, 0.92f),
            bottomLeft = PointF(0.08f, 0.92f),
            isAutoDetected = false
        )
    }
}

enum class DocumentFilterMode(val label: String) {
    COLOR("Color"),
    GRAYSCALE("Grayscale"),
    BLACK_WHITE("B&W")
}

/**
 * Comprehensive Computer Vision Document Scanner:
 * 1. Automatic boundary & four-corner detection using multi-scale Canny edge & contour approximation.
 * 2. Homography perspective warping (dewarping/deskewing).
 * 3. Shadow removal & illumination normalization.
 * 4. Text stroke clarity enhancement.
 * 5. Three document modes: Color, Grayscale, and crisp B&W binary text.
 */
class DocumentProcessor {

    /**
     * Analyzes an input image file and detects the four corners of a document.
     * Returns normalized coordinates (0.0 to 1.0) for UI display and manipulation.
     */
    fun detectCorners(imageFile: File): DocumentCorners {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            return DocumentCorners.defaultInset()
        }

        val srcMat = Imgcodecs.imread(imageFile.absolutePath)
        if (srcMat.empty()) {
            return DocumentCorners.defaultInset()
        }

        try {
            val imgW = srcMat.cols()
            val imgH = srcMat.rows()

            // 1. Downscale temporarily for fast, noise-free contour analysis
            val scale = 800.0 / max(imgW, imgH)
            val targetW = (imgW * scale).toInt()
            val targetH = (imgH * scale).toInt()
            val smallMat = Mat()
            Imgproc.resize(srcMat, smallMat, Size(targetW.toDouble(), targetH.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)

            // 2. Grayscale & Noise reduction
            val gray = Mat()
            Imgproc.cvtColor(smallMat, gray, Imgproc.COLOR_BGR2GRAY)
            Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

            // 3. Multi-threshold edge detection
            val edges = Mat()
            Imgproc.Canny(gray, edges, 35.0, 120.0)
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
            Imgproc.dilate(edges, edges, kernel)
            kernel.release()
            gray.release()
            smallMat.release()

            // 4. Find all external contours
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
            hierarchy.release()
            edges.release()

            val minArea = (targetW * targetH) * 0.12 // At least 12% of frame
            var bestQuad: List<Point>? = null
            var maxAreaFound = 0.0

            // Sort largest contours first
            val sortedContours = contours.sortedByDescending { Imgproc.contourArea(it) }

            for (contour in sortedContours.take(8)) {
                val area = Imgproc.contourArea(contour)
                if (area < minArea) break

                val c2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(c2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)

                val points = approx.toArray()
                c2f.release()
                approx.release()

                if (points.size == 4) {
                    val mop = MatOfPoint(*points)
                    val isConvex = Imgproc.isContourConvex(mop)
                    mop.release()

                    if (isConvex && area > maxAreaFound) {
                        maxAreaFound = area
                        // Map back to unscaled coordinates
                        bestQuad = points.map { pt ->
                            Point(pt.x / scale, pt.y / scale)
                        }
                    }
                }
            }
            contours.forEach { it.release() }

            if (bestQuad != null && bestQuad.size == 4) {
                val ordered = orderCorners(bestQuad)
                return DocumentCorners(
                    topLeft = PointF((ordered[0].x / imgW).toFloat().coerceIn(0f, 1f), (ordered[0].y / imgH).toFloat().coerceIn(0f, 1f)),
                    topRight = PointF((ordered[1].x / imgW).toFloat().coerceIn(0f, 1f), (ordered[1].y / imgH).toFloat().coerceIn(0f, 1f)),
                    bottomRight = PointF((ordered[2].x / imgW).toFloat().coerceIn(0f, 1f), (ordered[2].y / imgH).toFloat().coerceIn(0f, 1f)),
                    bottomLeft = PointF((ordered[3].x / imgW).toFloat().coerceIn(0f, 1f), (ordered[3].y / imgH).toFloat().coerceIn(0f, 1f)),
                    isAutoDetected = true
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Corner detection fallback: ${e.message}")
        } finally {
            srcMat.release()
        }

        return DocumentCorners.defaultInset()
    }

    /**
     * Warps the document using 4 corners, normalizes shadows, enhances text,
     * and exports the clean document to outputFile.
     */
    fun warpAndEnhance(
        inputFile: File,
        outputFile: File,
        corners: DocumentCorners,
        filterMode: DocumentFilterMode,
        rotationDegrees: Int = 0
    ): Result<File> {
        val srcMat = Imgcodecs.imread(inputFile.absolutePath)
        if (srcMat.empty()) {
            return Result.failure(IllegalStateException("Could not read image for document processing"))
        }

        try {
            val imgW = srcMat.cols()
            val imgH = srcMat.rows()
            val absPoints = corners.toAbsolute(imgW, imgH)
            val tl = absPoints[0]
            val tr = absPoints[1]
            val br = absPoints[2]
            val bl = absPoints[3]

            // 1. Compute target dewarped dimensions
            val widthTop = hypot(tr.x - tl.x, tr.y - tl.y)
            val widthBottom = hypot(br.x - bl.x, br.y - bl.y)
            val maxWidth = max(widthTop, widthBottom).toInt().coerceAtLeast(200)

            val heightLeft = hypot(bl.x - tl.x, bl.y - tl.y)
            val heightRight = hypot(br.x - tr.x, br.y - tr.y)
            val maxHeight = max(heightLeft, heightRight).toInt().coerceAtLeast(200)

            // 2. Homography perspective transform
            val srcCorners = MatOfPoint2f(tl, tr, br, bl)
            val dstCorners = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(maxWidth.toDouble(), 0.0),
                Point(maxWidth.toDouble(), maxHeight.toDouble()),
                Point(0.0, maxHeight.toDouble())
            )

            val perspectiveMat = Imgproc.getPerspectiveTransform(srcCorners, dstCorners)
            val warped = Mat()
            Imgproc.warpPerspective(
                srcMat,
                warped,
                perspectiveMat,
                Size(maxWidth.toDouble(), maxHeight.toDouble()),
                Imgproc.INTER_CUBIC,
                Core.BORDER_REPLICATE
            )

            srcCorners.release()
            dstCorners.release()
            perspectiveMat.release()
            srcMat.release()

            // Optional 90/180/270 degree rotation if requested by user
            val rotatedWarped = if (rotationDegrees % 360 != 0) {
                val rot = Mat()
                when ((rotationDegrees % 360 + 360) % 360) {
                    90 -> Core.rotate(warped, rot, Core.ROTATE_90_CLOCKWISE)
                    180 -> Core.rotate(warped, rot, Core.ROTATE_180)
                    270 -> Core.rotate(warped, rot, Core.ROTATE_90_COUNTERCLOCKWISE)
                    else -> warped.copyTo(rot)
                }
                warped.release()
                rot
            } else {
                warped
            }

            // 3. Apply document filter enhancement
            val finalMat = Mat()
            when (filterMode) {
                DocumentFilterMode.COLOR -> {
                    enhanceColorDocument(rotatedWarped, finalMat)
                }
                DocumentFilterMode.GRAYSCALE -> {
                    enhanceGrayscaleDocument(rotatedWarped, finalMat)
                }
                DocumentFilterMode.BLACK_WHITE -> {
                    enhanceBinaryDocument(rotatedWarped, finalMat)
                }
            }
            rotatedWarped.release()

            // 4. Save enhanced document
            val params = MatOfInt(
                Imgcodecs.IMWRITE_JPEG_QUALITY, 92,
                Imgcodecs.IMWRITE_JPEG_OPTIMIZE, 1
            )
            Imgcodecs.imwrite(outputFile.absolutePath, finalMat, params)
            finalMat.release()
            params.release()

            return Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Document warping failed: ${e.message}", e)
            srcMat.release()
            return Result.failure(e)
        }
    }

    /**
     * In-place or standard enhancement for backward compatibility with EnhancementPipeline.
     */
    fun process(srcBgr: Mat, dstBgr: Mat) {
        enhanceColorDocument(srcBgr, dstBgr)
    }

    private fun enhanceColorDocument(srcBgr: Mat, dstBgr: Mat) {
        val lab = Mat()
        Imgproc.cvtColor(srcBgr, lab, Imgproc.COLOR_BGR2Lab)

        val channels = mutableListOf<Mat>()
        Core.split(lab, channels)
        val lChannel = channels[0]

        // Background shadow removal
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(27.0, 27.0))
        val background = Mat()
        Imgproc.dilate(lChannel, background, kernel)
        Imgproc.medianBlur(background, background, 21)
        kernel.release()

        val lFloat = Mat()
        val bgFloat = Mat()
        lChannel.convertTo(lFloat, CvType.CV_32F)
        background.convertTo(bgFloat, CvType.CV_32F)
        background.release()

        val normFloat = Mat()
        Core.divide(lFloat, bgFloat, normFloat, 255.0)
        lFloat.release()
        bgFloat.release()

        val normL = Mat()
        normFloat.convertTo(normL, CvType.CV_8U)
        normFloat.release()

        // Gentle text stroke unsharp masking (no halo)
        val blur = Mat()
        Imgproc.GaussianBlur(normL, blur, Size(0.0, 0.0), 1.2)
        val sharpL = Mat()
        Core.addWeighted(normL, 1.35, blur, -0.35, 0.0, sharpL)
        blur.release()
        normL.release()

        lChannel.release()
        channels[0] = sharpL

        val cleanLab = Mat()
        Core.merge(channels, cleanLab)
        channels.forEach { it.release() }

        Imgproc.cvtColor(cleanLab, dstBgr, Imgproc.COLOR_Lab2BGR)
        cleanLab.release()
        lab.release()
    }

    private fun enhanceGrayscaleDocument(srcBgr: Mat, dstBgr: Mat) {
        val gray = Mat()
        Imgproc.cvtColor(srcBgr, gray, Imgproc.COLOR_BGR2GRAY)

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(27.0, 27.0))
        val bg = Mat()
        Imgproc.dilate(gray, bg, kernel)
        Imgproc.medianBlur(bg, bg, 21)
        kernel.release()

        val gFloat = Mat()
        val bgFloat = Mat()
        gray.convertTo(gFloat, CvType.CV_32F)
        bg.convertTo(bgFloat, CvType.CV_32F)
        bg.release()

        val normFloat = Mat()
        Core.divide(gFloat, bgFloat, normFloat, 255.0)
        gFloat.release()
        bgFloat.release()

        val normG = Mat()
        normFloat.convertTo(normG, CvType.CV_8U)
        normFloat.release()
        gray.release()

        val blur = Mat()
        Imgproc.GaussianBlur(normG, blur, Size(0.0, 0.0), 1.2)
        val sharpG = Mat()
        Core.addWeighted(normG, 1.35, blur, -0.35, 0.0, sharpG)
        blur.release()
        normG.release()

        Imgproc.cvtColor(sharpG, dstBgr, Imgproc.COLOR_GRAY2BGR)
        sharpG.release()
    }

    private fun enhanceBinaryDocument(srcBgr: Mat, dstBgr: Mat) {
        val gray = Mat()
        Imgproc.cvtColor(srcBgr, gray, Imgproc.COLOR_BGR2GRAY)

        val norm = Mat()
        Imgproc.medianBlur(gray, norm, 3)

        val bin = Mat()
        Imgproc.adaptiveThreshold(
            norm,
            bin,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            25,
            11.0
        )
        norm.release()
        gray.release()

        Imgproc.cvtColor(bin, dstBgr, Imgproc.COLOR_GRAY2BGR)
        bin.release()
    }

    /**
     * Orders 4 points clockwise: [TopLeft, TopRight, BottomRight, BottomLeft].
     */
    private fun orderCorners(pts: List<Point>): List<Point> {
        val sumSorted = pts.sortedBy { it.x + it.y }
        val tl = sumSorted.first()
        val br = sumSorted.last()

        val diffSorted = pts.sortedBy { it.y - it.x }
        val tr = diffSorted.first()
        val bl = diffSorted.last()

        return listOf(tl, tr, br, bl)
    }

    companion object {
        private const val TAG = "DocumentProcessor"
    }
}
