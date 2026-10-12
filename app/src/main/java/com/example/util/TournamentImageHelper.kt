package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object TournamentImageHelper {
    private const val TAG = "TournamentImageHelper"
    private const val MAX_DIMENSION = 960

    data class ProcessedTournamentImage(
        val localFilePath: String,
        val localFileUri: String,
        val base64DataUrl: String,
        val preferredSyncUrl: String
    )

    /**
     * Copies, rotates, downsamples, and permanently stores a tournament image picked from
     * gallery or photo picker into app-private storage. Also generates a compressed Base64
     * Data URL so the image synchronizes immediately to Cloud Firestore and Firebase RTDB.
     */
    fun processAndPersistPickedImage(context: Context, sourceUri: Uri): ProcessedTournamentImage? {
        return try {
            val tournamentsDir = File(context.filesDir, "tournaments").apply { if (!exists()) mkdirs() }
            val imageId = UUID.randomUUID().toString().replace("-", "").take(12)
            val destFile = File(tournamentsDir, "tourney_${imageId}.jpg")

            // 1. Decode bounds
            var inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight

            var sampleSize = 1
            if (origWidth > 0 && origHeight > 0) {
                while (origWidth / (sampleSize * 2) >= MAX_DIMENSION && origHeight / (sampleSize * 2) >= MAX_DIMENSION) {
                    sampleSize *= 2
                }
            }

            // 2. Decode bitmap
            inputStream = context.contentResolver.openInputStream(sourceUri)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (sampledBitmap == null) {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val fileUri = Uri.fromFile(destFile).toString()
                return ProcessedTournamentImage(
                    localFilePath = destFile.absolutePath,
                    localFileUri = fileUri,
                    base64DataUrl = fileUri,
                    preferredSyncUrl = fileUri
                )
            }

            // 3. EXIF rotation
            val rotatedBitmap = try {
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val rotationAngle = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                    if (rotationAngle != 0f) {
                        val matrix = Matrix().apply { postRotate(rotationAngle) }
                        Bitmap.createBitmap(sampledBitmap, 0, 0, sampledBitmap.width, sampledBitmap.height, matrix, true)
                    } else {
                        sampledBitmap
                    }
                } ?: sampledBitmap
            } catch (_: Exception) {
                sampledBitmap
            }

            // 4. Save to persistent file
            FileOutputStream(destFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            val fileUri = Uri.fromFile(destFile).toString()

            // 5. Generate high-efficiency base64 JPEG data URL for cloud sync
            val baos = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
            val base64Str = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            val dataUrl = "data:image/jpeg;base64,$base64Str"

            Log.i(TAG, "Processed tournament image successfully. File: $fileUri, Base64 len: ${base64Str.length}")
            ProcessedTournamentImage(
                localFilePath = destFile.absolutePath,
                localFileUri = fileUri,
                base64DataUrl = dataUrl,
                preferredSyncUrl = dataUrl
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process and persist picked tournament image", e)
            null
        }
    }

    /**
     * Resolves an image URL cleanly. If it's a raw base64 string, converts to data URL.
     * If it's a content URI, copies to internal storage.
     */
    fun sanitizeImageUrl(context: Context, rawUrl: String): String {
        val trimmed = rawUrl.trim().trim('"', '\'')
        if (trimmed.isBlank() || trimmed == "null" || trimmed == "undefined" || trimmed == "{}") {
            return ""
        }
        if (trimmed.startsWith("/9j/") || trimmed.startsWith("iVBORw0KGgo")) {
            return "data:image/jpeg;base64,$trimmed"
        }
        if (trimmed.startsWith("content://")) {
            try {
                val processed = processAndPersistPickedImage(context, Uri.parse(trimmed))
                if (processed != null) {
                    return processed.preferredSyncUrl
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not resolve content URI: $trimmed: ${e.message}")
            }
        }
        return trimmed
    }
}
