package com.clearshot.ai.ui

import android.app.Application
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.clearshot.ai.audio.AudioInputDevice
import com.clearshot.ai.audio.AudioInputManager
import com.clearshot.ai.camera.CameraHardwareInspector
import com.clearshot.ai.camera.CameraManager
import com.clearshot.ai.camera.CameraMode
import com.clearshot.ai.camera.CameraXManager
import com.clearshot.ai.camera.DeviceOrientationManager
import com.clearshot.ai.camera.FlashMode
import com.clearshot.ai.camera.OrientationLock
import com.clearshot.ai.camera.VideoRecordingEvent
import com.clearshot.ai.camera.VideoStabilizationMode
import com.clearshot.ai.processing.ImageProcessor
import com.clearshot.ai.processing.OpenCVImageProcessor
import com.clearshot.ai.processing.modes.DocumentCorners
import com.clearshot.ai.processing.modes.DocumentFilterMode
import com.clearshot.ai.processing.modes.DocumentProcessor
import com.clearshot.ai.storage.CameraPreferences
import com.clearshot.ai.storage.MediaStorageManager
import com.clearshot.ai.storage.MediaStoreStorageManager
import com.clearshot.ai.storage.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Main ViewModel orchestrating:
 * - Sub-150ms immediate photo capture & non-blocking asynchronous enhancement queue.
 * - End-to-end EXIF orientation preservation across portrait, landscape, and sensor rotations.
 * - Independent Normal Video & Pro Video recording with stabilization and audio level metering.
 * - Hardware camera enumeration, manual ISO, shutter, focus distance, and audio routing.
 */
class CameraViewModel @JvmOverloads constructor(
    application: Application,
    private val cameraManager: CameraManager = CameraXManager(application.applicationContext),
    private val mediaStorageManager: MediaStorageManager = MediaStoreStorageManager(application.applicationContext),
    private val imageProcessor: ImageProcessor = OpenCVImageProcessor(application.applicationContext),
    private val orientationManager: DeviceOrientationManager = DeviceOrientationManager(application.applicationContext),
    private val cameraPreferences: CameraPreferences = CameraPreferences(application.applicationContext),
    private val hardwareInspector: CameraHardwareInspector = CameraHardwareInspector(application.applicationContext)
) : AndroidViewModel(application) {

    private val audioInputManager: AudioInputManager by lazy {
        AudioInputManager(getApplication<Application>().applicationContext, viewModelScope)
    }

    private val documentProcessor = DocumentProcessor()
    private val pdfExporter = PdfExporter()

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null

    // Asynchronous background enhancement queue to eliminate shutter lag
    private data class EnhancementJob(
        val inputFiles: List<File>,
        val mode: CameraMode,
        val customSharpen: Double?,
        val customDenoise: Double?,
        val customClahe: Double?,
        val horizonRollDegrees: Float?,
        val shutterToCaptureMs: Long,
        val digitalZoomRatio: Float = 1.0f,
        val aspectRatio: String = "FULL",
        val panOffsetX: Float = 0f,
        val panOffsetY: Float = 0f,
        val activeCameraId: String = "0"
    )

    // Bounded asynchronous enhancement queue with backpressure (maximum 3 in flight)
    private val enhancementChannel = Channel<EnhancementJob>(
        capacity = 3,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        // Load saved preferences
        val savedOrientation = cameraPreferences.getOrientationLock()
        val savedHorizonLevel = cameraPreferences.isHorizonLevelEnabled()
        val savedAutoHorizon = cameraPreferences.isAutoHorizonCorrectionEnabled()
        val savedTolerance = cameraPreferences.getHorizonTolerance()
        orientationManager.toleranceDeg = savedTolerance

        cameraManager.setOrientationLock(savedOrientation)

        // Inspect hardware cameras and concurrent camera capability
        val detectedCameras = hardwareInspector.enumerateCameras()
        val (concurrentSupported, concurrentNote) = hardwareInspector.checkConcurrentCameraSupport()

        _uiState.update {
            it.copy(
                orientationLock = savedOrientation,
                isLevelEnabled = savedHorizonLevel,
                isAutoHorizonCorrectionEnabled = savedAutoHorizon,
                horizonToleranceDeg = savedTolerance,
                availableCameras = detectedCameras,
                concurrentCameraSupported = concurrentSupported,
                concurrentCameraNote = concurrentNote
            )
        }

        // Observe CameraManager state
        viewModelScope.launch {
            cameraManager.cameraState.collect { cameraState ->
                _uiState.update { current ->
                    current.copy(
                        isCameraInitialized = cameraState.isInitialized,
                        isFrontCamera = cameraState.isFrontCamera,
                        activeCameraId = cameraState.activeCameraId,
                        flashMode = cameraState.flashMode,
                        zoomRatio = cameraState.zoomRatio,
                        minZoomRatio = cameraState.minZoomRatio,
                        maxZoomRatio = cameraState.maxZoomRatio,
                        nativeMaxZoomRatio = cameraState.nativeMaxZoomRatio,
                        isCapturing = cameraState.isCapturing,
                        focusPoint = cameraState.focusPoint,
                        exposureIndex = cameraState.exposureIndex,
                        minExposureIndex = cameraState.minExposureIndex,
                        maxExposureIndex = cameraState.maxExposureIndex,
                        errorMessage = cameraState.errorMessage ?: current.errorMessage,
                        isRecording = cameraState.isRecording,
                        isRecordingPaused = cameraState.isRecordingPaused,
                        recordingDurationSeconds = cameraState.recordingDurationSeconds,
                        recordedBytes = cameraState.recordedBytes,
                        videoStabilizationMode = cameraState.videoStabilizationMode,
                        videoQuality = cameraState.videoQuality,
                        videoFps = cameraState.videoFps,
                        manualIso = cameraState.manualIso,
                        manualShutterNanos = cameraState.manualShutterNanos,
                        manualFocusDistance = cameraState.manualFocusDistance,
                        whiteBalanceMode = cameraState.whiteBalanceMode,
                        isFocusLocked = cameraState.isFocusLocked,
                        isExposureLocked = cameraState.isExposureLocked,
                        // Vivo X200-Inspired Zoom Finder (Active > 10X)
                        zoomFinderBitmap = cameraState.zoomFinderBitmap ?: current.zoomFinderBitmap,
                        zoomFinderPanOffset = cameraState.zoomFinderPanOffset,
                        isZoomFinderVisible = cameraState.zoomRatio > 10.0f
                    )
                }
            }
        }

        // Observe device roll orientation for virtual horizon
        viewModelScope.launch {
            orientationManager.rollAngle.collect { angle ->
                _uiState.update { it.copy(rollAngle = angle) }
            }
        }

        viewModelScope.launch {
            orientationManager.isLevel.collect { level ->
                _uiState.update { it.copy(isLevel = level) }
            }
        }

        // Observe audio inputs and VU level meter
        viewModelScope.launch {
            audioInputManager.availableDevices.collect { devices ->
                _uiState.update { it.copy(availableAudioDevices = devices) }
            }
        }

        viewModelScope.launch {
            audioInputManager.selectedDevice.collect { sel ->
                _uiState.update { it.copy(selectedAudioDevice = sel) }
            }
        }

        viewModelScope.launch {
            audioInputManager.audioLevelDb.collect { db ->
                _uiState.update { it.copy(audioLevelDb = db) }
            }
        }

        viewModelScope.launch {
            audioInputManager.isClipping.collect { clip ->
                _uiState.update { it.copy(isAudioClipping = clip) }
            }
        }

        // Start orientation sensor if horizon level or auto-correction is active
        if (savedHorizonLevel || savedAutoHorizon) {
            orientationManager.start()
        }

        // Load latest gallery thumbnail
        viewModelScope.launch {
            val latest = mediaStorageManager.getLatestCapturedImageUri()
            _uiState.update { it.copy(latestCapturedUri = latest) }
        }

        // Start background asynchronous enhancement pipeline worker
        startEnhancementWorker()
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = isGranted) }
    }

    fun onAudioPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(hasAudioPermission = isGranted) }
        audioInputManager.refreshDevices()
    }

    fun onPreviewReady(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView
        viewModelScope.launch {
            val result = cameraManager.bindCamera(lifecycleOwner, previewView, _uiState.value.cameraMode)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to bind camera")
                }
            }
        }
    }

    fun onSwitchCamera() {
        viewModelScope.launch {
            cameraManager.switchCamera()
        }
    }

    fun onSelectCameraId(cameraId: String) {
        viewModelScope.launch {
            cameraManager.selectCameraId(cameraId)
        }
    }

    fun onCameraModeChanged(mode: CameraMode) {
        _uiState.update {
            it.copy(
                cameraMode = mode,
                isProMode = mode == CameraMode.PRO,
                burstCount = mode.defaultBurstCount
            )
        }

        // Manage audio VU meter lifecycle based on video mode
        if (mode.isVideo) {
            audioInputManager.startMeter()
        } else {
            audioInputManager.stopMeter()
        }

        // Rebind use cases if switching between photo and video mode
        val owner = currentLifecycleOwner
        val view = currentPreviewView
        if (owner != null && view != null) {
            viewModelScope.launch {
                cameraManager.bindCamera(owner, view, mode)
            }
        }
    }

    fun onOrientationLockChanged(newLock: OrientationLock) {
        _uiState.update { it.copy(orientationLock = newLock) }
        cameraPreferences.setOrientationLock(newLock)
        cameraManager.setOrientationLock(newLock)
    }

    fun onToggleAutoHorizonCorrection(enabled: Boolean) {
        _uiState.update { it.copy(isAutoHorizonCorrectionEnabled = enabled) }
        cameraPreferences.setAutoHorizonCorrectionEnabled(enabled)
        if (enabled || _uiState.value.isLevelEnabled) {
            orientationManager.start()
        } else {
            orientationManager.stop()
        }
    }

    fun onHorizonToleranceChanged(tolerance: Float) {
        _uiState.update { it.copy(horizonToleranceDeg = tolerance) }
        orientationManager.toleranceDeg = tolerance
        cameraPreferences.setHorizonTolerance(tolerance)
    }

    fun onToggleGrid() {
        _uiState.update { it.copy(isGridVisible = !it.isGridVisible) }
    }

    fun onCycleTimer() {
        val next = when (_uiState.value.timerSeconds) {
            0 -> 3
            3 -> 10
            else -> 0
        }
        _uiState.update { it.copy(timerSeconds = next) }
    }

    fun onToggleLevel() {
        val next = !_uiState.value.isLevelEnabled
        _uiState.update { it.copy(isLevelEnabled = next) }
        cameraPreferences.setHorizonLevelEnabled(next)
        if (next || _uiState.value.isAutoHorizonCorrectionEnabled) {
            orientationManager.start()
        } else {
            orientationManager.stop()
        }
    }

    fun onOpenSettingsSheet() {
        _uiState.update { it.copy(isSettingsSheetVisible = true) }
    }

    fun onDismissSettingsSheet() {
        _uiState.update { it.copy(isSettingsSheetVisible = false) }
    }

    fun onAspectRatioToggle() {
        val next = when (_uiState.value.aspectRatio.uppercase()) {
            "3:4" -> "9:16"
            "9:16" -> "1:1"
            "1:1" -> "16:9"
            "16:9" -> "FULL"
            else -> "3:4"
        }
        _uiState.update { it.copy(aspectRatio = next) }
    }

    fun onAspectRatioSelected(ratio: String) {
        _uiState.update { it.copy(aspectRatio = ratio) }
    }

    fun onResolutionToggle() {
        val next = when (_uiState.value.photoResolution) {
            "12M" -> "50M"
            "50M" -> "5M"
            "5M" -> "2M"
            else -> "12M"
        }
        _uiState.update { it.copy(photoResolution = next) }
    }

    fun onHdrToggle() {
        _uiState.update { it.copy(isHdrEnabled = !it.isHdrEnabled) }
    }

    fun onSceneOptimizerToggle() {
        _uiState.update { it.copy(isSceneOptimizerEnabled = !it.isSceneOptimizerEnabled) }
    }

    fun onRawCaptureToggle() {
        _uiState.update { it.copy(isRawCaptureEnabled = !it.isRawCaptureEnabled) }
    }

    fun onEisToggle() {
        val next = !_uiState.value.isEisEnabled
        val stab = if (next) VideoStabilizationMode.SUPER_STEADY else VideoStabilizationMode.OFF
        _uiState.update { it.copy(isEisEnabled = next, videoStabilizationMode = stab) }
        viewModelScope.launch {
            cameraManager.setVideoStabilization(stab)
        }
    }

    fun onAiOptimizationToggle() {
        _uiState.update { it.copy(isAiOptimizationEnabled = !it.isAiOptimizationEnabled) }
    }

    fun onExposurePriorityChanged(priority: String) {
        _uiState.update { it.copy(exposurePriority = priority) }
    }

    fun onEvSliderChanged(ev: Float) {
        _uiState.update { it.copy(evValue = ev) }
        val index = kotlin.math.round((ev * 2f)).toInt()
        onExposureChanged(index)
    }

    fun onToggleMoreModesSheet() {
        _uiState.update { it.copy(isMoreModesSheetVisible = !it.isMoreModesSheetVisible) }
    }

    fun onDismissMoreModesSheet() {
        _uiState.update { it.copy(isMoreModesSheetVisible = false) }
    }

    fun onToggleToolsSheet() {
        _uiState.update { it.copy(isToolsSheetVisible = !it.isToolsSheetVisible) }
    }

    fun onDismissToolsSheet() {
        _uiState.update { it.copy(isToolsSheetVisible = false) }
    }

    fun onSelectToolsCategory(categoryIndex: Int) {
        _uiState.update { it.copy(activeToolsCategoryIndex = categoryIndex) }
    }

    fun onToggleProControlsSheet() {
        _uiState.update { it.copy(isProControlsSheetVisible = !it.isProControlsSheetVisible) }
    }

    fun onDismissProControlsSheet() {
        _uiState.update { it.copy(isProControlsSheetVisible = false) }
    }

    fun onToggleZoomPresets() {
        _uiState.update { it.copy(isZoomPresetsExpanded = !it.isZoomPresetsExpanded) }
    }

    fun onDismissZoomPresets() {
        _uiState.update { it.copy(isZoomPresetsExpanded = false) }
    }

    fun onProcessingStrengthChanged(strength: String) {
        _uiState.update { it.copy(processingStrength = strength) }
    }

    fun onColorStyleChanged(style: String) {
        _uiState.update { it.copy(colorStyle = style) }
    }

    fun onToggleHistogram() {
        _uiState.update { it.copy(isHistogramEnabled = !it.isHistogramEnabled) }
    }

    fun onToggleFocusPeaking() {
        _uiState.update { it.copy(isFocusPeakingEnabled = !it.isFocusPeakingEnabled) }
    }

    fun onFlashModeToggle(flashMode: FlashMode) {
        viewModelScope.launch {
            cameraManager.setFlashMode(flashMode)
        }
    }

    fun onZoomChanged(zoomRatio: Float) {
        val clampedZoom = zoomRatio.coerceIn(_uiState.value.minZoomRatio, _uiState.value.maxZoomRatio)
        val shouldResetPan = clampedZoom <= 10.0f && _uiState.value.zoomFinderPanOffset != androidx.compose.ui.geometry.Offset.Zero
        val newPan = if (shouldResetPan) androidx.compose.ui.geometry.Offset.Zero else _uiState.value.zoomFinderPanOffset

        _uiState.update {
            it.copy(
                zoomRatio = clampedZoom,
                zoomFinderPanOffset = newPan,
                isZoomFinderVisible = clampedZoom > 10.0f
            )
        }
        if (shouldResetPan) {
            cameraManager.setZoomFinderPanOffset(androidx.compose.ui.geometry.Offset.Zero)
        }

        viewModelScope.launch {
            if (!_uiState.value.isFrontCamera) {
                // Physical lens switching on Galaxy M15 rear camera array:
                // 0.6x (zoom < 0.9x) binds to Camera ID 2 (Ultra-Wide 5MP f/2.2)
                // 1.0x+ binds to Camera ID 0 (Main Wide 12.5MP f/1.8)
                if (clampedZoom < 0.9f && _uiState.value.activeCameraId != "2") {
                    cameraManager.selectCameraId("2")
                } else if (clampedZoom >= 1.0f && _uiState.value.activeCameraId == "2") {
                    cameraManager.selectCameraId("0")
                }
            }
            cameraManager.setZoomRatio(clampedZoom)
        }
    }

    fun onZoomFinderPanChanged(offset: androidx.compose.ui.geometry.Offset) {
        _uiState.update { it.copy(zoomFinderPanOffset = offset) }
        cameraManager.setZoomFinderPanOffset(offset)
    }

    fun onResetZoomFinderPan() {
        _uiState.update { it.copy(zoomFinderPanOffset = androidx.compose.ui.geometry.Offset.Zero) }
        cameraManager.setZoomFinderPanOffset(androidx.compose.ui.geometry.Offset.Zero)
    }

    fun onPinchZoom(zoomFactor: Float) {
        val current = _uiState.value.zoomRatio
        val newTarget = (current * zoomFactor).coerceIn(
            _uiState.value.minZoomRatio,
            _uiState.value.maxZoomRatio
        )
        onZoomChanged(newTarget)
    }

    fun onFocusTap(x: Float, y: Float, width: Int, height: Int) {
        viewModelScope.launch {
            cameraManager.focusAndMeter(x, y, width, height)
        }
    }

    fun onToggleProMode() {
        val newProState = !_uiState.value.isProMode
        val targetMode = if (newProState) CameraMode.PRO else CameraMode.PHOTO
        onCameraModeChanged(targetMode)
    }

    fun onBurstCountChanged(count: Int) {
        _uiState.update { it.copy(burstCount = count) }
    }

    fun onExposureChanged(index: Int) {
        viewModelScope.launch {
            cameraManager.setExposureCompensation(index)
        }
    }

    fun onSharpenChanged(value: Float) {
        _uiState.update { it.copy(customSharpen = value) }
    }

    fun onDenoiseChanged(value: Float) {
        _uiState.update { it.copy(customDenoise = value) }
    }

    fun onClaheChanged(value: Float) {
        _uiState.update { it.copy(customClahe = value) }
    }

    // --- SHUTTER & CAPTURE LOGIC ---

    fun onCaptureClicked() {
        val mode = _uiState.value.cameraMode

        if (mode.isVideo) {
            // Video or Pro Video mode: record toggle
            if (_uiState.value.isRecording) {
                stopVideoRecording()
            } else {
                startVideoRecording(isProVideo = mode == CameraMode.PRO_VIDEO)
            }
            return
        }

        if (_uiState.value.isCapturing) return

        val timer = _uiState.value.timerSeconds
        if (timer > 0) {
            viewModelScope.launch {
                for (s in timer downTo 1) {
                    _uiState.update { it.copy(countdownRemaining = s) }
                    delay(1000)
                }
                _uiState.update { it.copy(countdownRemaining = 0) }
                performImmediatePhotoCapture()
            }
        } else {
            performImmediatePhotoCapture()
        }
    }

    /**
     * Executes photo capture with instant shutter release (< 150ms) and queues enhancement asynchronously.
     */
    private fun performImmediatePhotoCapture() {
        val mode = _uiState.value.cameraMode
        val isPro = mode == CameraMode.PRO
        val burstCount = if (isPro) _uiState.value.burstCount else mode.defaultBurstCount
        val customSharpen = if (isPro) _uiState.value.customSharpen.toDouble() else null
        val customDenoise = if (isPro) _uiState.value.customDenoise.toDouble() else null
        val customClahe = if (isPro) _uiState.value.customClahe.toDouble() else null
        val horizonRoll = if (_uiState.value.isAutoHorizonCorrectionEnabled) _uiState.value.rollAngle else null
        val currentZoom = _uiState.value.zoomRatio
        val currentAspectRatio = _uiState.value.aspectRatio

        // Specialized handling for Independent Document Scanner mode
        if (mode == CameraMode.DOCUMENT) {
            viewModelScope.launch {
                val tShutterStart = System.currentTimeMillis()
                _uiState.update { it.copy(isCapturing = true, errorMessage = null) }
                try {
                    val captureResult = cameraManager.capturePhoto()
                    val rawFile = captureResult.getOrThrow()
                    val shutterLatency = System.currentTimeMillis() - tShutterStart

                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            shutterToCaptureMs = shutterLatency,
                            isDocumentProcessing = true
                        )
                    }

                    // Automatic 4-corner document boundary detection
                    val detectedCorners = documentProcessor.detectCorners(rawFile)
                    _uiState.update {
                        it.copy(
                            isDocumentScanVisible = true,
                            pendingDocumentFile = rawFile,
                            detectedDocumentCorners = detectedCorners,
                            isDocumentProcessing = false
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Document capture failed: ${e.message}", e)
                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            isDocumentProcessing = false,
                            errorMessage = "Document capture failed: ${e.localizedMessage}"
                        )
                    }
                }
            }
            return
        }

        viewModelScope.launch {
            val tShutterStart = System.currentTimeMillis()
            _uiState.update { it.copy(isCapturing = true, errorMessage = null) }

            try {
                val effectiveBurst = when {
                    burstCount > 1 -> burstCount
                    mode == CameraMode.NIGHT -> 3 // Multi-frame temporal night stacking
                    _uiState.value.isHdrEnabled -> 3 // Smart HDR multi-exposure fusion
                    else -> 1
                }

                if (effectiveBurst > 1) {
                    val burstResult = cameraManager.captureBurst(effectiveBurst)
                    val burstFiles = burstResult.getOrThrow()
                    val tCaptureEnd = System.currentTimeMillis()
                    val shutterLatency = tCaptureEnd - tShutterStart

                    val primaryIndex = if (burstFiles.size > 1) burstFiles.size / 2 else 0
                    val primaryFile = burstFiles[primaryIndex]

                    val originalSaveResult = mediaStorageManager.saveImageToGallery(primaryFile)
                    val originalUri = originalSaveResult.getOrNull()

                    // Free viewfinder immediately
                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            latestCapturedUri = originalUri ?: it.latestCapturedUri,
                            shutterToCaptureMs = shutterLatency,
                            originalCaptureFile = primaryFile
                        )
                    }

                    // Enqueue asynchronous multi-frame fusion
                    enhancementChannel.trySend(
                        EnhancementJob(
                            inputFiles = burstFiles,
                            mode = mode,
                            customSharpen = customSharpen,
                            customDenoise = customDenoise,
                            customClahe = customClahe,
                            horizonRollDegrees = horizonRoll,
                            shutterToCaptureMs = shutterLatency,
                            digitalZoomRatio = currentZoom,
                            aspectRatio = currentAspectRatio,
                            panOffsetX = _uiState.value.zoomFinderPanOffset.x,
                            panOffsetY = _uiState.value.zoomFinderPanOffset.y,
                            activeCameraId = _uiState.value.activeCameraId
                        )
                    )
                } else {
                    val captureResult = cameraManager.capturePhoto()
                    val rawFile = captureResult.getOrThrow()
                    val tCaptureEnd = System.currentTimeMillis()
                    val shutterLatency = tCaptureEnd - tShutterStart

                    // Save original immediately into DCIM/ClearShot
                    val originalSaveResult = mediaStorageManager.saveImageToGallery(rawFile)
                    val originalUri = originalSaveResult.getOrNull()

                    // Free viewfinder immediately
                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            latestCapturedUri = originalUri ?: it.latestCapturedUri,
                            shutterToCaptureMs = shutterLatency,
                            originalCaptureFile = rawFile
                        )
                    }

                    // Enqueue asynchronous AI enhancement
                    enhancementChannel.trySend(
                        EnhancementJob(
                            inputFiles = listOf(rawFile),
                            mode = mode,
                            customSharpen = customSharpen,
                            customDenoise = customDenoise,
                            customClahe = customClahe,
                            horizonRollDegrees = horizonRoll,
                            shutterToCaptureMs = shutterLatency,
                            digitalZoomRatio = currentZoom,
                            aspectRatio = currentAspectRatio,
                            panOffsetX = _uiState.value.zoomFinderPanOffset.x,
                            panOffsetY = _uiState.value.zoomFinderPanOffset.y,
                            activeCameraId = _uiState.value.activeCameraId
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fast capture failed: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isCapturing = false,
                        errorMessage = e.localizedMessage ?: "Capture error"
                    )
                }
            }
        }
    }

    /**
     * Dedicated asynchronous background worker executing OpenCV enhancements serially.
     */
    private fun startEnhancementWorker() {
        viewModelScope.launch(Dispatchers.Default) {
            for (job in enhancementChannel) {
                try {
                    _uiState.update {
                        it.copy(
                            isEnhancingInBackground = true,
                            processingProgress = 0.05f,
                            processingStatus = "AI Enhancing (${job.mode.title})..."
                        )
                    }

                    val tStart = System.currentTimeMillis()
                    val pipelineResult = if (job.inputFiles.size > 1) {
                        imageProcessor.processMultiFrame(
                            inputFiles = job.inputFiles,
                            cameraMode = job.mode,
                            customSharpen = job.customSharpen,
                            customDenoise = job.customDenoise,
                            customClahe = job.customClahe,
                            horizonRollDegrees = job.horizonRollDegrees,
                            digitalZoomRatio = job.digitalZoomRatio,
                            aspectRatio = job.aspectRatio,
                            panOffsetX = job.panOffsetX,
                            panOffsetY = job.panOffsetY,
                            activeCameraId = job.activeCameraId
                        ) { progress, message ->
                            _uiState.update {
                                it.copy(
                                    processingProgress = progress,
                                    processingStatus = message
                                )
                            }
                        }.getOrThrow()
                    } else {
                        imageProcessor.processWithDetails(
                            inputFile = job.inputFiles[0],
                            cameraMode = job.mode,
                            customSharpen = job.customSharpen,
                            customDenoise = job.customDenoise,
                            customClahe = job.customClahe,
                            horizonRollDegrees = job.horizonRollDegrees,
                            digitalZoomRatio = job.digitalZoomRatio,
                            aspectRatio = job.aspectRatio,
                            panOffsetX = job.panOffsetX,
                            panOffsetY = job.panOffsetY,
                            activeCameraId = job.activeCameraId
                        ) { progress, message ->
                            _uiState.update {
                                it.copy(
                                    processingProgress = progress,
                                    processingStatus = message
                                )
                            }
                        }.getOrThrow()
                    }

                    val tEnd = System.currentTimeMillis()
                    val durationMs = tEnd - tStart

                    // Auto-save enhanced photograph to DCIM/ClearShot/Enhanced
                    val enhancedSave = mediaStorageManager.saveEnhancedImageToGallery(pipelineResult.enhancedFile)
                    val enhancedUri = enhancedSave.getOrNull()

                    val statusToast = "${job.mode.title} enhanced in ${durationMs}ms (Shutter: ${job.shutterToCaptureMs}ms)"

                    _uiState.update {
                        it.copy(
                            isEnhancingInBackground = false,
                            processingProgress = 1.0f,
                            enhancementDurationMs = durationMs,
                            pipelineResult = pipelineResult,
                            latestCapturedUri = enhancedUri ?: it.latestCapturedUri,
                            userToastMessage = statusToast
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Asynchronous enhancement job failed: ${e.message}", e)
                    _uiState.update {
                        it.copy(
                            isEnhancingInBackground = false,
                            errorMessage = "Enhancement error: ${e.localizedMessage}"
                        )
                    }
                }
            }
        }
    }

    // --- DOCUMENT SCANNER INTERACTIVE ACTIONS ---

    fun onDocumentRetake() {
        _uiState.value.pendingDocumentFile?.delete()
        _uiState.update {
            it.copy(
                isDocumentScanVisible = false,
                pendingDocumentFile = null,
                detectedDocumentCorners = null,
                isDocumentProcessing = false
            )
        }
    }

    fun onDocumentAutoDetect() {
        val file = _uiState.value.pendingDocumentFile ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isDocumentProcessing = true) }
            val corners = documentProcessor.detectCorners(file)
            _uiState.update {
                it.copy(
                    detectedDocumentCorners = corners,
                    isDocumentProcessing = false
                )
            }
        }
    }

    fun onSaveDocumentJpeg(
        corners: DocumentCorners,
        filter: DocumentFilterMode,
        rotationDegrees: Int = 0
    ) {
        val rawFile = _uiState.value.pendingDocumentFile ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isDocumentProcessing = true) }
            try {
                val tempWarped = File.createTempFile("doc_scan_", ".jpg", getApplication<Application>().cacheDir)
                val warpedFile = documentProcessor.warpAndEnhance(
                    inputFile = rawFile,
                    outputFile = tempWarped,
                    corners = corners,
                    filterMode = filter,
                    rotationDegrees = rotationDegrees
                ).getOrThrow()

                val saveResult = mediaStorageManager.saveDocumentScan(warpedFile)
                val uri = saveResult.getOrThrow()

                _uiState.update {
                    it.copy(
                        isDocumentScanVisible = false,
                        pendingDocumentFile = null,
                        detectedDocumentCorners = null,
                        isDocumentProcessing = false,
                        latestCapturedUri = uri,
                        userToastMessage = "Document scan saved to Gallery"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save document JPEG: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isDocumentProcessing = false,
                        errorMessage = "Save failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onSaveDocumentPdf(
        corners: DocumentCorners,
        filter: DocumentFilterMode,
        rotationDegrees: Int = 0
    ) {
        val rawFile = _uiState.value.pendingDocumentFile ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isDocumentProcessing = true) }
            try {
                val tempWarped = File.createTempFile("doc_scan_", ".jpg", getApplication<Application>().cacheDir)
                val warpedFile = documentProcessor.warpAndEnhance(
                    inputFile = rawFile,
                    outputFile = tempWarped,
                    corners = corners,
                    filterMode = filter,
                    rotationDegrees = rotationDegrees
                ).getOrThrow()

                val tempPdf = File.createTempFile("ClearShot_Doc_", ".pdf", getApplication<Application>().cacheDir)
                pdfExporter.exportImageToPdf(warpedFile, tempPdf).getOrThrow()

                val saveResult = mediaStorageManager.savePdfDocument(tempPdf)
                val uri = saveResult.getOrThrow()

                _uiState.update {
                    it.copy(
                        isDocumentScanVisible = false,
                        pendingDocumentFile = null,
                        detectedDocumentCorners = null,
                        isDocumentProcessing = false,
                        latestCapturedUri = uri,
                        userToastMessage = "Document PDF saved to Documents/ClearShot"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export document PDF: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isDocumentProcessing = false,
                        errorMessage = "PDF export failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    // --- VIDEO RECORDING CONTROLS ---

    fun startVideoRecording(isProVideo: Boolean) {
        viewModelScope.launch {
            val audioDevice = _uiState.value.selectedAudioDevice?.rawDeviceInfo
            val result = cameraManager.startRecording(
                isProVideo = isProVideo,
                audioDeviceInfo = audioDevice
            ) { event ->
                when (event) {
                    is VideoRecordingEvent.Started -> {
                        _uiState.update { it.copy(isRecording = true, isRecordingPaused = false) }
                    }
                    is VideoRecordingEvent.Paused -> {
                        _uiState.update { it.copy(isRecordingPaused = true) }
                    }
                    is VideoRecordingEvent.Resumed -> {
                        _uiState.update { it.copy(isRecordingPaused = false) }
                    }
                    is VideoRecordingEvent.Status -> {
                        _uiState.update {
                            it.copy(
                                recordingDurationSeconds = event.durationSeconds,
                                recordedBytes = event.bytes
                            )
                        }
                    }
                    is VideoRecordingEvent.Finalized -> {
                        _uiState.update {
                            it.copy(
                                isRecording = false,
                                isRecordingPaused = false,
                                recordingDurationSeconds = 0L,
                                latestCapturedUri = event.outputUri ?: it.latestCapturedUri,
                                userToastMessage = if (event.outputUri != null) "Video saved to gallery" else "Recording failed: ${event.error?.localizedMessage}"
                            )
                        }
                    }
                }
            }
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to start recording")
                }
            }
        }
    }

    fun pauseVideoRecording() {
        viewModelScope.launch { cameraManager.pauseRecording() }
    }

    fun resumeVideoRecording() {
        viewModelScope.launch { cameraManager.resumeRecording() }
    }

    fun stopVideoRecording() {
        viewModelScope.launch { cameraManager.stopRecording() }
    }

    fun onStabilizationModeChanged(mode: VideoStabilizationMode) {
        viewModelScope.launch { cameraManager.setVideoStabilization(mode) }
    }

    fun onVideoQualityChanged(quality: String) {
        viewModelScope.launch { cameraManager.setVideoQuality(quality) }
    }

    fun onVideoFpsChanged(fps: Int) {
        viewModelScope.launch { cameraManager.setVideoFps(fps) }
    }

    fun onVideoBitrateChanged(bitrateMbps: Int) {
        _uiState.update { it.copy(videoBitrateMbps = bitrateMbps) }
        viewModelScope.launch { cameraManager.setVideoBitrate(bitrateMbps * 1_000_000) }
    }

    // --- PRO AUDIO & CAMERA SELECTION ---

    fun onSelectAudioDevice(device: AudioInputDevice) {
        audioInputManager.selectDevice(device)
    }

    // --- PRO MANUAL IMAGE CONTROLS ---

    fun onIsoChanged(iso: Int?) {
        viewModelScope.launch { cameraManager.setManualIso(iso) }
    }

    fun onShutterSpeedChanged(nanos: Long?) {
        viewModelScope.launch { cameraManager.setManualShutterSpeed(nanos) }
    }

    fun onFocusDistanceChanged(distance: Float?) {
        viewModelScope.launch { cameraManager.setManualFocusDistance(distance) }
    }

    fun onWhiteBalanceChanged(mode: Int) {
        viewModelScope.launch { cameraManager.setWhiteBalanceMode(mode) }
    }

    fun onToggleFocusLock() {
        viewModelScope.launch { cameraManager.toggleFocusLock() }
    }

    fun onToggleExposureLock() {
        viewModelScope.launch { cameraManager.toggleExposureLock() }
    }

    // --- COMPARISON & TOAST MANAGEMENT ---

    fun onThumbnailClicked() {
        if (_uiState.value.originalCaptureFile != null && _uiState.value.pipelineResult != null) {
            _uiState.update { it.copy(isComparisonVisible = true) }
        }
    }

    fun onSaveEnhancedClicked() {
        val pipelineResult = _uiState.value.pipelineResult ?: return
        viewModelScope.launch {
            try {
                val saveResult = mediaStorageManager.saveEnhancedImageToGallery(pipelineResult.enhancedFile)
                if (saveResult.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isEnhancedSaved = true,
                            latestCapturedUri = saveResult.getOrThrow(),
                            userToastMessage = "Enhanced photo saved to DCIM/ClearShot/Enhanced"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Manual save enhanced failed: ${e.message}", e)
            }
        }
    }

    fun onDismissComparison() {
        _uiState.update { it.copy(isComparisonVisible = false) }
    }

    fun onToastDismissed() {
        _uiState.update { it.copy(userToastMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioInputManager.release()
        cameraManager.release()
        orientationManager.stop()
    }

    companion object {
        private const val TAG = "CameraViewModel"
    }
}
