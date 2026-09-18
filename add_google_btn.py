import re

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "r") as f:
    content = f.read()

replacement = """        val context = androidx.compose.ui.platform.LocalContext.current
        var activityContext: android.content.Context = context
        while (activityContext is android.content.ContextWrapper) {
            if (activityContext is android.app.Activity) break
            activityContext = activityContext.baseContext
        }
        
        // Google Login Button
        AuthOptionButton(
            text = "Continue with Google",
            icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_google_colored),
            enabled = !isAuthLoading
        ) {
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(com.example.R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(activityContext, gso)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Text("""

content = re.sub(
    r"        val context = androidx\.compose\.ui\.platform\.LocalContext\.current.*?activityContext = activityContext\.baseContext\n        \}\s*Text\(",
    replacement,
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "w") as f:
    f.write(content)

