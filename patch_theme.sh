cat << 'INNER' > app/src/main/java/com/example/ui/theme/Color.kt
package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val md_theme_dark_primary = Color(0xFF8B5CF6) // Vibrant Violet
val md_theme_dark_onPrimary = Color(0xFFFFFFFF)
val md_theme_dark_primaryContainer = Color(0xFF6D28D9)
val md_theme_dark_onPrimaryContainer = Color(0xFFEEDDF7)

val md_theme_dark_secondary = Color(0xFF06B6D4) // Cyan
val md_theme_dark_onSecondary = Color(0xFFFFFFFF)
val md_theme_dark_secondaryContainer = Color(0xFF0891B2)
val md_theme_dark_onSecondaryContainer = Color(0xFFCFFAFE)

val md_theme_dark_tertiary = Color(0xFFF43F5E) // Rose
val md_theme_dark_onTertiary = Color(0xFFFFFFFF)

val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_background = Color(0xFF09090B) // Zinc 950
val md_theme_dark_onBackground = Color(0xFFF4F4F5)
val md_theme_dark_surface = Color(0xFF18181B) // Zinc 900
val md_theme_dark_onSurface = Color(0xFFF4F4F5)
val md_theme_dark_surfaceVariant = Color(0xFF27272A) // Zinc 800
val md_theme_dark_onSurfaceVariant = Color(0xFFA1A1AA)
val md_theme_dark_outline = Color(0xFF3F3F46) // Zinc 700
INNER

cat << 'INNER' > app/src/main/java/com/example/ui/theme/Theme.kt
package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for gaming app vibes
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
INNER
