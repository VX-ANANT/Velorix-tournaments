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
import androidx.compose.ui.draw.rotate
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
fun MaintenanceScreen(
    viewModel: PlatformViewModel,
    config: SystemAppConfig,
    isAdmin: Boolean = false,
    onAdminBypass: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isCheckingStatus by remember { mutableStateOf(false) }

    // Rotating gear animation
    val infiniteTransition = rememberInfiniteTransition(label = "gear_spin")
    val gearRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gear_rotation"
    )

    // Warm amber glow pulse
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val maintenanceTitle = config.maintenanceTitle.ifBlank { "System Under Maintenance" }
    val maintenanceMessage = config.maintenanceMessage.ifBlank {
        "We are currently upgrading tournament servers, anti-cheat matchmaking logic, and wallet gateways. All match data and funds are 100% safe."
    }
    val estimatedUntil = config.maintenanceUntil.ifBlank { "Expected Back Online Shortly" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Ambient Amber / Gold Radial Glow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-30).dp)
                .size(360.dp)
                .scale(glowScale)
                .blur(85.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFF59E0B).copy(alpha = glowAlpha), Color.Transparent)
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
            // Admin Bypass Banner (visible if current user is admin)
            if (isAdmin) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x33F59E0B),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
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
                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Admin Panel Control", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                Text("You can bypass maintenance to inspect the app", fontSize = 10.sp, color = Color(0xFFE2E8F0))
                            }
                        }
                        Button(
                            onClick = onAdminBypass,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Bypass", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Animated Gear / Server Construct Icon
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AF59E0B))
                        .border(2.dp, Brush.radialGradient(listOf(Color(0xFFF59E0B), Color(0x22F59E0B))), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color(0x33F59E0B))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Engineering,
                        contentDescription = "Maintenance",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier
                            .size(46.dp)
                            .rotate(gearRotation)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Live Maintenance Status Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x33F59E0B),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "MAINTENANCE IN PROGRESS",
                        color = Color(0xFFFDE68A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = maintenanceTitle.uppercase(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = maintenanceMessage,
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(24.dp))

            // Status Details Card
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
                            text = "SYSTEM METRICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "UPGRADE STAGE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF1E2333), thickness = 1.dp)
                    Spacer(Modifier.height(14.dp))

                    MaintenanceInfoRow(
                        icon = Icons.Rounded.Schedule,
                        title = "Estimated Return Time",
                        subtitle = estimatedUntil,
                        isHighlight = true
                    )

                    MaintenanceInfoRow(
                        icon = Icons.Rounded.Dns,
                        title = "Infrastructure Upgraded",
                        subtitle = "Real-time match room allocation & ping optimization"
                    )

                    MaintenanceInfoRow(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = "Wallet Balances & Stats",
                        subtitle = "All funds, tokens, and registration records are secured"
                    )

                    if (config.emergencyNotice.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x22F59E0B),
                            border = BorderStroke(1.dp, Color(0x55F59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Announcement, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = config.emergencyNotice,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFDE68A),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Check Live Server Status Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    isCheckingStatus = true
                    scope.launch {
                        viewModel.refreshHomeData()
                        delay(1200)
                        isCheckingStatus = false
                        viewModel.showToast("Server status re-checked with Firebase.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (isCheckingStatus) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                    Spacer(Modifier.width(10.dp))
                    Text("Pinging Tournament Servers...", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Check Live Server Status", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Community Channel Button
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    try {
                        val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/velorix_esports"))
                        context.startActivity(telegramIntent)
                    } catch (e: Exception) {
                        viewModel.showToast("Official Announcements: https://t.me/velorix_esports")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f))
            ) {
                Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Join Telegram for Live Announcements", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MaintenanceInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isHighlight) Color(0x33F59E0B) else Color(0xFF1E2435)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlight) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isHighlight) Color(0xFFFDE68A) else Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 15.sp
            )
        }
    }
}
