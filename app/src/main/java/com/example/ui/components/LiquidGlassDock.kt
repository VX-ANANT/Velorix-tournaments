package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GffDevanagariFontFamily
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

data class DockTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val DOCK_HEIGHT = 56.dp
private val DOCK_SHAPE = CircleShape

private val AppleLiquidGlassBackground = Brush.verticalGradient(
    colors = listOf(
        Color(0x350E121E), // ~21% translucent obsidian glass
        Color(0x55090C16)  // ~33% translucent deep glass tint
    )
)

private val AppleSpecularRimGradient = Brush.verticalGradient(
    0.0f to Color.White.copy(alpha = 0.55f), // Crisp top light catcher
    0.18f to Color.White.copy(alpha = 0.20f), // Smooth transmission
    0.75f to Color.White.copy(alpha = 0.05f), // Subdued body rim
    1.0f to Color.White.copy(alpha = 0.22f)  // Ambient bottom bounce reflection
)

private val ActivePillGlassBackground = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.18f),
        Color.White.copy(alpha = 0.07f)
    )
)

private val ActivePillSpecularGradient = Brush.verticalGradient(
    0.0f to Color.White.copy(alpha = 0.50f),
    0.4f to Color.White.copy(alpha = 0.18f),
    1.0f to Color.White.copy(alpha = 0.12f)
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

    val tabs = remember(homeIcon, matchesIcon, walletIcon) {
        listOf(
            DockTabItem("home", "Home", homeIcon),
            DockTabItem("matches", "Matches", matchesIcon),
            DockTabItem("leaderboard", "Ranks", Icons.Rounded.Leaderboard),
            DockTabItem("wallet", "Wallet", walletIcon)
        )
    }

    val selectedIndex = tabs.indexOfFirst { it.route == currentTab }.let { if (it == -1) 0 else it }

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val tabCount = tabs.size

    val indicatorOffsetAnim = remember { Animatable(0f) }
    val indicatorScaleX = remember { Animatable(1f) }
    val indicatorScaleY = remember { Animatable(1f) }

    var isDragging by remember { mutableStateOf(false) }
    val velocityTracker = remember { VelocityTracker() }

    LaunchedEffect(selectedIndex, containerWidthPx) {
        if (containerWidthPx > 0 && !isDragging) {
            val tabWidthPx = containerWidthPx / tabCount
            val targetOffset = selectedIndex * tabWidthPx
            indicatorOffsetAnim.animateTo(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 420f
                )
            )
        }
    }

    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- 1. THE MAIN FLOATING LIQUID GLASS DOCK ---
        Box(
            modifier = Modifier
                .weight(1f)
                .height(DOCK_HEIGHT)
                .shadow(
                    elevation = 16.dp,
                    shape = DOCK_SHAPE,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.55f)
                )
                .clip(DOCK_SHAPE)
                .hazeChild(
                    state = hazeState,
                    shape = DOCK_SHAPE
                )
                .background(brush = AppleLiquidGlassBackground)
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = 0.12f),
                            0.5f to Color.White.copy(alpha = 0.02f),
                            1.0f to Color.Transparent
                        ),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.55f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f)
                    )
                }
                .border(
                    width = 1.dp,
                    brush = AppleSpecularRimGradient,
                    shape = DOCK_SHAPE
                )
                .padding(horizontal = 5.dp, vertical = 5.dp)
                .onGloballyPositioned { coordinates ->
                    containerWidthPx = coordinates.size.width.toFloat()
                }
                .pointerInput(tabCount) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            velocityTracker.resetTracking()
                        },
                        onDragEnd = {
                            isDragging = false
                            if (containerWidthPx > 0) {
                                val tabWidthPx = containerWidthPx / tabCount
                                val velocity = velocityTracker.calculateVelocity().x
                                val currentOffset = indicatorOffsetAnim.value
                                val estimatedTargetOffset = currentOffset + (velocity * 0.10f)
                                val rawTargetIndex = (estimatedTargetOffset / tabWidthPx).roundToInt()
                                val targetIndex = rawTargetIndex.coerceIn(0, tabCount - 1)

                                val targetTab = tabs[targetIndex]
                                onTabSelected(targetTab.route)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                coroutineScope.launch {
                                    launch {
                                        indicatorScaleX.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 420f))
                                    }
                                    launch {
                                        indicatorScaleY.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 420f))
                                    }
                                    indicatorOffsetAnim.animateTo(
                                        targetValue = targetIndex * tabWidthPx,
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
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
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
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

                                val boundedVal = if (nextVal < 0) {
                                    currentVal + (dragAmount * 0.35f)
                                } else if (nextVal > maxOffset) {
                                    currentVal + (dragAmount * 0.35f)
                                } else {
                                    nextVal
                                }

                                coroutineScope.launch {
                                    indicatorOffsetAnim.snapTo(boundedVal)
                                    val stretchFactor = (abs(dragAmount) / 24f).coerceIn(0f, 0.12f)
                                    indicatorScaleX.snapTo(1f + stretchFactor)
                                    indicatorScaleY.snapTo(1f - (stretchFactor * 0.45f))
                                }

                                val crossedIndex = (boundedVal / tabWidthPx).roundToInt().coerceIn(0, tabCount - 1)
                                if (crossedIndex != selectedIndex && crossedIndex in 0 until tabCount) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                    )
                }
        ) {
            // --- SLIDING ACTIVE PILL CAPSULE ---
            if (containerWidthPx > 0) {
                val tabWidthDp = with(density) { (containerWidthPx / tabCount).toDp() }
                val currentOffsetDp = with(density) { indicatorOffsetAnim.value.toDp() }

                Box(
                    modifier = Modifier
                        .offset(x = currentOffsetDp)
                        .width(tabWidthDp)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                        .graphicsLayer {
                            scaleX = indicatorScaleX.value
                            scaleY = indicatorScaleY.value
                        }
                        .clip(CircleShape)
                        .background(brush = ActivePillGlassBackground)
                        .border(
                            width = 1.dp,
                            brush = ActivePillSpecularGradient,
                            shape = CircleShape
                        )
                )
            }

            // --- TAB ICONS & TYPOGRAPHY ROW ---
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { _, tab ->
                    val isSelected = currentTab == tab.route

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 0.95f,
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
                        label = "dockIconScale"
                    )

                    val tint by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color(0xFF94A3B8).copy(alpha = 0.70f),
                        animationSpec = tween(180),
                        label = "dockTint"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
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
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = tint,
                                modifier = Modifier
                                    .size(if (isSelected) 21.dp else 22.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                            )

                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(tween(160)),
                                exit = fadeOut(tween(100))
                            ) {
                                Text(
                                    text = tab.label,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = GffDevanagariFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. SATELLITE PROFILE ORB (56dp matching size) ---
        val isProfileSelected = currentTab == "profile"
        val profileScale by animateFloatAsState(
            targetValue = if (isProfileSelected) 1.05f else 1f,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = 450f
            ),
            label = "dockProfileScale"
        )
        val profileTint by animateColorAsState(
            targetValue = if (isProfileSelected) Color.White else Color(0xFF94A3B8).copy(alpha = 0.70f),
            animationSpec = tween(180),
            label = "dockProfileTint"
        )

        Box(
            modifier = Modifier
                .size(DOCK_HEIGHT)
                .graphicsLayer {
                    scaleX = profileScale
                    scaleY = profileScale
                }
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.55f)
                )
                .clip(CircleShape)
                .hazeChild(
                    state = hazeState,
                    shape = CircleShape
                )
                .background(brush = AppleLiquidGlassBackground)
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = 0.14f),
                            0.5f to Color.White.copy(alpha = 0.02f),
                            1.0f to Color.Transparent
                        ),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.55f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f)
                    )
                }
                .border(
                    width = 1.dp,
                    brush = if (isProfileSelected) {
                        Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = 0.65f),
                            0.2f to Color.White.copy(alpha = 0.30f),
                            0.8f to Color.White.copy(alpha = 0.10f),
                            1.0f to Color.White.copy(alpha = 0.35f)
                        )
                    } else AppleSpecularRimGradient,
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
            if (isProfileSelected) {
                Box(
                    modifier = Modifier
                        .size(DOCK_HEIGHT - 12.dp)
                        .clip(CircleShape)
                        .background(brush = ActivePillGlassBackground)
                        .border(
                            width = 1.dp,
                            brush = ActivePillSpecularGradient,
                            shape = CircleShape
                        )
                )
            }

            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_iconsax_profile),
                contentDescription = "Profile",
                tint = profileTint,
                modifier = Modifier.size(if (isProfileSelected) 24.dp else 22.dp)
            )
        }
    }
}
