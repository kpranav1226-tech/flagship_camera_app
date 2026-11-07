package com.clearshot.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.clearshot.ai.ui.CameraScreen
import com.clearshot.ai.ui.CameraViewModel
import com.clearshot.ai.ui.theme.ClearShotTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ClearShotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    // Dynamically apply orientation lock to Activity window
                    LaunchedEffect(uiState.orientationLock) {
                        requestedOrientation = uiState.orientationLock.activityOrientation
                    }

                    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
                        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
                        viewModel.onPermissionResult(cameraGranted)
                        viewModel.onAudioPermissionResult(audioGranted)
                    }

                    // Check initial permissions
                    LaunchedEffect(Unit) {
                        val hasCamera = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        val hasAudio = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        viewModel.onPermissionResult(hasCamera)
                        viewModel.onAudioPermissionResult(hasAudio)

                        val needed = mutableListOf<String>()
                        if (!hasCamera) needed.add(Manifest.permission.CAMERA)
                        if (!hasAudio) needed.add(Manifest.permission.RECORD_AUDIO)

                        if (needed.isNotEmpty()) {
                            multiplePermissionsLauncher.launch(needed.toTypedArray())
                        }
                    }

                    CameraScreen(
                        uiState = uiState,
                        onPermissionRequested = {
                            multiplePermissionsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.RECORD_AUDIO
                                )
                            )
                        },
                        onFlashToggle = viewModel::onFlashModeToggle,
                        onZoomChange = viewModel::onZoomChanged,
                        onPinchZoom = viewModel::onPinchZoom,
                        onFocusTap = viewModel::onFocusTap,
                        onCaptureClick = viewModel::onCaptureClicked,
                        onPreviewReady = viewModel::onPreviewReady,
                        onModeSelected = viewModel::onCameraModeChanged,
                        onSwitchCamera = viewModel::onSwitchCamera,
                        onToggleGrid = viewModel::onToggleGrid,
                        onCycleTimer = viewModel::onCycleTimer,
                        onToggleLevel = viewModel::onToggleLevel,
                        onToggleProMode = viewModel::onToggleProMode,
                        onBurstCountChanged = viewModel::onBurstCountChanged,
                        onExposureChanged = viewModel::onExposureChanged,
                        onSharpenChanged = viewModel::onSharpenChanged,
                        onDenoiseChanged = viewModel::onDenoiseChanged,
                        onClaheChanged = viewModel::onClaheChanged,
                        onOpenSettings = viewModel::onOpenSettingsSheet,
                        onDismissSettings = viewModel::onDismissSettingsSheet,
                        onOrientationLockChanged = viewModel::onOrientationLockChanged,
                        onToggleAutoHorizonCorrection = viewModel::onToggleAutoHorizonCorrection,
                        onHorizonToleranceChanged = viewModel::onHorizonToleranceChanged,
                        onSaveEnhanced = viewModel::onSaveEnhancedClicked,
                        onDismissComparison = viewModel::onDismissComparison,
                        onToastDismissed = viewModel::onToastDismissed,
                        // Samsung Galaxy M15 Design callbacks
                        onAspectRatioToggle = viewModel::onAspectRatioToggle,
                        onResolutionToggle = viewModel::onResolutionToggle,
                        onHdrToggle = viewModel::onHdrToggle,
                        onSceneOptimizerToggle = viewModel::onSceneOptimizerToggle,
                        onRawCaptureToggle = viewModel::onRawCaptureToggle,
                        onEisToggle = viewModel::onEisToggle,
                        onAiOptimizationToggle = viewModel::onAiOptimizationToggle,
                        onExposurePriorityChanged = viewModel::onExposurePriorityChanged,
                        onEvSliderChanged = viewModel::onEvSliderChanged,
                        onToggleMoreModesSheet = viewModel::onToggleMoreModesSheet,
                        onDismissMoreModesSheet = viewModel::onDismissMoreModesSheet,
                        // Video & Pro Video callbacks
                        onStartRecording = viewModel::startVideoRecording,
                        onPauseRecording = viewModel::pauseVideoRecording,
                        onResumeRecording = viewModel::resumeVideoRecording,
                        onStopRecording = viewModel::stopVideoRecording,
                        onStabilizationModeChanged = viewModel::onStabilizationModeChanged,
                        onVideoQualityChanged = viewModel::onVideoQualityChanged,
                        onVideoFpsChanged = viewModel::onVideoFpsChanged,
                        onVideoBitrateChanged = viewModel::onVideoBitrateChanged,
                        onSelectCameraId = viewModel::onSelectCameraId,
                        onSelectAudioDevice = viewModel::onSelectAudioDevice,
                        onIsoChanged = viewModel::onIsoChanged,
                        onShutterSpeedChanged = viewModel::onShutterSpeedChanged,
                        onFocusDistanceChanged = viewModel::onFocusDistanceChanged,
                        onWhiteBalanceChanged = viewModel::onWhiteBalanceChanged,
                        onToggleFocusLock = viewModel::onToggleFocusLock,
                        onToggleExposureLock = viewModel::onToggleExposureLock,
                        onThumbnailClick = {
                            if (uiState.originalCaptureFile != null && uiState.pipelineResult != null) {
                                viewModel.onThumbnailClicked()
                            } else if (uiState.latestCapturedUri != null) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uiState.latestCapturedUri, "image/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    viewModel.onThumbnailClicked()
                                }
                            } else {
                                viewModel.onThumbnailClicked()
                            }
                        },
                        onToggleToolsSheet = viewModel::onToggleToolsSheet,
                        onDismissToolsSheet = viewModel::onDismissToolsSheet,
                        onSelectToolsCategory = viewModel::onSelectToolsCategory,
                        onToggleProControlsSheet = viewModel::onToggleProControlsSheet,
                        onDismissProControlsSheet = viewModel::onDismissProControlsSheet,
                        onToggleZoomPresets = viewModel::onToggleZoomPresets,
                        onProcessingStrengthChanged = viewModel::onProcessingStrengthChanged,
                        onColorStyleChanged = viewModel::onColorStyleChanged,
                        onToggleHistogram = viewModel::onToggleHistogram,
                        onToggleFocusPeaking = viewModel::onToggleFocusPeaking,
                        onDocumentRetake = viewModel::onDocumentRetake,
                        onDocumentAutoDetect = viewModel::onDocumentAutoDetect,
                        onSaveDocumentJpeg = viewModel::onSaveDocumentJpeg,
                        onSaveDocumentPdf = viewModel::onSaveDocumentPdf,
                        onZoomFinderPanChanged = viewModel::onZoomFinderPanChanged,
                        onResetZoomFinderPan = viewModel::onResetZoomFinderPan
                    )
                }
            }
        }
    }
}
