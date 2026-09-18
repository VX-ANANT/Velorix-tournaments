with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "r") as f:
    text = f.read()

bad_ending = """    }
        com.example.ui.components.ConfettiAnimation(isVisible = showConfetti, modifier = Modifier.fillMaxSize())
    }
}"""
if bad_ending in text:
    text = text.replace(bad_ending, "    }\n}")

# Now find where ProfileScreen ends and FaqSection begins.
target = """    }
}

@Composable
fun FaqSection() {"""

replacement = """    }
        com.example.ui.components.ConfettiAnimation(isVisible = showConfetti, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun FaqSection() {"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "w") as f:
    f.write(text)
