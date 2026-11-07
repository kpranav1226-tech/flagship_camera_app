package com.clearshot.ai.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Professional audio device enumeration, routing, and real-time VU metering manager.
 * Supports built-in mic array, wired 3.5mm/Type-C headsets, Bluetooth SCO/BLE microphones,
 * and external USB audio interfaces.
 */
class AudioInputManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _availableDevices = MutableStateFlow<List<AudioInputDevice>>(emptyList())
    val availableDevices: StateFlow<List<AudioInputDevice>> = _availableDevices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<AudioInputDevice?>(null)
    val selectedDevice: StateFlow<AudioInputDevice?> = _selectedDevice.asStateFlow()

    private val _audioLevelDb = MutableStateFlow(-60f)
    val audioLevelDb: StateFlow<Float> = _audioLevelDb.asStateFlow()

    private val _isClipping = MutableStateFlow(false)
    val isClipping: StateFlow<Boolean> = _isClipping.asStateFlow()

    private var meterJob: Job? = null
    private var audioRecord: AudioRecord? = null

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices added: ${addedDevices?.size ?: 0}")
            refreshDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices removed: ${removedDevices?.size ?: 0}")
            refreshDevices()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        refreshDevices()
    }

    /**
     * Enumerates connected input devices using Android AudioManager.
     */
    fun refreshDevices() {
        val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        val deviceList = mutableListOf<AudioInputDevice>()

        // Always provide default built-in mic
        var builtinFound = false
        for (dev in inputDevices) {
            when (dev.type) {
                AudioDeviceInfo.TYPE_BUILTIN_MIC -> {
                    builtinFound = true
                    deviceList.add(
                        AudioInputDevice(
                            id = dev.id,
                            name = dev.productName?.toString() ?: "Built-in Microphone",
                            type = AudioInputType.BUILTIN_MIC,
                            rawDeviceInfo = dev
                        )
                    )
                }
                AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> {
                    deviceList.add(
                        AudioInputDevice(
                            id = dev.id,
                            name = dev.productName?.toString() ?: "Wired Headset Mic",
                            type = AudioInputType.WIRED_HEADSET,
                            rawDeviceInfo = dev
                        )
                    )
                }
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                    deviceList.add(
                        AudioInputDevice(
                            id = dev.id,
                            name = dev.productName?.toString() ?: "Bluetooth Headset",
                            type = AudioInputType.BLUETOOTH_SCO,
                            rawDeviceInfo = dev
                        )
                    )
                }
                AudioDeviceInfo.TYPE_USB_DEVICE -> {
                    deviceList.add(
                        AudioInputDevice(
                            id = dev.id,
                            name = dev.productName?.toString() ?: "USB Audio Mic",
                            type = AudioInputType.USB_AUDIO,
                            rawDeviceInfo = dev
                        )
                    )
                }
            }
        }

        if (!builtinFound) {
            deviceList.add(
                0,
                AudioInputDevice(
                    id = -1,
                    name = "Internal Phone Mic",
                    type = AudioInputType.BUILTIN_MIC,
                    rawDeviceInfo = null
                )
            )
        }

        _availableDevices.value = deviceList

        // Keep existing selection if valid, or default to first available
        val current = _selectedDevice.value
        if (current == null || deviceList.none { it.id == current.id }) {
            selectDevice(deviceList.first())
        }
    }

    /**
     * Selects an audio input device and routes system audio to it.
     */
    fun selectDevice(device: AudioInputDevice) {
        _selectedDevice.value = device
        val updated = _availableDevices.value.map {
            it.copy(isSelected = it.id == device.id)
        }
        _availableDevices.value = updated

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            device.rawDeviceInfo?.let { info ->
                try {
                    val success = audioManager.setCommunicationDevice(info)
                    Log.i(TAG, "Audio routed to ${device.name}: success=$success")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to route communication device: ${e.message}")
                }
            } ?: run {
                audioManager.clearCommunicationDevice()
            }
        }
    }

    /**
     * Starts background real-time VU meter sampling.
     */
    @SuppressLint("MissingPermission")
    fun startMeter() {
        if (meterJob?.isActive == true) return

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "Cannot start VU meter: RECORD_AUDIO permission not granted")
            return
        }

        meterJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = (minBufSize * 2).coerceAtLeast(2048)

            try {
                val record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                _selectedDevice.value?.rawDeviceInfo?.let { devInfo ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        record.setPreferredDevice(devInfo)
                    }
                }

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(TAG, "AudioRecord failed to initialize for VU meter")
                    record.release()
                    return@launch
                }

                audioRecord = record
                record.startRecording()

                val buffer = ShortArray(1024)
                while (isActive) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var sumSq = 0.0
                        var peak = 0
                        for (i in 0 until read) {
                            val sample = buffer[i].toInt()
                            val absSample = kotlin.math.abs(sample)
                            if (absSample > peak) peak = absSample
                            sumSq += sample * sample
                        }

                        val rms = sqrt(sumSq / read)
                        val db = if (rms > 0.1) {
                            (20.0 * log10(rms / 32767.0)).toFloat().coerceIn(-60f, 0f)
                        } else {
                            -60f
                        }

                        _audioLevelDb.value = db
                        _isClipping.value = peak >= 32700
                    }
                    kotlinx.coroutines.delay(50) // ~20 updates/second for smooth Compose VU meter
                }
            } catch (e: Exception) {
                Log.w(TAG, "VU meter sampling exception: ${e.message}")
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
                audioRecord = null
                _audioLevelDb.value = -60f
                _isClipping.value = false
            }
        }
    }

    /**
     * Stops VU meter sampling to save battery.
     */
    fun stopMeter() {
        meterJob?.cancel()
        meterJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null
        _audioLevelDb.value = -60f
        _isClipping.value = false
    }

    fun release() {
        stopMeter()
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }

    companion object {
        private const val TAG = "AudioInputManager"
    }
}
