package com.example.ui.screens.situations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Banner
import com.example.data.model.SituationPreviewType
import com.example.data.model.SystemAppConfig
import com.example.data.model.Tournament
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

    val banners by viewModel.banners.collectAsState()
    val tournaments by viewModel.tournaments.collectAsState()

    var newBannerTitle by remember { mutableStateOf("") }
    var newBannerSubtitle by remember { mutableStateOf("") }
    var newBannerBadge by remember { mutableStateOf("ANNOUNCEMENT") }
    var newBannerImageUrl by remember { mutableStateOf("") }
    var newBannerCtaText by remember { mutableStateOf("EXPLORE NOW") }
    var newBannerActionType by remember { mutableStateOf("ANNOUNCEMENT") }
    var newBannerTargetId by remember { mutableStateOf("") }
    var newBannerValidUntil by remember { mutableStateOf("") }
    var newBannerTheme by remember { mutableStateOf("CYAN_PURPLE") }
    var isPublishingBanner by remember { mutableStateOf(false) }

    // Tournament Creator States
    var tourneyGame by remember { mutableStateOf("Free Fire") }
    var tourneyCategory by remember { mutableStateOf("CLASH_SQUAD") } // "BATTLE_ROYALE", "CLASH_SQUAD", "LONE_WOLF", "SPECIAL_MODE"
    var tourneyFormat by remember { mutableStateOf("1v1") } // "1v1", "2v2", "3v3", "4v4", "SOLO", "DUO", "SQUAD"
    var tourneyMode by remember { mutableStateOf("HEADSHOT_ONLY") } // "HEADSHOT_ONLY", "BODY_DAMAGE_ON", "SNIPER_ONLY", "PER_KILL", "SURVIVAL", "LIMITED_AMMO", "UNLIMITED_AMMO"
    var tourneyTitle by remember { mutableStateOf("FF CS 1v1 • Headshot Only Grand Duel") }
    var tourneyEntryFee by remember { mutableStateOf("50") }
    var tourneyPrizePool by remember { mutableStateOf("90") }
    var tourneyKillBounty by remember { mutableStateOf("0") }
    var tourneyMap by remember { mutableStateOf("Bermuda") }
    var tourneyPerspective by remember { mutableStateOf("TPP") }
    var tourneyMaxSlots by remember { mutableStateOf("2") }
    var isPublishingTourney by remember { mutableStateOf(false) }

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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusIndicatorPill(label = "Maintenance", isActive = systemConfig.isMaintenance, modifier = Modifier.weight(1f))
                        StatusIndicatorPill(label = "Banned", isActive = user?.isBanned == true, modifier = Modifier.weight(1f))
                        StatusIndicatorPill(label = "Banners", isActive = systemConfig.showBanners, modifier = Modifier.weight(1f))
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

            Spacer(Modifier.height(18.dp))

            // Section: App Banners & Announcements System
            Text("BANNER & ANNOUNCEMENT SYSTEM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 0.8.sp)
            Spacer(Modifier.height(8.dp))

            // Master Banner Visibility Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A26)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (systemConfig.showBanners) Color(0xFF3B82F6) else Color(0xFF262E42)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Promotional Banners Carousel", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (systemConfig.showBanners) Color(0x3310B981) else Color(0x3364748B)
                            ) {
                                Text(
                                    text = if (systemConfig.showBanners) "VISIBLE" else "HIDDEN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (systemConfig.showBanners) Color(0xFF34D399) else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (systemConfig.showBanners) "Banners are currently DISPLAYED on Home screen" else "Banners are completely HIDDEN from all users",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = systemConfig.showBanners,
                        onCheckedChange = { isChecked ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.toggleShowBanners(enabled = isChecked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B82F6),
                            uncheckedThumbColor = Color(0xFF64748B),
                            uncheckedTrackColor = Color(0xFF1E293B)
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Manual Banner & Announcement Creator Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
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
                        Text("Create Manual Banner / Notice", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Icon(Icons.Rounded.Campaign, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }
                    Text("Type custom announcements and attach custom banner images directly", fontSize = 11.sp, color = Color(0xFF94A3B8))

                    Spacer(Modifier.height(12.dp))

                    // Title
                    OutlinedTextField(
                        value = newBannerTitle,
                        onValueChange = { newBannerTitle = it },
                        label = { Text("Banner / Announcement Title *", fontSize = 12.sp) },
                        placeholder = { Text("e.g., Sunday Grand BGMI Showdown", fontSize = 12.sp, color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    // Subtitle / Description
                    OutlinedTextField(
                        value = newBannerSubtitle,
                        onValueChange = { newBannerSubtitle = it },
                        label = { Text("Announcement Subtitle / Details", fontSize = 12.sp) },
                        placeholder = { Text("e.g., Win ₹5,000 VT Prize Pool. Direct slot entry.", fontSize = 12.sp, color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    // Image URL
                    OutlinedTextField(
                        value = newBannerImageUrl,
                        onValueChange = { newBannerImageUrl = it },
                        label = { Text("Banner Image URL (Optional)", fontSize = 12.sp) },
                        placeholder = { Text("https://... or leave empty for gradient card", fontSize = 12.sp, color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    // Row: Badge & CTA Text
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newBannerBadge,
                            onValueChange = { newBannerBadge = it },
                            label = { Text("Badge Label", fontSize = 11.sp) },
                            placeholder = { Text("HOT / NOTICE", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF38BDF8),
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = newBannerCtaText,
                            onValueChange = { newBannerCtaText = it },
                            label = { Text("Button CTA", fontSize = 11.sp) },
                            placeholder = { Text("JOIN NOW", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF38BDF8),
                                unfocusedLabelColor = Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Action Type Selector
                    Text("Action Destination", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ANNOUNCEMENT", "MATCH", "WALLET", "SUPPORT", "LEADERBOARD").forEach { action ->
                            val isSelected = newBannerActionType == action
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E2433),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { newBannerActionType = action }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = action.take(4),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Buttons: Publish to Cloud & Broadcast Notification
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (newBannerTitle.isBlank()) {
                                    viewModel.showError("Please provide a Banner Title")
                                    return@Button
                                }
                                isPublishingBanner = true
                                val newBanner = Banner(
                                    id = java.util.UUID.randomUUID().toString(),
                                    title = newBannerTitle.trim(),
                                    subtitle = newBannerSubtitle.trim(),
                                    badgeText = newBannerBadge.trim().ifBlank { "ANNOUNCEMENT" },
                                    imageUrl = newBannerImageUrl.trim(),
                                    ctaText = newBannerCtaText.trim().ifBlank { "EXPLORE" },
                                    actionType = newBannerActionType,
                                    targetId = newBannerTargetId.trim(),
                                    gradientTheme = newBannerTheme,
                                    active = true
                                )
                                viewModel.saveBanner(newBanner) { success ->
                                    isPublishingBanner = false
                                    if (success) {
                                        newBannerTitle = ""
                                        newBannerSubtitle = ""
                                        newBannerImageUrl = ""
                                    }
                                }
                            },
                            enabled = !isPublishingBanner && newBannerTitle.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Publish Banner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (newBannerTitle.isBlank()) {
                                    viewModel.showError("Please provide a Title for the announcement")
                                    return@OutlinedButton
                                }
                                viewModel.publishAnnouncementNotification(
                                    title = newBannerTitle.trim(),
                                    message = newBannerSubtitle.trim().ifBlank { "Check the latest announcement in VeloRix Tournaments!" }
                                )
                            },
                            enabled = newBannerTitle.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = BorderStroke(1.dp, Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Push Notice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Active Banners List
            Text("ACTIVE PUBLISHED BANNERS (${banners.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 0.8.sp)
            Spacer(Modifier.height(8.dp))

            if (banners.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No active banners published. Add one using the manual creator above.", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    banners.forEach { banner ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161A26)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF262E42)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    if (banner.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = banner.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF1E293B))
                                        )
                                        Spacer(Modifier.width(10.dp))
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0x3338BDF8)
                                            ) {
                                                Text(
                                                    banner.badgeText.ifBlank { "BANNER" },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF38BDF8),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                banner.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (banner.subtitle.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                banner.subtitle,
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        viewModel.deleteBanner(banner.id)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Banner", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ==========================================
            // TOURNAMENT MANAGER & INSTANT CLOUD SYNC
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("TOURNAMENT MANAGER & CLOUD SYNC", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x2210B981),
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Text(
                        "LIVE CLOUD SYNC ON",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFA7F3D0),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Quick Category Presets
            Text("QUICK CATEGORY PRESETS (1-TAP LOAD):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        tourneyGame = "Free Fire"
                        tourneyCategory = "CLASH_SQUAD"
                        tourneyFormat = "1v1"
                        tourneyMode = "HEADSHOT_ONLY"
                        tourneyTitle = "FF CS 1v1 • Headshot Only Grand Duel"
                        tourneyEntryFee = "50"
                        tourneyPrizePool = "90"
                        tourneyKillBounty = "0"
                        tourneyMaxSlots = "2"
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🎯 CS 1v1 HEADSHOT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFCA5A5))
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        tourneyGame = "BGMI"
                        tourneyCategory = "BATTLE_ROYALE"
                        tourneyFormat = "SOLO"
                        tourneyMode = "PER_KILL"
                        tourneyTitle = "BGMI Solo • ₹50 Per Kill Domination"
                        tourneyEntryFee = "50"
                        tourneyPrizePool = "4500"
                        tourneyKillBounty = "50"
                        tourneyMaxSlots = "100"
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78350F)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("💀 BR PER-KILL ₹50", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFDE68A))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        tourneyGame = "Free Fire"
                        tourneyCategory = "CLASH_SQUAD"
                        tourneyFormat = "4v4"
                        tourneyMode = "BODY_DAMAGE_ON"
                        tourneyTitle = "FF CS 4v4 Squad Clash • All Weapons On"
                        tourneyEntryFee = "200"
                        tourneyPrizePool = "360"
                        tourneyKillBounty = "0"
                        tourneyMaxSlots = "8"
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C4A6E)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⚔️ CS 4v4 SQUAD", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFBAE6FD))
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        tourneyGame = "Free Fire"
                        tourneyCategory = "LONE_WOLF"
                        tourneyFormat = "2v2"
                        tourneyMode = "BODY_DAMAGE_ON"
                        tourneyTitle = "FF Lone Wolf 2v2 Duo Clash Championship"
                        tourneyEntryFee = "100"
                        tourneyPrizePool = "180"
                        tourneyKillBounty = "0"
                        tourneyMaxSlots = "4"
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C2D12)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🐺 LONE WOLF 2v2", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFDBA74))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Tournament Creator Form Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A26)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF262E42)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CREATE & SYNC NEW TOURNAMENT", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(Modifier.height(10.dp))

                    // Title
                    OutlinedTextField(
                        value = tourneyTitle,
                        onValueChange = { tourneyTitle = it },
                        label = { Text("Match Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(Modifier.height(8.dp))

                    // Game & Category Selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tourneyGame,
                            onValueChange = { tourneyGame = it },
                            label = { Text("Game (FF / BGMI)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = tourneyCategory,
                            onValueChange = { tourneyCategory = it },
                            label = { Text("Category (BR/CS/LONE)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Format & Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tourneyFormat,
                            onValueChange = { tourneyFormat = it },
                            label = { Text("Format (1v1, 2v2, SOLO, SQUAD)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = tourneyMode,
                            onValueChange = { tourneyMode = it },
                            label = { Text("Mode (HEADSHOT_ONLY/PER_KILL)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Entry Fee & Prize Pool
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tourneyEntryFee,
                            onValueChange = { tourneyEntryFee = it },
                            label = { Text("Entry Fee (₹)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = tourneyPrizePool,
                            onValueChange = { tourneyPrizePool = it },
                            label = { Text("Prize Pool (₹)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Kill Bounty & Max Slots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tourneyKillBounty,
                            onValueChange = { tourneyKillBounty = it },
                            label = { Text("Kill Bounty (₹)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = tourneyMaxSlots,
                            onValueChange = { tourneyMaxSlots = it },
                            label = { Text("Max Slots") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            isPublishingTourney = true
                            val fee = tourneyEntryFee.toDoubleOrNull() ?: 50.0
                            val prize = tourneyPrizePool.toDoubleOrNull() ?: 90.0
                            val bounty = tourneyKillBounty.toDoubleOrNull() ?: 0.0
                            val slots = tourneyMaxSlots.toIntOrNull() ?: 2

                            val newTourney = Tournament(
                                id = java.util.UUID.randomUUID().toString(),
                                title = tourneyTitle.trim().ifEmpty { "Tactical Match" },
                                game = tourneyGame.trim().ifEmpty { "Free Fire" },
                                prizePool = prize,
                                entryFee = fee,
                                maxSlots = slots,
                                filledSlots = 0,
                                joined = false,
                                dateTimeStr = "Today at 9:00 PM",
                                mapType = tourneyMap,
                                perspective = tourneyPerspective,
                                format = tourneyFormat,
                                status = "UPCOMING",
                                rank1Prize = prize,
                                killBounty = bounty,
                                matchCategory = tourneyCategory,
                                matchMode = tourneyMode,
                                customRuleBadge = when {
                                    tourneyMode.contains("HEADSHOT", true) -> "HEADSHOT ONLY (NO BODY)"
                                    tourneyMode.contains("PER_KILL", true) && bounty > 0 -> "₹${bounty.toInt()} PER KILL BOUNTY"
                                    tourneyCategory.contains("CS", true) -> "CS $tourneyFormat CLASH"
                                    tourneyCategory.contains("LONE", true) -> "LONE WOLF $tourneyFormat"
                                    else -> "$tourneyFormat BATTLE ROYALE"
                                }
                            )

                            viewModel.saveTournament(newTourney) {
                                isPublishingTourney = false
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        enabled = !isPublishingTourney
                    ) {
                        if (isPublishingTourney) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CloudUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("PUBLISH LIVE TO CLOUD (SYNC)", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Active Tournaments list with Instant Delete
            Text("ACTIVE TOURNAMENTS (${tournaments.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tournaments.forEach { match ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A26)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF262E42)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0x3310B981)
                                    ) {
                                        Text(
                                            match.displayCategoryBadge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF34D399),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        match.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${match.game} • ${match.format} • Prize: ₹${match.prizePool.toInt()} • Fee: ₹${match.entryFee.toInt()} • Slots: ${match.filledSlots}/${match.maxSlots}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    viewModel.deleteTournament(match.id)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Tournament", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIndicatorPill(label: String, isActive: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) Color(0x2210B981) else Color(0x2264748B),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF10B981) else Color(0xFF334155)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color(0xFF10B981) else Color(0xFF64748B))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isActive) "$label: ON" else "$label: OFF",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color(0xFFA7F3D0) else Color(0xFF94A3B8)
            )
        }
    }
}
