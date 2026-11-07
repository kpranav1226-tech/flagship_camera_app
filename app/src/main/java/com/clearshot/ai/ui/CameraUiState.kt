package com.clearshot.ai.ui

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.clearshot.ai.audio.AudioInputDevice
import com.clearshot.ai.camera.CameraMode
import com.clearshot.ai.camera.CameraSpecs
import com.clearshot.ai.camera.FlashMode
import com.clearshot.ai.camera.OrientationLock
import com.clearshot.ai.camera.VideoStabilizationMode
import com.clearshot.ai.processing.PipelineResult
import java.io.File

/**
 * Complete immutable UI state for ClearShot camera across Photo, Video, Pro Video,
 * scene modes, hardware controls, audio routing, overlays, and comparison view.
 */
data class CameraUiState(
    val hasCameraPermission: Boolean = false,
    val hasAudioPermission: Boolean = false,
    val isCameraInitialized: Boolean = false,
    val isFrontCamera: Boolean = false,
    val activeCameraId: String = "0",
    val flashMode: FlashMode = FlashMode.OFF,
    val cameraMode: CameraMode = CameraMode.PHOTO,
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 0.5f,
    val maxZoomRatio: Float = 100.0f,
    val nativeMaxZoomRatio: Float = 8.0f,
    val isCapturing: Boolean = false,
    val focusPoint: Offset? = null,
    val latestCapturedUri: Uri? = null,
    val errorMessage: String? = null,
    val userToastMessage: String? = null,

    // Latency & Background Processing Metrics
    val shutterToCaptureMs: Long = 0L,
    val enhancementDurationMs: Long = 0L,
    val isProcessing: Boolean = false,
    val isEnhancingInBackground: Boolean = false,
    val processingProgress: Float = 0f,
    val processingStatus: String = "",

    val originalCaptureFile: File? = null,
    val pipelineResult: PipelineResult? = null,
    val isComparisonVisible: Boolean = false,
    val isEnhancedSaved: Boolean = false,

    // Photo Pro Mode Controls
    val isProMode: Boolean = false,
    val burstCount: Int = 1,
    val exposureIndex: Int = 0,
    val minExposureIndex: Int = -4,
    val maxExposureIndex: Int = 4,
    val customSharpen: Float = 0.65f,
    val customDenoise: Float = 25f,
    val customClahe: Float = 1.6f,

    // Overlays & Horizon
    val isGridVisible: Boolean = false,
    val timerSeconds: Int = 0,
    val countdownRemaining: Int = 0,
    val isLevelEnabled: Boolean = false,
    val rollAngle: Float = 0f,
    val isLevel: Boolean = false,
    val orientationLock: OrientationLock = OrientationLock.AUTO,
    val isAutoHorizonCorrectionEnabled: Boolean = false,
    val horizonToleranceDeg: Float = 1.0f,
    val isSettingsSheetVisible: Boolean = false,

    // Video & Pro Video States
    val isRecording: Boolean = false,
    val isRecordingPaused: Boolean = false,
    val recordingDurationSeconds: Long = 0L,
    val recordedBytes: Long = 0L,
    val videoStabilizationMode: VideoStabilizationMode = VideoStabilizationMode.STANDARD,
    val videoQuality: String = "1080p", // "1080p", "720p"
    val videoFps: Int = 30, // 30, 60
    val videoBitrateMbps: Int = 9, // 6, 9, 14, 20

    // Pro Video Hardware Controls & Camera Selection
    val availableCameras: List<CameraSpecs> = emptyList(),
    val concurrentCameraSupported: Boolean = false,
    val concurrentCameraNote: String? = null,

    // Pro Audio Selection & VU Meter
    val availableAudioDevices: List<AudioInputDevice> = emptyList(),
    val selectedAudioDevice: AudioInputDevice? = null,
    val audioLevelDb: Float = -60f,
    val isAudioClipping: Boolean = false,

    // Pro Manual Image Controls
    val manualIso: Int? = null,
    val manualShutterNanos: Long? = null,
    val manualFocusDistance: Float? = null,
    val whiteBalanceMode: Int = 1, // 1 = Auto
    val isFocusLocked: Boolean = false,
    val isExposureLocked: Boolean = false,

    // Samsung Galaxy M15 Full UI Design Controls
    val aspectRatio: String = "3:4", // "1:1", "3:4", "9:16", "FULL"
    val photoResolution: String = "12M", // "12M", "50M", "5M", "2M"
    val isHdrEnabled: Boolean = true,
    val isSceneOptimizerEnabled: Boolean = true,
    val isRawCaptureEnabled: Boolean = false,
    val isEisEnabled: Boolean = true,
    val isAiOptimizationEnabled: Boolean = true,
    val detectedScene: String = "Sunset",
    val exposurePriority: String = "S", // "A", "M", "S", "I"
    val shutterSpeedText: String = "1/60",
    val evValue: Float = 0.0f,
    val isoValue: Int = 100,
    val isMoreModesSheetVisible: Boolean = false,

    // On-Demand Minimal UI & 100X Zoom State
    val isToolsSheetVisible: Boolean = false,
    val isProControlsSheetVisible: Boolean = false,
    val isZoomPresetsExpanded: Boolean = false,
    val processingStrength: String = "Balanced", // "Natural", "Balanced", "Max Clarity"
    val colorStyle: String = "Natural", // "Natural", "Vivid"
    val isHistogramEnabled: Boolean = false,
    val isFocusPeakingEnabled: Boolean = false,
    val activeToolsCategoryIndex: Int = 0, // 0: Basic, 1: Image, 2: Professional, 3: Advanced

    // Document Scanner State
    val isDocumentScanVisible: Boolean = false,
    val pendingDocumentFile: File? = null,
    val detectedDocumentCorners: com.clearshot.ai.processing.modes.DocumentCorners? = null,
    val isDocumentProcessing: Boolean = false,

    // Vivo X200-Inspired Zoom Finder (10X and Above)
    val isZoomFinderVisible: Boolean = false,
    val zoomFinderBitmap: android.graphics.Bitmap? = null,
    val zoomFinderPanOffset: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero
)
