with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "r") as f:
    text = f.read()

text = text.replace("import androidx.compose.foundation.background", "import androidx.compose.foundation.background\nimport androidx.compose.foundation.border")

with open("app/src/main/java/com/example/ui/components/TournamentCard.kt", "w") as f:
    f.write(text)
