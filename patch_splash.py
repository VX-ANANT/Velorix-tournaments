with open("app/src/main/java/com/example/ui/screens/SplashScreen.kt", "r") as f:
    text = f.read()

target = """            if (isLoggedIn) {
                onNavigateToHome()
            } else {
                onNavigateToAuth()
            }"""

replacement = """            //if (isLoggedIn) {
                onNavigateToHome()
            //} else {
            //    onNavigateToAuth()
            //}"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/SplashScreen.kt", "w") as f:
    f.write(text)
