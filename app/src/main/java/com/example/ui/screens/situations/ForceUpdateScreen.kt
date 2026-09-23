package com.example.ui.screens.situations

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SystemAppConfig
import com.example.ui.viewmodel.PlatformViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ForceUpdateScreen(
    viewModel: PlatformViewModel,
    config: SystemAppConfig,
    isAdmin: Boolean = false,
    onAdminBypass: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isCheckingStatus by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "update_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "update_scale"
    )

    val updateTitle = config.updateTitle.ifBlank { "Mandatory Update Available" }
    val updateMessage = config.updateMessage.ifBlank {
        "A critical update has been published by the VeloRix Team. This update includes crucial security patches, anti-cheat enhancements, and server compatibility."
    }
    val requiredVersion = config.minRequiredVersion.ifBlank { "v2.0.0" }
    val updateUrl = config.updateUrl.ifBlank { "https://velorix.esports/download" }
    val changelog = config.changelog.ifBlank {
        "• High-speed matchmaking room engine\n• Next-generation anti-cheat heuristics\n• Instant wallet deposit & withdrawal pipelines\n• Performance optimizations and bug fixes"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Cyan / Sky Blue ambient glow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-30).dp)
                .size(350.dp)
                .scale(pulseScale)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF0284C7).copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Admin Bypass Banner
            if (isAdmin) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x330284C7),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Admin Bypass Available", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                        Button(
                            onClick = onAdminBypass,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Bypass", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Rocket / Cloud Download Icon
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0x220284C7))
                        .border(2.dp, Brush.radialGradient(listOf(Color(0xFF38BDF8), Color(0x330284C7))), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0x440284C7))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RocketLaunch,
                        contentDescription = "Update",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x330284C7),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "VERSION $requiredVersion REQUIRED",
                        color = Color(0xFFBAE6FD),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = updateTitle.uppercase(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = updateMessage,
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(24.dp))

            // Changelog Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13151D)),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0xFF262B3D))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WHAT'S NEW IN $requiredVersion",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "CRITICAL PATCH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF1E2333), thickness = 1.dp)
                    Spacer(Modifier.height(14.dp))

                    changelog.split("\n").forEach { line ->
                        if (line.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = line.removePrefix("•").trim(),
                                    fontSize = 12.sp,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Update Action Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                        context.startActivity(browserIntent)
                    } catch (e: Exception) {
                        viewModel.showToast("Update link: $updateUrl")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Download & Update Now", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(12.dp))

            // Retry / Refresh Check Button
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    isCheckingStatus = true
                    scope.launch {
                        viewModel.refreshHomeData()
                        delay(1200)
                        isCheckingStatus = false
                        viewModel.showToast("Version requirements checked with cloud server.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                border = BorderStroke(1.dp, Color(0xFF262B3D))
            ) {
                if (isCheckingStatus) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Verifying Version...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Re-check App Version", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
