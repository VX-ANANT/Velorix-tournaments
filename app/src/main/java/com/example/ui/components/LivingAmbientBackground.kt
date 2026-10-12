package com.example.ui.components

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlin.math.cos
import kotlin.math.sin

/**
 * LivingAmbientBackground.kt
 *
 * Implements a slow-moving, subtle living ambient atmospheric background.
 * Makes the entire application feel alive and premium through organic motion.
 *
 * Battery & Accessibility Awareness:
 * - Detects Low Power Mode (PowerManager.isPowerSaveMode) and pauses animation.
 * - Detects Accessibility Reduce Motion and freezes to a calm, static gradient.
 */
@Composable
fun LivingAmbientBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current

    // Check system power saving & accessibility settings
    val isPowerSaveMode = remember(context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isPowerSaveMode == true
        } catch (_: Exception) {
            false
        }
    }

    val isReduceMotion = remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }

    val shouldAnimate = enabled && !isPowerSaveMode && !isReduceMotion

    if (!shouldAnimate) {
        // Static ambient calm gradient (Low Power / Reduce Motion)
        Canvas(modifier = modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(w * 0.25f, h * 0.2f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.25f, h * 0.2f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.04f), Color.Transparent),
                    center = Offset(w * 0.75f, h * 0.7f),
                    radius = w * 0.7f
                ),
                radius = w * 0.7f,
                center = Offset(w * 0.75f, h * 0.7f)
            )
        }
        return
    }

    // Smooth, slow living animation (~28 seconds cycle)
    val infiniteTransition = rememberInfiniteTransition(label = "living_ambient_bg")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Orb 1: Cyan/Sky tactical light, drifting softly around upper-left quadrant
        val x1 = w * (0.28f + 0.12f * sin(phase.toDouble()).toFloat())
        val y1 = h * (0.22f + 0.08f * cos(phase.toDouble()).toFloat())
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.06f),
                    Color(0xFF38BDF8).copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = Offset(x1, y1),
                radius = w * 0.75f
            ),
            radius = w * 0.75f,
            center = Offset(x1, y1)
        )

        // Orb 2: Purple/Indigo neon aura, drifting around center-right
        val x2 = w * (0.72f - 0.14f * cos((phase * 0.85f).toDouble()).toFloat())
        val y2 = h * (0.55f + 0.10f * sin((phase * 0.85f).toDouble()).toFloat())
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF8B5CF6).copy(alpha = 0.05f),
                    Color(0xFF6366F1).copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = Offset(x2, y2),
                radius = w * 0.85f
            ),
            radius = w * 0.85f,
            center = Offset(x2, y2)
        )

        // Orb 3: Subtle emerald accent near bottom left
        val x3 = w * (0.35f + 0.10f * cos((phase * 1.2f).toDouble()).toFloat())
        val y3 = h * (0.82f - 0.06f * sin((phase * 1.2f).toDouble()).toFloat())
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF10B981).copy(alpha = 0.035f),
                    Color.Transparent
                ),
                center = Offset(x3, y3),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(x3, y3)
        )
    }
}
