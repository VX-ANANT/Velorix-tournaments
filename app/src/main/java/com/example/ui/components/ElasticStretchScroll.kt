package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Adds a responsive, elastic stretch & rubber-band bounce overscroll effect
 * when a user scrolls or drags past the top or bottom edges of any list or scrollable page.
 */
fun Modifier.stretchOverscroll(
    stretchFactor: Float = 0.35f,
    maxStretch: Float = 140f
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    val overscrollOffset = remember { Animatable(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = overscrollOffset.value
                if (current != 0f && source == NestedScrollSource.UserInput) {
                    val isPullingOpposite = (current > 0 && available.y < 0) || (current < 0 && available.y > 0)
                    if (isPullingOpposite) {
                        val newOffset = current + available.y * stretchFactor
                        val consumed = if ((current > 0 && newOffset < 0) || (current < 0 && newOffset > 0)) {
                            coroutineScope.launch { overscrollOffset.snapTo(0f) }
                            -current / stretchFactor
                        } else {
                            coroutineScope.launch { overscrollOffset.snapTo(newOffset.coerceIn(-maxStretch, maxStretch)) }
                            available.y
                        }
                        return Offset(0f, consumed)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    val current = overscrollOffset.value
                    // Rubber-band resistance dampens with displacement distance
                    val resistance = (1f - (abs(current) / (maxStretch * 1.4f))).coerceIn(0.12f, 1f)
                    val delta = available.y * stretchFactor * resistance
                    val target = (current + delta).coerceIn(-maxStretch, maxStretch)
                    coroutineScope.launch {
                        overscrollOffset.snapTo(target)
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscrollOffset.value != 0f) {
                    overscrollOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (overscrollOffset.value != 0f) {
                    overscrollOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                return Velocity.Zero
            }
        }
    }

    this
        .nestedScroll(nestedScrollConnection)
        .graphicsLayer {
            val offset = overscrollOffset.value
            translationY = offset
            val stretchScale = 1f + (abs(offset) / 900f) * 0.12f
            scaleY = stretchScale
            transformOrigin = TransformOrigin(0.5f, if (offset >= 0) 0f else 1f)
        }
}
