package com.example.ui.screens

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Tournament
import com.example.data.model.TournamentParticipant
import com.example.ui.components.VeloRixButton
import com.example.ui.components.stretchOverscroll
import com.example.ui.components.SlotPickerModal
import com.example.ui.components.MatchPassDialog
import com.example.ui.viewmodel.PlatformViewModel
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.compose.ui.graphics.drawscope.Stroke
import android.widget.Toast
import com.example.ui.components.TournamentLobby
import com.example.ui.components.TournamentPrizePieChart
import com.example.service.NotificationHelper
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries

fun simulateLocalNotification(context: Context, matchTitle: String) {
    val channelId = "tournament_reminders"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Tournament Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts 15 mins before tournament starts"
        }
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Tournament Starts in 15 Min!")
        .setContentText("Room ID and Password are now available for $matchTitle")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    Toast.makeText(context, "Notification Sent! (Check notification tray)", Toast.LENGTH_SHORT).show()
}

@Composable
fun TournamentDetailsScreen(
    viewModel: PlatformViewModel,
    tournamentId: String,
    onNavigateBack: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(key1 = tournamentId) {
        viewModel.selectTournament(tournamentId)
    }

    val actionCooldowns by viewModel.actionCooldownSeconds.collectAsState()
    val joinCooldownKey = "tournament_join_$tournamentId"
    val slotCooldownKey = "slot_join_$tournamentId"
    val activeCooldown = maxOf(actionCooldowns[joinCooldownKey] ?: 0, actionCooldowns[slotCooldownKey] ?: 0)

    val match by viewModel.selectedTournament.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val participants by viewModel.getParticipants(tournamentId).collectAsState(initial = emptyList())
    val currentUser by viewModel.userState.collectAsState()
    val myParticipant by if (currentUser != null) {
        viewModel.getMyParticipant(tournamentId, currentUser!!.id).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<TournamentParticipant?>(null) }
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showSuccessAnim by remember { mutableStateOf(false) }
    var showSlotPicker by remember { mutableStateOf(false) }
    var showMatchPass by remember { mutableStateOf(false) }
    val tabs = listOf("DETAILS", "PRIZE POOL", "RULES")

    val isAnyPopupOpen = showSlotPicker || showMatchPass
    val bgBlurRadius by animateDpAsState(
        targetValue = if (isAnyPopupOpen) 22.dp else 0.dp,
        animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing),
        label = "tournament_bg_blur"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        match?.let { t ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = bgBlurRadius)
                    .stretchOverscroll()
                    .verticalScroll(scrollState)
                    .padding(bottom = 90.dp) // space for sticky button
            ) {
                // Parallax-style Gradient Header Cover
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    val gameThumbnailUrl = if (t.game.contains("BGMI", true)) {
                        "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=800&q=80"
                    } else {
                        "https://images.unsplash.com/photo-1552820728-8b83bb6b773f?auto=format&fit=crop&w=800&q=80"
                    }

                    coil.compose.AsyncImage(
                        model = gameThumbnailUrl,
                        contentDescription = "Tournament Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay bottom shader
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.background
                                    ),
                                    startY = 150f
                                )
                            )
                    )

                    // Top Bar back button icon overlay
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(top = 40.dp, start = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Game badge overlays
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (t.game == "BGMI") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = t.game,
                            color = if (t.game == "BGMI") MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Title Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = t.title,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Prominent Tactical Category and Match Combat Rules Alert Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            t.displayCategoryBadge.contains("HEADSHOT", true) -> Color(0xFF2A0A0A)
                            t.displayCategoryBadge.contains("SNIPER", true) -> Color(0xFF1E0A2A)
                            t.displayCategoryBadge.contains("SURVIVAL", true) -> Color(0xFF06281E)
                            t.displayCategoryBadge.contains("CS", true) || t.displayCategoryBadge.contains("CLASH", true) -> Color(0xFF0A1E2A)
                            else -> Color(0xFF2A200A)
                        },
                        border = BorderStroke(
                            1.5.dp,
                            when {
                                t.displayCategoryBadge.contains("HEADSHOT", true) -> Color(0xFFEF4444)
                                t.displayCategoryBadge.contains("SNIPER", true) -> Color(0xFFA855F7)
                                t.displayCategoryBadge.contains("SURVIVAL", true) -> Color(0xFF10B981)
                                t.displayCategoryBadge.contains("CS", true) || t.displayCategoryBadge.contains("CLASH", true) -> Color(0xFF38BDF8)
                                else -> Color(0xFFF59E0B)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    t.displayCategoryBadge.contains("HEADSHOT", true) || t.displayCategoryBadge.contains("SNIPER", true) -> 
                                        androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_target)
                                    t.displayCategoryBadge.contains("SURVIVAL", true) -> 
                                        androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_trophy)
                                    t.displayCategoryBadge.contains("CS", true) || t.displayCategoryBadge.contains("CLASH", true) -> 
                                        androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_shield)
                                    else -> 
                                        androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_target)
                                },
                                contentDescription = null,
                                tint = when {
                                    t.displayCategoryBadge.contains("HEADSHOT", true) -> Color(0xFFEF4444)
                                    t.displayCategoryBadge.contains("SNIPER", true) -> Color(0xFFA855F7)
                                    t.displayCategoryBadge.contains("SURVIVAL", true) -> Color(0xFF10B981)
                                    t.displayCategoryBadge.contains("CS", true) || t.displayCategoryBadge.contains("CLASH", true) -> Color(0xFF38BDF8)
                                    else -> Color(0xFFF59E0B)
                                },
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "OFFICIAL MATCH FORMAT & RULES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${t.displayCategoryBadge.uppercase()} • ${t.format} • ${t.mapType} (${t.perspective})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_matches),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "TOTAL TOURNAMENT PRIZE",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "VT ${t.prizePool.toInt()}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Tabbed Layout selectors
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tab Content Rendering
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = selectedTabIndex,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (androidx.compose.animation.slideInHorizontally(
                                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 450f),
                                    initialOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() }
                                ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)))
                                    .togetherWith(
                                        androidx.compose.animation.slideOutHorizontally(
                                            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 450f),
                                            targetOffsetX = { fullWidth -> (-fullWidth * 0.25f).toInt() }
                                        ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
                                    )
                            } else {
                                (androidx.compose.animation.slideInHorizontally(
                                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 450f),
                                    initialOffsetX = { fullWidth -> (-fullWidth * 0.25f).toInt() }
                                ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)))
                                    .togetherWith(
                                        androidx.compose.animation.slideOutHorizontally(
                                            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 450f),
                                            targetOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() }
                                        ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
                                    )
                            }
                        },
                        label = "TournamentTabContentAnimation"
                    ) { targetTab ->
                        when (targetTab) {
                            0 -> DetailsTabContent(t)
                            1 -> PrizePoolTabContent(t)
                            2 -> RulesTabContent()
                        }
                    }
                }
            }

            // Sticky Bottom Join/Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.95f), MaterialTheme.colorScheme.background)
                        )
                    )
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp, top = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ENTRY CHARGE",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (t.entryFee == 0.0) "FREE" else "VT ${t.entryFee.toInt()}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    var showRulesModal by remember { mutableStateOf(false) }

                    if (!t.joined) {
                        VeloRixButton(
                            text = when {
                                activeCooldown > 0 -> "Wait ${activeCooldown}s"
                                t.isFull -> "Slots Full"
                                else -> "Select Slot & Join"
                            },
                            onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showSlotPicker = true
                            },
                            enabled = !t.isFull && activeCooldown == 0,
                            modifier = Modifier
                                .width(210.dp)
                                .height(50.dp),
                            accentColor = if (t.isFull || activeCooldown > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            glowColor = MaterialTheme.colorScheme.primary,
                            testTag = "sticky_join_button"
                        )
                    } else {
                        Button(
                            onClick = { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                showMatchPass = true
                            },
                            modifier = Modifier
                                .width(210.dp)
                                .height(50.dp)
                                .testTag("view_match_pass_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E3A8A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Icon(androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_barcode), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("VIEW MATCH PASS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    
                    if (showSlotPicker) {
                        SlotPickerModal(
                            tournament = t,
                            currentUser = currentUser,
                            occupiedParticipants = participants,
                            cooldownSeconds = activeCooldown,
                            onDismiss = { showSlotPicker = false },
                            onConfirmSlot = { slotNum, ign, charId, team, onFinished ->
                                viewModel.registerForTournamentWithSlot(
                                    tournamentId = t.id,
                                    slotNumber = slotNum,
                                    inGameName = ign,
                                    characterId = charId,
                                    teamName = team
                                ) { success, _ ->
                                    onFinished(success)
                                    if (success) {
                                        showSlotPicker = false
                                        showSuccessAnim = true
                                    }
                                }
                            }
                        )
                    }

                    if (showMatchPass) {
                        MatchPassDialog(
                            tournament = t,
                            participant = myParticipant,
                            onDismiss = { showMatchPass = false }
                        )
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                com.example.ui.components.AnimatedLoaders(
                    pathColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    dotColor = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        AnimatedVisibility(
            visible = showSuccessAnim,
            enter = fadeIn(animationSpec = tween(300)) + androidx.compose.animation.scaleIn(initialScale = 0.8f, animationSpec = tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
            exit = androidx.compose.animation.fadeOut(animationSpec = tween(300)) + androidx.compose.animation.scaleOut(targetScale = 1.2f, animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "REGISTRATION SUCCESSFUL",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedButton(
                        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); showSuccessAnim = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("CLOSE")
                    }
                }
            }
        }
    }
}

@Composable
fun RoomDetailsCard(match: Tournament) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MATCH & ROOM ACCESS",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Surface(
                    color = Color(0xFF1B4E3E),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "REGISTERED",
                        color = Color(0xFF4ECCA3),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            if (match.roomId.isNotBlank() && match.roomPassword.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Room ID", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(match.roomId, fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(match.roomId))
                                    Toast.makeText(context, "Room ID Copied: ${match.roomId}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Room ID",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Password", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(match.roomPassword, fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(match.roomPassword))
                                    Toast.makeText(context, "Password Copied: ${match.roomPassword}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Password",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Room ID and Password will be broadcasted 15 mins before match starts via Push Notification.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        NotificationHelper.showTournamentStartingNotification(
                            context = context,
                            tournamentTitle = match.title,
                            roomId = if (match.roomId.isNotBlank()) match.roomId else "784920",
                            roomPass = if (match.roomPassword.isNotBlank()) match.roomPassword else "VELO99"
                        )
                        Toast.makeText(context, "Push Alert Sent: Room Details Broadcast", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_mail_send), contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Room Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        NotificationHelper.showMatchResultNotification(
                            context = context,
                            tournamentTitle = match.title,
                            position = 1,
                            kills = 8,
                            winnings = match.prizePool * 0.5
                        )
                        Toast.makeText(context, "Push Alert Sent: Match Results Published", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Result Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetailsTabContent(match: Tournament) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current



    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (match.joined) {
            RoomDetailsCard(match)
        }
        TournamentLobby(tournament = match)
        
        // Interactive Donut/Pie Chart for Tournament Details
        TournamentPrizePieChart(tournament = match)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailItemCard(
                modifier = Modifier.weight(1f),
                icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_timer),
                title = "DATE & TIME",
                value = match.dateTimeStr
            )
            DetailItemCard(
                modifier = Modifier.weight(1f),
                icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_landscape),
                title = "MAP",
                value = match.mapType
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailItemCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Visibility,
                title = "PERSPECTIVE",
                value = match.perspective
            )
            DetailItemCard(
                modifier = Modifier.weight(1f),
                icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_profile),
                title = "TEAM TYPE",
                value = "SOLO ARENA"
            )
        }

        // Additional Match Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TOURNAMENT INFO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Welcome to the ultimate esports arena! Join top-tier players in ${match.game} and prove your skills. Room ID and custom passwords will be displayed in the matches section and sent dynamically via notifications 15 to 20 minutes before the official countdown starts.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.8f),
                    lineHeight = 18.sp
                )
                
                if (match.joined) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); simulateLocalNotification(context, match.title) },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_inbox_notification), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "SIMULATE 15-MIN ALERT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItemCard(
    modifier: Modifier = Modifier,
    modifierIcon: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current



    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = modifierIcon.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PrizePoolTabContent(match: Tournament) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val level1 = match.prizePool * 0.5
    val level2 = match.prizePool * 0.25
    val level3 = match.prizePool * 0.15
    val level4_10 = (match.prizePool * 0.10 / 7).coerceAtLeast(5.0)

    val rankingPrizes = listOf(
        RankPrize("Rank 1 (Winner)", level1),
        RankPrize("Rank 2 (Runner-Up)", level2),
        RankPrize("Rank 3 (3rd Place)", level3),
        RankPrize("Rank 4 - 10 (Top 10)", level4_10),
        RankPrize("Per Kill Bounty", 10.0)
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Dynamic Interactive Donut / Pie Chart for Prize Distribution
        TournamentPrizePieChart(tournament = match)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RANKING REWARDS BREAKDOWN",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "TOTAL: VT ${match.prizePool.toInt()}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                rankingPrizes.forEachIndexed { idx, p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val iconColor = when (idx) {
                                0 -> Color(0xFFFFD700)
                                1 -> Color(0xFFC0C0C0)
                                2 -> Color(0xFFCD7F32)
                                else -> MaterialTheme.colorScheme.secondary
                            }
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = p.rank,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "VT ${p.amount.toInt()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (idx < rankingPrizes.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun RulesTabContent() {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current



    val rules = listOf(
        "No Emulator play is allowed. Standard mobile touch interface only.",
        "Teaming up with rival players will lead to instant disqualification and wallet ban.",
        "Hackers, scripts, or exploit users will receive permanent account suspensions.",
        "The Room ID and Password will be broadcasted exactly 15 minutes prior to start time.",
        "Ensure your game app is pre-updated and you enter the lobby within the 10-minute grace window.",
        "All bounty/kill winnings will be synthesized and added instantly post Match Referee verification."
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "STRICT REGULATIONS",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.Red
                )
            )
            Spacer(modifier = Modifier.height(14.dp))

            rules.forEach { rule ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = rule,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.8f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

data class RankPrize(val rank: String, val amount: Double)

