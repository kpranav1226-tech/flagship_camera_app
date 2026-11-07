package com.clearshot.ai.camera

import androidx.compose.ui.geometry.Offset

/**
 * Immutable state representing the camera subsystem across Photo, Video, and Pro Video.
 */
data class CameraState(
    val isInitialized: Boolean = false,
    val isFrontCamera: Boolean = false,
    val activeCameraId: String = "0",
    val flashMode: FlashMode = FlashMode.OFF,
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 0.5f,
    val maxZoomRatio: Float = 100.0f,
    val nativeMaxZoomRatio: Float = 8.0f,
    val isCapturing: Boolean = false,
    val focusPoint: Offset? = null,
    val errorMessage: String? = null,
    val exposureIndex: Int = 0,
    val minExposureIndex: Int = -4,
    val maxExposureIndex: Int = 4,
    val isProMode: Boolean = false,
    val burstCount: Int = 1,
    // Video recording states
    val isRecording: Boolean = false,
    val isRecordingPaused: Boolean = false,
    val recordingDurationSeconds: Long = 0L,
    val recordedBytes: Long = 0L,
    val videoStabilizationMode: VideoStabilizationMode = VideoStabilizationMode.STANDARD,
    val videoQuality: String = "1080p", // "1080p", "720p"
    val videoFps: Int = 30, // 30, 60
    val videoBitrateBps: Int = 9_000_000,
    // Manual Pro controls
    val manualIso: Int? = null,
    val manualShutterNanos: Long? = null,
    val manualFocusDistance: Float? = null,
    val whiteBalanceMode: Int = 1,
    val isFocusLocked: Boolean = false,
    val isExposureLocked: Boolean = false,
    // Vivo X200-Inspired Zoom Finder (10X and Above)
    val zoomFinderBitmap: android.graphics.Bitmap? = null,
    val zoomFinderPanOffset: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero
)
