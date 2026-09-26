package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
val DeepSpaceBlack = Color(0xFF09090B)
val CardSurfaceLight = Color(0xFF18181B)
@Composable
fun TournamentCard(
    thumbnailUrl: String,
    title: String,
    entryFee: Double,
    prizePool: Double,
    filledSlots: Int,
    maxSlots: Int,
    mapType: String,
    perspective: String,
    modifier: Modifier = Modifier,
    categoryBadge: String = "",
    format: String = "SOLO",
    matchCategory: String = "BATTLE_ROYALE",
    matchMode: String = "PER_KILL",
    killBounty: Double = 0.0,
    isJoined: Boolean = false,
    joinCooldownSeconds: Int = 0,
    liveUpdate: com.example.data.model.LiveMatchUpdate? = null,
    isGlassCard: Boolean = false,
    onClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val isFull = filledSlots >= maxSlots
    val rawProgress = if (maxSlots > 0) filledSlots.toFloat() / maxSlots.toFloat() else 0f
    val progress = rawProgress.coerceIn(0f, 1f)

    // Compute tactical badge info
    val tacticalBadgeText = when {
        categoryBadge.isNotBlank() -> categoryBadge
        matchCategory.equals("CLASH_SQUAD", true) || matchCategory.equals("CS", true) -> when (matchMode.uppercase()) {
            "HEADSHOT_ONLY", "ONLY_HEAD" -> "HEADSHOT ONLY (NO BODY)"
            "BODY_DAMAGE_ON", "ALL_WEAPONS" -> "ALL WEAPONS & BODY DMG"
            "SNIPER_ONLY" -> "SNIPER ONLY DUEL"
            "LIMITED_AMMO" -> "LIMITED AMMO TACTICAL"
            "UNLIMITED_AMMO" -> "UNLIMITED AMMO RUSH"
            "PISTOL_ONLY" -> "DESERT EAGLE ONLY"
            else -> "CLASH SQUAD $format"
        }
        matchCategory.equals("LONE_WOLF", true) -> if (format.contains("2", true)) "LONE WOLF 2v2" else "LONE WOLF 1v1 DUEL"
        matchMode.equals("PER_KILL", true) || matchMode.equals("PER_KILL_DOMINATION", true) -> {
            if (killBounty > 0) "₹${killBounty.toInt()}/KILL BOUNTY" else "PER-KILL DOMINATION"
        }
        matchMode.equals("SURVIVAL", true) || matchMode.equals("SURVIVAL_WWCD", true) -> "SURVIVAL / WWCD"
        else -> if (killBounty > 0) "₹${killBounty.toInt()}/KILL" else "$format BATTLE ROYALE"
    }

    // Determine tactical badge color styling
    val isHeadshotOnly = tacticalBadgeText.contains("HEADSHOT", true) || matchMode.contains("HEAD", true)
    val isSniperOnly = tacticalBadgeText.contains("SNIPER", true)
    val isClashSquad = matchCategory.contains("CLASH", true) || matchCategory.contains("CS", true) || format.contains("v", true)
    val isLoneWolf = matchCategory.contains("LONE", true)
    val isSurvival = tacticalBadgeText.contains("SURVIVAL", true) || tacticalBadgeText.contains("WWCD", true)

    val badgeBgColor = when {
        isHeadshotOnly -> Color(0xEE7F1D1D) // Dark Crimson
        isSniperOnly -> Color(0xEE581C87) // Dark Purple
        isLoneWolf -> Color(0xEE7C2D12) // Dark Orange
        isClashSquad -> Color(0xEE0C4A6E) // Dark Cyan/Navy
        isSurvival -> Color(0xEE064E3B) // Dark Emerald
        else -> Color(0xEE1E293B) // Dark Slate
    }

    val badgeBorderColor = when {
        isHeadshotOnly -> Color(0xFFEF4444)
        isSniperOnly -> Color(0xFFA855F7)
        isLoneWolf -> Color(0xFFF97316)
        isClashSquad -> Color(0xFF38BDF8)
        isSurvival -> Color(0xFF10B981)
        else -> Color(0xFFF59E0B)
    }

    val badgeTextColor = when {
        isHeadshotOnly -> Color(0xFFFCA5A5)
        isSniperOnly -> Color(0xFFE9D5FF)
        isLoneWolf -> Color(0xFFFDBA74)
        isClashSquad -> Color(0xFFBAE6FD)
        isSurvival -> Color(0xFF6EE7B7)
        else -> Color(0xFFFDE68A)
    }

    val cardContainerColor = if (isGlassCard) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val cardBorder = if (isGlassCard) {
        BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.06f)
                )
            )
        )
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("custom_tournament_card"),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        shape = RoundedCornerShape(24.dp),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail Image Container (tappable to view tournament details)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.foundation.LocalIndication.current
                    ) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
            ) {
                // Main Game Background Image
                coil.compose.AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Game Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark Gradient overlay for text readability and cinematic look
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, CardSurfaceLight),
                            startY = 0f
                        )
                    )
                )

                // Top Badges Row: Left = Heavy Tactical Mode Badge, Right = Match Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Tactical Category Badge (Unmissable for players)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeBgColor,
                        border = BorderStroke(1.5.dp, badgeBorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(badgeBorderColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tacticalBadgeText.uppercase(),
                                color = badgeTextColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Right: Status BADGE
                    if (liveUpdate != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color.Red.copy(alpha = 0.9f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isJoined) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color(0xFF10B981))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "JOINED",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isFull) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(Color(0xFF64748B))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "FULL",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            // Card Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_landscape),
                            contentDescription = "Map",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$mapType • $perspective • $format",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeBgColor.copy(alpha = 0.5f),
                        border = BorderStroke(0.8.dp, badgeBorderColor.copy(alpha = 0.8f))
                    ) {
                        Text(
                            text = tacticalBadgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Metadata Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PRIZE POOL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        com.example.ui.components.AnimatedRollingCounter(
                            targetValue = prizePool.toInt(),
                            prefix = "VT ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ENTRY FEE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (entryFee == 0.0) {
                            Text(
                                text = "FREE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            com.example.ui.components.AnimatedRollingCounter(
                                targetValue = entryFee.toInt(),
                                prefix = "VT ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "SLOTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            com.example.ui.components.AnimatedRollingCounter(
                                targetValue = filledSlots,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "/$maxSlots",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Slots Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = Color(0xFF2A2A35)
                )
                if (liveUpdate != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF291010).copy(alpha = 0.5f))
                            .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = liveUpdate.matchPhase.uppercase(java.util.Locale.getDefault()),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                                Text(
                                    text = "ALIVE: ${liveUpdate.alivePlayers}/${liveUpdate.totalPlayers}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Top Player: ${liveUpdate.topPlayer}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${liveUpdate.topKills} Kills",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                val remindMeClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                        data = android.provider.CalendarContract.Events.CONTENT_URI
                        putExtra(android.provider.CalendarContract.Events.TITLE, "VeloRix Tournament: $title")
                    }
                    context.startActivity(intent)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        // Join Button
                        if (!isJoined) {
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            
                            val buttonScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.98f else 1f,
                                animationSpec = tween(durationMillis = 150)
                            )
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onJoinClick()
                                },
                                enabled = !isFull && joinCooldownSeconds == 0,
                                interactionSource = interactionSource,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .graphicsLayer {
                                        scaleX = buttonScale
                                        scaleY = buttonScale
                                    }
                                    
                                    .testTag("join_now_action_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = Color(0xFF3A3A45),
                                    disabledContentColor = Color(0xFF888888)
                                )
                            ) {
                                Text(
                                    text = when {
                                        joinCooldownSeconds > 0 -> "Wait ${joinCooldownSeconds}s"
                                        isFull -> "Slots Full"
                                        else -> "Join Now"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = onClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("view_details_button"),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.secondary,
                                    containerColor = Color.Transparent
                                )
                            ) {
                                Text(
                                    text = "REGISTERED",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                    // Remind Me Button
                    OutlinedButton(
                        onClick = remindMeClick,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("remind_me_button"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = "REMIND",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun TournamentCardSkeleton(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val shimmerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            shimmerColor,
            baseColor
        ),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("tournament_card_skeleton"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column {
            // Thumbnail Header Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(shimmerBrush)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Game Badge / Map Skeleton Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title Skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Fee & Prize Pool Row Skeleton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Slots Progress Bar Skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Button Skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmerBrush)
                )
            }
        }
    }
}