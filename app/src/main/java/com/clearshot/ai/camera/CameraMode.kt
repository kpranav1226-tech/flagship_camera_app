package com.clearshot.ai.camera

/**
 * Shooting modes supported by the ClearShot AI computational photography engine.
 */
enum class CameraMode(
    val title: String,
    val subtitle: String,
    val defaultBurstCount: Int,
    val isVideo: Boolean = false
) {
    PHOTO(
        title = "PHOTO",
        subtitle = "Natural computational clarity & dynamic range",
        defaultBurstCount = 1
    ),
    PORTRAIT(
        title = "PORTRAIT",
        subtitle = "Depth-of-field computational bokeh & subject isolation",
        defaultBurstCount = 1
    ),
    NIGHT(
        title = "NIGHT SIGHT",
        subtitle = "Multi-frame shadow synthesis & low-light denoising",
        defaultBurstCount = 5
    ),
    DOCUMENT(
        title = "DOCUMENT",
        subtitle = "High-contrast text clarity & background flattening",
        defaultBurstCount = 1
    ),
    MACRO(
        title = "MACRO",
        subtitle = "Micro-texture enhancement & high-frequency detail",
        defaultBurstCount = 3
    ),
    PRO(
        title = "PRO",
        subtitle = "Manual parametric controls & hardware EXIF",
        defaultBurstCount = 3
    ),
    VIDEO(
        title = "VIDEO",
        subtitle = "Everyday stabilized video recording",
        defaultBurstCount = 0,
        isVideo = true
    ),
    PRO_VIDEO(
        title = "PRO VIDEO",
        subtitle = "Independent professional cine controls & multi-mic",
        defaultBurstCount = 0,
        isVideo = true
    )
}
