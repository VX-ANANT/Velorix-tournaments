import re
with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "r") as f:
    text = f.read()

target_login = """fun LoginScreen(
    viewModel: PlatformViewModel,
    onNavigateToSignup: () -> Unit,
    onAuthSuccess: () -> Unit
) {"""

replacement_login = """fun LoginScreen(
    viewModel: PlatformViewModel,
    onNavigateToSignup: () -> Unit,
    onAuthSuccess: () -> Unit
) {
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
                // error
            }
        } catch (e: Exception) {
            // error
        }
    }
"""

text = text.replace(target_login, replacement_login)


target_reg = """fun RegistrationScreen(
    viewModel: PlatformViewModel,
    onNavigateToLogin: () -> Unit,
    onAuthSuccess: () -> Unit
) {"""

replacement_reg = """fun RegistrationScreen(
    viewModel: PlatformViewModel,
    onNavigateToLogin: () -> Unit,
    onAuthSuccess: () -> Unit
) {
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
                // error
            }
        } catch (e: Exception) {
            // error
        }
    }
"""

text = text.replace(target_reg, replacement_reg)

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "w") as f:
    f.write(text)
print("AuthScreen patched launchers!")
