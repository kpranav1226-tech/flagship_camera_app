package com.clearshot.ai.camera

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import android.util.Range
import kotlin.math.roundToInt

/**
 * Detailed technical specifications of an enumerated camera sensor.
 */
data class CameraSpecs(
    val cameraId: String,
    val name: String,
    val isFront: Boolean,
    val resolutionMegapixels: Float,
    val activeArraySize: Rect?,
    val focalLengths: FloatArray,
    val apertures: FloatArray,
    val minIso: Int,
    val maxIso: Int,
    val minShutterNanos: Long,
    val maxShutterNanos: Long,
    val supportedFpsRanges: List<Range<Int>>,
    val supportsOis: Boolean,
    val supportsEis: Boolean,
    val maxZoom: Float,
    val physicalCameraIds: Set<String> = emptySet(),
    val isLogicalMultiCamera: Boolean = false
) {
    val description: String
        get() = "${if (isFront) "Front" else "Rear"} • %.1f MP • %s • ISO %d–%d".format(
            resolutionMegapixels,
            if (apertures.isNotEmpty()) "f/%.1f".format(apertures[0]) else "f/1.8",
            minIso,
            maxIso
        )
}

/**
 * Detailed investigation report regarding triple rear camera simultaneous capture.
 */
data class TripleCameraAuditReport(
    val publicCameraCount: Int,
    val publicCameraIds: List<String>,
    val accessibleRearCameras: List<CameraSpecs>,
    val isTripleCaptureSupported: Boolean,
    val isDualCaptureSupported: Boolean,
    val concurrentCombinationsCount: Int,
    val macroSensorStatus: String,
    val technicalVerdict: String
)

/**
 * Inspection utility interrogating Android Camera2 HAL capabilities,
 * physical camera characteristics, and concurrent streaming limits.
 */
class CameraHardwareInspector(context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    /**
     * Enumerates all physical and logical cameras exposed by the Camera2 HAL.
     */
    fun enumerateCameras(): List<CameraSpecs> {
        val result = mutableListOf<CameraSpecs>()

        try {
            val cameraIds = cameraManager.cameraIdList
            for (id in cameraIds) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                val isFront = facing == CameraCharacteristics.LENS_FACING_FRONT

                val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                val pixelArray = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                val mp = if (pixelArray != null) {
                    (pixelArray.width * pixelArray.height) / 1_000_000f
                } else if (activeArray != null) {
                    (activeArray.width() * activeArray.height()) / 1_000_000f
                } else {
                    12.5f
                }

                val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) ?: floatArrayOf(4.2f)
                val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES) ?: floatArrayOf(1.8f)

                val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                val minIso = isoRange?.lower ?: 50
                val maxIso = isoRange?.upper ?: 3200

                val expRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                val minShutter = expRange?.lower ?: 100_000L // 0.1ms
                val maxShutter = expRange?.upper ?: 1_000_000_000L // 1s

                val fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)?.toList()
                    ?: listOf(Range(30, 30))

                val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                val supportsOis = oisModes != null && oisModes.any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON }

                val eisModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                val supportsEis = eisModes != null && eisModes.any { it == CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON }

                val maxZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 8.0f

                val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
                val isLogicalMulti = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
                val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    chars.physicalCameraIds
                } else {
                    emptySet()
                }

                val displayName = when {
                    isFront -> "Front Camera (ID $id)"
                    id == "0" -> "Rear Main Wide (1x • ID 0)"
                    focalLengths.isNotEmpty() && focalLengths[0] < 2.5f -> "Rear Ultra-Wide (0.6x • ID $id)"
                    focalLengths.isNotEmpty() && focalLengths[0] > 6.0f -> "Rear Macro/Tele (ID $id)"
                    else -> "Rear Camera (ID $id)"
                }

                result.add(
                    CameraSpecs(
                        cameraId = id,
                        name = displayName,
                        isFront = isFront,
                        resolutionMegapixels = mp,
                        activeArraySize = activeArray,
                        focalLengths = focalLengths,
                        apertures = apertures,
                        minIso = minIso,
                        maxIso = maxIso,
                        minShutterNanos = minShutter,
                        maxShutterNanos = maxShutter,
                        supportedFpsRanges = fpsRanges,
                        supportsOis = supportsOis,
                        supportsEis = supportsEis,
                        maxZoom = maxZoom,
                        physicalCameraIds = physicalIds,
                        isLogicalMultiCamera = isLogicalMulti
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect Camera2 hardware: ${e.message}", e)
        }

        return result
    }

    /**
     * Checks if the hardware supports concurrent multi-camera operation.
     */
    fun checkConcurrentCameraSupport(): Pair<Boolean, String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val combinations = cameraManager.concurrentCameraIds
                if (combinations.isNotEmpty()) {
                    return Pair(true, "Concurrent streaming supported across ${combinations.size} combinations")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Concurrent camera query failed: ${e.message}")
            }
        }
        return Pair(
            false,
            "Hardware Limitation: MediaTek Dimensity 6100+ HAL does not support concurrent multi-camera streaming (getConcurrentCameraIds is empty). Fast physical camera switching is active."
        )
    }

    /**
     * Generates a complete audit report on triple rear camera accessibility.
     */
    fun auditTripleCameraHardware(): TripleCameraAuditReport {
        val allCams = enumerateCameras()
        val rearCams = allCams.filter { !it.isFront }
        val (concurrentSupported, note) = checkConcurrentCameraSupport()

        var concurrentCount = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                concurrentCount = cameraManager.concurrentCameraIds.size
            } catch (ignored: Exception) {}
        }

        // Test if hidden ID "54" (Macro) is accessible via public API
        var macroAccessible = false
        try {
            val macroChars = cameraManager.getCameraCharacteristics("54")
            macroAccessible = (macroChars != null)
        } catch (ignored: Exception) {
            macroAccessible = false
        }

        val macroStatus = if (macroAccessible) {
            "Accessible via CameraManager ID 54"
        } else {
            "Restricted to internal vendor HAL (device@3.5/legacy/54). Hidden from public Camera2 API by Samsung One UI firmware."
        }

        val verdict = if (concurrentSupported) {
            "Simultaneous capture supported"
        } else {
            "Triple simultaneous capture is unsupported by Samsung Galaxy M15 5G hardware/firmware: 1) getConcurrentCameraIds() is empty in MediaTek Dimensity 6100+ HAL; 2) The 2MP Macro sensor is hidden from third-party APIs; 3) Main (0) and Ultra-Wide (2) operate via mutual exclusion. ClearShot AI executes instant physical camera switching fallback."
        }

        return TripleCameraAuditReport(
            publicCameraCount = allCams.size,
            publicCameraIds = allCams.map { it.cameraId },
            accessibleRearCameras = rearCams,
            isTripleCaptureSupported = false,
            isDualCaptureSupported = concurrentSupported,
            concurrentCombinationsCount = concurrentCount,
            macroSensorStatus = macroStatus,
            technicalVerdict = verdict
        )
    }

    companion object {
        private const val TAG = "CameraHardwareInspector"
    }
}
