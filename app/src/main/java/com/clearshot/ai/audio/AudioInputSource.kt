package com.clearshot.ai.audio

import android.media.AudioDeviceInfo

/**
 * Categorized audio input sources detected on the Android device.
 */
enum class AudioInputType(val label: String, val description: String) {
    BUILTIN_MIC("Phone Mic", "Internal studio microphone array"),
    WIRED_HEADSET("Wired Mic", "3.5mm / Type-C headset microphone"),
    BLUETOOTH_SCO("Bluetooth Mic", "Wireless headset / lavalier microphone"),
    USB_AUDIO("USB Mic", "External USB microphone / audio interface")
}

data class AudioInputDevice(
    val id: Int,
    val name: String,
    val type: AudioInputType,
    val rawDeviceInfo: AudioDeviceInfo? = null,
    val isSelected: Boolean = false
)
