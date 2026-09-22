package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.GffDevanagariFontFamily
import com.example.util.CrashIncidentData
import com.example.util.CrashReporter

@Composable
fun FatalCrashDialog(
    incident: CrashIncidentData,
    onDismiss: () -> Unit,
    onRestartSafe: () -> Unit = onDismiss
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    var isSendingReport by remember { mutableStateOf(false) }
    var reportSentSuccess by remember { mutableStateOf(incident.isReported) }
    var showTechDetails by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xDD000000))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = Color(0xFF0C0E14),
                border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.6f)),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Header with Shield Icon & Alert Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0x22DC2626), CircleShape)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_untitledui_shield),
                                    contentDescription = "Sentinel Security Shield",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Critical Error Intercepted",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GffDevanagariFontFamily,
                                    color = Color(0xFFF3F4F6)
                                )
                                Text(
                                    text = "Sentinel Data Leakage Guard",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }

                        // Leakage Prevention Status Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x2210B981),
                            border = BorderStroke(1.dp, Color(0x5510B981))
                        ) {
                            Text(
                                text = "DATA SECURED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notice Message
                    Text(
                        text = "A runtime anomaly was caught before application memory, account tokens, or confidential data could be exposed. An encrypted diagnostics log has been isolated.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFD1D5DB)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Incident ID & Summary Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF141722),
                        border = BorderStroke(1.dp, Color(0xFF262B3D))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "INCIDENT CODE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF9CA3AF)
                                )
                                Text(
                                    text = incident.incidentId,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "EXCEPTION",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                                Text(
                                    text = incident.exceptionName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFF87171),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "TIME",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                                Text(
                                    text = incident.formattedTime,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE5E7EB)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle Details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTechDetails = !showTechDetails }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (showTechDetails) "Hide Sanitized Stack Logs" else "View Sanitized Diagnostics (Zero Data Leak)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF60A5FA)
                        )
                        Icon(
                            imageVector = if (showTechDetails) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = showTechDetails) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF07090D),
                            border = BorderStroke(1.dp, Color(0xFF1E2433))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = incident.sanitizedStackTrace.take(1500),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = statusMessage ?: "",
                            fontSize = 12.sp,
                            color = if (reportSentSuccess) Color(0xFF34D399) else Color(0xFFF87171)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons: Send Report & Safe Restart
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // SEND CRASH REPORT BUTTON
                        Button(
                            onClick = {
                                if (isSendingReport || reportSentSuccess) return@Button
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                isSendingReport = true
                                statusMessage = "Transmitting report to Admin Sentinel and Crashlytics..."

                                CrashReporter.sendCrashReportToAdmin(incident) { success, msg ->
                                    isSendingReport = false
                                    reportSentSuccess = success
                                    statusMessage = msg
                                    if (success) {
                                        Toast.makeText(context, "Crash report shared with Admin Panel & Crashlytics.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (reportSentSuccess) Color(0xFF059669) else Color(0xFFEF4444)
                            ),
                            enabled = !isSendingReport
                        ) {
                            if (isSendingReport) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Transmitting Report...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            } else if (reportSentSuccess) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Report Transmitted to Admin Panel",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send Crash Report to Admin",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GffDevanagariFontFamily,
                                    color = Color.White
                                )
                            }
                        }

                        // Secondary Buttons: Copy Incident ID & Dismiss/Safe Return
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(incident.incidentId))
                                    Toast.makeText(context, "Incident ID copied: ${incident.incidentId}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF374151)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD1D5DB))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Copy Code",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    CrashReporter.dismissFatalCrash()
                                    onRestartSafe()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Safe Return",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
