import re

with open("app/src/main/java/com/example/ui/components/ConfettiAnimation.kt", "r") as f:
    text = f.read()

text = text.replace("com.example.R.raw.confetti", "com.example.R.raw.confetti") # Actually just needs full build or import R

with open("app/src/main/java/com/example/ui/components/ConfettiAnimation.kt", "w") as f:
    f.write(text)

