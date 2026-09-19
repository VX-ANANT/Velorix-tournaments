package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * CyberTacticalAssets.kt
 *
 * Elite cybernetic and militaristic esports design assets.
 * Implements animated HUD scanlines, rotating radiant energy borders,
 * corner target reticles, and cryptographic telemetry ribbons.
 * Strictly 0% emojis - 100% defense-grade visual craftsmanship.
 */

@Composable
fun CyberScanlineOverlay(
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF38BDF8).copy(alpha = 0.08f),
    glowColor: Color = Color(0xFF00E5FF).copy(alpha = 0.15f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanline_anim")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val sweepY = size.height * sweepProgress
        // Draw the sweeping beam
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    glowColor,
                    glowColor.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                startY = sweepY - 30.dp.toPx(),
                endY = sweepY + 30.dp.toPx()
            ),
            topLeft = Offset(0f, sweepY - 30.dp.toPx()),
            size = Size(size.width, 60.dp.toPx())
        )

        // Subtle horizontal CRT raster lines
        val step = 6.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.8f
            )
            y += step
        }
    }
}

@Composable
fun TacticalReticleFrame(
    modifier: Modifier = Modifier,
    cornerLength: Dp = 16.dp,
    strokeWidth: Dp = 2.dp,
    accentColor: Color = Color(0xFF38BDF8),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            val cl = cornerLength.toPx()
            val sw = strokeWidth.toPx()
            val c = accentColor

            // Top-Left corner
            drawLine(color = c, start = Offset(0f, 0f), end = Offset(cl, 0f), strokeWidth = sw)
            drawLine(color = c, start = Offset(0f, 0f), end = Offset(0f, cl), strokeWidth = sw)

            // Top-Right corner
            drawLine(color = c, start = Offset(size.width, 0f), end = Offset(size.width - cl, 0f), strokeWidth = sw)
            drawLine(color = c, start = Offset(size.width, 0f), end = Offset(size.width, cl), strokeWidth = sw)

            // Bottom-Left corner
            drawLine(color = c, start = Offset(0f, size.height), end = Offset(cl, size.height), strokeWidth = sw)
            drawLine(color = c, start = Offset(0f, size.height), end = Offset(0f, size.height - cl), strokeWidth = sw)

            // Bottom-Right corner
            drawLine(color = c, start = Offset(size.width, size.height), end = Offset(size.width - cl, size.height), strokeWidth = sw)
            drawLine(color = c, start = Offset(size.width, size.height), end = Offset(size.width, size.height - cl), strokeWidth = sw)
        }
    ) {
        content()
    }
}

@Composable
fun AnimatedRadiantBorderCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.5.dp,
    glowColors: List<Color> = listOf(
        Color(0xFF38BDF8),
        Color(0xFF818CF8),
        Color(0xFFC084FC),
        Color(0xFF22D3EE),
        Color(0xFF38BDF8)
    ),
    backgroundColor: Color = Color(0xFF0C0D12),
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radiant_border_anim")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_angle"
    )

    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(
            width = borderWidth,
            brush = Brush.sweepGradient(glowColors)
        )
    ) {
        content()
    }
}

@Composable
fun TacticalTelemetryRibbon(
    modifier: Modifier = Modifier,
    systemTag: String = "SENTINEL-DEFENSE",
    protocolCode: String = "AES-256-GCM",
    statusText: String = "ARMED // ACTIVE",
    accentColor: Color = Color(0xFF10B981)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ribbon_pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_pulse"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111319),
        border = BorderStroke(1.dp, Color(0xFF1F2430)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = alphaPulse))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = systemTag,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
                Text(
                    text = " :: $protocolCode",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B)
                )
            }

            Text(
                text = statusText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp,
                color = accentColor
            )
        }
    }
}
