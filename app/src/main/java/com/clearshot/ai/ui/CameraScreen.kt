package com.clearshot.ai.ui

import android.content.res.Configuration
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.clearshot.ai.audio.AudioInputDevice
import com.clearshot.ai.camera.CameraMode
import com.clearshot.ai.camera.FlashMode
import com.clearshot.ai.camera.OrientationLock
import com.clearshot.ai.camera.VideoStabilizationMode
import com.clearshot.ai.processing.modes.DocumentCorners
import com.clearshot.ai.processing.modes.DocumentFilterMode
import com.clearshot.ai.ui.components.BackgroundEnhancementIndicator
import com.clearshot.ai.ui.components.CameraLeftToolbar
import com.clearshot.ai.ui.components.CameraPreviewView
import com.clearshot.ai.ui.components.CameraSettingsSheet
import com.clearshot.ai.ui.components.CenterFramingReticle
import com.clearshot.ai.ui.components.CountdownTimerOverlay
import com.clearshot.ai.ui.components.ExposurePrioritySelector
import com.clearshot.ai.ui.components.ExposureRulerSlider
import com.clearshot.ai.ui.components.FocusIndicator
import com.clearshot.ai.ui.components.GridOverlay
import com.clearshot.ai.ui.components.HorizonLevelIndicator
import com.clearshot.ai.ui.components.MinimalCameraTopBar
import com.clearshot.ai.ui.components.MoreModesSheet
import com.clearshot.ai.ui.components.NormalVideoControls
import com.clearshot.ai.ui.components.OnDemandToolsSheet
import com.clearshot.ai.ui.components.PermissionRationale
import com.clearshot.ai.ui.components.ProManualReadoutRow
import com.clearshot.ai.ui.components.ProPhotoControlsSheet
import com.clearshot.ai.ui.components.ProVideoControls
import com.clearshot.ai.ui.components.QuickZoomBar
import com.clearshot.ai.ui.components.SamsungCameraTopBar
import com.clearshot.ai.ui.components.SamsungModeCarousel
import com.clearshot.ai.ui.components.ShutterButton
import com.clearshot.ai.ui.components.ThumbnailView
import com.clearshot.ai.ui.components.VerticalZoomSelector
import com.clearshot.ai.ui.components.ZoomFinderView
import com.clearshot.ai.ui.theme.BackgroundDark
import com.clearshot.ai.ui.theme.OverlayPill

/**
 * Clean, minimal, professional Camera Interface:
 *
 * Default Viewfinder:
 * - Unobstructed full-screen live camera preview.
 * - Minimal top bar: Settings (⚙️), Flash shortcut (⚡), Tools button (🛠️).
 * - Zoom pill: Shows current zoom ratio; expands to 0.5X - 100X presets on tap.
 * - Bottom action bar: Gallery thumbnail, Shutter button, Camera switch button.
 * - Bottom mode selector carousel: PRO, PHOTO, VIDEO, PORTRAIT, NIGHT, MORE.
 *
 * On-Demand Controls:
 * - Tools button opens organized floating sheet (Basic, Image, Professional, Advanced).
 * - Pro Photo mode has its own Pro Controls sheet.
 * - Pro Video mode has its own independent cine recording dashboard.
 * - 100X digital zoom with bicubic sharpening and bilateral noise reduction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    uiState: CameraUiState,
    onPermissionRequested: () -> Unit,
    onFlashToggle: (FlashMode) -> Unit,
    onZoomChange: (Float) -> Unit,
    onPinchZoom: (Float) -> Unit,
    onFocusTap: (x: Float, y: Float, width: Int, height: Int) -> Unit,
    onCaptureClick: () -> Unit,
    onPreviewReady: (LifecycleOwner, PreviewView) -> Unit,
    onModeSelected: (CameraMode) -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleGrid: () -> Unit,
    onCycleTimer: () -> Unit,
    onToggleLevel: () -> Unit,
    onToggleProMode: () -> Unit,
    onBurstCountChanged: (Int) -> Unit,
    onExposureChanged: (Int) -> Unit,
    onSharpenChanged: (Float) -> Unit,
    onDenoiseChanged: (Float) -> Unit,
    onClaheChanged: (Float) -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onOrientationLockChanged: (OrientationLock) -> Unit,
    onToggleAutoHorizonCorrection: (Boolean) -> Unit,
    onHorizonToleranceChanged: (Float) -> Unit,
    onSaveEnhanced: () -> Unit,
    onDismissComparison: () -> Unit,
    onToastDismissed: () -> Unit,
    // Samsung M15 Design Callbacks
    onAspectRatioToggle: () -> Unit = {},
    onResolutionToggle: () -> Unit = {},
    onHdrToggle: () -> Unit = {},
    onSceneOptimizerToggle: () -> Unit = {},
    onRawCaptureToggle: () -> Unit = {},
    onEisToggle: () -> Unit = {},
    onAiOptimizationToggle: () -> Unit = {},
    onExposurePriorityChanged: (String) -> Unit = {},
    onEvSliderChanged: (Float) -> Unit = {},
    onToggleMoreModesSheet: () -> Unit = {},
    onDismissMoreModesSheet: () -> Unit = {},
    // Video and Pro Video controls
    onStartRecording: (isProVideo: Boolean) -> Unit = {},
    onPauseRecording: () -> Unit = {},
    onResumeRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onStabilizationModeChanged: (VideoStabilizationMode) -> Unit = {},
    onVideoQualityChanged: (String) -> Unit = {},
    onVideoFpsChanged: (Int) -> Unit = {},
    onVideoBitrateChanged: (Int) -> Unit = {},
    onSelectCameraId: (String) -> Unit = {},
    onSelectAudioDevice: (AudioInputDevice) -> Unit = {},
    onIsoChanged: (Int?) -> Unit = {},
    onShutterSpeedChanged: (Long?) -> Unit = {},
    onFocusDistanceChanged: (Float?) -> Unit = {},
    onWhiteBalanceChanged: (Int) -> Unit = {},
    onToggleFocusLock: () -> Unit = {},
    onToggleExposureLock: () -> Unit = {},
    onThumbnailClick: () -> Unit = {},
    // On-Demand Tools & Pro Controls Sheet Callbacks
    onToggleToolsSheet: () -> Unit = {},
    onDismissToolsSheet: () -> Unit = {},
    onSelectToolsCategory: (Int) -> Unit = {},
    onToggleProControlsSheet: () -> Unit = {},
    onDismissProControlsSheet: () -> Unit = {},
    onToggleZoomPresets: () -> Unit = {},
    onProcessingStrengthChanged: (String) -> Unit = {},
    onColorStyleChanged: (String) -> Unit = {},
    onToggleHistogram: () -> Unit = {},
    onToggleFocusPeaking: () -> Unit = {},
    // Document Scanner Interactive Callbacks
    onDocumentRetake: () -> Unit = {},
    onDocumentAutoDetect: () -> Unit = {},
    onSaveDocumentJpeg: (DocumentCorners, DocumentFilterMode, Int) -> Unit = { _, _, _ -> },
    onSaveDocumentPdf: (DocumentCorners, DocumentFilterMode, Int) -> Unit = { _, _, _ -> },
    // Vivo X200-Inspired Zoom Finder Callbacks
    onZoomFinderPanChanged: (Offset) -> Unit = {},
    onResetZoomFinderPan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Toast feedback
    LaunchedEffect(uiState.userToastMessage) {
        uiState.userToastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            onToastDismissed()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    if (!uiState.hasCameraPermission) {
        PermissionRationale(onRequestPermission = onPermissionRequested)
        return
    }

    // Comparison screen
    if (uiState.isComparisonVisible && uiState.originalCaptureFile != null && uiState.pipelineResult != null) {
        ComparisonScreen(
            originalFile = uiState.originalCaptureFile,
            enhancedResult = uiState.pipelineResult,
            isSavedToGallery = uiState.isEnhancedSaved,
            onSaveEnhancedClicked = onSaveEnhanced,
            onBackClicked = onDismissComparison
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // 1. Full Screen Camera Viewfinder Preview
        CameraPreviewView(
            zoomRatio = uiState.zoomRatio,
            nativeMaxZoom = uiState.nativeMaxZoomRatio,
            panOffset = uiState.zoomFinderPanOffset,
            onPreviewReady = { previewView ->
                onPreviewReady(lifecycleOwner, previewView)
            },
            onTapToFocus = onFocusTap,
            onPinchZoom = onPinchZoom,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Center Overlays & Reticle
        GridOverlay(isVisible = uiState.isGridVisible)

        HorizonLevelIndicator(
            rollAngle = uiState.rollAngle,
            isEnabled = uiState.isLevelEnabled,
            toleranceDeg = uiState.horizonToleranceDeg
        )

        CenterFramingReticle()

        FocusIndicator(focusPoint = uiState.focusPoint)

        CountdownTimerOverlay(secondsRemaining = uiState.countdownRemaining)

        // Vivo X200-Inspired High-Magnification Zoom Finder Window (Active at zoom > 10X)
        ZoomFinderView(
            zoomRatio = uiState.zoomRatio,
            previewBitmap = uiState.zoomFinderBitmap,
            panOffset = uiState.zoomFinderPanOffset,
            onPanOffsetChanged = onZoomFinderPanChanged,
            onResetPan = onResetZoomFinderPan,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 64.dp, end = 16.dp)
        )

        // Minimal Clean Top Bar: Settings, Flash, Aspect Ratio & Resolution badge, On-Demand Tools Button (🛠️)
        MinimalCameraTopBar(
            flashMode = uiState.flashMode,
            aspectRatio = uiState.aspectRatio,
            photoResolution = uiState.photoResolution,
            isToolsOpen = uiState.isToolsSheetVisible,
            onSettingsClick = onOpenSettings,
            onFlashToggle = {
                val next = when (uiState.flashMode) {
                    FlashMode.OFF -> FlashMode.AUTO
                    FlashMode.AUTO -> FlashMode.ON
                    FlashMode.ON -> FlashMode.TORCH
                    FlashMode.TORCH -> FlashMode.OFF
                }
                onFlashToggle(next)
            },
            onAspectRatioClick = onAspectRatioToggle,
            onToolsClick = onToggleToolsSheet,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // Background Enhancement Status Pill
        BackgroundEnhancementIndicator(
            isEnhancing = uiState.isEnhancingInBackground,
            progress = uiState.processingProgress,
            statusMessage = uiState.processingStatus,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 54.dp)
        )

        // High Zoom Stability Warning (30X - 100X)
        if (uiState.zoomRatio >= 30.0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC000000))
                    .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Stabilize Phone • %.0fX Digital Zoom".format(uiState.zoomRatio),
                    color = Color(0xFFFFD54F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom Dashboard: Exposure Ruler + Pro Stats + Priority Selector + Shutter Bar + Modes Carousel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState.cameraMode) {
                CameraMode.VIDEO -> {
                    // Normal Video Mode
                    NormalVideoControls(
                        isRecording = uiState.isRecording,
                        isPaused = uiState.isRecordingPaused,
                        durationSeconds = uiState.recordingDurationSeconds,
                        quality = uiState.videoQuality,
                        fps = uiState.videoFps,
                        stabilizationMode = uiState.videoStabilizationMode,
                        currentZoom = uiState.zoomRatio,
                        minZoom = uiState.minZoomRatio,
                        maxZoom = uiState.maxZoomRatio,
                        onStartRecording = { onStartRecording(false) },
                        onPauseRecording = onPauseRecording,
                        onResumeRecording = onResumeRecording,
                        onStopRecording = onStopRecording,
                        onQualityToggle = {
                            val next = if (uiState.videoQuality == "1080p") "720p" else "1080p"
                            onVideoQualityChanged(next)
                        },
                        onFpsToggle = {
                            val next = if (uiState.videoFps == 30) 60 else 30
                            onVideoFpsChanged(next)
                        },
                        onStabilizationCycle = {
                            val next = when (uiState.videoStabilizationMode) {
                                VideoStabilizationMode.OFF -> VideoStabilizationMode.STANDARD
                                VideoStabilizationMode.STANDARD -> VideoStabilizationMode.SUPER_STEADY
                                VideoStabilizationMode.SUPER_STEADY -> VideoStabilizationMode.HORIZON_LOCK
                                VideoStabilizationMode.HORIZON_LOCK -> VideoStabilizationMode.OFF
                            }
                            onStabilizationModeChanged(next)
                        },
                        onSwitchCamera = onSwitchCamera,
                        onZoomChanged = onZoomChange
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    SamsungModeCarousel(
                        currentMode = uiState.cameraMode,
                        isMoreActive = uiState.isMoreModesSheetVisible,
                        onModeSelected = onModeSelected,
                        onMoreClicked = onToggleMoreModesSheet
                    )
                }

                CameraMode.PRO_VIDEO -> {
                    // Professional Video Mode
                    ProVideoControls(
                        isRecording = uiState.isRecording,
                        isPaused = uiState.isRecordingPaused,
                        durationSeconds = uiState.recordingDurationSeconds,
                        quality = uiState.videoQuality,
                        fps = uiState.videoFps,
                        bitrateMbps = uiState.videoBitrateMbps,
                        stabilizationMode = uiState.videoStabilizationMode,
                        availableCameras = uiState.availableCameras,
                        activeCameraId = uiState.activeCameraId,
                        concurrentCameraSupported = uiState.concurrentCameraSupported,
                        concurrentCameraNote = uiState.concurrentCameraNote,
                        availableAudioDevices = uiState.availableAudioDevices,
                        selectedAudioDevice = uiState.selectedAudioDevice,
                        audioLevelDb = uiState.audioLevelDb,
                        isAudioClipping = uiState.isAudioClipping,
                        manualIso = uiState.manualIso,
                        manualShutterNanos = uiState.manualShutterNanos,
                        manualFocusDistance = uiState.manualFocusDistance,
                        whiteBalanceMode = uiState.whiteBalanceMode,
                        isFocusLocked = uiState.isFocusLocked,
                        isExposureLocked = uiState.isExposureLocked,
                        currentZoom = uiState.zoomRatio,
                        minZoom = uiState.minZoomRatio,
                        maxZoom = uiState.maxZoomRatio,
                        isProSheetOpen = uiState.isProControlsSheetVisible,
                        onToggleProSheet = onToggleProControlsSheet,
                        onDismissProSheet = onDismissProControlsSheet,
                        onStartRecording = { onStartRecording(true) },
                        onPauseRecording = onPauseRecording,
                        onResumeRecording = onResumeRecording,
                        onStopRecording = onStopRecording,
                        onSelectCameraId = onSelectCameraId,
                        onSelectAudioDevice = onSelectAudioDevice,
                        onStabilizationModeChanged = onStabilizationModeChanged,
                        onQualityChanged = onVideoQualityChanged,
                        onFpsChanged = onVideoFpsChanged,
                        onBitrateChanged = onVideoBitrateChanged,
                        onIsoChanged = onIsoChanged,
                        onShutterSpeedChanged = onShutterSpeedChanged,
                        onFocusDistanceChanged = onFocusDistanceChanged,
                        onWhiteBalanceChanged = onWhiteBalanceChanged,
                        onToggleFocusLock = onToggleFocusLock,
                        onToggleExposureLock = onToggleExposureLock,
                        onZoomChanged = onZoomChange
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    SamsungModeCarousel(
                        currentMode = uiState.cameraMode,
                        isMoreActive = uiState.isMoreModesSheetVisible,
                        onModeSelected = onModeSelected,
                        onMoreClicked = onToggleMoreModesSheet
                    )
                }

                else -> {
                    // PHOTO, PRO, PORTRAIT, NIGHT, DOCUMENT, MACRO
                    // Quick Zoom Bar: 0.5x, 1x, 2x, 4x, 10x, 30x, 50x, 100x presets with tap-to-expand
                    QuickZoomBar(
                        currentZoom = uiState.zoomRatio,
                        minZoom = uiState.minZoomRatio,
                        maxZoom = uiState.maxZoomRatio,
                        isExpanded = uiState.isZoomPresetsExpanded,
                        onToggleExpand = onToggleZoomPresets,
                        onZoomSelected = onZoomChange,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // In PRO Mode: Dedicated Pro Controls Shortcut Pill (tap opens Pro Controls Sheet)
                    if (uiState.cameraMode == CameraMode.PRO) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (uiState.isProControlsSheetVisible) Color(0xFFFFB300) else OverlayPill)
                                .clickable { onToggleProControlsSheet() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = "Pro Controls",
                                    tint = if (uiState.isProControlsSheetVisible) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Pro Controls",
                                    color = if (uiState.isProControlsSheetVisible) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Shutter Action Bar: Gallery Thumbnail, Shutter Button, Switch Camera
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThumbnailView(
                            latestImageUri = uiState.latestCapturedUri,
                            onClick = onThumbnailClick
                        )

                        ShutterButton(
                            isCapturing = uiState.isCapturing,
                            isVideo = false,
                            isRecording = false,
                            onClick = onCaptureClick
                        )

                        IconButton(
                            onClick = onSwitchCamera,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(OverlayPill)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Modes Carousel: PRO, PHOTO, VIDEO, PORTRAIT, NIGHT, MORE
                    SamsungModeCarousel(
                        currentMode = uiState.cameraMode,
                        isMoreActive = uiState.isMoreModesSheetVisible,
                        onModeSelected = onModeSelected,
                        onMoreClicked = onToggleMoreModesSheet
                    )
                }
            }
        }

        // 6. On-Demand Camera Tools Sheet (Basic, Image, Professional, Advanced)
        OnDemandToolsSheet(
            isVisible = uiState.isToolsSheetVisible,
            activeCategoryIndex = uiState.activeToolsCategoryIndex,
            onSelectCategory = onSelectToolsCategory,
            onDismiss = onDismissToolsSheet,
            flashMode = uiState.flashMode,
            onFlashModeChanged = onFlashToggle,
            isHdrEnabled = uiState.isHdrEnabled,
            onToggleHdr = onHdrToggle,
            aspectRatio = uiState.aspectRatio,
            onToggleAspectRatio = onAspectRatioToggle,
            timerSeconds = uiState.timerSeconds,
            onCycleTimer = onCycleTimer,
            isGridVisible = uiState.isGridVisible,
            onToggleGrid = onToggleGrid,
            isLevelVisible = uiState.isLevelEnabled,
            onToggleLevel = onToggleLevel,
            photoResolution = uiState.photoResolution,
            onToggleResolution = onResolutionToggle,
            isRawEnabled = uiState.isRawCaptureEnabled,
            onToggleRaw = onRawCaptureToggle,
            isSceneOptimizerEnabled = uiState.isSceneOptimizerEnabled,
            onToggleSceneOptimizer = onSceneOptimizerToggle,
            processingStrength = uiState.processingStrength,
            onProcessingStrengthChanged = onProcessingStrengthChanged,
            colorStyle = uiState.colorStyle,
            onColorStyleChanged = onColorStyleChanged,
            evValue = uiState.evValue,
            onEvChanged = onEvSliderChanged,
            exposurePriority = uiState.exposurePriority,
            onExposurePriorityChanged = onExposurePriorityChanged,
            manualIso = uiState.manualIso,
            onIsoChanged = onIsoChanged,
            shutterSpeedText = uiState.shutterSpeedText,
            onShutterSpeedChanged = onShutterSpeedChanged,
            whiteBalanceMode = uiState.whiteBalanceMode,
            onWhiteBalanceChanged = onWhiteBalanceChanged,
            isHistogramEnabled = uiState.isHistogramEnabled,
            onToggleHistogram = onToggleHistogram,
            isFocusPeakingEnabled = uiState.isFocusPeakingEnabled,
            onToggleFocusPeaking = onToggleFocusPeaking,
            stabilizationMode = uiState.videoStabilizationMode,
            onStabilizationModeChanged = onStabilizationModeChanged,
            isAutoHorizonEnabled = uiState.isAutoHorizonCorrectionEnabled,
            onToggleAutoHorizon = onToggleAutoHorizonCorrection
        )

        // 7. On-Demand Pro Photo Controls Sheet
        ProPhotoControlsSheet(
            isVisible = uiState.isProControlsSheetVisible && uiState.cameraMode == CameraMode.PRO,
            onDismiss = onDismissProControlsSheet,
            manualIso = uiState.manualIso,
            onIsoChanged = onIsoChanged,
            shutterSpeedText = uiState.shutterSpeedText,
            onShutterSpeedChanged = onShutterSpeedChanged,
            evValue = uiState.evValue,
            onEvChanged = onEvSliderChanged,
            manualFocusDistance = uiState.manualFocusDistance,
            onFocusDistanceChanged = onFocusDistanceChanged,
            whiteBalanceMode = uiState.whiteBalanceMode,
            onWhiteBalanceChanged = onWhiteBalanceChanged,
            isRawEnabled = uiState.isRawCaptureEnabled,
            onToggleRaw = onRawCaptureToggle,
            isHdrEnabled = uiState.isHdrEnabled,
            onToggleHdr = onHdrToggle,
            isFocusPeakingEnabled = uiState.isFocusPeakingEnabled,
            onToggleFocusPeaking = onToggleFocusPeaking,
            isHistogramEnabled = uiState.isHistogramEnabled,
            onToggleHistogram = onToggleHistogram,
            isGridVisible = uiState.isGridVisible,
            onToggleGrid = onToggleGrid,
            isLevelVisible = uiState.isLevelEnabled,
            onToggleLevel = onToggleLevel
        )

        // 8. Extended More Modes Grid Sheet
        AnimatedVisibility(
            visible = uiState.isMoreModesSheetVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            MoreModesSheet(
                onSelectMode = onModeSelected,
                onDismiss = onDismissMoreModesSheet
            )
        }

        // 9. Camera Settings Sheet
        CameraSettingsSheet(
            isVisible = uiState.isSettingsSheetVisible,
            orientationLock = uiState.orientationLock,
            isHorizonLevelEnabled = uiState.isLevelEnabled,
            isAutoHorizonCorrectionEnabled = uiState.isAutoHorizonCorrectionEnabled,
            horizonToleranceDeg = uiState.horizonToleranceDeg,
            onOrientationLockChanged = onOrientationLockChanged,
            onToggleHorizonLevel = onToggleLevel,
            onToggleAutoHorizonCorrection = onToggleAutoHorizonCorrection,
            onHorizonToleranceChanged = onHorizonToleranceChanged,
            onDismiss = onDismissSettings
        )

        // 10. Interactive Document Review & Dewarping Screen
        if (uiState.isDocumentScanVisible && uiState.pendingDocumentFile != null) {
            DocumentScanScreen(
                capturedFile = uiState.pendingDocumentFile,
                initialCorners = uiState.detectedDocumentCorners ?: DocumentCorners.defaultInset(),
                isProcessing = uiState.isDocumentProcessing,
                onRetake = onDocumentRetake,
                onRotate = { /* Screen manages rotation internally */ },
                onAutoDetect = onDocumentAutoDetect,
                onSaveJpeg = onSaveDocumentJpeg,
                onSavePdf = onSaveDocumentPdf,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
