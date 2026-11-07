package com.clearshot.ai.storage

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Robust MediaStore implementation for saving photographs directly to device Gallery.
 * Supports distinct subfolders for originals (DCIM/ClearShot) and enhanced outputs (DCIM/ClearShot/Enhanced).
 */
class MediaStoreStorageManager(
    private val context: Context
) : MediaStorageManager {

    override suspend fun saveImageToGallery(
        imageFile: File,
        displayName: String?
    ): Result<Uri> = saveToDirectory(
        imageFile = imageFile,
        subfolder = "DCIM/ClearShot",
        defaultPrefix = "ClearShot",
        displayName = displayName,
        cleanUpSource = false // Keep source file in cache for comparison screen
    )

    override suspend fun saveEnhancedImageToGallery(
        imageFile: File,
        displayName: String?
    ): Result<Uri> = saveToDirectory(
        imageFile = imageFile,
        subfolder = "DCIM/ClearShot/Enhanced",
        defaultPrefix = "ClearShot_Enhanced",
        displayName = displayName,
        cleanUpSource = false
    )

    override suspend fun saveDocumentScan(
        imageFile: File,
        displayName: String?
    ): Result<Uri> = saveToDirectory(
        imageFile = imageFile,
        subfolder = "DCIM/ClearShot/Documents",
        defaultPrefix = "ClearShot_Doc",
        displayName = displayName,
        cleanUpSource = false
    )

    override suspend fun savePdfDocument(
        pdfFile: File,
        displayName: String?
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val name = displayName ?: "ClearShot_Doc_$timeStamp.pdf"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/ClearShot/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        var uri: Uri? = null
        try {
            uri = resolver.insert(collection, values)
                ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore PDF entry"))

            resolver.openOutputStream(uri)?.use { os ->
                FileInputStream(pdfFile).use { `is` ->
                    `is`.copyTo(os)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open output stream for PDF Uri: $uri"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            Log.i(TAG, "PDF document saved to MediaStore: $uri")
            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save PDF to MediaStore: ${e.message}", e)
            uri?.let { resolver.delete(it, null, null) }
            Result.failure(e)
        }
    }

    private suspend fun saveToDirectory(
        imageFile: File,
        subfolder: String,
        defaultPrefix: String,
        displayName: String?,
        cleanUpSource: Boolean
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val name = displayName ?: "${defaultPrefix}_$timeStamp.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val normalizedSubfolder = if (subfolder.endsWith("/")) subfolder else "$subfolder/"
                put(MediaStore.Images.Media.RELATIVE_PATH, normalizedSubfolder)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        var uri: Uri? = null
        try {
            uri = resolver.insert(collection, values)
                ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore entry for $subfolder"))

            resolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(imageFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open output stream for Uri: $uri"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                val targetDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    subfolder.removePrefix("DCIM/")
                )
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val targetFile = File(targetDir, name)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("image/jpeg"),
                    null
                )
            }

            if (cleanUpSource && imageFile.exists()) {
                imageFile.delete()
            }

            Log.i(TAG, "Photograph saved to Gallery in $subfolder: $uri")
            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write photograph to $subfolder: ${e.message}", e)
            uri?.let { resolver.delete(it, null, null) }
            Result.failure(e)
        }
    }

    override suspend fun getLatestCapturedImageUri(): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        try {
            resolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val id = cursor.getLong(idColumn)
                    return@withContext ContentUris.withAppendedId(collection, id)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query latest image: ${e.message}")
        }
        null
    }

    override fun createVideoOutputOptions(
        isProVideo: Boolean,
        displayName: String?
    ): androidx.camera.video.MediaStoreOutputOptions {
        val folder = if (isProVideo) "DCIM/ClearShot/Pro Videos" else "DCIM/ClearShot/Videos"
        val prefix = if (isProVideo) "PRO_VID" else "VID"
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val name = displayName ?: "${prefix}_$timeStamp.mp4"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val normalizedFolder = if (folder.endsWith("/")) folder else "$folder/"
                put(MediaStore.Video.Media.RELATIVE_PATH, normalizedFolder)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        return androidx.camera.video.MediaStoreOutputOptions.Builder(
            context.contentResolver,
            collection
        )
            .setContentValues(values)
            .build()
    }

    companion object {
        private const val TAG = "MediaStoreStorage"
    }
}
