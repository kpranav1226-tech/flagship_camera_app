package com.clearshot.ai.camera

/**
 * Supported stabilization modes for Video and Pro Video recording.
 */
enum class VideoStabilizationMode(
    val title: String,
    val subtitle: String
) {
    OFF(
        title = "OFF",
        subtitle = "No electronic stabilization"
    ),
    STANDARD(
        title = "STANDARD",
        subtitle = "Native hardware Camera2 EIS/OIS stabilization"
    ),
    SUPER_STEADY(
        title = "SUPER STEADY",
        subtitle = "Aggressive electronic & motion-damped stabilization"
    ),
    HORIZON_LOCK(
        title = "HORIZON LOCK",
        subtitle = "Sensor-fused 360° roll leveling & crop margin"
    )
}
