import re
with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "r") as f:
    text = f.read()

text = text.replace("Icons.Default.Person", "androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_profile)")
text = text.replace("Icons.Default.Lock", "androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_lock)")
text = text.replace("Icons.Default.Email", "androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_mail)")

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "w") as f:
    f.write(text)
