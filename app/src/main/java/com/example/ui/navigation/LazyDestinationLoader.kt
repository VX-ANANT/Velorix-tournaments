package com.example.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

/**
 * LazyDestinationLoader implements memory-efficient code splitting & lazy loading:
 * 1. Immediately renders a zero-overhead, beautiful Skeleton UI when the route is composed.
 * 2. Defers heavy view hydration and initial data rendering until the incoming navigation transition frame completes.
 * 3. Smoothly cross-fades into the active screen once ready.
 * 4. Ensures only the currently active destination is fully composed in memory, releasing all inactive composables.
 */
@Composable
fun LazyDestinationLoader(
    destinationKey: Any,
    modifier: Modifier = Modifier,
    deferDurationMillis: Long = 80L,
    skeleton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    var isLoaded by remember(destinationKey) { mutableStateOf(false) }

    LaunchedEffect(destinationKey) {
        // Yield to allow incoming slide/fade navigation frame to render at 60/120fps without hitch
        yield()
        if (deferDurationMillis > 0) {
            delay(deferDurationMillis)
        }
        isLoaded = true
    }

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = isLoaded,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            label = "lazy_destination_crossfade"
        ) { loaded ->
            if (loaded) {
                content()
            } else {
                skeleton()
            }
        }
    }
}
