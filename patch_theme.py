with open("app/src/main/java/com/example/ui/theme/Theme.kt", "r") as f:
    text = f.read()

light_colors = """private val LightColorScheme = lightColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = Color.White,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = Color.White,
    secondary = md_theme_dark_secondary,
    onSecondary = Color.White,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = Color.White,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1E1E1E),
    surface = Color.White,
    onSurface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFFE9ECEF),
    onSurfaceVariant = Color(0xFF495057),
    outline = Color(0xFFCED4DA)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
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
    }"""

old_theme = """@Composable
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
    }"""

text = text.replace(old_theme, light_colors)

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "w") as f:
    f.write(text)
