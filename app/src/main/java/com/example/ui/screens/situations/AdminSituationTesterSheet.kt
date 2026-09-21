package com.example.ui.screens.situations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SituationPreviewType
import com.example.data.model.SystemAppConfig
import com.example.data.model.User
import com.example.ui.viewmodel.PlatformViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSituationTesterSheet(
    viewModel: PlatformViewModel,
    user: User?,
    systemConfig: SystemAppConfig,
    currentPreview: SituationPreviewType,
    onDismiss: () -> Unit,
    onOpenDeveloperModal: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var customMaintMsg by remember { mutableStateOf(systemConfig.maintenanceMessage) }
    var customEta by remember { mutableStateOf(systemConfig.maintenanceUntil) }
    var customBanReason by remember { mutableStateOf(user?.banReason ?: "Anti-Cheat Detection - Integrity Violation") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF10131B),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFF334155))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33F59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Admin Situation Controls", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("Real-time Firebase RTDB toggles & visual screen previews", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Live State Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A26)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF262E42)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("LIVE CLOUD DATABASE STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.8.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusIndicatorPill(label = "Maintenance", isActive = systemConfig.isMaintenance)
                        StatusIndicatorPill(label = "Account Banned", isActive = user?.isBanned == true)
                        StatusIndicatorPill(label = "Dev Modal", isActive = systemConfig.showDeveloperModal)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Section 1: Instant Visual Screen Previews
            Text("INSTANT SCREEN PREVIEW (TESTING)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 0.8.sp)
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        viewModel.setSituationPreview(SituationPreviewType.BANNED)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Banned Screen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        viewModel.setSituationPreview(SituationPreviewType.MAINTENANCE)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Maintenance", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        viewModel.setSituationPreview(SituationPreviewType.SUSPENDED)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Suspended", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        viewModel.setSituationPreview(SituationPreviewType.FORCE_UPDATE)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Force Update", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        viewModel.setSituationPreview(SituationPreviewType.VPN_BLOCKED)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("VPN Blocked", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (currentPreview != SituationPreviewType.NONE) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.setSituationPreview(SituationPreviewType.NONE)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reset to Live", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }
            }

            if (onOpenDeveloperModal != null) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onOpenDeveloperModal()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A192F)),
                    border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Preview Developer Modal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }
            }

            Spacer(Modifier.height(20.dp))

            // Section 2: Real-time Cloud Toggles
            Text("REALTIME DATABASE TOGGLES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 0.8.sp)
            Spacer(Modifier.height(8.dp))

            // Maintenance Mode Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A26)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF262E42)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Maintenance Mode (Global RTDB)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Writes to /app_config/is_maintenance", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = systemConfig.isMaintenance,
                            onCheckedChange = { isChecked ->
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                viewModel.updateMaintenanceMode(
                                    enabled = isChecked,
                                    message = customMaintMsg,
                                    eta = customEta
                                )
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFF59E0B))
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customMaintMsg,
                        onValueChange = { customMaintMsg = it },
                        label = { Text("Maintenance Reason", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFFF59E0B)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customEta,
                        onValueChange = { customEta = it },
                        label = { Text("Expected End (ETA)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFFF59E0B)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Account Ban Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A26)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF262E42)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ban Current Account (RTDB)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("UID: ${user?.id?.take(10) ?: "None"}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Switch(
                            checked = user?.isBanned == true,
                            onCheckedChange = { isChecked ->
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                val uid = user?.id ?: ""
                                if (uid.isNotBlank()) {
                                    viewModel.toggleUserBan(
                                        userId = uid,
                                        isBanned = isChecked,
                                        reason = customBanReason
                                    )
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFDC2626))
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customBanReason,
                        onValueChange = { customBanReason = it },
                        label = { Text("Sanction / Ban Reason", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFDC2626),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFFDC2626)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Account Suspension Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A26)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF262E42)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Suspend Current Account", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Triggers temporary integrity hold", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = user?.isSuspended == true,
                        onCheckedChange = { isChecked ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            val uid = user?.id ?: ""
                            if (uid.isNotBlank()) {
                                viewModel.toggleUserSuspension(
                                    userId = uid,
                                    isSuspended = isChecked,
                                    reason = "Account under security review by administration."
                                )
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFEA580C))
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Force Update Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A26)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF262E42)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Force Mandatory Update", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Requires v2.0.0+", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = systemConfig.isForceUpdate,
                        onCheckedChange = { isChecked ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.toggleForceUpdate(enabled = isChecked)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0284C7))
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Developer Modal Remote Config Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A26)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF262E42)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Developer Profile Modal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Remote toggle for in-app developer highlights", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = systemConfig.showDeveloperModal,
                        onCheckedChange = { isChecked ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.toggleDeveloperModal(enabled = isChecked)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0284C7))
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicatorPill(label: String, isActive: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) Color(0x33EF4444) else Color(0x2210B981),
        border = BorderStroke(1.dp, if (isActive) Color(0xFFEF4444) else Color(0xFF10B981))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color(0xFFEF4444) else Color(0xFF10B981))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isActive) "$label: ON" else "$label: OFF",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color(0xFFFCA5A5) else Color(0xFFA7F3D0)
            )
        }
    }
}
