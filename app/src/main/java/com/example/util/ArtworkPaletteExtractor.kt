package com.example.util

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Adaptive Color Palette extracted from artwork (thumbnail) for Tournament Cards.
 * Implements:
 * 1. Deep cinematic card background derived from artwork tones for OLED/Dark theme.
 * 2. Multi-stop uncover gradient colors for seamless artwork-to-card blending.
 * 3. 180° contrast inverted text colors (pure white #FFFFFF, crisp slate #CBD5E1) - zero unwanted yellow artifacts.
 * 4. Extracted vibrant accent color for primary action buttons & badges.
 */
data class ArtworkAdaptivePalette(
    val cardBackground: Color,
    val surfaceGradientEnd: Color,
    val accentColor: Color,
    val onAccentColor: Color,
    val highContrastText: Color,
    val mutedText: Color
)

object ArtworkPaletteManager {
    private val paletteCache = ConcurrentHashMap<String, ArtworkAdaptivePalette>()

    fun getFallbackPalette(key: String, title: String = ""): ArtworkAdaptivePalette {
        val hash = (key + title).hashCode()
        val index = abs(hash) % 5
        val baseDark = when (index) {
            0 -> Color(0xFF130E1B) // Deep purple-black (cyber/tactical)
            1 -> Color(0xFF0F1722) // Deep navy slate (night battle)
            2 -> Color(0xFF1C0D11) // Deep crimson dark (battle royale / Free Fire)
            3 -> Color(0xFF0D1A16) // Deep tactical teal/moss (survival / BGMI)
            else -> Color(0xFF14141E) // Deep obsidian indigo
        }
        val accent = when (index) {
            0 -> Color(0xFFA855F7) // Neon purple
            1 -> Color(0xFF38BDF8) // Sky cyan
            2 -> Color(0xFFF43F5E) // Crimson red
            3 -> Color(0xFF10B981) // Emerald green
            else -> Color(0xFF818CF8) // Indigo
        }
        val luminance = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
        val onAccent = if (luminance > 0.5f) Color(0xFF0F172A) else Color.White

        return ArtworkAdaptivePalette(
            cardBackground = baseDark,
            surfaceGradientEnd = baseDark.copy(alpha = 0.96f),
            accentColor = accent,
            onAccentColor = onAccent,
            highContrastText = Color(0xFFFFFFFF), // 180° contrast text (Pure white on dark surface, NO yellow)
            mutedText = Color(0xFFCBD5E1)        // 180° contrast muted text (Slate 300)
        )
    }

    suspend fun extractPalette(context: Context, imageUrl: String, title: String = ""): ArtworkAdaptivePalette {
        if (imageUrl.isBlank()) return getFallbackPalette(imageUrl, title)
        paletteCache[imageUrl]?.let { return it }

        return try {
            withContext(Dispatchers.IO) {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // Required for pixel access
                    .size(64, 64)        // Downsampled for instant extraction
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap(64, 64, Bitmap.Config.ARGB_8888)
                    val palette = withContext(Dispatchers.Default) {
                        computePaletteFromBitmap(bitmap, imageUrl, title)
                    }
                    paletteCache[imageUrl] = palette
                    palette
                } else {
                    getFallbackPalette(imageUrl, title)
                }
            }
        } catch (_: Throwable) {
            getFallbackPalette(imageUrl, title)
        }
    }

    private fun computePaletteFromBitmap(bitmap: Bitmap, key: String, title: String): ArtworkAdaptivePalette {
        val width = bitmap.width
        val height = bitmap.height
        val step = 4
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var sampleCount = 0

        var maxSat = 0f
        var dominantVibrantR = 56
        var dominantVibrantG = 189
        var dominantVibrantB = 248

        for (x in 0 until width step step) {
            for (y in 0 until height step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                totalR += r
                totalG += g
                totalB += b
                sampleCount++

                val hsv = FloatArray(3)
                android.graphics.Color.RGBToHSV(r, g, b, hsv)
                val sat = hsv[1]
                val v = hsv[2]
                if (sat > maxSat && v > 0.35f && v < 0.95f) {
                    maxSat = sat
                    dominantVibrantR = r
                    dominantVibrantG = g
                    dominantVibrantB = b
                }
            }
        }

        if (sampleCount == 0) return getFallbackPalette(key, title)

        val avgR = (totalR / sampleCount).toInt()
        val avgG = (totalG / sampleCount).toInt()
        val avgB = (totalB / sampleCount).toInt()

        // Create deep rich card background by preserving hue while pinning luminance to dark OLED levels (~10%)
        val darkHsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(avgR, avgG, avgB, darkHsv)
        darkHsv[1] = (darkHsv[1] * 0.75f).coerceIn(0.20f, 0.60f) // Keep subtle hue saturation
        darkHsv[2] = 0.09f // Deep dark background for max contrast with text
        val cardBgInt = android.graphics.Color.HSVToColor(darkHsv)

        // Accent color with vibrant saturation
        val accentHsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(dominantVibrantR, dominantVibrantG, dominantVibrantB, accentHsv)
        accentHsv[1] = accentHsv[1].coerceAtLeast(0.65f)
        accentHsv[2] = accentHsv[2].coerceIn(0.75f, 0.95f)
        val accentInt = android.graphics.Color.HSVToColor(accentHsv)

        val cardBgColor = Color(cardBgInt)
        val accentColor = Color(accentInt)

        val luminance = 0.299f * accentColor.red + 0.587f * accentColor.green + 0.114f * accentColor.blue
        val onAccent = if (luminance > 0.5f) Color(0xFF0F172A) else Color.White

        return ArtworkAdaptivePalette(
            cardBackground = cardBgColor,
            surfaceGradientEnd = cardBgColor.copy(alpha = 0.96f),
            accentColor = accentColor,
            onAccentColor = onAccent,
            highContrastText = Color(0xFFFFFFFF), // 180° contrast text (Pure white on dark surface, NO yellow)
            mutedText = Color(0xFFCBD5E1)        // 180° contrast muted text (Slate 300)
        )
    }
}

/**
 * Composable helper that remembers and asynchronously loads the adaptive color palette
 * for a tournament thumbnail artwork.
 */
@Composable
fun rememberArtworkPalette(imageUrl: String, title: String = ""): ArtworkAdaptivePalette {
    val context = LocalContext.current
    val paletteState = remember(imageUrl, title) {
        mutableStateOf(ArtworkPaletteManager.getFallbackPalette(imageUrl, title))
    }

    LaunchedEffect(imageUrl, title) {
        val extracted = ArtworkPaletteManager.extractPalette(context, imageUrl, title)
        paletteState.value = extracted
    }

    return paletteState.value
}
