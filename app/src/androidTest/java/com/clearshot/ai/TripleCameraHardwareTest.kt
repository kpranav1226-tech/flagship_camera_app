package com.clearshot.ai

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TripleCameraHardwareTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private lateinit var bgThread: HandlerThread
    private lateinit var bgHandler: Handler

    @Before
    fun setup() {
        bgThread = HandlerThread("CameraTestBg").apply { start() }
        bgHandler = Handler(bgThread.looper)
    }

    @After
    fun tearDown() {
        bgThread.quitSafely()
    }

    @Test
    fun testCameraHardwareDiscovery() {
        val tag = "TRIPLE_CAM_DISCOVERY"
        val cameraIds = cameraManager.cameraIdList
        Log.i(tag, "================ HARDWARE DISCOVERY ================")
        Log.i(tag, "Total public camera IDs count: ${cameraIds.size}")
        Log.i(tag, "Camera IDs: ${cameraIds.joinToString(", ")}")

        for (id in cameraIds) {
            val chars = cameraManager.getCameraCharacteristics(id)
            val facing = when (chars.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                else -> "UNKNOWN"
            }
            val focal = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.joinToString() ?: "N/A"
            val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.joinToString() ?: "N/A"
            val pixelArray = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
            val hasLogicalMulti = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
            val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                chars.physicalCameraIds.joinToString(", ").ifEmpty { "None" }
            } else {
                "N/A (< Android P)"
            }

            Log.i(tag, "--- Camera ID: $id ---")
            Log.i(tag, "  Facing: $facing")
            Log.i(tag, "  Focal lengths (mm): $focal")
            Log.i(tag, "  Apertures (f): $apertures")
            Log.i(tag, "  Pixel array: ${pixelArray?.width}x${pixelArray?.height}")
            Log.i(tag, "  Active array: ${activeArray?.width()}x${activeArray?.height()}")
            Log.i(tag, "  LOGICAL_MULTI_CAMERA capability: $hasLogicalMulti")
            Log.i(tag, "  physicalCameraIds: $physicalIds")
        }

        // Test concurrent camera combinations
        Log.i(tag, "================ CONCURRENT CAMERAS ================")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val concurrentSets = cameraManager.concurrentCameraIds
            Log.i(tag, "getConcurrentCameraIds() size: ${concurrentSets.size}")
            for ((index, set) in concurrentSets.withIndex()) {
                Log.i(tag, "  Set #$index: [${set.joinToString(", ")}]")
            }
        } else {
            Log.i(tag, "getConcurrentCameraIds() requires Android R+")
        }

        // Test HAL hidden camera access (e.g. 54 for Macro, 23 for Logical)
        Log.i(tag, "================ PROBING HIDDEN HAL IDS ================")
        val probeIds = listOf("23", "50", "54", "4")
        for (hiddenId in probeIds) {
            try {
                val chars = cameraManager.getCameraCharacteristics(hiddenId)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                val pixelArray = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                Log.i(tag, "Hidden ID $hiddenId ACCESSIBLE! Facing: $facing, Res: ${pixelArray?.width}x${pixelArray?.height}")
            } catch (e: Exception) {
                Log.i(tag, "Hidden ID $hiddenId NOT accessible: ${e.javaClass.simpleName} - ${e.message}")
            }
        }
    }

    @Test
    fun testSimultaneousCameraOpening() = runBlocking {
        val tag = "TRIPLE_CAM_SIMULTANEOUS"
        Log.i(tag, "================ SIMULTANEOUS OPENING TEST ================")
        // Check if we can open Camera 0 (Rear Main) and Camera 2 (Rear Ultra-Wide) simultaneously
        val id0 = "0"
        val id2 = "2"

        var device0: CameraDevice? = null
        var device2: CameraDevice? = null
        var device0Disconnected = false
        var device0ErrorCode: Int? = null
        var device2ErrorCode: Int? = null

        val open0Latch = CountDownLatch(1)
        val open2Latch = CountDownLatch(1)

        try {
            Log.i(tag, "Step 1: Opening Camera $id0 (Main)...")
            cameraManager.openCamera(id0, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.i(tag, "Camera $id0 successfully opened!")
                    device0 = camera
                    open0Latch.countDown()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(tag, "Camera $id0 was DISCONNECTED!")
                    device0Disconnected = true
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(tag, "Camera $id0 received onError: $error")
                    device0ErrorCode = error
                    open0Latch.countDown()
                }
            }, bgHandler)

            val open0Ok = open0Latch.await(5, TimeUnit.SECONDS)
            Log.i(tag, "Camera $id0 open result: ok=$open0Ok, device0=${device0 != null}, err=$device0ErrorCode")

            if (device0 != null) {
                Log.i(tag, "Step 2: With Camera $id0 open, attempting to open Camera $id2 (Ultra-Wide)...")
                cameraManager.openCamera(id2, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        Log.i(tag, "Camera $id2 successfully opened simultaneously!")
                        device2 = camera
                        open2Latch.countDown()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        Log.w(tag, "Camera $id2 was disconnected!")
                        camera.close()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.e(tag, "Camera $id2 received onError: $error")
                        device2ErrorCode = error
                        open2Latch.countDown()
                    }
                }, bgHandler)

                val open2Ok = open2Latch.await(5, TimeUnit.SECONDS)
                Log.i(tag, "Camera $id2 open result: ok=$open2Ok, device2=${device2 != null}, err=$device2ErrorCode")

                // Sleep 2 seconds to see if device0 gets disconnected or evicted
                Thread.sleep(2000)
                Log.i(tag, "Step 3: State after 2 seconds:")
                Log.i(tag, "  device0 active: ${device0 != null && !device0Disconnected}")
                Log.i(tag, "  device0 disconnected: $device0Disconnected")
                Log.i(tag, "  device2 active: ${device2 != null}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during simultaneous open: ${e.message}", e)
        } finally {
            try { device0?.close() } catch (ignored: Exception) {}
            try { device2?.close() } catch (ignored: Exception) {}
        }
    }
}
