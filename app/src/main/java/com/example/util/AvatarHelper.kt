package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

object AvatarHelper {
    private const val TAG = "AvatarHelper"
    private const val MAX_DIMENSION = 512

    data class PresetAvatar(val name: String, val url: String)

    val PRESET_AVATARS = listOf(
        PresetAvatar("Cyber Ninja", "https://images.unsplash.com/photo-1566492031773-4f4e44671857?w=300&auto=format&fit=crop"),
        PresetAvatar("Neon Phoenix", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop"),
        PresetAvatar("Shadow Assassin", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop"),
        PresetAvatar("Apex Mech", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop"),
        PresetAvatar("Valkyrie Queen", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&auto=format&fit=crop"),
        PresetAvatar("Master Sniper", "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=300&auto=format&fit=crop"),
        PresetAvatar("Golden Champion", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop"),
        PresetAvatar("Cyber Officer", "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=300&auto=format&fit=crop")
    )

    /**
     * Copies and downsamples an image picked from the system gallery/PhotoPicker
     * into private persistent app storage. Returns a permanent file:// URI.
     */
    fun saveGalleryImageToAppStorage(context: Context, sourceUri: Uri, userId: String): String? {
        return try {
            val avatarsDir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
            val cleanId = userId.filter { it.isLetterOrDigit() }.ifEmpty { "default_user" }
            val destFile = File(avatarsDir, "avatar_${cleanId}.jpg")

            // 1. Decode bounds to determine sample size (protect against OOM on huge camera photos)
            var inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                // Fallback: copy stream bytes directly
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return Uri.fromFile(destFile).toString()
            }

            // Calculate scale
            var sampleSize = 1
            while (origWidth / (sampleSize * 2) >= MAX_DIMENSION && origHeight / (sampleSize * 2) >= MAX_DIMENSION) {
                sampleSize *= 2
            }

            // 2. Decode actual bitmap
            inputStream = context.contentResolver.openInputStream(sourceUri)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (sampledBitmap == null) {
                return null
            }

            // 3. Handle EXIF rotation if possible
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
            } catch (e: Exception) {
                Log.w(TAG, "Notice EXIF rotation reading: ${e.message}")
                sampledBitmap
            }

            // 4. Center-crop to square
            val minSide = min(rotatedBitmap.width, rotatedBitmap.height)
            val cropX = (rotatedBitmap.width - minSide) / 2
            val cropY = (rotatedBitmap.height - minSide) / 2
            val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, cropX, cropY, minSide, minSide)

            // 5. Scale to target MAX_DIMENSION
            val finalBitmap = if (minSide > MAX_DIMENSION) {
                Bitmap.createScaledBitmap(croppedBitmap, MAX_DIMENSION, MAX_DIMENSION, true)
            } else {
                croppedBitmap
            }

            // 6. Write to private file
            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                out.flush()
            }

            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save gallery image to app storage", e)
            null
        }
    }

    /**
     * Resolves any avatar URL into a guaranteed usable Coil model.
     * Sanitizes broken temporary content:// URIs and dead local file paths.
     */
    fun resolveAvatarModel(context: Context, avatarUrl: String, userId: String = ""): String {
        val trimmed = avatarUrl.trim()
        if (trimmed.isEmpty()) return ""

        if (trimmed.startsWith("file://")) {
            val path = Uri.parse(trimmed).path
            if (path != null && File(path).exists()) {
                return trimmed
            }
            // Check fallback in avatars dir
            if (userId.isNotBlank()) {
                val cleanId = userId.filter { it.isLetterOrDigit() }
                val fallbackFile = File(context.filesDir, "avatars/avatar_${cleanId}.jpg")
                if (fallbackFile.exists()) {
                    return Uri.fromFile(fallbackFile).toString()
                }
            }
            return ""
        }

        if (trimmed.startsWith("content://")) {
            // content:// URIs are transient and can throw SecurityException if permission expired.
            // Check if we have a cached file for this user:
            if (userId.isNotBlank()) {
                val cleanId = userId.filter { it.isLetterOrDigit() }
                val fallbackFile = File(context.filesDir, "avatars/avatar_${cleanId}.jpg")
                if (fallbackFile.exists()) {
                    return Uri.fromFile(fallbackFile).toString()
                }
            }
            // Try reading it once to internal storage
            try {
                val saved = saveGalleryImageToAppStorage(context, Uri.parse(trimmed), userId)
                if (saved != null) return saved
            } catch (e: Exception) {
                Log.w(TAG, "Content URI unreadable: ${e.message}")
            }
            return ""
        }

        return trimmed
    }
}
