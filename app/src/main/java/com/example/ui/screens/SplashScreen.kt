package com.example.ui.screens

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.PlatformViewModel
import kotlinx.coroutines.delay
@Composable
fun SplashScreen(
    viewModel: PlatformViewModel,
    onNavigateToAuth: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulsing")
    // Progress bar animation
    val progressAnim by infiniteTransition.animateFloat(
        initialValue = 0.01f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing)
        ),
        label = "progress_bar"
    )
    val isCheckingAuth by viewModel.isCheckingAuth.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    LaunchedEffect(isCheckingAuth) {
        if (!isCheckingAuth) {
            delay(2600) // Minimum display time for splash
            if (isLoggedIn) {
                onNavigateToHome()
            } else {
                onNavigateToAuth()
            }
        }
    }
    val velorixRed = Color(0xFFC70039) // Deep crimson red for the theme
    val darkRed = Color(0xFF420012)
    val almostBlack = Color(0xFF070505)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        darkRed,
                        almostBlack
                    ),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 64.dp) // shift content up a bit to match optical center
        ) {
            // Glowing concentric circles around the core logo
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "splash")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.15f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "pulseScale"
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
            ) {
                // Outer faint glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, velorixRed.copy(alpha = 0.05f), CircleShape)
                )
                // Mid glow
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .border(1.dp, velorixRed.copy(alpha = 0.15f), CircleShape)
                )
                // Inner core logo container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(200.dp)
                        .border(1.dp, velorixRed.copy(alpha = 0.8f), CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF0F0004)) // Pitch black with slight red tint
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.velorix_logo_image),
                        contentDescription = "Velorix Core Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            // VELORIX Text
            Text(
                text = "V E L O R I X",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                    color = Color(0xFFF91E4E) // A slightly brighter, more fluorescent red as seen in screenshot
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            // LOADING Text
            Text(
                text = "LOADING THE ARENA",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 4.sp,
                    color = Color(0xFFF91E4E).copy(alpha = 0.6f),
                    fontSize = 9.sp
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Animated HTML-like Geometric Loader
            Spacer(modifier = Modifier.height(32.dp))
            com.example.ui.components.AnimatedLoaders(
                modifier = Modifier.wrapContentSize(),
                pathColor = Color(0xFFF91E4E).copy(alpha = 0.5f),
                dotColor = Color(0xFFF91E4E)
            )
        }
    }
}