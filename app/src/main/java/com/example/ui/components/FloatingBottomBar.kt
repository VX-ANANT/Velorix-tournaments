package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlin.math.abs
import kotlin.math.roundToInt

data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Exact BitChord metrics and constants
 */
internal val PILL_INSET = 6.dp
internal val TAB_VERTICAL_PADDING = 8.dp
internal val TAB_ICON_LABEL_GAP = 2.dp
internal val PAGE_GUTTER = 12.dp

/**
 * BitChord Damped Spring: Damping 0.72f, Stiffness 320f
 */
internal val GlassSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 320f)

/**
 * Liquid stretch and squash momentum math from BitChord
 */
internal const val STRETCH = 0.16f
internal const val SQUASH = 0.5f

internal val GLASS_EDGE_WIDTH = 0.5.dp
internal val GLASS_EDGE_COLOR = Color.White.copy(alpha = 0.16f)

private val SpecularHighlightBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.32f),
        Color.White.copy(alpha = 0.06f)
    )
)

@Composable
fun FloatingBottomBar(
    tabs: List<BottomTab>,
    profileTab: BottomTab,
    currentRoute: String,
    onTabSelected: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(percent = 50)
    val circleShape = CircleShape
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Determine if one of the 4 main tabs is selected
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }
    val isProfileSelected = currentRoute == profileTab.route

    val glassSpec: AnimationSpec<Float> = GlassSpring
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val currentSelectedIndex by rememberUpdatedState(if (selectedIndex != -1) selectedIndex else 0)

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    val gapPx = with(density) { 4.dp.toPx() }
    val n = tabs.size

    val tabWidthPx = if (rowSize.width > 0 && n > 0) {
        (rowSize.width - gapPx * (n - 1)) / n
    } else 0f
    val tabStepPx = if (rowSize.width > 0 && n > 0) {
        (rowSize.width + gapPx) / n
    } else 0f

    val pillTargetPx = if (tabStepPx > 0f && selectedIndex >= 0) {
        selectedIndex * tabStepPx + dragOffset
    } else 0f

    val animatedPillOffset by animateFloatAsState(
        targetValue = pillTargetPx,
        animationSpec = glassSpec,
        label = "pillOffset",
    )

    val lag = if (tabStepPx > 0f && selectedIndex >= 0) {
        (abs(pillTargetPx - animatedPillOffset) / tabStepPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    var lastHapticTab by remember { mutableIntStateOf(if (selectedIndex >= 0) selectedIndex else 0) }

    LaunchedEffect(selectedIndex) { dragOffset = 0f }

    // Floating row: Main Tabs Pill on Left + Standalone Profile Orb on Right
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = PAGE_GUTTER)
            .padding(bottom = 2.dp) // Sits low right above the system navigation bar
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // -------------------------------------------------------------
        // 1. LEFT PILL: Main Navigation Tabs (Home, Matches, Ranks, Wallet)
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(pillShape)
                .hazeChild(state = hazeState, shape = pillShape)
                .background(Color(0x2B111827))
                .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, pillShape)
                .border(GLASS_EDGE_WIDTH, SpecularHighlightBrush, pillShape)
                .padding(horizontal = PILL_INSET, vertical = PILL_INSET),
        ) {
            // Active Tab Indicator: Smooth elongated capsule (Pill shape)
            if (tabWidthPx > 0f && selectedIndex >= 0) {
                Box(
                    modifier = Modifier
                        .width(with(density) { tabWidthPx.toDp() })
                        .height(with(density) { rowSize.height.toDp() })
                        .graphicsLayer {
                            translationX = animatedPillOffset
                            // BitChord Fluid Momentum Math:
                            scaleX = 1f + lag * STRETCH
                            scaleY = 1f - lag * STRETCH * SQUASH
                        }
                        .clip(pillShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(
                            GLASS_EDGE_WIDTH,
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            ),
                            pillShape
                        ),
                )
            }

            // Tabs Row with horizontal drag gesture tracking
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { rowSize = it }
                    .pointerInput(Unit) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragCancel = { dragOffset = 0f },
                            onDragEnd = {
                                if (tabStepPx > 0f) {
                                    val ratio = totalDrag / tabStepPx
                                    val shift = when {
                                        ratio > 0.35f -> kotlin.math.max(1, ratio.roundToInt())
                                        ratio < -0.35f -> kotlin.math.min(-1, ratio.roundToInt())
                                        else -> 0
                                    }
                                    val newIndex = (currentSelectedIndex + shift).coerceIn(0, tabs.lastIndex)
                                    if (newIndex != currentSelectedIndex) {
                                        onTabSelected(tabs[newIndex].route)
                                    }
                                }
                                dragOffset = 0f
                            },
                            onHorizontalDrag = { _, delta ->
                                totalDrag += delta
                                val rawPx = when {
                                    totalDrag > 0 && currentSelectedIndex == tabs.lastIndex ->
                                        totalDrag * 0.25f
                                    totalDrag < 0 && currentSelectedIndex == 0 ->
                                        totalDrag * 0.25f
                                    else -> totalDrag
                                }
                                dragOffset = rawPx

                                val approxTab = (currentSelectedIndex + dragOffset / tabStepPx)
                                    .coerceIn(0f, tabs.lastIndex.toFloat())
                                    .roundToInt()
                                if (approxTab != lastHapticTab) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastHapticTab = approxTab
                                }
                            },
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedIndex
                    BottomBarItem(
                        tab = tab,
                        selected = isSelected,
                        glassSpec = glassSpec,
                        selectedTint = Color.White,
                        unselectedTint = Color(0xFF94A3B8).copy(alpha = 0.65f),
                        onClick = { onTabSelected(tab.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 2. RIGHT STANDALONE ORB: Profile Capsule
        // -------------------------------------------------------------
        val profileScale by animateFloatAsState(
            targetValue = if (isProfileSelected) 1.06f else 1f,
            animationSpec = glassSpec,
            label = "profileScale"
        )
        val profileTint by animateColorAsState(
            targetValue = if (isProfileSelected) Color.White else Color(0xFF94A3B8).copy(alpha = 0.65f),
            animationSpec = tween(200),
            label = "profileTint"
        )

        Box(
            modifier = Modifier
                .size(54.dp)
                .graphicsLayer {
                    scaleX = profileScale
                    scaleY = profileScale
                }
                .clip(circleShape)
                .hazeChild(state = hazeState, shape = circleShape)
                .background(Color(0x2B111827))
                .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, circleShape)
                .border(GLASS_EDGE_WIDTH, SpecularHighlightBrush, circleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(profileTab.route)
                },
            contentAlignment = Alignment.Center,
        ) {
            // Selected active indicator inside the profile orb
            if (isProfileSelected) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(circleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(
                            GLASS_EDGE_WIDTH,
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            ),
                            circleShape
                        )
                )
            }

            Icon(
                imageVector = profileTab.icon,
                contentDescription = profileTab.label,
                tint = profileTint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    tab: BottomTab,
    selected: Boolean,
    glassSpec: AnimationSpec<Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTint: Color? = null,
    unselectedTint: Color? = null,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = glassSpec,
        label = "tabScale",
    )
    val haptic = LocalHapticFeedback.current
    val tint by animateColorAsState(
        targetValue = if (selected) {
            selectedTint ?: MaterialTheme.colorScheme.primary
        } else {
            unselectedTint ?: MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "tabTint",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (!selected) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
            .padding(vertical = TAB_VERTICAL_PADDING),
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
        Spacer(Modifier.height(TAB_ICON_LABEL_GAP))
        Text(
            text = tab.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
