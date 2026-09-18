package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TournamentSyncStatusBar(
    isConnected: Boolean,
    isSyncing: Boolean,
    lastSyncedTimestamp: Long,
    cachedTournamentCount: Int,
    onForceSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showDetailsDialog by remember { mutableStateOf(false) }

    // Pulsing animation for active live status
    val infiniteTransition = rememberInfiniteTransition(label = "sync_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    // Calculate relative time string
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10000)
            currentTime = System.currentTimeMillis()
        }
    }

    val syncTimeText = remember(lastSyncedTimestamp, currentTime, isConnected, isSyncing) {
        if (!isConnected) {
            "Offline (Cached Mode)"
        } else if (isSyncing) {
            "Syncing with Cloud..."
        } else {
            val diffSec = ((currentTime - lastSyncedTimestamp) / 1000).coerceAtLeast(0)
            when {
                diffSec < 15 -> "Live Synced"
                diffSec < 60 -> "Synced ${diffSec}s ago"
                diffSec < 3600 -> "Synced ${diffSec / 60}m ago"
                else -> "Synced"
            }
        }
    }

    val statusColor by animateColorAsState(
        targetValue = when {
            !isConnected -> Color(0xFFEF4444) // Red
            isSyncing -> Color(0xFF38BDF8) // Sky/Cyan
            else -> Color(0xFF10B981) // Emerald Green
        },
        label = "status_color"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isConnected -> Color(0xFF7F1D1D).copy(alpha = 0.25f)
            isSyncing -> Color(0xFF075985).copy(alpha = 0.25f)
            else -> Color(0xFF064E3B).copy(alpha = 0.25f)
        },
        label = "background_color"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !isConnected -> Color(0xFFEF4444).copy(alpha = 0.4f)
            isSyncing -> Color(0xFF38BDF8).copy(alpha = 0.4f)
            else -> Color(0xFF10B981).copy(alpha = 0.35f)
        },
        label = "border_color"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showDetailsDialog = true
            }
            .testTag("tournament_sync_status_bar"),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier.size(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSyncing) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_cloud_download),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier
                                .size(14.dp)
                                .scale(pulseScale)
                        )
                    } else if (!isConnected) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_alert),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(13.dp)
                        )
                    } else {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_cloud_ok),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (!isConnected) "OFFLINE MODE" else if (isSyncing) "LIVE SYNCING" else "RTDB CLOUD CONNECTED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• $syncTimeText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Right action pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isConnected) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                        border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onForceSync()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "RECONNECT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Sync Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }

    if (showDetailsDialog) {
        SyncInfoDialog(
            isConnected = isConnected,
            isSyncing = isSyncing,
            lastSyncedTimestamp = lastSyncedTimestamp,
            cachedTournamentCount = cachedTournamentCount,
            onDismiss = { showDetailsDialog = false },
            onForceSync = {
                onForceSync()
                showDetailsDialog = false
            }
        )
    }
}

@Composable
fun SyncInfoDialog(
    isConnected: Boolean,
    isSyncing: Boolean,
    lastSyncedTimestamp: Long,
    cachedTournamentCount: Int,
    onDismiss: () -> Unit,
    onForceSync: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val formattedTime = remember(lastSyncedTimestamp) {
        if (lastSyncedTimestamp > 0) {
            SimpleDateFormat("hh:mm:ss a, dd MMM", Locale.getDefault()).format(Date(lastSyncedTimestamp))
        } else "Never"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        ApplyDialogWindowBlur()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .testTag("sync_info_dialog"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1420)),
                border = BorderStroke(1.2.dp, if (isConnected) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = if (isConnected) com.example.R.drawable.ic_cloud_ok else com.example.R.drawable.ic_alert),
                            contentDescription = null,
                            tint = if (isConnected) Color(0xFF34D399) else Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isConnected) "Realtime Sync Active" else "Offline / Cached Mode",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (isConnected)
                            "Your tournament slots, wallet transactions, and room credentials sync instantly via Firebase Realtime Database."
                        else
                            "No internet connection detected. You are viewing cached tournaments and wallet balances from your local database.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    HorizontalDivider(color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats Grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SyncDetailRow(
                            label = "RTDB Cloud Channel",
                            value = if (isConnected) "Connected (WebSocket)" else "Disconnected",
                            valueColor = if (isConnected) Color(0xFF34D399) else Color(0xFFEF4444)
                        )
                        SyncDetailRow(
                            label = "Last Successful Sync",
                            value = formattedTime,
                            valueColor = MaterialTheme.colorScheme.onSurface
                        )
                        SyncDetailRow(
                            label = "Room DB Offline Cache",
                            value = "$cachedTournamentCount Tournaments Cached",
                            valueColor = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Close",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onForceSync()
                            },
                            enabled = !isSyncing,
                            modifier = Modifier
                                .weight(1.35f)
                                .heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(15.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSyncing) "Syncing..." else "Force Refresh",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncDetailRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141A28))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
