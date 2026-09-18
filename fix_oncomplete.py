import re

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "r") as f:
    text = f.read()

text = text.replace(
    "viewModel.login(emailOrPhone, password, method, onComplete = { if (it) onAuthSuccess() })",
    "viewModel.login(emailOrPhone, password, method, onComplete = onAuthSuccess)"
)

text = text.replace(
    "onClick = { viewModel.loginWithGoogle(activityContext, onComplete = { if (it) onAuthSuccess() }) }",
    "onClick = { viewModel.loginWithGoogle(activityContext, onComplete = onAuthSuccess) }"
)

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "w") as f:
    f.write(text)

