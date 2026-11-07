package com.clearshot.ai.camera

import android.content.pm.ActivityInfo

/**
 * Supported viewfinder orientation lock modes.
 */
enum class OrientationLock(
    val title: String,
    val subtitle: String,
    val activityOrientation: Int
) {
    AUTO(
        title = "Auto (Sensor)",
        subtitle = "Free rotation following device posture",
        activityOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    ),
    PORTRAIT(
        title = "Portrait Lock",
        subtitle = "Fixed vertical orientation",
        activityOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    ),
    LANDSCAPE(
        title = "Landscape Lock",
        subtitle = "Fixed widescreen landscape orientation",
        activityOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    )
}
