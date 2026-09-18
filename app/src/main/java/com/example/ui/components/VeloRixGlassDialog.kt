package com.example.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.ui.theme.NeonGreen

/**
 * Attaches real-time hardware background blur (Android 12+ / API 31+) and custom dark dimming
 * to any active Jetpack Compose Dialog window.
 */
@Composable
fun ApplyDialogWindowBlur(dimAmount: Float = 0.35f, blurRadius: Int = 100) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window != null) {
            window.setDimAmount(dimAmount)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                try {
                    window.setBackgroundBlurRadius(blurRadius)
                } catch (_: Throwable) {}
            }
        }
        onDispose {}
    }
}

/**
 * Universal Vercel + Xiaomi HyperOS glassmorphic modal dialog with hardware-accelerated background blur (API 31+),
 * smooth spring entrance animation, 90% transparent glass scrim, crisp hairline border illumination, and squircle curvature.
 */
@Composable
fun VeloRixGlassDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = true,
        dismissOnClickOutside = true
    ),
    shape: Shape = RoundedCornerShape(28.dp),
    containerColor: Color = Color(0xF20B0E14),
    borderColor: Color = Color(0x24FFFFFF),
    dimAmount: Float = 0.35f,
    blurRadius: Int = 100,
    showTopAccentHandle: Boolean = true,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        ApplyDialogWindowBlur(dimAmount = dimAmount, blurRadius = blurRadius)

        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            visible = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x2E000000)) // ~90% transparent subtle tinted dark scrim
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (properties.dismissOnClickOutside) {
                        onDismissRequest()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(200, easing = LinearOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = 380f
                            )
                        ),
                exit = fadeOut(animationSpec = tween(150)) +
                        scaleOut(targetScale = 0.94f, animationSpec = tween(150))
            ) {
                Surface(
                    modifier = modifier
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .widthIn(max = 480.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = shape,
                            ambientColor = Color(0x60000000),
                            spotColor = Color(0x90000000)
                        )
                        .clip(shape)
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x40FFFFFF),
                                        Color(0x14FFFFFF),
                                        Color(0x06FFFFFF)
                                    )
                                )
                            ),
                            shape = shape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Catch clicks so tapping the card doesn't dismiss dialog
                        },
                    shape = shape,
                    color = containerColor,
                    tonalElevation = 6.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (showTopAccentHandle) {
                            // Xiaomi HyperOS subtle top accent pill indicator
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 10.dp)
                                    .size(width = 36.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color(0x28FFFFFF))
                            )
                        }
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Drop-in Glassmorphic Alert Dialog supporting Title, Text/Content, Confirm Button, Dismiss Button,
 * and Optional Icon with blurred dark backdrop transition matching Vercel + Xiaomi aesthetic.
 */
@Composable
fun VeloRixGlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    containerColor: Color = Color(0xF20B0E14),
    borderColor: Color = Color(0x24FFFFFF)
) {
    VeloRixGlassDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        borderColor = borderColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 14.dp)
                ) {
                    icon()
                }
            }

            if (title != null) {
                CompositionLocalProvider(
                    LocalContentColor provides Color.White
                ) {
                    ProvideTextStyle(
                        MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    ) {
                        title()
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (text != null) {
                CompositionLocalProvider(
                    LocalContentColor provides Color(0xFF94A3B8)
                ) {
                    ProvideTextStyle(
                        MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp
                        )
                    ) {
                        text()
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dismissButton != null) {
                    dismissButton()
                    Spacer(modifier = Modifier.width(10.dp))
                }
                confirmButton()
            }
        }
    }
}

