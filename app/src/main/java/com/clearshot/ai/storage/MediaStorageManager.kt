package com.clearshot.ai.storage

import android.net.Uri
import java.io.File

/**
 * Contract for storing photographs into the device gallery and querying stored media.
 */
interface MediaStorageManager {
    /**
     * Saves a captured original image file into the public Gallery under DCIM/ClearShot.
     */
    suspend fun saveImageToGallery(imageFile: File, displayName: String? = null): Result<Uri>

    /**
     * Saves an enhanced photograph into the public Gallery under DCIM/ClearShot/Enhanced.
     * Guaranteed never to overwrite the original photograph.
     */
    suspend fun saveEnhancedImageToGallery(imageFile: File, displayName: String? = null): Result<Uri>

    /**
     * Saves a dewarped and enhanced document scan into DCIM/ClearShot/Documents.
     */
    suspend fun saveDocumentScan(imageFile: File, displayName: String? = null): Result<Uri>

    /**
     * Saves an exported document PDF file into public Documents/ClearShot.
     */
    suspend fun savePdfDocument(pdfFile: File, displayName: String? = null): Result<Uri>

    /**
     * Queries the latest photograph captured by the app for displaying in the thumbnail preview.
     */
    suspend fun getLatestCapturedImageUri(): Uri?

    /**
     * Creates MediaStoreOutputOptions configured for direct hardware video streaming
     * into DCIM/ClearShot/Videos (Normal Video) or DCIM/ClearShot/Pro Videos (Pro Video).
     */
    fun createVideoOutputOptions(
        isProVideo: Boolean = false,
        displayName: String? = null
    ): androidx.camera.video.MediaStoreOutputOptions
}
