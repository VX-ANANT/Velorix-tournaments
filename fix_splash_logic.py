import re

with open("app/src/main/java/com/example/ui/screens/SplashScreen.kt", "r") as f:
    text = f.read()

text = text.replace("//if (isLoggedIn) {", "if (isLoggedIn) {")
text = text.replace("//} else {", "} else {")
text = text.replace("//    onNavigateToAuth()", "    onNavigateToAuth()")
text = text.replace("//}", "}")

with open("app/src/main/java/com/example/ui/screens/SplashScreen.kt", "w") as f:
    f.write(text)

