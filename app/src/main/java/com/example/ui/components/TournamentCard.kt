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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.rememberArtworkPalette
val DeepSpaceBlack = Color(0xFF09090B)
val CardSurfaceLight = Color(0xFF18181B)

/**
 * Glowing tactical blinking/pulsing LED indicator with radiating neon halo aura.
 */
@Composable
fun BlinkingLedIndicator(
    color: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 6.5.dp,
    isBlinking: Boolean = true,
    pulseSpeedMs: Int = 1100
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blinking_led")
    val alphaAnim by if (isBlinking) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = pulseSpeedMs / 2, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "led_alpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val glowScale by if (isBlinking) {
        infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = pulseSpeedMs / 2, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "led_scale"
        )
    } else {
        remember { mutableFloatStateOf(1.15f) }
    }

    Box(
        modifier = modifier.size(size * 2),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing neon aura
        Box(
            modifier = Modifier
                .size(size * 1.55f)
                .graphicsLayer {
                    scaleX = glowScale
                    scaleY = glowScale
                    alpha = (alphaAnim * 0.40f).coerceIn(0f, 1f)
                }
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color)
        )
        // Core bright LED bulb
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    alpha = if (isBlinking) alphaAnim.coerceAtLeast(0.65f) else 1f
                }
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color)
                .border(0.75.dp, Color.White.copy(alpha = 0.75f), androidx.compose.foundation.shape.CircleShape)
        )
    }
}

private data class TacticalBadgeStyle(
    val text: String,
    val bgColor: Color,
    val borderColor: Color,
    val textColor: Color
)

@Composable
fun TournamentCard(
    tournament: com.example.data.model.Tournament,
    modifier: Modifier = Modifier,
    liveUpdate: com.example.data.model.LiveMatchUpdate? = null,
    joinCooldownSeconds: Int = 0,
    isGlassCard: Boolean = false,
    liquidGlassConfig: com.example.data.model.LiquidGlassConfig = com.example.data.model.LiquidGlassConfig(),
    onClick: () -> Unit,
    onJoinClick: () -> Unit
) = TournamentCard(
    thumbnailUrl = tournament.displayImageUrl,
    title = tournament.title,
    entryFee = tournament.entryFee,
    prizePool = tournament.prizePool,
    filledSlots = tournament.filledSlots,
    maxSlots = tournament.maxSlots,
    mapType = tournament.mapType,
    perspective = tournament.perspective,
    modifier = modifier,
    dateTimeStr = tournament.dateTimeStr,
    status = tournament.status,
    game = tournament.game,
    rules = tournament.rules,
    categoryBadge = tournament.displayCategoryBadge,
    format = tournament.format,
    matchCategory = tournament.matchCategory,
    matchMode = tournament.matchMode,
    killBounty = tournament.killBounty,
    isJoined = tournament.joined,
    joinCooldownSeconds = joinCooldownSeconds,
    liveUpdate = liveUpdate,
    isGlassCard = isGlassCard,
    liquidGlassConfig = liquidGlassConfig,
    onClick = onClick,
    onJoinClick = onJoinClick
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
    dateTimeStr: String = "",
    status: String = "UPCOMING",
    game: String = "",
    rules: String = "",
    characterSkill: Boolean? = null,
    gunAttributes: Boolean? = null,
    limitedAmmo: Boolean? = null,
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
    val isFull by remember(filledSlots, maxSlots) {
        derivedStateOf { filledSlots >= maxSlots }
    }
    val progress by remember(filledSlots, maxSlots) {
        derivedStateOf {
            val raw = if (maxSlots > 0) filledSlots.toFloat() / maxSlots.toFloat() else 0f
            raw.coerceIn(0f, 1f)
        }
    }

    val displayTimeText = remember(dateTimeStr) {
        when {
            dateTimeStr.isNotBlank() -> dateTimeStr
            else -> "Today at 8:00 PM"
        }
    }

    // Smart detection of major room rules and specs
    val isSkillOn: Boolean = remember(characterSkill, game, rules, matchMode, title, categoryBadge) {
        if (characterSkill != null) return@remember characterSkill
        val lower = "$rules $matchMode $title $categoryBadge".lowercase()
        when {
            lower.contains("no skill") || lower.contains("skill off") || lower.contains("skills off") || lower.contains("skill: off") || lower.contains("skill: no") || lower.contains("headshot_only") || lower.contains("only head") -> false
            game.contains("BGMI", true) || title.contains("BGMI", true) -> false
            else -> true
        }
    }

    val isGunAttrOn: Boolean = remember(gunAttributes, rules, matchMode, title) {
        if (gunAttributes != null) return@remember gunAttributes
        val lower = "$rules $matchMode $title".lowercase()
        lower.contains("gun attr: on") || lower.contains("gun attribute: on") || lower.contains("skins on")
    }

    val isLimitedAmmo: Boolean = remember(limitedAmmo, rules, matchMode, title) {
        if (limitedAmmo != null) return@remember limitedAmmo
        val lower = "$rules $matchMode $title".lowercase()
        !lower.contains("unlimited ammo") && !lower.contains("ammo: unlimited") && !matchMode.equals("UNLIMITED_AMMO", true)
    }

    val isHeadshotOnly: Boolean = remember(matchMode, title, rules, categoryBadge) {
        val lower = "$rules $matchMode $title $categoryBadge".lowercase()
        lower.contains("headshot") || lower.contains("only head")
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val remindMeClick = remember(context, title, haptic) {
        {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                    data = android.provider.CalendarContract.Events.CONTENT_URI
                    putExtra(android.provider.CalendarContract.Events.TITLE, "VeloRix Tournament: $title")
                    putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Match Time: $displayTimeText. Format: $format. Prize: VT ${prizePool.toInt()}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Match scheduled for $displayTimeText", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Compute tactical badge styling with remember
    val badgeStyle = remember(categoryBadge, matchCategory, matchMode, killBounty, format) {
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
            else -> Color(0x330284C7)
        }
        val border = when {
            isHeadshotOnly -> Color(0xFFEF4444)
            isSniperOnly -> Color(0xFFA855F7)
            isLoneWolf -> Color(0xFFF97316)
            isClashSquad -> Color(0xFF38BDF8)
            isSurvival -> Color(0xFF10B981)
            else -> Color(0xFF38BDF8)
        }
        val text = when {
            isHeadshotOnly -> Color(0xFFFCA5A5)
            isSniperOnly -> Color(0xFFE9D5FF)
            isLoneWolf -> Color(0xFFFDBA74)
            isClashSquad -> Color(0xFFBAE6FD)
            isSurvival -> Color(0xFF6EE7B7)
            else -> Color.White
        }
        TacticalBadgeStyle(tacticalBadgeText, bg, border, text)
    }

    val isGlassEnabled = remember(isGlassCard, liquidGlassConfig.enableLiquidGlass, liquidGlassConfig.glassCards) {
        isGlassCard && liquidGlassConfig.enableLiquidGlass && liquidGlassConfig.glassCards
    }

    // Solid, opaque premium dark gaming card surface - prevents muddy background bleed through
    val cardContainerColor = remember(isGlassEnabled, liquidGlassConfig.surfaceTint) {
        if (isGlassEnabled) {
            when (liquidGlassConfig.surfaceTint.lowercase()) {
                "crimson" -> Color(0xFF180A12)
                "midnight" -> Color(0xFF0C1322)
                "emerald" -> Color(0xFF091813)
                "gold" -> Color(0xFF18150A)
                else -> Color(0xFF0F1420)
            }
        } else {
            Color(0xFF0F1420)
        }
    }

    val cardBorder = remember(isGlassEnabled, liquidGlassConfig.lensRefractionAmount) {
        if (isGlassEnabled) {
            val specular = liquidGlassConfig.lensRefractionAmount.coerceIn(0.1f, 0.45f)
            BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF38BDF8).copy(alpha = (specular * 1.5f).coerceIn(0.2f, 0.7f)),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
        } else {
            BorderStroke(1.dp, Color(0xFF1E283C))
        }
    }

    val cardTitleColor = Color.White

    val cardElevation = remember(isGlassEnabled, liquidGlassConfig.depthEffect) {
        if (isGlassEnabled && liquidGlassConfig.depthEffect) 6.dp else 2.dp
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

    val interactionSource = remember { MutableInteractionSource() }
    val isCardPressed by interactionSource.collectIsPressedAsState()
    val cardSpringScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f),
        label = "cardSpringScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .graphicsLayer {
                scaleX = cardSpringScale
                scaleY = cardSpringScale
            }
            .shadow(
                elevation = cardElevation,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .testTag("custom_tournament_card"),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        shape = RoundedCornerShape(22.dp),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail Image Container (Crisp, high-definition banner with NO color bleed)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                val imageRequest = remember(thumbnailUrl, context, lifecycleOwner) {
                    val cleanUrl = thumbnailUrl.trim().trim('"', '\'')
                    val model = when {
                        cleanUrl.startsWith("/9j/") || cleanUrl.startsWith("iVBORw0KGgo") -> "data:image/jpeg;base64,$cleanUrl"
                        cleanUrl.isNotBlank() && cleanUrl != "null" && cleanUrl != "undefined" -> cleanUrl
                        else -> if (format.contains("BGMI", true) || title.contains("BGMI", true)) {
                            "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=800&q=80"
                        } else {
                            "https://images.unsplash.com/photo-1552820728-8b83bb6b773f?auto=format&fit=crop&w=800&q=80"
                        }
                    }
                    coil.request.ImageRequest.Builder(context)
                        .data(model)
                        .lifecycle(lifecycleOwner)
                        .crossfade(true)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .size(coil.size.Size(720, 360))
                        .build()
                }

                // Main Game Background Image - Crisp and sharp banner
                coil.compose.AsyncImage(
                    model = imageRequest,
                    contentDescription = "Game Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Smooth dissolving bottom blend into card body (BitChord artwork blend style)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.55f to cardContainerColor.copy(alpha = 0.65f),
                                    1.0f to cardContainerColor
                                )
                            )
                        )
                )

                // Subtle Top Vignette strictly for badge legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Top Badges Row: Left = Tactical Mode Badge, Right = Match Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Tactical Category Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xDD0A0F1D),
                        border = BorderStroke(1.dp, badgeStyle.borderColor.copy(alpha = 0.8f))
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

                    // Right: Match Status Badge with Blinking Light Indicator
                    if (liveUpdate != null || status.equals("LIVE", true)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xEEDC2626),
                            border = BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BlinkingLedIndicator(
                                    color = Color.White,
                                    size = 5.5.dp,
                                    isBlinking = true,
                                    pulseSpeedMs = 650
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE NOW",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    } else if (isJoined) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xDD064E3B),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BlinkingLedIndicator(
                                    color = Color(0xFF10B981),
                                    size = 5.5.dp,
                                    isBlinking = false
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "JOINED",
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    } else if (isFull) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xDD1E293B),
                            border = BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.5.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color(0xFF94A3B8))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SLOTS FULL",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (progress >= 0.85f) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xEE78350F),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BlinkingLedIndicator(
                                    color = Color(0xFFF59E0B),
                                    size = 5.5.dp,
                                    isBlinking = true,
                                    pulseSpeedMs = 750
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CLOSING SOON",
                                    color = Color(0xFFFDE68A),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xDD064E3B),
                            border = BorderStroke(1.dp, Color(0xFF059669))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BlinkingLedIndicator(
                                    color = Color(0xFF10B981),
                                    size = 5.5.dp,
                                    isBlinking = true,
                                    pulseSpeedMs = 1200
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "OPEN",
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Bottom Banner Overlay: Schedule pill on left, Game brand badge on right
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Schedule Tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xEE0A0F1D),
                        border = BorderStroke(1.dp, Color(0x5538BDF8))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_timer),
                                contentDescription = "Schedule",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = displayTimeText,
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Game Badge
                    val gameLabel = remember(game, title) {
                        if (game.isNotBlank()) {
                            if (game.contains("BGMI", true)) "BGMI" else "FREE FIRE"
                        } else if (title.contains("BGMI", true)) {
                            "BGMI"
                        } else {
                            "FREE FIRE"
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xEE0A0F1D),
                        border = BorderStroke(1.dp, if (gameLabel == "BGMI") Color(0x55F59E0B) else Color(0x55EF4444))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (gameLabel == "BGMI") Color(0xFFF59E0B) else Color(0xFFEF4444))
                            )
                            Text(
                                text = gameLabel,
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Card Body - Pure, high-contrast, clean esports layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Title
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = cardTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Map & Format Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_landscape),
                        contentDescription = "Map",
                        modifier = Modifier.size(13.dp),
                        tint = Color(0xFF94A3B8)
                    )
                    Text(
                        text = mapFormatText,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 1. DEDICATED SCHEDULED MATCH TIME HUD BAR
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF131A29),
                    border = BorderStroke(1.dp, Color(0xFF1E2B45))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_timer),
                                contentDescription = "Match Time",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "MATCH SCHEDULE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = displayTimeText,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        // Quick Remind Bell button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x2238BDF8),
                            border = BorderStroke(0.5.dp, Color(0x6638BDF8)),
                            modifier = Modifier.clickable { remindMeClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_untitledui_bell),
                                    contentDescription = "Remind Me",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "REMIND",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. MAJOR ROOM RULES & SPECS STRIP (Skills ON/OFF, Gun Attr ON/OFF, Limited Ammo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. CHARACTER SKILL CHIP (Blinking / Glowing LED: Green if ON, Red if OFF)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSkillOn) Color(0x2210B981) else Color(0x22EF4444),
                        border = BorderStroke(1.dp, if (isSkillOn) Color(0x6610B981) else Color(0x66EF4444)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            BlinkingLedIndicator(
                                color = if (isSkillOn) Color(0xFF10B981) else Color(0xFFEF4444),
                                size = 5.5.dp,
                                isBlinking = true,
                                pulseSpeedMs = if (isSkillOn) 1200 else 800
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSkillOn) "SKILL ON" else "SKILL OFF",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSkillOn) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                maxLines = 1
                            )
                        }
                    }

                    // 2. GUN ATTRIBUTES CHIP (Fair play competitive standard: Red if OFF, Green if ON)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isGunAttrOn) Color(0x22F43F5E) else Color(0x2210B981),
                        border = BorderStroke(1.dp, if (!isGunAttrOn) Color(0x66F43F5E) else Color(0x6610B981)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            BlinkingLedIndicator(
                                color = if (!isGunAttrOn) Color(0xFFF43F5E) else Color(0xFF10B981),
                                size = 5.5.dp,
                                isBlinking = !isGunAttrOn,
                                pulseSpeedMs = 1300
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (!isGunAttrOn) "GUN ATTR: OFF" else "GUN ATTR: ON",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (!isGunAttrOn) Color(0xFFFDA4AF) else Color(0xFF6EE7B7),
                                maxLines = 1
                            )
                        }
                    }

                    // 3. LIMITED AMMO CHIP
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLimitedAmmo) Color(0x220284C7) else Color(0x22F59E0B),
                        border = BorderStroke(1.dp, if (isLimitedAmmo) Color(0x660284C7) else Color(0x66F59E0B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            BlinkingLedIndicator(
                                color = if (isLimitedAmmo) Color(0xFF38BDF8) else Color(0xFFF59E0B),
                                size = 5.5.dp,
                                isBlinking = !isLimitedAmmo,
                                pulseSpeedMs = 1000
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLimitedAmmo) "AMMO: LTD" else "AMMO: UNLTD",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isLimitedAmmo) Color(0xFFBAE6FD) else Color(0xFFFDE68A),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3-Column Bento Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. PRIZE POOL
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF161C2A),
                        border = BorderStroke(1.dp, Color(0xFF232D42))
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PRIZE POOL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = prizePoolText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    // 2. ENTRY FEE / PER KILL
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF161C2A),
                        border = BorderStroke(1.dp, Color(0xFF232D42))
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (killBounty > 0) "PER KILL" else "ENTRY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (killBounty > 0) "VT ${killBounty.toInt()}" else entryFeeText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = if (entryFee == 0.0) Color(0xFF10B981) else Color.White
                            )
                        }
                    }

                    // 3. SLOTS
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF161C2A),
                        border = BorderStroke(1.dp, Color(0xFF232D42))
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SLOTS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = slotsText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Slots Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E2638)
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
                                    color = Color.White
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
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "${liveUpdate.topKills} Kills",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .graphicsLayer {
                                        scaleX = buttonScale
                                        scaleY = buttonScale
                                    }
                                    .testTag("join_now_action_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF1E2638),
                                    disabledContentColor = Color(0xFF64748B)
                                )
                            ) {
                                Text(
                                    text = joinButtonText.uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = onClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("view_details_button"),
                                border = BorderStroke(1.2.dp, Color(0xFF10B981)),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF10B981),
                                    containerColor = Color(0x1A10B981)
                                )
                            ) {
                                Text(
                                    text = "VIEW ROOM PASS",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
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
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("remind_me_button"),
                        border = BorderStroke(1.dp, Color(0xFF2A364F)),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF94A3B8),
                            containerColor = Color(0xFF141A28)
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