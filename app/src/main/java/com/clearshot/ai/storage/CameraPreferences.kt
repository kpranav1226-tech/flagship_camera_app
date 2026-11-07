package com.clearshot.ai.storage

import android.content.Context
import android.content.SharedPreferences
import com.clearshot.ai.camera.OrientationLock

/**
 * Persistent preferences manager for ClearShot camera settings.
 */
class CameraPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getOrientationLock(): OrientationLock {
        val name = prefs.getString(KEY_ORIENTATION_LOCK, OrientationLock.PORTRAIT.name)
        return try {
            OrientationLock.valueOf(name ?: OrientationLock.PORTRAIT.name)
        } catch (e: Exception) {
            OrientationLock.PORTRAIT
        }
    }

    fun setOrientationLock(lock: OrientationLock) {
        prefs.edit().putString(KEY_ORIENTATION_LOCK, lock.name).apply()
    }

    fun isHorizonLevelEnabled(): Boolean {
        return prefs.getBoolean(KEY_HORIZON_LEVEL_ENABLED, false)
    }

    fun setHorizonLevelEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HORIZON_LEVEL_ENABLED, enabled).apply()
    }

    fun isAutoHorizonCorrectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_HORIZON_CORRECTION, false)
    }

    fun setAutoHorizonCorrectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_HORIZON_CORRECTION, enabled).apply()
    }

    fun getHorizonTolerance(): Float {
        return prefs.getFloat(KEY_HORIZON_TOLERANCE, 1.0f)
    }

    fun setHorizonTolerance(toleranceDeg: Float) {
        prefs.edit().putFloat(KEY_HORIZON_TOLERANCE, toleranceDeg).apply()
    }

    companion object {
        private const val PREFS_NAME = "clearshot_camera_preferences"
        private const val KEY_ORIENTATION_LOCK = "key_orientation_lock"
        private const val KEY_HORIZON_LEVEL_ENABLED = "key_horizon_level_enabled"
        private const val KEY_AUTO_HORIZON_CORRECTION = "key_auto_horizon_correction"
        private const val KEY_HORIZON_TOLERANCE = "key_horizon_tolerance"
    }
}
