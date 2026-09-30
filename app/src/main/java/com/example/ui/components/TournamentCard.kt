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
import androidx.compose.ui.draw.drawBehind
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
import com.example.util.rememberArtworkPalette
val DeepSpaceBlack = Color(0xFF09090B)
val CardSurfaceLight = Color(0xFF18181B)
private data class TacticalBadgeStyle(
    val text: String,
    val bgColor: Color,
    val borderColor: Color,
    val textColor: Color
)

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
    liquidGlassConfig: com.example.data.model.LiquidGlassConfig = com.example.data.model.LiquidGlassConfig(),
    onClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val palette = rememberArtworkPalette(imageUrl = thumbnailUrl, title = title)
    val isFull by remember(filledSlots, maxSlots) {
        derivedStateOf { filledSlots >= maxSlots }
    }
    val progress by remember(filledSlots, maxSlots) {
        derivedStateOf {
            val raw = if (maxSlots > 0) filledSlots.toFloat() / maxSlots.toFloat() else 0f
            raw.coerceIn(0f, 1f)
        }
    }

    // Compute tactical badge styling with remember
    val badgeStyle = remember(categoryBadge, matchCategory, matchMode, killBounty, format, palette.accentColor) {
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
            else -> if (killBounty > 0) "₹${killBounty.toInt()}/KILL" else "$format SURVIVAL"
        }

        val isHeadshotOnly = tacticalBadgeText.contains("HEADSHOT", true) || matchMode.contains("HEAD", true)
        val isSniperOnly = tacticalBadgeText.contains("SNIPER", true)
        val isClashSquad = matchCategory.contains("CLASH", true) || matchCategory.contains("CS", true) || format.contains("v", true)
        val isLoneWolf = matchCategory.contains("LONE", true)
        val isSurvival = tacticalBadgeText.contains("SURVIVAL", true) || tacticalBadgeText.contains("WWCD", true)

        val bg = when {
            isHeadshotOnly -> Color(0xEE7F1D1D)
            isSniperOnly -> Color(0xEE581C87)
            isLoneWolf -> Color(0xEE7C2D12)
            isClashSquad -> Color(0xEE0C4A6E)
            isSurvival -> Color(0xEE064E3B)
            else -> palette.accentColor.copy(alpha = 0.20f)
        }
        val border = when {
            isHeadshotOnly -> Color(0xFFEF4444)
            isSniperOnly -> Color(0xFFA855F7)
            isLoneWolf -> Color(0xFFF97316)
            isClashSquad -> Color(0xFF38BDF8)
            isSurvival -> Color(0xFF10B981)
            else -> palette.accentColor.copy(alpha = 0.85f)
        }
        val text = when {
            isHeadshotOnly -> Color(0xFFFCA5A5)
            isSniperOnly -> Color(0xFFE9D5FF)
            isLoneWolf -> Color(0xFFFDBA74)
            isClashSquad -> Color(0xFFBAE6FD)
            isSurvival -> Color(0xFF6EE7B7)
            else -> Color(0xFFFFFFFF) // 180° opposite contrast text, NO YELLOW
        }
        TacticalBadgeStyle(tacticalBadgeText, bg, border, text)
    }

    val isGlassEnabled = remember(isGlassCard, liquidGlassConfig.enableLiquidGlass, liquidGlassConfig.glassCards) {
        isGlassCard && liquidGlassConfig.enableLiquidGlass && liquidGlassConfig.glassCards
    }

    val cardContainerColor = remember(isGlassEnabled, liquidGlassConfig.surfaceTint, liquidGlassConfig.surfaceOpacity, liquidGlassConfig.vibrancy, palette.cardBackground) {
        if (isGlassEnabled) {
            val baseTint = when (liquidGlassConfig.surfaceTint.lowercase()) {
                "crimson" -> Color(0xFF1E030B)
                "midnight" -> Color(0xFF091122)
                "clear" -> Color(0xFF06080E)
                "emerald" -> Color(0xFF031A0F)
                "gold" -> Color(0xFF1E1704)
                else -> Color(0xFF0E131F)
            }
            baseTint.copy(alpha = (liquidGlassConfig.surfaceOpacity * 0.9f * liquidGlassConfig.vibrancy).coerceIn(0.12f, 0.88f))
        } else {
            palette.cardBackground
        }
    }

    val cardBorder = remember(isGlassEnabled, liquidGlassConfig.lensRefractionAmount, liquidGlassConfig.chromaticAberration) {
        if (isGlassEnabled) {
            val specular = liquidGlassConfig.lensRefractionAmount.coerceIn(0.05f, 0.5f)
            val borderColors = if (liquidGlassConfig.chromaticAberration) {
                listOf(
                    Color(0xFF38BDF8).copy(alpha = (specular * 1.3f).coerceIn(0.1f, 0.6f)),
                    Color.White.copy(alpha = (specular * 0.9f).coerceIn(0.08f, 0.5f)),
                    Color(0xFFF43F5E).copy(alpha = (specular * 0.5f).coerceIn(0.05f, 0.35f)),
                    Color.White.copy(alpha = (specular * 0.2f).coerceIn(0.02f, 0.15f))
                )
            } else {
                listOf(
                    Color.White.copy(alpha = (specular * 1.2f).coerceIn(0.1f, 0.55f)),
                    Color.White.copy(alpha = (specular * 0.2f).coerceIn(0.02f, 0.15f))
                )
            }
            BorderStroke(1.dp, Brush.verticalGradient(borderColors))
        } else {
            BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        }
    }

    val cardTitleColor = remember(isGlassEnabled, liquidGlassConfig.glassTextColor, palette.highContrastText) {
        if (isGlassEnabled) {
            when (liquidGlassConfig.glassTextColor.lowercase()) {
                "platinum" -> Color(0xFFE2E8F0)
                "adaptive" -> Color(0xFFF1F5F9)
                else -> Color.White
            }
        } else {
            palette.highContrastText
        }
    }

    val cardElevation = remember(isGlassEnabled, liquidGlassConfig.depthEffect) {
        if (isGlassEnabled && liquidGlassConfig.depthEffect) 8.dp else 0.dp
    }

    // Memoized text formatting
    val prizePoolText = remember(prizePool) { "VT ${prizePool.toInt()}" }
    val entryFeeText = remember(entryFee) { if (entryFee == 0.0) "FREE" else "VT ${entryFee.toInt()}" }
    val slotsText = remember(filledSlots, maxSlots) { "$filledSlots/$maxSlots" }
    val mapFormatText = remember(mapType, perspective, format) { "$mapType • $perspective • $format" }

    val joinButtonText by remember(joinCooldownSeconds, isFull) {
        derivedStateOf {
            when {
                joinCooldownSeconds > 0 -> "Wait ${joinCooldownSeconds}s"
                isFull -> "Slots Full"
                else -> "Join Now"
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .shadow(
                elevation = cardElevation,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(24.dp))
            .drawBehind {
                if (isGlassEnabled) {
                    val glareHeight = size.height * liquidGlassConfig.lensRefractionHeight.coerceIn(0.2f, 0.9f)
                    val glareAlpha = (liquidGlassConfig.lensRefractionAmount * 0.35f).coerceIn(0.02f, 0.18f)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = glareAlpha),
                            0.65f to Color.White.copy(alpha = glareAlpha * 0.2f),
                            1.0f to Color.Transparent
                        ),
                        size = androidx.compose.ui.geometry.Size(size.width, glareHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(48f, 48f)
                    )
                }
            }
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
                val context = androidx.compose.ui.platform.LocalContext.current
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                val imageRequest = remember(thumbnailUrl, context, lifecycleOwner) {
                    coil.request.ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .lifecycle(lifecycleOwner)
                        .crossfade(true)
                        .memoryCacheKey(thumbnailUrl)
                        .diskCacheKey(thumbnailUrl)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .size(coil.size.Size(720, 360))
                        .build()
                }

                // Main Game Background Image
                coil.compose.AsyncImage(
                    model = imageRequest,
                    contentDescription = "Game Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient Uncover Overlay: Smoothly uncovers the artwork above and dissolves into cardContainerColor below
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.Black.copy(alpha = 0.35f),
                                0.20f to Color.Transparent,
                                0.40f to Color.Transparent,
                                0.65f to cardContainerColor.copy(alpha = 0.45f),
                                0.82f to cardContainerColor.copy(alpha = 0.88f),
                                0.96f to cardContainerColor.copy(alpha = 0.98f),
                                1.00f to cardContainerColor
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
                        color = badgeStyle.bgColor,
                        border = BorderStroke(1.5.dp, badgeStyle.borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(badgeStyle.borderColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = badgeStyle.text.uppercase(),
                                color = badgeStyle.textColor,
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
                    color = cardTitleColor,
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
                            tint = palette.mutedText
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = mapFormatText,
                            fontSize = 12.sp,
                            color = palette.mutedText
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeStyle.bgColor.copy(alpha = 0.5f),
                        border = BorderStroke(0.8.dp, badgeStyle.borderColor.copy(alpha = 0.8f))
                    ) {
                        Text(
                            text = badgeStyle.text,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeStyle.textColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Metadata Stats (180° Contrast Text - No Yellow)
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
                            color = palette.mutedText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = prizePoolText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.highContrastText
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ENTRY FEE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.mutedText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = entryFeeText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.highContrastText
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "SLOTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.mutedText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = slotsText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.highContrastText
                        )
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
                    color = palette.accentColor,
                    trackColor = Color.White.copy(alpha = 0.12f)
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
                                    color = palette.highContrastText
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
                                    color = palette.mutedText
                                )
                                Text(
                                    text = "${liveUpdate.topKills} Kills",
                                    fontSize = 12.sp,
                                    color = palette.mutedText
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                val remindMeClick = remember(context, title, haptic) {
                    {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                            data = android.provider.CalendarContract.Events.CONTENT_URI
                            putExtra(android.provider.CalendarContract.Events.TITLE, "VeloRix Tournament: $title")
                        }
                        context.startActivity(intent)
                    }
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
                                    .clip(RoundedCornerShape(18.dp))
                                    .graphicsLayer {
                                        scaleX = buttonScale
                                        scaleY = buttonScale
                                    }
                                    .testTag("join_now_action_button"),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accentColor,
                                    contentColor = palette.onAccentColor,
                                    disabledContainerColor = Color(0xFF2E313C),
                                    disabledContentColor = Color(0xFF7E8494)
                                )
                            ) {
                                Text(
                                    text = joinButtonText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = onClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .testTag("view_details_button"),
                                border = BorderStroke(1.5.dp, palette.accentColor),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = palette.accentColor,
                                    containerColor = palette.accentColor.copy(alpha = 0.12f)
                                )
                            ) {
                                Text(
                                    text = "REGISTERED",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                    // Remind Me Button
                    OutlinedButton(
                        onClick = remindMeClick,
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .testTag("remind_me_button"),
                        border = BorderStroke(1.dp, palette.highContrastText.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = palette.highContrastText,
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = "REMIND",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
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