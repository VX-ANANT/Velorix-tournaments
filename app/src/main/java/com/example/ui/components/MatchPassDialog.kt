package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.*
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.GffDevanagariFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Tournament
import com.example.data.model.TournamentParticipant
import com.example.util.UpiPaymentManager

@Composable
fun MatchPassDialog(
    tournament: Tournament,
    participant: TournamentParticipant?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val ticketCode = participant?.ticketCode?.ifBlank { "TKT-${tournament.game.take(2)}-${(1000..9999).random()}" } 
        ?: "TKT-VRX-${(1000..9999).random()}"
    
    val slotNumber = participant?.slotNumber ?: 1
    val ign = participant?.inGameName?.ifBlank { "Warrior" } ?: "Warrior"
    val characterId = participant?.characterId?.ifBlank { "Not Set" } ?: "Not Set"
    val teamName = participant?.teamName?.ifBlank { "Solo Contender" } ?: "Solo Contender"

    // Theme-aware detection: dark theme vs light theme
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Generate dynamic QR Bitmap for match pass verification matching active theme
    val qrBitmap = remember(ticketCode, isDarkTheme) {
        UpiPaymentManager.generateQrBitmap(
            content = "VELORIX-MATCH-PASS:$ticketCode:$slotNumber:${tournament.id}",
            sizePx = 380,
            context = context,
            isDarkTheme = isDarkTheme
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        ApplyDialogWindowBlur()
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            isVisible = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x2E000000))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.88f,
                            animationSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = 380f
                            )
                        ),
                exit = fadeOut(animationSpec = tween(150)) +
                        scaleOut(targetScale = 0.92f, animationSpec = tween(150))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .testTag("match_pass_dialog"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF20B0E14)),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0x40FFFFFF),
                                Color(0x14FFFFFF),
                                Color(0x06FFFFFF)
                            )
                        )
                    )
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Xiaomi Top Handle Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 12.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0x28FFFFFF))
                    )

                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0x18FFFFFF),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, Color(0x25FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_barcode),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("OFFICIAL ENTRY PASS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_x_close),
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Esports Ticket Header
                    Text(
                        text = tournament.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${tournament.game} • ${tournament.mapType} • ${tournament.perspective}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR Code Ticket Box
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        qrBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Match Pass QR",
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: run {
                            Icon(Icons.Outlined.QrCode, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = ticketCode,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GffDevanagariFontFamily,
                        color = Color(0xFF34D399),
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ticket Info Grid
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181F2F)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF27354A))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("ASSIGNED SLOT", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                    Text("Slot #$slotNumber", fontSize = 16.sp, color = Color(0xFF60A5FA), fontWeight = FontWeight.Black)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("MATCH TIME", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                    Text(tournament.dateTimeStr, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFF27354A))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("IN-GAME NAME (IGN)", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                    Text(ign, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("CHARACTER UID", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                    Text(characterId, fontSize = 13.sp, color = Color(0xFFCBD5E1), fontFamily = GffDevanagariFontFamily)
                                }
                            }

                            if (teamName.isNotBlank() && teamName != "Solo Contender") {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFF27354A))
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("TEAM / SQUAD", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                    Text(teamName, fontSize = 13.sp, color = Color(0xFFA855F7), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Room ID & Password Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (tournament.roomId.isNotBlank()) Color(0xFF0D2818) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (tournament.roomId.isNotBlank()) Color(0xFF10B981) else Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = if (tournament.roomId.isNotBlank()) com.example.R.drawable.ic_pin_ok else com.example.R.drawable.ic_alert),
                                        contentDescription = null,
                                        tint = if (tournament.roomId.isNotBlank()) Color(0xFF34D399) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (tournament.roomId.isNotBlank()) "ROOM CREDENTIALS LIVE" else "ROOM CREDENTIALS PENDING",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tournament.roomId.isNotBlank()) Color(0xFF34D399) else Color(0xFF94A3B8)
                                    )
                                }

                                if (tournament.roomId.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Room Details", "Room ID: ${tournament.roomId}\nPassword: ${tournament.roomPassword}")
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Room ID & Password copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_copy),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color(0xFF34D399)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy", fontSize = 11.sp, color = Color(0xFF34D399))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (tournament.roomId.isNotBlank() && tournament.roomPassword.isNotBlank()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Room ID", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        Text(tournament.roomId, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Password", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        Text(tournament.roomPassword, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                            } else {
                                Text(
                                    text = "Credentials will be automatically populated here 15 minutes before the match starts.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Share Match Pass Action
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "VeloRix Match Pass Confirmed!\n\n" +
                                    "Tournament: ${tournament.title}\n" +
                                    "Game: ${tournament.game}\n" +
                                    "Time: ${tournament.dateTimeStr}\n" +
                                    "Slot: #$slotNumber\n" +
                                    "Player: $ign (UID: $characterId)\n" +
                                    "Ticket: $ticketCode\n\n" +
                                    "Join custom tournaments on VeloRix Esports Platform!"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Match Pass"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("share_match_pass_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_untitledui_share_2),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Match Pass", fontWeight = FontWeight.Bold)
                    }
                }
            }
            }
        }
    }
}
