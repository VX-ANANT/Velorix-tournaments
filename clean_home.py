import re

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    text = f.read()

# Remove avatar scale animation
text = re.sub(r'val infiniteTransition.*?label = "avatarScale"\n\s*\)', '', text, flags=re.DOTALL)
text = text.replace(".graphicsLayer { scaleX = avatarScale; scaleY = avatarScale }", "")
# Simplify avatar background
text = re.sub(r'Brush\.linearGradient\(.*?\)', 'MaterialTheme.colorScheme.surfaceVariant', text, flags=re.DOTALL)

# Simplify shape of Balance Pill
text = text.replace("RoundedCornerShape(24.dp)", "RoundedCornerShape(8.dp)")
text = text.replace("RoundedCornerShape(20.dp)", "RoundedCornerShape(8.dp)") # for the search bar

# Change "READY FOR BATTLE" to "Active"
text = text.replace('"READY FOR BATTLE"', '"Active"')
text = text.replace('letterSpacing = 1.sp', 'letterSpacing = 0.5.sp')

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(text)

