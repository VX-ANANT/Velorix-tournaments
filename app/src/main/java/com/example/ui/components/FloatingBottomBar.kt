/*
 * Directly cloned from BitChord (com.music.bitchord.ui.components.FloatingBottomBar & GlassNavBar)
 * Provides:
 * - Liquid glass selection pill that enlarges and lifts past the bar edges on hold, tap, and drag.
 * - Refraction lens with chromatic aberration rainbow rim and contact shadow (GlassSelectionPill).
 * - Under-damped spring kinematics (GlassPillMotion) with acceleration squash & stretch.
 * - Exact BitChord padding, sizing, and tab hierarchy.
 * - Standalone circular glass profile button.
 */
package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.LiquidGlassConfig
import com.example.ui.components.backdrop.backdrops.layerBackdrop
import com.example.ui.components.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class BottomTab(
    val route: String = "",
    val label: String,
    val icon: ImageVector,
)

/**
 * The gap between the pill's glass edge and the tabs inside it.
 * Sourced directly from BitChord.
 */
internal val PILL_INSET = 6.dp

/**
 * How far the floating bars stand off the bottom edge: the system navigation
 * bar's inset, but never less than a gesture bar's floor.
 */
internal val floatingBarInsets: WindowInsets
    @Composable get() = WindowInsets.navigationBars.union(WindowInsets(bottom = GESTURE_BAR_FLOOR))

private val GESTURE_BAR_FLOOR = 15.dp

/** Each tab's vertical padding, counterweight to PILL_INSET. Sourced directly from BitChord. */
internal val TAB_VERTICAL_PADDING = 9.dp

/** Gap between tab icon and label text. Sourced directly from BitChord. */
internal val TAB_ICON_LABEL_GAP = 2.dp

/** Margin from side edges. */
internal val BAR_GUTTER = 16.dp

/**
 * The spring the selection indicator and the tab glyphs both travel on.
 * Sourced directly from BitChord.
 */
internal val GlassSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 320f)

/**
 * Liquid glass bottom bar cloned from BitChord:
 * - Liquid glass pill lens that inflates and enlarges past the bar's top and bottom on hold, tap, and drag.
 * - Real-time refraction lens with chromatic aberration rainbow highlight rim and contact shadow.
 * - Precise physics spring model (GlassPillMotion) with acceleration squash & stretch.
 * - BitChord vertical icon + label layout with equal width weighting.
 * - Standalone circular glass profile button.
 */
@Composable
fun FloatingBottomBar(
    tabs: List<BottomTab>,
    currentRoute: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    profileTab: BottomTab? = null,
    userAvatarUrl: String? = null,
    config: LiquidGlassConfig? = null,
) {
    val (groupedTabs, resolvedProfileTab) = remember(tabs, profileTab) {
        if (profileTab != null) {
            tabs.filter { it.route != profileTab.route } to profileTab
        } else {
            val foundProfile = tabs.firstOrNull {
                it.route == "profile" || it.label.equals("Profile", ignoreCase = true)
            }
            if (foundProfile != null && tabs.size > 1) {
                tabs.filter { it != foundProfile } to foundProfile
            } else {
                tabs to null
            }
        }
    }

    val isProfileSelected = resolvedProfileTab != null && currentRoute == resolvedProfileTab.route
    val selectedIndex = if (isProfileSelected) -1 else groupedTabs.indexOfFirst { it.route == currentRoute }

    FloatingBottomBarContent(
        groupedTabs = groupedTabs,
        selectedIndex = selectedIndex,
        onTabSelected = { index ->
            if (index in groupedTabs.indices) {
                onTabSelected(groupedTabs[index].route)
            }
        },
        profileTab = resolvedProfileTab,
        isProfileSelected = isProfileSelected,
        onProfileSelected = {
            if (resolvedProfileTab != null) {
                onTabSelected(resolvedProfileTab.route)
            }
        },
        userAvatarUrl = userAvatarUrl,
        hazeState = hazeState,
        modifier = modifier
    )
}

@Composable
fun FloatingBottomBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    profileTab: BottomTab? = null,
    isProfileSelected: Boolean = false,
    onProfileSelected: () -> Unit = {},
    userAvatarUrl: String? = null,
) {
    FloatingBottomBarContent(
        groupedTabs = tabs,
        selectedIndex = selectedIndex,
        onTabSelected = onTabSelected,
        profileTab = profileTab,
        isProfileSelected = isProfileSelected,
        onProfileSelected = onProfileSelected,
        userAvatarUrl = userAvatarUrl,
        hazeState = hazeState,
        modifier = modifier
    )
}

@Composable
internal fun FloatingBottomBarContent(
    groupedTabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    profileTab: BottomTab? = null,
    isProfileSelected: Boolean = false,
    onProfileSelected: () -> Unit = {},
    userAvatarUrl: String? = null,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    val pillShape = remember { RoundedCornerShape(percent = 50) }
    val container = MaterialTheme.colorScheme.surface
    val useGlass = LocalLiquidGlassEnabled.current && isGlassSupported()
    val glassPill = isGlassSupported()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Glass pill motion physics instance from BitChord
    val pill = remember(groupedTabs.size) {
        GlassPillMotion(scope, initialIndex = selectedIndex.coerceAtLeast(0))
    }

    var lastHapticTab by remember { mutableIntStateOf(selectedIndex) }
    var lastSelectedTabIndex by remember { mutableIntStateOf(selectedIndex) }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            if (lastSelectedTabIndex < 0) {
                pill.snapTo(selectedIndex)
            } else {
                pill.animateTo(selectedIndex)
            }
        }
        lastSelectedTabIndex = selectedIndex
        lastHapticTab = selectedIndex
    }

    // Backdrop recorders for bar glass and tab row refraction
    val barGlass = rememberLayerBackdrop()
    val tabRow = rememberLayerBackdrop()
    var rowSize by remember { mutableStateOf(IntSize.Zero) }

    val tabSpacingPx = with(density) { 6.dp.toPx() }
    val tabCount = groupedTabs.size
    val tabWidthPx = if (rowSize.width > 0 && tabCount > 0) {
        (rowSize.width - tabSpacingPx * (tabCount - 1)) / tabCount
    } else 0f
    val tabStepPx = if (rowSize.width > 0 && tabCount > 0) {
        (rowSize.width + tabSpacingPx) / tabCount
    } else 0f
    pill.stepDp = if (density.density > 0f) tabStepPx / density.density else 0f

    val padLeftPx = with(density) { PILL_INSET.toPx() }
    val padTopPx = with(density) { PILL_INSET.toPx() }
    val padBottomPx = with(density) { PILL_INSET.toPx() }
    val rowHeightPx = rowSize.height.toFloat()

    val restSize = Size(tabWidthPx, rowHeightPx)
    val liftedHeightPx = rowHeightPx + padTopPx + padBottomPx + with(density) { PILL_GROW_HEIGHT.toPx() }
    val extraWidthPx = with(density) { 14.dp.toPx() }
    val liftedSize = if (rowHeightPx > 0f) {
        Size(liftedHeightPx * tabWidthPx / rowHeightPx + extraWidthPx, liftedHeightPx)
    } else {
        restSize
    }

    val pillCenterInRow: (Float) -> Offset = { position ->
        val x = position * tabStepPx + tabWidthPx / 2f
        Offset(x, rowHeightPx / 2f)
    }

    val showPill = selectedIndex >= 0 && tabWidthPx > 0f

    Row(
        modifier = modifier
            .testTag("floating_bottom_bar")
            .windowInsetsPadding(floatingBarInsets)
            .padding(horizontal = BAR_GUTTER)
            .padding(bottom = 2.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Grouped Core Tabs Dock
        // Outer wrapper is unclipped so the enlarged/lifted pill can stand taller and wider than the bar!
        Box(
            modifier = Modifier
                .weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            // Bar surface and resting flat pill
            CompositionLocalProvider(LocalGlassExport provides barGlass) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(pillShape)
                        .layerBackdrop(barGlass)
                        .then(
                            if (useGlass && LocalAppBackdrop.current != null) {
                                Modifier.liquidGlass(shape = pillShape)
                            } else if (hazeState != null) {
                                Modifier.optimizedHazeEffect(
                                    state = hazeState,
                                    shape = pillShape,
                                    style = bitChordGlassStyle(container, alpha = 0.20f),
                                )
                            } else {
                                Modifier.background(container.copy(alpha = 0.35f))
                            }
                        )
                        .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, pillShape)
                        .padding(horizontal = PILL_INSET, vertical = PILL_INSET)
                ) {
                    // Flat selection pill (drawn behind tabs when resting/flat)
                    if (showPill && (!glassPill || pill.isFlat)) {
                        FlatSelectionPill(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            modifier = Modifier
                                .matchParentSize()
                                .pillPlacement(
                                    center = { pillCenterInRow(pill.position) },
                                    size = { pill.liveSize(restSize, liftedSize) },
                                ),
                        )
                    }

                    // Tabs Row (recorded into tabRow backdrop so the lifted lens can refract it)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { rowSize = it }
                            .layerBackdrop(tabRow)
                            .pointerInput(tabCount, tabStepPx, selectedIndex) {
                                if (tabStepPx <= 0f) return@pointerInput
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        totalDrag = 0f
                                        pill.startDrag()
                                    },
                                    onDragCancel = {
                                        pill.release(currentSelectedIndex.coerceAtLeast(0))
                                    },
                                    onDragEnd = {
                                        val ratio = totalDrag / tabStepPx
                                        val shift = when {
                                            ratio > 0.35f -> max(1, ratio.roundToInt())
                                            ratio < -0.35f -> min(-1, ratio.roundToInt())
                                            else -> 0
                                        }
                                        val newIndex = (currentSelectedIndex + shift)
                                            .coerceIn(0, groupedTabs.lastIndex)
                                        pill.release(newIndex)
                                        if (newIndex != currentSelectedIndex) {
                                            onTabSelected(newIndex)
                                        }
                                    },
                                    onHorizontalDrag = { _, delta ->
                                        totalDrag += delta
                                        val dragOffset = when {
                                            totalDrag > 0 && currentSelectedIndex == groupedTabs.lastIndex ->
                                                totalDrag * 0.25f
                                            totalDrag < 0 && currentSelectedIndex == 0 ->
                                                totalDrag * 0.25f
                                            else -> totalDrag
                                        }
                                        pill.dragTo(currentSelectedIndex + dragOffset / tabStepPx)
                                        val approxTab = (currentSelectedIndex + dragOffset / tabStepPx)
                                            .coerceIn(0f, groupedTabs.lastIndex.toFloat())
                                            .roundToInt()
                                        if (approxTab != lastHapticTab) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            lastHapticTab = approxTab
                                        }
                                    }
                                )
                            },
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val glassTint = glassContentColor()
                        val adaptiveTint = if (useGlass) glassTint else null

                        groupedTabs.forEachIndexed { index, tab ->
                            val isSelected = index == selectedIndex
                            BottomBarItem(
                                tab = tab,
                                selected = isSelected,
                                onClick = {
                                    pill.animateTo(index)
                                    onTabSelected(index)
                                },
                                onHoldStart = {
                                    pill.startDrag()
                                },
                                onHoldEnd = {
                                    pill.release(index)
                                },
                                selectedTint = adaptiveTint,
                                unselectedTint = adaptiveTint?.copy(alpha = 0.65f),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // 2. The Lifted Glass Lens Pill (Drawn ABOVE the bar and free to stand past the bar's edges)
            // Exactly matching BitChord's held glass expansion physics!
            if (glassPill && showPill && !pill.isFlat) {
                GlassSelectionPill(
                    motion = pill,
                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    barGlass = barGlass,
                    tabRow = tabRow,
                    modifier = Modifier
                        .matchParentSize()
                        .pillPlacement(
                            center = { pillCenterInRow(pill.position) + Offset(padLeftPx, padTopPx) },
                            size = { pill.liveSize(restSize, liftedSize) },
                        ),
                )
            }
        }

        // 3. Standalone Separated Profile Tab (Circular glass button)
        if (profileTab != null) {
            val profBorderColor by animateColorAsState(
                targetValue = if (isProfileSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                } else {
                    GLASS_EDGE_COLOR
                },
                animationSpec = tween(200),
                label = "profileBorder"
            )
            val profBgColor by animateColorAsState(
                targetValue = if (isProfileSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                } else {
                    Color.Transparent
                },
                animationSpec = tween(200),
                label = "profileBg"
            )
            val profTint by animateColorAsState(
                targetValue = if (isProfileSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.White.copy(alpha = 0.70f)
                },
                animationSpec = tween(200),
                label = "profileTint"
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .then(
                        if (useGlass && LocalAppBackdrop.current != null) {
                            Modifier.liquidGlass(shape = CircleShape)
                        } else if (hazeState != null) {
                            Modifier.optimizedHazeEffect(
                                state = hazeState,
                                shape = CircleShape,
                                style = bitChordGlassStyle(container, alpha = 0.20f),
                            )
                        } else {
                            Modifier.background(container.copy(alpha = 0.35f))
                        }
                    )
                    .background(profBgColor)
                    .border(
                        if (isProfileSelected) 1.5.dp else GLASS_EDGE_WIDTH,
                        profBorderColor,
                        CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onProfileSelected()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (userAvatarUrl != null) {
                    AsyncImage(
                        model = userAvatarUrl,
                        contentDescription = profileTab.label,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = profileTab.icon,
                        contentDescription = profileTab.label,
                        tint = profTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * BottomBarItem: individual tab chip inside the floating bar.
 * Matches BitChord's layout:
 * - 25dp glyph icon with 1.08x scale on select
 * - 2dp gap (TAB_ICON_LABEL_GAP)
 * - labelSmall text
 * - 9dp vertical padding (TAB_VERTICAL_PADDING)
 * - Detects touch down to immediately lift & enlarge the glass pill!
 */
@Composable
private fun BottomBarItem(
    tab: BottomTab,
    selected: Boolean,
    onClick: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTint: Color? = null,
    unselectedTint: Color? = null,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = GlassSpring,
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

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onHoldStart()
        } else {
            onHoldEnd()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = interactionSource,
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
                .size(25.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
        Spacer(Modifier.height(TAB_ICON_LABEL_GAP))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
