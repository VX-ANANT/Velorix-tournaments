import os

files = [
    "app/src/main/java/com/example/MainActivity.kt",
    "app/src/main/java/com/example/ui/screens/HomeScreen.kt",
    "app/src/main/java/com/example/ui/screens/SplashScreen.kt"
]

for file in files:
    with open(file, "r") as f:
        text = f.read()
    
    text = text.replace("import androidx.compose.ui.draw.scale\n", "")
    
    text = text.replace(".scale(scale1)", ".graphicsLayer { scaleX = scale1; scaleY = scale1 }")
    text = text.replace(".scale(scale2)", ".graphicsLayer { scaleX = scale2; scaleY = scale2 }")
    text = text.replace(".scale(scale3)", ".graphicsLayer { scaleX = scale3; scaleY = scale3 }")
    text = text.replace(".scale(avatarScale)", ".graphicsLayer { scaleX = avatarScale; scaleY = avatarScale }")
    text = text.replace(".scale(pulseScale)", ".graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }")
    
    with open(file, "w") as f:
        f.write(text)

