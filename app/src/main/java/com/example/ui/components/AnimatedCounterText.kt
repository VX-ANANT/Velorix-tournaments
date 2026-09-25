package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AnimatedRollingCounter.kt
 *
 * Smooth Apple-style physical ticker & rolling number counter.
 * Whenever a numerical value updates:
 * - If increasing: New digits roll in from the top and slide downwards.
 * - If decreasing: New digits roll in from the bottom and slide upwards.
 * - Supports integer, float, currency prefixes, suffixes, and custom formatting.
 */
@Composable
fun AnimatedRollingCounter(
    targetValue: Number,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.Unspecified,
    letterSpacing: TextUnit = 0.sp,
    style: TextStyle = LocalTextStyle.current,
    animateValueGradually: Boolean = true
) {
    val doubleTarget = targetValue.toDouble()
    
    // Smoothly interpolate numeric value if gradual animation is enabled
    val animatedNumber by animateFloatAsState(
        targetValue = doubleTarget.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessLow
        ),
        label = "rolling_number_interpolation"
    )

    val displayValue = if (animateValueGradually) animatedNumber.toDouble() else doubleTarget

    // Format number string based on whether original is integer or float
    val isInt = targetValue is Int || targetValue is Long || (doubleTarget % 1.0 == 0.0)
    val formattedString = if (isInt) {
        displayValue.toLong().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", displayValue)
    }

    var previousTarget by remember { mutableDoubleStateOf(doubleTarget) }
    val isIncreasing = doubleTarget >= previousTarget

    LaunchedEffect(doubleTarget) {
        previousTarget = doubleTarget
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
                letterSpacing = letterSpacing,
                style = style
            )
        }

        formattedString.forEachIndexed { index, char ->
            if (char.isDigit()) {
                AnimatedDigit(
                    digit = char,
                    isIncreasing = isIncreasing,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = color,
                    letterSpacing = letterSpacing,
                    style = style
                )
            } else {
                Text(
                    text = char.toString(),
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = color,
                    letterSpacing = letterSpacing,
                    style = style
                )
            }
        }

        if (suffix.isNotEmpty()) {
            Text(
                text = suffix,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
                letterSpacing = letterSpacing,
                style = style
            )
        }
    }
}

/**
 * Animated individual digit with directional slide & fluid Apple-style spring
 */
@Composable
private fun AnimatedDigit(
    digit: Char,
    isIncreasing: Boolean,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    letterSpacing: TextUnit,
    style: TextStyle
) {
    AnimatedContent(
        targetState = digit,
        transitionSpec = {
            if (isIncreasing) {
                // Number increasing: comes in from top, leaves to bottom
                (slideInVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 550f),
                    initialOffsetY = { -it }
                ) + fadeIn(animationSpec = tween(150))) togetherWith
                (slideOutVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 550f),
                    targetOffsetY = { it }
                ) + fadeOut(animationSpec = tween(120)))
            } else {
                // Number decreasing: comes in from bottom, leaves to top
                (slideInVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 550f),
                    initialOffsetY = { it }
                ) + fadeIn(animationSpec = tween(150))) togetherWith
                (slideOutVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 550f),
                    targetOffsetY = { -it }
                ) + fadeOut(animationSpec = tween(120)))
            }.using(SizeTransform(clip = false))
        },
        label = "animated_digit_$digit"
    ) { targetDigit ->
        Text(
            text = targetDigit.toString(),
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            letterSpacing = letterSpacing,
            style = style
        )
    }
}

/**
 * Smooth Apple-style In/Out Animated Text for titles, badges, and status labels
 */
@Composable
fun AnimatedFadeSlideText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    letterSpacing: TextUnit = 0.sp,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
             slideInVertically(
                 animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                 initialOffsetY = { it / 3 }
             )) togetherWith
            (fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing)) +
             slideOutVertically(
                 animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                 targetOffsetY = { -it / 3 }
             ))
        },
        label = "animated_text_$text",
        modifier = modifier
    ) { targetText ->
        Text(
            text = targetText,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            letterSpacing = letterSpacing,
            style = style,
            maxLines = maxLines
        )
    }
}
