package com.clearshot.ai.processing.analysis

import android.util.Log
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Comprehensive quality metrics and computational parameter recommendations.
 * Fulfills multi-metric evaluation without relying on Laplacian variance alone.
 */
data class QualityAnalysisResult(
    val sharpnessScore: Double,          // Laplacian variance
    val tenengradScore: Double = 0.0,    // Tenengrad gradient energy (Sobel Sx^2 + Sy^2)
    val noiseScore: Double,              // High-frequency residual standard deviation
    val snrDb: Double = 32.0,            // Signal-to-Noise Ratio in decibels
    val underexposureRatio: Float,       // Shadow clipping ratio (< 15 luma)
    val overexposureRatio: Float,        // Highlight clipping ratio (> 240 luma)
    val highlightClippingPercent: Float = 0f, // Severe highlight blowout (> 250 luma)
    val shadowClippingPercent: Float = 0f,    // Severe shadow crush (< 10 luma)
    val dynamicRangeStops: Double = 8.0, // Measured dynamic range in photographic EV stops
    val meanLuminance: Double,
    val meanSaturation: Float = 0.35f,   // Mean saturation in HSV space [0, 1]
    val colorEntropy: Double = 7.2,      // Shannon color information entropy
    val isHighDynamicRangeScene: Boolean = false,
    val recommendedDenoiseSigma: Double,
    val recommendedSharpenAmount: Double,
    val recommendedClaheClip: Double,
    val shouldApplyClahe: Boolean
)

/**
 * Advanced Multi-Metric Image Quality Analyzer.
 * 
 * Computes:
 * 1. Sharpness: Dual-metric evaluation combining Tenengrad Sobel gradient energy and Laplacian variance.
 * 2. Sensor Noise & SNR: Residual noise standard deviation and Signal-to-Noise Ratio (SNR dB).
 * 3. Dynamic Range & Exposure: Percentile-based EV stops calculation and true highlight/shadow clipping.
 * 4. Color Distribution: Mean saturation in HSV space and Shannon entropy across RGB histograms.
 * 5. High Dynamic Range (HDR) Scene Detection: Triggers Smart HDR pipeline when DR > 7.5 EV stops.
 */
class ImageQualityAnalyzer {

    fun analyze(sourceBgr: Mat): QualityAnalysisResult {
        val width = sourceBgr.cols()
        val height = sourceBgr.rows()
        if (width <= 0 || height <= 0) {
            return QualityAnalysisResult(
                sharpnessScore = 500.0,
                tenengradScore = 150.0,
                noiseScore = 4.0,
                snrDb = 30.0,
                underexposureRatio = 0.02f,
                overexposureRatio = 0.01f,
                highlightClippingPercent = 0.005f,
                shadowClippingPercent = 0.01f,
                dynamicRangeStops = 8.0,
                meanLuminance = 120.0,
                meanSaturation = 0.35f,
                colorEntropy = 7.0,
                isHighDynamicRangeScene = false,
                recommendedDenoiseSigma = 16.0,
                recommendedSharpenAmount = 0.50,
                recommendedClaheClip = 1.4,
                shouldApplyClahe = false
            )
        }

        // Downscale to max 1080px dimension for lightning-fast statistical analysis
        val scale = min(1.0, 1080.0 / max(height, width))
        val sample = Mat()
        if (scale < 1.0) {
            val targetSize = Size(width * scale, height * scale)
            Imgproc.resize(sourceBgr, sample, targetSize, 0.0, 0.0, Imgproc.INTER_AREA)
        } else {
            sourceBgr.copyTo(sample)
        }

        val gray = Mat()
        Imgproc.cvtColor(sample, gray, Imgproc.COLOR_BGR2GRAY)
        val totalPixels = (gray.rows() * gray.cols()).toDouble()

        // 1. Sharpness Metric 1: Variance of the Laplacian
        val laplacian = Mat()
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)
        val meanLap = MatOfDouble()
        val stdDevLap = MatOfDouble()
        Core.meanStdDev(laplacian, meanLap, stdDevLap)
        val lapStd = stdDevLap.toArray().firstOrNull() ?: 0.0
        val sharpnessScore = lapStd * lapStd
        laplacian.release()
        meanLap.release()
        stdDevLap.release()

        // 2. Sharpness Metric 2: Tenengrad Gradient Energy (Sobel magnitude squared)
        val sobelX = Mat()
        val sobelY = Mat()
        Imgproc.Sobel(gray, sobelX, CvType.CV_32F, 1, 0, 3)
        Imgproc.Sobel(gray, sobelY, CvType.CV_32F, 0, 1, 3)
        val sobelX2 = Mat()
        val sobelY2 = Mat()
        Core.multiply(sobelX, sobelX, sobelX2)
        Core.multiply(sobelY, sobelY, sobelY2)
        sobelX.release()
        sobelY.release()

        val gradMag2 = Mat()
        Core.add(sobelX2, sobelY2, gradMag2)
        sobelX2.release()
        sobelY2.release()

        val meanGrad2 = Core.mean(gradMag2).`val`[0]
        gradMag2.release()
        val tenengradScore = sqrt(meanGrad2)

        // 3. Noise Estimation: Standard deviation of high-frequency detail after Gaussian blur
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(3.0, 3.0), 0.8)
        val highFreq = Mat()
        Core.absdiff(gray, blurred, highFreq)
        blurred.release()

        val meanHf = MatOfDouble()
        val stdDevHf = MatOfDouble()
        Core.meanStdDev(highFreq, meanHf, stdDevHf)
        highFreq.release()
        meanHf.release()
        val noiseScore = (stdDevHf.toArray().firstOrNull() ?: 1.0).coerceAtLeast(0.1)
        stdDevHf.release()

        // 4. Mean Luminance & SNR (Signal-to-Noise Ratio in dB)
        val meanL = MatOfDouble()
        val stdDevL = MatOfDouble()
        Core.meanStdDev(gray, meanL, stdDevL)
        val meanLuminance = (meanL.toArray().firstOrNull() ?: 128.0).coerceAtLeast(1.0)
        meanL.release()
        stdDevL.release()

        val snrLinear = (meanLuminance / noiseScore).coerceAtLeast(1.0)
        val snrDb = 20.0 * log10(snrLinear)

        // 5. Exposure Analysis (Highlights & Shadow clipping)
        val underMask = Mat()
        val overMask = Mat()
        val severeUnderMask = Mat()
        val severeOverMask = Mat()

        Core.compare(gray, Scalar(15.0), underMask, Core.CMP_LT)
        Core.compare(gray, Scalar(240.0), overMask, Core.CMP_GT)
        Core.compare(gray, Scalar(10.0), severeUnderMask, Core.CMP_LT)
        Core.compare(gray, Scalar(250.0), severeOverMask, Core.CMP_GT)

        val underexposureRatio = (Core.countNonZero(underMask) / totalPixels).toFloat()
        val overexposureRatio = (Core.countNonZero(overMask) / totalPixels).toFloat()
        val shadowClippingPercent = (Core.countNonZero(severeUnderMask) / totalPixels).toFloat()
        val highlightClippingPercent = (Core.countNonZero(severeOverMask) / totalPixels).toFloat()

        underMask.release()
        overMask.release()
        severeUnderMask.release()
        severeOverMask.release()

        // 6. Dynamic Range Estimation (in EV stops from 1st and 99th percentiles)
        val hist = Mat()
        Imgproc.calcHist(
            listOf(gray),
            MatOfInt(0),
            Mat(),
            hist,
            MatOfInt(256),
            MatOfFloat(0f, 256f)
        )
        gray.release()

        val histArray = FloatArray(256)
        hist.get(0, 0, histArray)
        hist.release()

        var cum = 0f
        var p01 = 5
        var p99 = 250
        val target01 = (totalPixels * 0.01).toFloat()
        val target99 = (totalPixels * 0.99).toFloat()

        for (i in 0..255) {
            cum += histArray[i]
            if (cum >= target01 && p01 == 5) p01 = i
            if (cum >= target99 && p99 == 250) {
                p99 = i
                break
            }
        }
        val dynamicRangeStops = log2(((p99 + 1.0) / (p01 + 1.0)).coerceAtLeast(1.0)).coerceIn(2.0, 14.0)

        // 7. Color Distribution (Mean Saturation & Shannon Entropy)
        val hsvSample = Mat()
        Imgproc.cvtColor(sample, hsvSample, Imgproc.COLOR_BGR2HSV)
        val hsvPlanes = mutableListOf<Mat>()
        Core.split(hsvSample, hsvPlanes)
        val meanSatVal = Core.mean(hsvPlanes[1]).`val`[0]
        val meanSaturation = (meanSatVal / 255.0).toFloat().coerceIn(0f, 1f)
        hsvPlanes.forEach { it.release() }
        hsvPlanes.clear()
        hsvSample.release()

        // Shannon Entropy across RGB channels
        val bgrPlanes = mutableListOf<Mat>()
        Core.split(sample, bgrPlanes)
        sample.release()

        var totalEntropy = 0.0
        for (channel in bgrPlanes) {
            val cHist = Mat()
            Imgproc.calcHist(listOf(channel), MatOfInt(0), Mat(), cHist, MatOfInt(256), MatOfFloat(0f, 256f))
            channel.release()
            val cArray = FloatArray(256)
            cHist.get(0, 0, cArray)
            cHist.release()

            var chEntropy = 0.0
            for (count in cArray) {
                if (count > 0f) {
                    val p = count / totalPixels
                    chEntropy -= p * (ln(p) / ln(2.0))
                }
            }
            totalEntropy += chEntropy
        }
        bgrPlanes.clear()
        val colorEntropy = totalEntropy / 3.0

        // 8. High Dynamic Range (HDR) Scene Classification
        val isHdrScene = dynamicRangeStops > 7.4 || (highlightClippingPercent > 0.018f && shadowClippingPercent > 0.04f)

        // 9. Adaptive Parameter Tuning Recommendations
        val recDenoiseSigma = when {
            noiseScore > 7.0 || snrDb < 24.0 -> 22.0 // Low-light high ISO
            noiseScore > 4.0 || snrDb < 30.0 -> 16.0 // Normal indoor
            else -> 12.0                             // Daylight clean
        }

        val recSharpenAmount = when {
            tenengradScore > 200.0 -> 0.40 // Crisp edge details
            tenengradScore > 100.0 -> 0.60 // Average scene
            else -> 0.75                  // Soft scene, controlled clarity boost
        }

        val shouldApplyClahe = isHdrScene || underexposureRatio > 0.05f || overexposureRatio > 0.05f
        val recClaheClip = when {
            underexposureRatio > 0.12f -> 2.0
            underexposureRatio > 0.04f -> 1.5
            else -> 1.2
        }

        Log.i(TAG, "Multi-Metric Audit: Tenengrad=%.1f, LapVar=%.1f, Noise=%.2f, SNR=%.1fdB, DR=%.1f stops, HDR=%b, Sat=%.2f, Entropy=%.2f"
            .format(tenengradScore, sharpnessScore, noiseScore, snrDb, dynamicRangeStops, isHdrScene, meanSaturation, colorEntropy))

        return QualityAnalysisResult(
            sharpnessScore = sharpnessScore,
            tenengradScore = tenengradScore,
            noiseScore = noiseScore,
            snrDb = snrDb,
            underexposureRatio = underexposureRatio,
            overexposureRatio = overexposureRatio,
            highlightClippingPercent = highlightClippingPercent,
            shadowClippingPercent = shadowClippingPercent,
            dynamicRangeStops = dynamicRangeStops,
            meanLuminance = meanLuminance,
            meanSaturation = meanSaturation,
            colorEntropy = colorEntropy,
            isHighDynamicRangeScene = isHdrScene,
            recommendedDenoiseSigma = recDenoiseSigma,
            recommendedSharpenAmount = recSharpenAmount,
            recommendedClaheClip = recClaheClip,
            shouldApplyClahe = shouldApplyClahe
        )
    }

    companion object {
        private const val TAG = "ImageQualityAnalyzer"
    }
}
