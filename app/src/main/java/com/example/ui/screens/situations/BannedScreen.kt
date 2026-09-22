package com.example.ui.screens.situations

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import com.example.data.model.User
import com.example.ui.viewmodel.PlatformViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BannedScreen(
    viewModel: PlatformViewModel,
    user: User?,
    onLogout: () -> Unit = {},
    onAdminBypass: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isCheckingStatus by remember { mutableStateOf(false) }
    var showAppealDialog by remember { mutableStateOf(false) }
    var appealText by remember { mutableStateOf("") }
    var appealSubmitted by remember { mutableStateOf(false) }

    // Pulsing danger glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "ban_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val banReason = user?.banReason?.ifBlank { "Violation of Fair Play Terms / Anti-Cheat & Third-Party Tool Detection" }
        ?: "Violation of Fair Play Terms / Anti-Cheat & Third-Party Tool Detection"
    val banType = user?.banType?.ifBlank { "PERMANENT BAN" } ?: "PERMANENT BAN"
    val caseId = "BAN-" + (user?.id?.takeLast(6)?.uppercase() ?: "88419X")

    val bannedAtFormatted = remember(user?.bannedAt) {
        val time = user?.bannedAt ?: 0L
        if (time > 0L) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
            sdf.format(Date(time))
        } else {
            "Recent Incident"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0E))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Deep Crimson Background Radial Glow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-40).dp)
                .size(340.dp)
                .scale(pulseScale)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFDC2626).copy(alpha = glowAlpha), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Admin Preview / Testing Banner
            if (user?.phoneOrEmail.equals("service.veloxyra@gmail.com", ignoreCase = true) || user?.phoneOrEmail.equals("anantisback47@gmail.com", ignoreCase = true) || user?.role?.contains("admin", true) == true) {
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
                            Text("Admin Mode Active", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                        }
                        TextButton(
                            onClick = onAdminBypass,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Bypass", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Danger Lock Badge
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0x22DC2626))
                        .border(2.dp, Brush.radialGradient(listOf(Color(0xFFEF4444), Color(0x33DC2626))), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0x44DC2626))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Gavel,
                        contentDescription = "Banned",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Restricted Status Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x33DC2626),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = banType.uppercase(),
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "ACCOUNT RESTRICTED",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Your account has been suspended by the VeloRix Administration due to detected violations of fair play integrity.",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(24.dp))

            // Details Card
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
                            text = "INCIDENT SUMMARY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = caseId,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF1E2333), thickness = 1.dp)
                    Spacer(Modifier.height(14.dp))

                    DetailItem(label = "Account Username", value = user?.username ?: "Player")
                    DetailItem(label = "In-Game IGN", value = user?.inGameName?.ifBlank { "Not Linked" } ?: "Not Linked")
                    DetailItem(label = "Player Game ID", value = user?.freeFireId?.ifBlank { user?.id ?: "N/A" } ?: "N/A")
                    DetailItem(label = "Violation Reason", value = banReason, isHighlight = true)
                    DetailItem(label = "Sanction Timestamp", value = bannedAtFormatted)

                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x15EF4444),
                        border = BorderStroke(1.dp, Color(0x33EF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Block, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Matchmaking, tournament entry, and wallet withdrawals are locked.",
                                fontSize = 11.sp,
                                color = Color(0xFFFCA5A5),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    isCheckingStatus = true
                    scope.launch {
                        viewModel.refreshHomeData()
                        delay(1200)
                        isCheckingStatus = false
                        viewModel.showToast("Account status re-verified with Firebase.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E2435),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                if (isCheckingStatus) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text("Syncing with Admin Database...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Check Live Ban Status", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    showAppealDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Rounded.SupportAgent, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Submit Appeal to Admin", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                border = BorderStroke(1.dp, Color(0xFF262B3D))
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Log Out Account", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Appeal Submission Dialog
    if (showAppealDialog) {
        AlertDialog(
            onDismissRequest = { showAppealDialog = false },
            containerColor = Color(0xFF13151D),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("File Official Ban Appeal", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column {
                    Text(
                        "Explain your situation directly to the VeloRix integrity review board. Include any relevant match details, device specifications, or proof.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = appealText,
                        onValueChange = { appealText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        placeholder = { Text("Describe why you believe this sanction is an error...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (appealText.isNotBlank()) {
                            viewModel.submitReport(
                                category = "ACCOUNT_ISSUE",
                                title = "Ban Appeal: $caseId",
                                description = "Player Appeal:\n$appealText\n\nUser ID: ${user?.id}\nIn-Game IGN: ${user?.inGameName}\nGame ID: ${user?.freeFireId}",
                                contactInfo = user?.phoneOrEmail ?: "",
                                relatedId = caseId
                            )
                            appealSubmitted = true
                            showAppealDialog = false
                            viewModel.showToast("Appeal submitted directly to Admin Review Queue.")
                        }
                    },
                    enabled = appealText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Submit Appeal", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppealDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
fun DetailItem(label: String, value: String, isHighlight: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF64748B))
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) Color(0xFFFCA5A5) else Color.White
        )
    }
}
