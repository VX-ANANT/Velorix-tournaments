package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

/**
 * High-performance frosted glass haze effect with translucent blur and subtle tint.
 * Calibrated for Haze 0.7.3.
 */
fun Modifier.optimizedHazeEffect(
    state: HazeState,
    shape: Shape = RectangleShape,
    style: HazeStyle = HazeStyle.Unspecified,
): Modifier = hazeChild(state = state, shape = shape, style = style)

/**
 * Creates standard BitChord frosted glass style with transparent tint.
 */
fun bitChordGlassStyle(
    tint: Color,
    blurRadius: Dp = 20.dp,
    alpha: Float = 0.22f,
): HazeStyle = HazeStyle(
    tint = tint.copy(alpha = alpha),
    blurRadius = blurRadius,
    noiseFactor = 0.05f
)
