import re
with open("app/src/main/java/com/example/ui/screens/MatchesScreen.kt", "r") as f:
    text = f.read()

# Add a closing brace before `@Composable\nfun UpcomingJoinedRow`
target = """            }
        }
    }
}

@Composable
fun UpcomingJoinedRow"""

replacement = """            }
        }
        }
    }
}

@Composable
fun UpcomingJoinedRow"""

text = text.replace(target, replacement)

# Replace the inner weight(1f)
text = text.replace("modifier = Modifier.weight(1f),", "modifier = Modifier.fillMaxSize(),")
text = text.replace("modifier = Modifier.weight(1f).fillMaxWidth(),", "modifier = Modifier.fillMaxSize(),")

with open("app/src/main/java/com/example/ui/screens/MatchesScreen.kt", "w") as f:
    f.write(text)
print("Fixed braces")
