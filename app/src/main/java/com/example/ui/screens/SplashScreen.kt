package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GffDevanagariFontFamily
import com.example.ui.viewmodel.PlatformViewModel
import kotlinx.coroutines.delay

/**
 * LinearDots Loader inspired by loading-dev:
 * Renders 3 horizontally aligned dots oscillating with a smooth, staggered wave animation.
 */
@Composable
fun LinearDotsLoader(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFF0060),
    dotSize: Dp = 9.dp,
    spacing: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "linear_dots_transition")

    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, delayMillis = 0, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer {
                    scaleX = dot1Scale
                    scaleY = dot1Scale
                    alpha = 0.30f + (dot1Scale * 0.70f)
                }
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer {
                    scaleX = dot2Scale
                    scaleY = dot2Scale
                    alpha = 0.30f + (dot2Scale * 0.70f)
                }
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer {
                    scaleX = dot3Scale
                    scaleY = dot3Scale
                    alpha = 0.30f + (dot3Scale * 0.70f)
                }
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun SplashScreen(
    viewModel: PlatformViewModel,
    onNavigateToAuth: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val isCheckingAuth by viewModel.isCheckingAuth.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isCheckingAuth) {
        if (!isCheckingAuth) {
            delay(2400) // Optimal display time for splash & boot feel
            if (isLoggedIn) {
                onNavigateToHome()
            } else {
                onNavigateToAuth()
            }
        }
    }

    // Ultra-subtle, executive ambient breathing transition for the emblem
    val infiniteTransition = rememberInfiniteTransition(label = "splash_subtle_ambient")
    val subtlePulse by infiniteTransition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "subtle_logo_scale"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    val velorixRed = Color(0xFFFF0060)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
    ) {
        // Deep ambient radial background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x38FF0060),
                            Color(0x12FF003C),
                            Color(0xFF07090E)
                        ),
                        radius = 1100f
                    )
                )
        )

        // -------------------------------------------------------------
        // CENTER BRAND LOGO & TITLE (Mature, Bold & Sized Proportionately)
        // -------------------------------------------------------------
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 40.dp)
        ) {
            // Elegant concentric emblem container with dead-center optical precision
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(210.dp)
                    .graphicsLayer {
                        scaleX = subtlePulse
                        scaleY = subtlePulse
                    }
            ) {
                // Outer ambient aura ring
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .border(
                            width = 1.dp,
                            color = velorixRed.copy(alpha = 0.15f * auraAlpha),
                            shape = CircleShape
                        )
                )
                // Mid subtle accent ring
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .border(
                            width = 1.dp,
                            color = velorixRed.copy(alpha = 0.28f * auraAlpha),
                            shape = CircleShape
                        )
                )
                // Inner core logo container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(150.dp)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    velorixRed.copy(alpha = 0.90f),
                                    velorixRed.copy(alpha = 0.30f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF18040B),
                                    Color(0xFF0B0206)
                                )
                            )
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.velorix_logo_image),
                        contentDescription = "Velorix Core Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 22.dp)
                            .offset(y = (-2).dp), // Exact optical dead-centering compensation
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brand Title - Mature, Bold Typography
            Text(
                text = "VELORIX",
                fontFamily = GffDevanagariFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = 8.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "ESPORTS TOURNAMENT ARENA",
                fontFamily = GffDevanagariFontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 4.sp,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
            )
        }

        // -------------------------------------------------------------
        // BOTTOM FOOTER: LinearDots Loader & Uploaded "Powered by Android" Logo
        // -------------------------------------------------------------
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // LinearDots Animated Loader (#FF0060)
            LinearDotsLoader(
                color = Color(0xFFFF0060),
                dotSize = 8.dp,
                spacing = 8.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Official "Powered by Android" Uploaded Emblem
            Image(
                painter = painterResource(id = R.drawable.img_powered_by_android),
                contentDescription = "Powered by Android",
                modifier = Modifier
                    .width(120.dp)
                    .height(80.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
