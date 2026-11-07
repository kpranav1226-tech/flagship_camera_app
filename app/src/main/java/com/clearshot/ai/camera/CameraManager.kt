package com.clearshot.ai.camera

import android.media.AudioDeviceInfo
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Event updates emitted during video recording.
 */
sealed class VideoRecordingEvent {
    data object Started : VideoRecordingEvent()
    data object Paused : VideoRecordingEvent()
    data object Resumed : VideoRecordingEvent()
    data class Status(val durationSeconds: Long, val bytes: Long) : VideoRecordingEvent()
    data class Finalized(val outputUri: android.net.Uri?, val error: Throwable? = null) : VideoRecordingEvent()
}

/**
 * Contract for device camera operations across Photo, Video, and Pro Video.
 */
interface CameraManager {
    /**
     * Observes real-time camera state (zoom, flash, lens, capture progress, exposure, video recording).
     */
    val cameraState: StateFlow<CameraState>

    /**
     * Binds CameraX lifecycle to the provided LifecycleOwner and targets PreviewView.
     * Can selectively bind photo or video use cases based on active mode.
     */
    suspend fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        mode: CameraMode = CameraMode.PHOTO
    ): Result<Unit>

    /**
     * Switches between rear and front lens facing.
     */
    suspend fun switchCamera(): Result<Unit>

    /**
     * Selects a specific Camera2 cameraId directly (for Pro Video camera selector).
     */
    suspend fun selectCameraId(cameraId: String): Result<Unit>

    /**
     * Sets flash mode (OFF, ON, AUTO).
     */
    suspend fun setFlashMode(flashMode: FlashMode)

    /**
     * Sets linear or digital zoom ratio within supported hardware bounds.
     */
    suspend fun setZoomRatio(ratio: Float)

    /**
     * Sets exposure compensation index within hardware range.
     */
    suspend fun setExposureCompensation(index: Int): Result<Int>

    /**
     * Triggers autofocus and auto-exposure metering at the specified viewfinder coordinates.
     */
    suspend fun focusAndMeter(x: Float, y: Float, viewWidth: Int, viewHeight: Int)

    /**
     * Captures a high-resolution photograph and saves to a temporary cache file.
     */
    suspend fun capturePhoto(): Result<File>

    /**
     * Captures a rapid bracketed/multi-frame sequence of photographs for computational fusion.
     */
    suspend fun captureBurst(frameCount: Int = 3): Result<List<File>>

    /**
     * Updates target rotation according to active OrientationLock or sensor posture.
     */
    fun setOrientationLock(lock: OrientationLock)

    // --- VIDEO & PRO VIDEO RECORDING ---

    /**
     * Starts video recording using CameraX VideoCapture & Recorder.
     * Directs output to DCIM/ClearShot/Videos (Normal) or DCIM/ClearShot/Pro Videos (Pro).
     */
    suspend fun startRecording(
        isProVideo: Boolean,
        audioDeviceInfo: AudioDeviceInfo? = null,
        onEvent: (VideoRecordingEvent) -> Unit
    ): Result<Unit>

    /**
     * Pauses the active video recording.
     */
    suspend fun pauseRecording()

    /**
     * Resumes a paused video recording.
     */
    suspend fun resumeRecording()

    /**
     * Stops the active video recording and finalizes MP4 in MediaStore.
     */
    suspend fun stopRecording()

    /**
     * Sets video stabilization mode (OFF, STANDARD, SUPER_STEADY, HORIZON_LOCK).
     */
    suspend fun setVideoStabilization(mode: VideoStabilizationMode)

    /**
     * Sets target recording resolution (e.g. "1080p", "720p").
     */
    suspend fun setVideoQuality(quality: String)

    /**
     * Sets target frame rate (30, 60 FPS).
     */
    suspend fun setVideoFps(fps: Int)

    /**
     * Sets video encoding bitrate in bps.
     */
    suspend fun setVideoBitrate(bitrateBps: Int)

    // --- MANUAL PRO IMAGE CONTROLS ---

    suspend fun setManualIso(iso: Int?)

    suspend fun setManualShutterSpeed(nanos: Long?)

    suspend fun setManualFocusDistance(distance: Float?)

    suspend fun setWhiteBalanceMode(mode: Int)

    suspend fun toggleFocusLock(): Boolean

    suspend fun toggleExposureLock(): Boolean

    /**
     * Sets the Zoom Finder normalized pan offset (-0.5 to +0.5).
     */
    fun setZoomFinderPanOffset(offset: androidx.compose.ui.geometry.Offset)

    /**
     * Cleans up camera resources and unbinds from lifecycle.
     */
    fun release()
}
