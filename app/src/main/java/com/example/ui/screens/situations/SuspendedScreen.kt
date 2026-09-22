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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.viewmodel.PlatformViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SuspendedScreen(
    viewModel: PlatformViewModel,
    user: User?,
    onLogout: () -> Unit = {},
    onAdminBypass: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isCheckingStatus by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var verificationText by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "suspend_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "suspend_scale"
    )

    val suspendReason = user?.suspendReason?.ifBlank {
        "Temporary security review of tournament match activity or withdrawal verification."
    } ?: "Temporary security review of tournament match activity or withdrawal verification."

    val caseId = "REV-" + (user?.id?.takeLast(6)?.uppercase() ?: "72199B")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0E))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Orange / Amber ambient glow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-40).dp)
                .size(340.dp)
                .scale(pulseScale)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFEA580C).copy(alpha = 0.45f), Color.Transparent)
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
            // Admin Banner
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

            // Suspended / Review Shield
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0x22EA580C))
                        .border(2.dp, Brush.radialGradient(listOf(Color(0xFFEA580C), Color(0x33EA580C))), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0x44EA580C))
                        .border(1.dp, Color(0xFFFB923C).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HourglassBottom,
                        contentDescription = "Suspended",
                        tint = Color(0xFFFB923C),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x33EA580C),
                border = BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFB923C))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ACCOUNT UNDER REVIEW",
                        color = Color(0xFFFED7AA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "TEMPORARY SECURITY HOLD",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Your account is temporarily paused while our administration team reviews your recent gameplay or verification submission.",
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
                            text = "REVIEW AUDIT DETAILS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = caseId,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFB923C)
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF1E2333), thickness = 1.dp)
                    Spacer(Modifier.height(14.dp))

                    DetailItem(label = "In-Game IGN", value = user?.inGameName?.ifBlank { "Not Linked" } ?: "Not Linked")
                    DetailItem(label = "Review Trigger", value = suspendReason, isHighlight = true)
                    DetailItem(label = "Estimated Resolution", value = "Typically resolved within 12 - 24 hours")
                    DetailItem(label = "Account Balances", value = "Secure & Preserved in Cloud")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Action: Check Status
            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    isCheckingStatus = true
                    scope.launch {
                        viewModel.refreshHomeData()
                        delay(1200)
                        isCheckingStatus = false
                        viewModel.showToast("Review status checked with server.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
            ) {
                if (isCheckingStatus) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text("Refreshing Audit Queue...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Check Resolution Status", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action: Submit Evidence
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    showVerifyDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFED7AA)),
                border = BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.5f))
            ) {
                Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Submit Verification Evidence", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = { showVerifyDialog = false },
            containerColor = Color(0xFF13151D),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Submit Verification Evidence", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column {
                    Text(
                        "Provide any proof, screenshot links, or clarification to assist the admin team in approving your account.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = verificationText,
                        onValueChange = { verificationText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Enter statement or proof...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEA580C),
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
                        if (verificationText.isNotBlank()) {
                            viewModel.submitReport(
                                category = "VERIFICATION",
                                title = "Suspension Evidence: $caseId",
                                description = verificationText,
                                contactInfo = user?.phoneOrEmail ?: "",
                                relatedId = caseId
                            )
                            showVerifyDialog = false
                            viewModel.showToast("Evidence transmitted to review team.")
                        }
                    },
                    enabled = verificationText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                ) {
                    Text("Submit Evidence", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVerifyDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
