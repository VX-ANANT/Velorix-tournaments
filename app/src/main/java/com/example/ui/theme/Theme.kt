package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = vercel_dark_primary,
    onPrimary = vercel_dark_onPrimary,
    primaryContainer = vercel_dark_primaryContainer,
    onPrimaryContainer = vercel_dark_onPrimaryContainer,
    secondary = vercel_dark_secondary,
    onSecondary = vercel_dark_onSecondary,
    secondaryContainer = vercel_dark_secondaryContainer,
    onSecondaryContainer = vercel_dark_onSecondaryContainer,
    tertiary = vercel_dark_tertiary,
    onTertiary = vercel_dark_onTertiary,
    error = vercel_dark_error,
    onError = vercel_dark_onError,
    background = vercel_dark_background,
    onBackground = vercel_dark_onBackground,
    surface = vercel_dark_surface,
    onSurface = vercel_dark_onSurface,
    surfaceVariant = vercel_dark_surfaceVariant,
    onSurfaceVariant = vercel_dark_onSurfaceVariant,
    outline = vercel_dark_outline
)

private val LightColorScheme = lightColorScheme(
    primary = vercel_light_primary,
    onPrimary = vercel_light_onPrimary,
    primaryContainer = vercel_light_primaryContainer,
    onPrimaryContainer = vercel_light_onPrimaryContainer,
    secondary = vercel_light_secondary,
    onSecondary = vercel_light_onSecondary,
    secondaryContainer = vercel_light_secondaryContainer,
    onSecondaryContainer = vercel_light_onSecondaryContainer,
    background = vercel_light_background,
    onBackground = vercel_light_onBackground,
    surface = vercel_light_surface,
    onSurface = vercel_light_onSurface,
    surfaceVariant = vercel_light_surfaceVariant,
    onSurfaceVariant = vercel_light_onSurfaceVariant,
    outline = vercel_light_outline
)

@Composable
fun animateColorScheme(target: ColorScheme): ColorScheme {
    val animSpec = tween<Color>(durationMillis = 400, easing = FastOutSlowInEasing)
    return target.copy(
        primary = animateColorAsState(target.primary, animSpec, label = "primary").value,
        onPrimary = animateColorAsState(target.onPrimary, animSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(target.primaryContainer, animSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, animSpec, label = "onPrimaryContainer").value,
        secondary = animateColorAsState(target.secondary, animSpec, label = "secondary").value,
        onSecondary = animateColorAsState(target.onSecondary, animSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, animSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, animSpec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(target.tertiary, animSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(target.onTertiary, animSpec, label = "onTertiary").value,
        error = animateColorAsState(target.error, animSpec, label = "error").value,
        onError = animateColorAsState(target.onError, animSpec, label = "onError").value,
        background = animateColorAsState(target.background, animSpec, label = "background").value,
        onBackground = animateColorAsState(target.onBackground, animSpec, label = "onBackground").value,
        surface = animateColorAsState(target.surface, animSpec, label = "surface").value,
        onSurface = animateColorAsState(target.onSurface, animSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, animSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, animSpec, label = "onSurfaceVariant").value,
        outline = animateColorAsState(target.outline, animSpec, label = "outline").value
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val targetColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val animatedColorScheme = animateColorScheme(targetColorScheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = Typography,
        shapes = Shapes
    ) {
        CompositionLocalProvider(
            LocalOverscrollConfiguration provides OverscrollConfiguration(
                glowColor = animatedColorScheme.primary.copy(alpha = 0.25f),
                drawPadding = PaddingValues(0.dp)
            )
        ) {
            content()
        }
    }
}
