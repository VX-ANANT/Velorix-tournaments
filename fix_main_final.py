import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

# Fix haptic in AlertDialog
text = text.replace("androidx.compose.material3.TextButton(onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); viewModel.clearDbError() })", "androidx.compose.material3.TextButton(onClick = { viewModel.clearDbError() })")
text = text.replace("androidx.compose.material3.TextButton(onClick = { viewModel.clearDbError() })", "androidx.compose.material3.TextButton(onClick = { viewModel.clearDbError() })")

# Fix haptic in Card clickable
text = text.replace("clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); currentNotification = null }", "clickable { currentNotification = null }")
text = text.replace("clickable { currentNotification = null }", "clickable { currentNotification = null }")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
