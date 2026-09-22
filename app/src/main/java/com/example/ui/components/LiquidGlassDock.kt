package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * High-performance, GPU accelerated Liquid Glass Floating Dock with Apple-like
 * interactive draggable physics, rubberband stretch dynamics, and spring overshoot bounce.
 * Inspired by BitChord & Apple fluid design systems.
 */
data class DockTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun LiquidGlassDock(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val homeIcon = ImageVector.vectorResource(id = R.drawable.ic_iconsax_home)
    val matchesIcon = ImageVector.vectorResource(id = R.drawable.ic_iconsax_matches)
    val walletIcon = ImageVector.vectorResource(id = R.drawable.ic_iconsax_wallet)
    val profileIcon = ImageVector.vectorResource(id = R.drawable.ic_iconsax_profile)

    val tabs = remember(homeIcon, matchesIcon, walletIcon) {
        listOf(
            DockTabItem("home", "Home", homeIcon),
            DockTabItem("matches", "Matches", matchesIcon),
            DockTabItem("leaderboard", "Ranks", Icons.Rounded.Leaderboard),
            DockTabItem("wallet", "Wallet", walletIcon)
        )
    }

    val selectedIndex = tabs.indexOfFirst { it.route == currentTab }.let { if (it == -1) 0 else it }

    // Physical measurements for fluid spring tracking
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val tabCount = tabs.size

    // Indicator animated offset (continuous float in pixels)
    val indicatorOffsetAnim = remember { Animatable(0f) }
    // Elastic stretch scales
    val indicatorScaleX = remember { Animatable(1f) }
    val indicatorScaleY = remember { Animatable(1f) }

    var isDragging by remember { mutableStateOf(false) }
    val velocityTracker = remember { VelocityTracker() }

    // Synchronize indicator when currentTab changes externally (or upon initial layout)
    LaunchedEffect(selectedIndex, containerWidthPx) {
        if (containerWidthPx > 0 && !isDragging) {
            val tabWidthPx = containerWidthPx / tabCount
            val targetOffset = selectedIndex * tabWidthPx
            indicatorOffsetAnim.animateTo(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = 0.68f, // Apple-style fluid overshoot bounce
                    stiffness = 380f
                )
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- 1. THE MAIN FLOATING LIQUID GLASS DOCK ---
        Box(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .hazeChild(
                    state = hazeState,
                    shape = RoundedCornerShape(36.dp)
                )
                .lensRefraction(
                    refractionIndex = 1.45f,
                    lensCurvature = 0.85f,
                    chromaticSplit = 0.035f
                )
                // Multi-layered Realtime Frosted Glass Refraction Tint
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xCC090D14), // Deep subtle obsidian-blue glass tint
                            Color(0x99121824)
                        )
                    )
                )
                // 1px Specular Rim Light Gradient (Subtle top sheen highlight)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f), // High specular light reflection
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
                .onGloballyPositioned { coordinates ->
                    containerWidthPx = coordinates.size.width.toFloat()
                }
                .pointerInput(tabCount) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            velocityTracker.resetTracking()
                        },
                        onDragEnd = {
                            isDragging = false
                            if (containerWidthPx > 0) {
                                val tabWidthPx = containerWidthPx / tabCount
                                val velocity = velocityTracker.calculateVelocity().x

                                // Calculate which tab to snap to, accounting for fling momentum
                                val currentOffset = indicatorOffsetAnim.value
                                val estimatedTargetOffset = currentOffset + (velocity * 0.12f)
                                val rawTargetIndex = (estimatedTargetOffset / tabWidthPx).roundToInt()
                                val targetIndex = rawTargetIndex.coerceIn(0, tabCount - 1)

                                val targetTab = tabs[targetIndex]
                                onTabSelected(targetTab.route)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                coroutineScope.launch {
                                    // Elastic spring snap back with squish-release bounce
                                    launch {
                                        indicatorScaleX.animateTo(
                                            1f,
                                            spring(dampingRatio = 0.58f, stiffness = 420f)
                                        )
                                    }
                                    launch {
                                        indicatorScaleY.animateTo(
                                            1f,
                                            spring(dampingRatio = 0.58f, stiffness = 420f)
                                        )
                                    }
                                    indicatorOffsetAnim.animateTo(
                                        targetValue = targetIndex * tabWidthPx,
                                        animationSpec = spring(
                                            dampingRatio = 0.65f, // Bouncy spring settle
                                            stiffness = 340f
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            if (containerWidthPx > 0) {
                                val tabWidthPx = containerWidthPx / tabCount
                                coroutineScope.launch {
                                    indicatorScaleX.snapTo(1f)
                                    indicatorScaleY.snapTo(1f)
                                    indicatorOffsetAnim.animateTo(
                                        targetValue = selectedIndex * tabWidthPx,
                                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 400f)
                                    )
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)

                            if (containerWidthPx > 0) {
                                val tabWidthPx = containerWidthPx / tabCount
                                val maxOffset = (tabCount - 1) * tabWidthPx
                                val currentVal = indicatorOffsetAnim.value
                                val nextVal = currentVal + dragAmount

                                // Rubberband overscroll resistance when dragged past boundaries
                                val boundedVal = if (nextVal < 0) {
                                    currentVal + (dragAmount * 0.35f)
                                } else if (nextVal > maxOffset) {
                                    currentVal + (dragAmount * 0.35f)
                                } else {
                                    nextVal
                                }

                                coroutineScope.launch {
                                    indicatorOffsetAnim.snapTo(boundedVal)

                                    // Dynamic Fluid Stretching Math based on drag magnitude
                                    val stretchFactor = (abs(dragAmount) / 18f).coerceIn(0f, 0.22f)
                                    indicatorScaleX.snapTo(1f + stretchFactor)
                                    indicatorScaleY.snapTo(1f - (stretchFactor * 0.55f))
                                }

                                // Haptic detent feedback when sliding across tabs
                                val crossedIndex = (boundedVal / tabWidthPx).roundToInt().coerceIn(0, tabCount - 1)
                                if (crossedIndex != selectedIndex && crossedIndex in 0 until tabCount) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                    )
                }
        ) {
            // --- SLIDING LIQUID PILL CAPSULE & GLOW (BACKGROUND LAYER) ---
            if (containerWidthPx > 0) {
                val tabWidthDp = with(density) { (containerWidthPx / tabCount).toDp() }
                val currentOffsetDp = with(density) { indicatorOffsetAnim.value.toDp() }

                Box(
                    modifier = Modifier
                        .offset(x = currentOffsetDp)
                        .width(tabWidthDp)
                        .fillMaxHeight()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .graphicsLayer {
                            scaleX = indicatorScaleX.value
                            scaleY = indicatorScaleY.value
                        }
                ) {
                    // Radiant Radial Bloom / Neon Glow behind active pill
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.18f)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF0070F3).copy(alpha = 0.55f), // Vercel/Electric Blue liquid bloom
                                        Color(0xFF38BDF8).copy(alpha = 0.20f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(30.dp)
                            )
                    )

                    // Frosted Liquid Pill Body with Inner Refraction Stroke
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.18f), // Glass top specular sheen
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.35f), // High-clarity refraction rim
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = RoundedCornerShape(30.dp)
                            )
                    )
                }
            }

            // --- TAB ICONS & TYPOGRAPHY ROW (FOREGROUND LAYER) ---
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = currentTab == tab.route

                    // Touch down scale bounce for snappy feedback
                    val itemScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.04f else 0.96f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "tabItemScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(36.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (currentTab != tab.route) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelected(tab.route)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                            }
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) Color.White else Color(0xFF94A3B8).copy(alpha = 0.82f),
                                modifier = Modifier.size(if (isSelected) 25.dp else 23.dp)
                            )

                            AnimatedVisibility(
                                visible = isSelected,
                                enter = androidx.compose.animation.expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                                exit = androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                            ) {
                                Text(
                                    text = tab.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. THE SATELLITE PROFILE LIQUID ORB ---
        val isProfileSelected = currentTab == "profile"
        val profileScale by animateFloatAsState(
            targetValue = if (isProfileSelected) 1.06f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "profileScale"
        )

        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = profileScale
                    scaleY = profileScale
                }
                .clip(CircleShape)
                .hazeChild(
                    state = hazeState,
                    shape = CircleShape
                )
                .lensRefraction(
                    refractionIndex = 1.45f,
                    lensCurvature = 0.9f,
                    chromaticSplit = 0.035f
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xCC090D14),
                            Color(0x99121824)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isProfileSelected) Color(0xFF0070F3).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected("profile")
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Radiant Bloom around Profile Orb when selected
            val bloomAlpha by animateFloatAsState(
                targetValue = if (isProfileSelected) 1f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "profileBloomAlpha"
            )
            if (bloomAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { alpha = bloomAlpha }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF0070F3).copy(alpha = 0.65f),
                                    Color(0xFF38BDF8).copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_iconsax_profile),
                contentDescription = "Profile",
                tint = if (isProfileSelected) Color.White else Color(0xFF94A3B8).copy(alpha = 0.82f),
                modifier = Modifier.size(if (isProfileSelected) 28.dp else 24.dp)
            )
        }
    }
}
