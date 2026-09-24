package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.data.model.LiquidGlassConfig
import com.example.ui.theme.GffDevanagariFontFamily
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
 * Sleek 56dp Stadium Pill Navigation Metrics
 */
private val BAR_HEIGHT = 56.dp
private val BAR_PILL_SHAPE = CircleShape // 100% Stadium Capsule Shape
private val ACTIVE_PILL_SHAPE = CircleShape // 100% Stadium Capsule Indicator
private val PILL_INSET_VERTICAL = 5.dp
private val PILL_INSET_HORIZONTAL = 5.dp
private val PAGE_GUTTER = 14.dp

/**
 * Ultra-smooth, stutter-free spring physics
 */
private val GlassSpring: AnimationSpec<Float> = spring(
    dampingRatio = 0.82f,
    stiffness = 420f
)

/**
 * Floating Pill Bottom Navigation Bar with Apple Liquid Glass Lens Optics:
 * - True Stadium / Pill Shape (CircleShape / 100% Rounded Capsule)
 * - Compact 56dp height matching reference design
 * - Dynamic high transparency & low tint overlay per user request
 * - Softened specular rim light without blinding white harshness
 * - Fully customizable via runtime LiquidGlassConfig (Beta)
 */
@Composable
fun FloatingBottomBar(
    tabs: List<BottomTab>,
    profileTab: BottomTab,
    currentRoute: String,
    onTabSelected: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    config: LiquidGlassConfig = LiquidGlassConfig()
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }
    val isProfileSelected = currentRoute == profileTab.route

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val currentSelectedIndex by rememberUpdatedState(if (selectedIndex != -1) selectedIndex else 0)

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    val tabGap = 2.dp
    val gapPx = with(density) { tabGap.toPx() }
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
        animationSpec = GlassSpring,
        label = "pillOffset",
    )

    val lag = if (tabStepPx > 0f && selectedIndex >= 0) {
        (abs(pillTargetPx - animatedPillOffset) / tabStepPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    var lastHapticTab by remember { mutableIntStateOf(if (selectedIndex >= 0) selectedIndex else 0) }

    LaunchedEffect(selectedIndex) { dragOffset = 0f }

    // -----------------------------------------------------------------
    // Dynamic Apple Liquid Glass Material Calculation
    // -----------------------------------------------------------------
    val isGlassEnabled = config.enableLiquidGlass && config.glassNavBar
    val tintOpacity = config.surfaceOpacity.coerceIn(0.08f, 0.85f)
    val specularIntensity = (config.lensRefractionAmount * 1.6f).coerceIn(0.08f, 0.55f)
    val glareHeightFraction = config.lensRefractionHeight.coerceIn(0.15f, 0.95f)

    // Base surface tint colors
    val baseTint = when (config.surfaceTint.lowercase()) {
        "crimson" -> Color(0xFF1E030B)
        "midnight" -> Color(0xFF091122)
        "clear" -> Color(0xFF06080E)
        else -> Color(0xFF0D111A) // obsidian
    }

    val liquidGlassBackground = Brush.verticalGradient(
        colors = listOf(
            baseTint.copy(alpha = tintOpacity * 0.75f),
            baseTint.copy(alpha = tintOpacity * 1.15f)
        )
    )

    // Non-harsh specular top light catcher with smooth refraction
    val specularRimGradient = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = specularIntensity),
        0.20f to Color.White.copy(alpha = specularIntensity * 0.35f),
        0.75f to Color.White.copy(alpha = specularIntensity * 0.10f),
        1.0f to Color.White.copy(alpha = specularIntensity * 0.40f)
    )

    val activePillBackground = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = (specularIntensity * 0.55f).coerceIn(0.08f, 0.25f)),
            Color.White.copy(alpha = (specularIntensity * 0.25f).coerceIn(0.03f, 0.12f))
        )
    )

    val activePillSpecular = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = (specularIntensity * 1.1f).coerceIn(0.15f, 0.45f)),
        0.4f to Color.White.copy(alpha = (specularIntensity * 0.4f).coerceIn(0.05f, 0.20f)),
        1.0f to Color.White.copy(alpha = (specularIntensity * 0.25f).coerceIn(0.04f, 0.15f))
    )

    val textColor = when (config.glassTextColor.lowercase()) {
        "platinum" -> Color(0xFFE2E8F0)
        "adaptive" -> Color(0xFFF1F5F9)
        else -> Color.White
    }

    val bottomPadding = if (config.floatingNavBar) 10.dp else 2.dp
    val sidePadding = if (config.floatingNavBar) PAGE_GUTTER else 6.dp

    // Outer Floating Layout Container
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = sidePadding)
            .padding(bottom = bottomPadding)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // -------------------------------------------------------------
        // 1. LEFT MAIN PILL CONTAINER (Stadium Capsule)
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .weight(1f)
                .height(BAR_HEIGHT)
                .shadow(
                    elevation = if (config.depthEffect) 14.dp else 0.dp,
                    shape = BAR_PILL_SHAPE,
                    ambientColor = Color.Black.copy(alpha = 0.30f),
                    spotColor = Color.Black.copy(alpha = 0.45f)
                )
                .clip(BAR_PILL_SHAPE)
                .then(
                    if (isGlassEnabled) {
                        Modifier.hazeChild(state = hazeState, shape = BAR_PILL_SHAPE)
                    } else {
                        Modifier.background(Color(0xFF161922))
                    }
                )
                // Apple Liquid Glass Translucent Base
                .background(brush = liquidGlassBackground)
                // Upper Curvature Glare & Lens Refraction
                .drawBehind {
                    if (isGlassEnabled) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.White.copy(alpha = (specularIntensity * 0.35f).coerceIn(0.02f, 0.14f)),
                                0.6f to Color.White.copy(alpha = (specularIntensity * 0.08f).coerceIn(0.005f, 0.04f)),
                                1.0f to Color.Transparent
                            ),
                            size = androidx.compose.ui.geometry.Size(size.width, size.height * glareHeightFraction),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f)
                        )
                    }
                }
                // Specular Glass Rim Border
                .border(
                    width = 1.dp,
                    brush = specularRimGradient,
                    shape = BAR_PILL_SHAPE
                )
                .padding(horizontal = PILL_INSET_HORIZONTAL, vertical = PILL_INSET_VERTICAL),
            contentAlignment = Alignment.CenterStart,
        ) {
            // Sliding Active Capsule Pill Indicator
            if (tabWidthPx > 0f && selectedIndex >= 0) {
                Box(
                    modifier = Modifier
                        .width(with(density) { tabWidthPx.toDp() })
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = animatedPillOffset
                            // Subtle momentum stretch (Apple physical response)
                            scaleX = 1f + lag * 0.08f
                            scaleY = 1f - lag * 0.04f
                        }
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                        .clip(ACTIVE_PILL_SHAPE)
                        // Active Pill Fill: Translucent Glass with subtle ambient depth
                        .background(brush = activePillBackground)
                        // Active Pill Specular Inner Bevel
                        .border(
                            width = 1.dp,
                            brush = activePillSpecular,
                            shape = ACTIVE_PILL_SHAPE
                        )
                )
            }

            // Interactive Tabs Row with horizontal drag gesture tracking
            Row(
                modifier = Modifier
                    .fillMaxSize()
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
                horizontalArrangement = Arrangement.spacedBy(tabGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedIndex
                    BottomBarItem(
                        tab = tab,
                        selected = isSelected,
                        textColor = textColor,
                        onClick = { onTabSelected(tab.route) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 2. RIGHT STANDALONE ORB: Profile Button (Exact matching 56dp size)
        // -------------------------------------------------------------
        val profileScale by animateFloatAsState(
            targetValue = if (isProfileSelected) 1.05f else 1f,
            animationSpec = GlassSpring,
            label = "profileScale"
        )
        val profileTint by animateColorAsState(
            targetValue = if (isProfileSelected) textColor else Color(0xFF94A3B8).copy(alpha = 0.75f),
            animationSpec = tween(180),
            label = "profileTint"
        )

        Box(
            modifier = Modifier
                .size(BAR_HEIGHT) // Exactly 56dp x 56dp - matching main nav bar height perfectly
                .graphicsLayer {
                    scaleX = profileScale
                    scaleY = profileScale
                }
                .shadow(
                    elevation = if (config.depthEffect) 14.dp else 0.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.30f),
                    spotColor = Color.Black.copy(alpha = 0.45f)
                )
                .clip(CircleShape)
                .then(
                    if (isGlassEnabled) {
                        Modifier.hazeChild(state = hazeState, shape = CircleShape)
                    } else {
                        Modifier.background(Color(0xFF161922))
                    }
                )
                .background(brush = liquidGlassBackground)
                .drawBehind {
                    if (isGlassEnabled) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.White.copy(alpha = (specularIntensity * 0.38f).coerceIn(0.02f, 0.15f)),
                                0.6f to Color.White.copy(alpha = (specularIntensity * 0.08f).coerceIn(0.005f, 0.04f)),
                                1.0f to Color.Transparent
                            ),
                            size = androidx.compose.ui.geometry.Size(size.width, size.height * glareHeightFraction),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f)
                        )
                    }
                }
                .border(
                    width = 1.dp,
                    brush = if (isProfileSelected) {
                        Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = (specularIntensity * 1.5f).coerceIn(0.25f, 0.65f)),
                            0.2f to Color.White.copy(alpha = (specularIntensity * 0.7f).coerceIn(0.10f, 0.30f)),
                            0.8f to Color.White.copy(alpha = (specularIntensity * 0.25f).coerceIn(0.03f, 0.12f)),
                            1.0f to Color.White.copy(alpha = (specularIntensity * 0.8f).coerceIn(0.12f, 0.35f))
                        )
                    } else specularRimGradient,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(profileTab.route)
                },
            contentAlignment = Alignment.Center,
        ) {
            // Active selection pill inside the profile orb
            if (isProfileSelected) {
                Box(
                    modifier = Modifier
                        .size(BAR_HEIGHT - 12.dp)
                        .clip(CircleShape)
                        .background(brush = activePillBackground)
                        .border(
                            width = 1.dp,
                            brush = activePillSpecular,
                            shape = CircleShape
                        )
                )
            }

            Icon(
                imageVector = profileTab.icon,
                contentDescription = profileTab.label,
                tint = profileTint,
                modifier = Modifier.size(if (isProfileSelected) 24.dp else 22.dp),
            )
        }
    }
}

/**
 * Individual Tab Item:
 * - When UNSELECTED: Displays ONLY the clean, centered icon (no text clutter).
 * - When SELECTED: Displays the icon + smooth animated label text underneath.
 */
@Composable
private fun BottomBarItem(
    tab: BottomTab,
    selected: Boolean,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 0.95f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
        label = "tabIconScale",
    )

    val tint by animateColorAsState(
        targetValue = if (selected) textColor else Color(0xFF94A3B8).copy(alpha = 0.75f),
        animationSpec = tween(durationMillis = 180),
        label = "tabTint",
    )

    Box(
        modifier = modifier
            .clip(ACTIVE_PILL_SHAPE)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (!selected) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            },
        contentAlignment = Alignment.Center,
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
                    .size(if (selected) 21.dp else 22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )

            // Name / Label appears ONLY when selected (Smooth, non-blocking fade & expand)
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(animationSpec = tween(160)),
                exit = fadeOut(animationSpec = tween(100))
            ) {
                Text(
                    text = tab.label,
                    fontSize = 10.sp,
                    fontFamily = GffDevanagariFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    letterSpacing = 0.2.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}
