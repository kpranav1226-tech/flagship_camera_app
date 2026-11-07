package com.clearshot.ai.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Utility exporting scanned document images into standard PDF files.
 */
class PdfExporter {

    suspend fun exportImageToPdf(imageFile: File, outputFile: File): Result<File> = withContext(Dispatchers.IO) {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            return@withContext Result.failure(IllegalArgumentException("Source document image does not exist"))
        }

        var bitmap: Bitmap? = null
        val pdfDoc = PdfDocument()

        try {
            bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                ?: return@withContext Result.failure(IllegalStateException("Could not decode image for PDF export"))

            val bmpW = bitmap.width
            val bmpH = bitmap.height

            // Standard page size based on image dimensions (e.g. A4 proportional)
            val pageInfo = PdfDocument.PageInfo.Builder(bmpW, bmpH, 1).create()
            val page = pdfDoc.startPage(pageInfo)

            val paint = Paint().apply {
                isFilterBitmap = true
                isAntiAlias = true
                isDither = true
            }

            page.canvas.drawBitmap(bitmap, 0f, 0f, paint)
            pdfDoc.finishPage(page)

            if (outputFile.parentFile?.exists() == false) {
                outputFile.parentFile?.mkdirs()
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDoc.writeTo(fos)
            }

            Log.i(TAG, "Document successfully exported as PDF: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "PDF generation failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                pdfDoc.close()
            } catch (ignored: Exception) {}
            bitmap?.recycle()
        }
    }

    companion object {
        private const val TAG = "PdfExporter"
    }
}
