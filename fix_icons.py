import re
with open("app/src/main/java/com/example/ui/screens/OnboardingScreen.kt", "r") as f:
    text = f.read()

text = text.replace("Icons.Default.Phone", "androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_phone)")

with open("app/src/main/java/com/example/ui/screens/OnboardingScreen.kt", "w") as f:
    f.write(text)
