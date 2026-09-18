import re
with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "r") as f:
    text = f.read()

# Add imports
imports = """import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
"""
text = text.replace("import androidx.compose.material3.MaterialTheme", "import androidx.compose.material3.MaterialTheme\n" + imports)

# Find the start of LoginSection
target_login = """@Composable
fun LoginSection(
    viewModel: PlatformViewModel,
    onNavigateToSignup: () -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {"""

replacement_login = """@Composable
fun LoginSection(
    viewModel: PlatformViewModel,
    onNavigateToSignup: () -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
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

text = text.replace(target_login, replacement_login)


# Find the start of SignupSection
target_signup = """@Composable
fun SignupSection(
    viewModel: PlatformViewModel,
    onNavigateToLogin: () -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {"""

replacement_signup = """@Composable
fun SignupSection(
    viewModel: PlatformViewModel,
    onNavigateToLogin: () -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
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

text = text.replace(target_signup, replacement_signup)


# Replace the button clicks
btn_target_login = """        AuthOptionButton(
            text = "Continue with Google",
            icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_mail),
            onClick = { viewModel.loginWithGoogle(activityContext, onComplete = onAuthSuccess) }
        )"""

btn_replacement_login = """        AuthOptionButton(
            text = "Continue with Google",
            icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_mail),
            onClick = { 
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("27931798964-h6fiau2df3i6e3049fontnctpsh06763.apps.googleusercontent.com")
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(activityContext, gso)
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        )"""

text = text.replace(btn_target_login, btn_replacement_login)


btn_target_signup = """        AuthOptionButton(
            text = "Sign up with Google",
            icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_mail),
            onClick = { viewModel.loginWithGoogle(activityContext, onComplete = onAuthSuccess) }
        )"""

btn_replacement_signup = """        AuthOptionButton(
            text = "Sign up with Google",
            icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_mail),
            onClick = { 
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("27931798964-h6fiau2df3i6e3049fontnctpsh06763.apps.googleusercontent.com")
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(activityContext, gso)
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        )"""

text = text.replace(btn_target_signup, btn_replacement_signup)

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "w") as f:
    f.write(text)
print("AuthScreen Patched!")
