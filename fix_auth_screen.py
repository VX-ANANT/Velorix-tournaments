import re
with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "r") as f:
    text = f.read()

launcher_code = """    val activityContext = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity ?: return
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.loginWithGoogleToken(idToken, onComplete = onAuthSuccess)
            } else {
                viewModel.setDbError("Google Login Failed: Missing ID Token")
            }
        } catch (e: ApiException) {
            viewModel.setDbError("Google Login Failed: Code ${e.statusCode}")
        } catch (e: Exception) {
            viewModel.setDbError("Google Login Failed: ${e.message}")
        }
    }
"""

def insert_after(text, func_name):
    # Find the start of the function body
    match = re.search(r"fun " + func_name + r".*?\{", text, re.DOTALL)
    if match:
        idx = match.end()
        return text[:idx] + "\n" + launcher_code + text[idx:]
    return text

text = insert_after(text, "LoginScreen")
text = insert_after(text, "RegistrationScreen")

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "w") as f:
    f.write(text)
print("AuthScreen Fixed!")
