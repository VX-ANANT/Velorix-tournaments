with open("app/src/main/java/com/example/ui/theme/Color.kt", "r") as f:
    text = f.read()

# Increase saturation and contrast for lavender theme
text = text.replace("val md_theme_dark_primary = Color(0xFFC4B5FD)", "val md_theme_dark_primary = Color(0xFFA855F7)") # vibrant purple
text = text.replace("val md_theme_dark_onPrimary = Color(0xFF2E1065)", "val md_theme_dark_onPrimary = Color(0xFFFFFFFF)")
text = text.replace("val md_theme_dark_primaryContainer = Color(0xFF4C1D95)", "val md_theme_dark_primaryContainer = Color(0xFF7E22CE)")
text = text.replace("val md_theme_dark_onPrimaryContainer = Color(0xFFEDE9FE)", "val md_theme_dark_onPrimaryContainer = Color(0xFFF3E8FF)")
text = text.replace("val md_theme_dark_secondary = Color(0xFFF0ABFC)", "val md_theme_dark_secondary = Color(0xFFD946EF)") # vibrant fuchsia
text = text.replace("val md_theme_dark_onSecondary = Color(0xFF4A044E)", "val md_theme_dark_onSecondary = Color(0xFFFFFFFF)")
text = text.replace("val md_theme_dark_secondaryContainer = Color(0xFF701A75)", "val md_theme_dark_secondaryContainer = Color(0xFFA21CAF)")
text = text.replace("val md_theme_dark_onSecondaryContainer = Color(0xFFFAE8FF)", "val md_theme_dark_onSecondaryContainer = Color(0xFFFAEAFF)")
text = text.replace("val md_theme_dark_background = Color(0xFF09090B)", "val md_theme_dark_background = Color(0xFF000000)") # amoled black
text = text.replace("val md_theme_dark_surface = Color(0xFF18181B)", "val md_theme_dark_surface = Color(0xFF09090B)") # darker surface
text = text.replace("val md_theme_dark_surfaceVariant = Color(0xFF27272A)", "val md_theme_dark_surfaceVariant = Color(0xFF18181B)")

with open("app/src/main/java/com/example/ui/theme/Color.kt", "w") as f:
    f.write(text)
