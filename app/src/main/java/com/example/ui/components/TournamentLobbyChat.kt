package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEffectManager
import com.example.data.model.Tournament
import com.example.data.model.TournamentChatMessage
import com.example.data.model.TournamentParticipant
import com.example.data.model.User
import com.example.data.repository.TournamentChatRepository
import com.example.ui.theme.GffDevanagariFontFamily
import com.example.ui.viewmodel.PlatformViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real-time Tournament Lobby Chat Interface
 * Powered by Firebase Firestore.
 * Allows squad members, teammates, and tournament participants to coordinate in real time.
 */
@Composable
fun TournamentLobbyChat(
    viewModel: PlatformViewModel,
    tournament: Tournament,
    currentUser: User?,
    myParticipant: TournamentParticipant?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val soundManager = remember { SoundEffectManager.getInstance(context) }

    val messagesFlow = remember(tournament.id) {
        viewModel.getTournamentChatMessages(tournament.id)
    }
    val messages by messagesFlow.collectAsState(initial = emptyList())

    var inputMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll to the latest message smoothly whenever new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val timeFormatter = remember {
        SimpleDateFormat("h:mm a", Locale.getDefault())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0C0F17))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        // --- 1. LOBBY CHAT HEADER & TACTICAL STATUS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = "MATCH LOBBY CHAT",
                    fontFamily = GffDevanagariFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.8.sp,
                    color = Color.White
                )
            }

            // Squad / Mode Badge
            Surface(
                color = Color(0x330070F3),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF0070F3).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${tournament.game} • ${tournament.format}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }

        // --- 2. PARTICIPANT STATUS PILL ---
        if (myParticipant != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                color = Color(0x1F10B981),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Registered",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "You are registered in Slot #${myParticipant.slotNumber} • Team ${myParticipant.teamName.ifBlank { "Squad" }}",
                        fontSize = 11.5.sp,
                        color = Color(0xFFD1FAE5),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- 3. QUICK TACTICAL CALLOUT PRESETS ---
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TournamentChatRepository.TACTICAL_PRESETS.forEach { preset ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            soundManager.playBadSnap()
                            viewModel.sendTournamentChatMessage(
                                tournamentId = tournament.id,
                                text = preset,
                                messageType = "TACTICAL"
                            )
                        },
                    color = Color(0x221E293B),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Text(
                        text = preset,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // --- 4. REAL-TIME MESSAGES FEED ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 320.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF07090E))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                .padding(8.dp)
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF0070F3),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Connecting to match lobby...",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id.ifBlank { "${it.senderId}_${it.timestamp}" } }) { msg ->
                        val isMe = (currentUser != null && msg.senderId == currentUser.id) ||
                                (currentUser == null && msg.senderName.equals("You", ignoreCase = true))

                        ChatMessageBubble(
                            message = msg,
                            isMe = isMe,
                            timeFormatter = timeFormatter
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 5. INTERACTIVE MESSAGE INPUT ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputMessage,
                onValueChange = { if (it.length <= 250) inputMessage = it },
                placeholder = {
                    Text(
                        "Coordinate with squad...",
                        fontSize = 12.5.sp,
                        color = Color(0xFF64748B)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tournament_chat_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF131722),
                    unfocusedContainerColor = Color(0xFF131722),
                    focusedBorderColor = Color(0xFF0070F3),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 2,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputMessage.isNotBlank() && !isSending) {
                            val textToSend = inputMessage
                            inputMessage = ""
                            isSending = true
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            soundManager.playBadSnap()

                            viewModel.sendTournamentChatMessage(
                                tournamentId = tournament.id,
                                text = textToSend
                            ) { success, _ ->
                                isSending = false
                            }
                        }
                    }
                )
            )

            // Send Button with Glowing Pill Aesthetic
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputMessage.isNotBlank()) {
                            Brush.linearGradient(
                                listOf(Color(0xFF0070F3), Color(0xFF38BDF8))
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        }
                    )
                    .clickable(enabled = inputMessage.isNotBlank() && !isSending) {
                        val textToSend = inputMessage
                        inputMessage = ""
                        isSending = true
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        soundManager.playBadSnap()

                        viewModel.sendTournamentChatMessage(
                            tournamentId = tournament.id,
                            text = textToSend
                        ) { _, _ ->
                            isSending = false
                        }
                    }
                    .testTag("tournament_chat_send_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send Message",
                        tint = if (inputMessage.isNotBlank()) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: TournamentChatMessage,
    isMe: Boolean,
    timeFormatter: SimpleDateFormat
) {
    val formattedTime = remember(message.timestamp) {
        try {
            timeFormatter.format(Date(message.timestamp))
        } catch (_: Exception) {
            ""
        }
    }

    if (message.isSystemMessage || message.messageType == "ANNOUNCEMENT") {
        // System / Lobby Announcement Message
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            color = Color(0x2A0070F3),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF0070F3).copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = message.text,
                    fontSize = 11.5.sp,
                    color = Color(0xFFBAE6FD),
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            // Sender Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.senderName.take(1).uppercase(),
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            // Header Info: Sender Name + Team / Slot Tag + Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 2.dp, start = 2.dp, end = 2.dp)
            ) {
                Text(
                    text = if (isMe) "You" else message.senderName,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMe) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                )

                if (message.senderTeam.isNotBlank() && !isMe) {
                    Text(
                        text = "• ${message.senderTeam}",
                        fontSize = 9.5.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = formattedTime,
                    fontSize = 9.sp,
                    color = Color(0xFF475569)
                )
            }

            // Message Bubble Box
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                            bottomStart = if (isMe) 14.dp else 2.dp,
                            bottomEnd = if (isMe) 2.dp else 14.dp
                        )
                    )
                    .background(
                        if (isMe) {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0070F3).copy(alpha = 0.85f),
                                    Color(0xFF1E40AF).copy(alpha = 0.85f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1E2230),
                                    Color(0xFF141721)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (isMe) 0.35f else 0.12f),
                                Color.White.copy(alpha = 0.03f)
                            )
                        ),
                        shape = RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                            bottomStart = if (isMe) 14.dp else 2.dp,
                            bottomEnd = if (isMe) 2.dp else 14.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 12.5.sp,
                    color = Color.White,
                    lineHeight = 17.sp,
                    fontWeight = if (message.messageType == "TACTICAL") FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
