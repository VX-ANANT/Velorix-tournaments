package com.example.ui.screens

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RawRes
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing

import androidx.compose.material3.MaterialTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import com.example.ui.theme.get180DegreeAdaptiveColor
import com.example.ui.theme.get180DegreeAdaptiveMutedColor


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.LegalComplianceModal
import com.example.ui.components.LegalTab
import com.example.ui.viewmodel.PlatformViewModel

/**
 * High-performance hardware-accelerated video background with continuous loop
 * and non-distorted center-crop scaling for seamless presentation.
 */
@Composable
fun AuthVideoBackground(
    @RawRes rawResId: Int = com.example.R.raw.auth_bg_video,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (_: Throwable) {}
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val tv = TextureView(ctx)
                tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        try {
                            mediaPlayer?.release()
                            val mp = MediaPlayer().apply {
                                val afd = ctx.resources.openRawResourceFd(rawResId)
                                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                afd.close()
                                setSurface(Surface(surfaceTexture))
                                isLooping = true
                                setVolume(0f, 0f) // Muted continuous loop
                                setOnVideoSizeChangedListener { _, vWidth, vHeight ->
                                    adjustVideoAspectRatio(tv, vWidth, vHeight, width, height)
                                }
                                setOnPreparedListener { mpInstance ->
                                    adjustVideoAspectRatio(tv, mpInstance.videoWidth, mpInstance.videoHeight, width, height)
                                    mpInstance.start()
                                }
                                prepareAsync()
                            }
                            mediaPlayer = mp
                        } catch (e: Throwable) {
                            android.util.Log.e("AuthVideoBg", "Error preparing background video: ${e.message}")
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                        mediaPlayer?.let { mp ->
                            adjustVideoAspectRatio(tv, mp.videoWidth, mp.videoHeight, width, height)
                        }
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        try {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null
                        } catch (_: Throwable) {}
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
                tv
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top subtle dark vignette for status bar readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xD006080F),
                            Color(0x6006080F),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom smooth uncovered gradient fade (revealing video above, blending into dark background below)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.25f to Color.Transparent,
                        0.45f to Color(0x5506080F),
                        0.65f to Color(0xBB06080F),
                        0.85f to Color(0xF506080F),
                        1.00f to Color(0xFF06080F)
                    )
                )
        )
    }
}

private fun adjustVideoAspectRatio(
    textureView: TextureView,
    videoWidth: Int,
    videoHeight: Int,
    viewWidth: Int,
    viewHeight: Int
) {
    if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return
    val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
    val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
    val scaleX: Float
    val scaleY: Float
    if (viewRatio > videoRatio) {
        scaleX = 1f
        scaleY = (viewWidth.toFloat() / videoWidth.toFloat()) / (viewHeight.toFloat() / videoHeight.toFloat())
    } else {
        scaleX = (viewHeight.toFloat() / videoHeight.toFloat()) / (viewWidth.toFloat() / videoWidth.toFloat())
        scaleY = 1f
    }
    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}

@Composable
fun AuthScreen(
    viewModel: PlatformViewModel,
    onAuthSuccess: () -> Unit,
    onRegisterSuccess: (() -> Unit)? = null
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var showLegalModal by remember { mutableStateOf(false) }
    var selectedLegalTab by remember { mutableStateOf(LegalTab.TERMS) }

    val scrollState = rememberScrollState()
    val authBgColor = Color(0xFF06080F)
    val hazeState = remember { HazeState() }

    // Dynamic 180° Complementary Adaptive Color Engine (Live opposite color to background)
    val adaptiveTextColor = remember(authBgColor) { get180DegreeAdaptiveColor(authBgColor) }
    val adaptiveMutedTextColor = remember(authBgColor) { get180DegreeAdaptiveMutedColor(authBgColor) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(authBgColor)
    ) {
        // 1. Uncovered Video Background (spans upper portion of screen, smoothly uncovered with bottom gradient)
        AuthVideoBackground(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.68f)
                .align(Alignment.TopCenter)
                .haze(state = hazeState)
        )

        // 2. Foreground Full-Screen Scrollable Content (No window/card container - completely full-screen elements)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Top spacing so the animated 3D video is uncovered & prominently visible
                Spacer(modifier = Modifier.height(20.dp))

                // Prominent Velorix Logo (Bada sa dikhe as explicitly requested)
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.velorix_logo_image),
                    contentDescription = "Velorix Logo",
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(130.dp)
                        .padding(bottom = 6.dp)
                )

                // Clean Subtitle - "Sign in with" / "Register with to continue" (Adaptive 180° opposite color)
                Text(
                    text = if (isSignUpMode) "Register with to continue" else "Sign in with to continue",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = adaptiveTextColor.copy(alpha = 0.95f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.padding(bottom = 28.dp)
                )

                // Direct Full-Screen Layout for Login / Registration (No card/window wrapper)
                if (isSignUpMode) {
                    RegistrationScreen(
                        viewModel = viewModel,
                        hazeState = hazeState,
                        onAuthSuccess = {
                            if (onRegisterSuccess != null) {
                                onRegisterSuccess()
                            } else {
                                onAuthSuccess()
                            }
                        },
                        onSwitchToLogin = { isSignUpMode = false },
                        onOpenLegal = { tab ->
                            selectedLegalTab = tab
                            showLegalModal = true
                        },
                        textColor = adaptiveTextColor,
                        mutedTextColor = adaptiveMutedTextColor
                    )
                } else {
                    LoginScreen(
                        viewModel = viewModel,
                        hazeState = hazeState,
                        onAuthSuccess = onAuthSuccess,
                        onSwitchToSignUp = { isSignUpMode = true },
                        onOpenLegal = { tab ->
                            selectedLegalTab = tab
                            showLegalModal = true
                        },
                        textColor = adaptiveTextColor,
                        mutedTextColor = adaptiveMutedTextColor
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showLegalModal) {
        LegalComplianceModal(
            initialTab = selectedLegalTab,
            onDismissRequest = { showLegalModal = false }
        )
    }
}

/**
 * Standalone Liquid Glass effect for individual text input boxes matching the bottom nav bar.
 * Applies the frosted glass blur via Haze, translucent obsidian gradient, specular rim border,
 * lens glare reflection, and depth shadow directly to the exact edges of the text box itself.
 */
@Composable
fun Modifier.glassTextBox(
    hazeState: HazeState? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val specularRim = Brush.verticalGradient(
        0.0f to Color(0xFF38BDF8).copy(alpha = if (isFocused) 0.90f else 0.55f),
        0.20f to Color.White.copy(alpha = if (isFocused) 0.65f else 0.35f),
        0.75f to Color(0xFFF43F5E).copy(alpha = if (isFocused) 0.35f else 0.15f),
        1.0f to Color.White.copy(alpha = if (isFocused) 0.70f else 0.40f)
    )

    return this
        .shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.30f),
            spotColor = Color.Black.copy(alpha = 0.45f)
        )
        .clip(shape)
        .then(
            if (hazeState != null) {
                Modifier.hazeChild(
                    state = hazeState,
                    shape = shape
                )
            } else {
                Modifier
            }
        )
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x350E121E), // ~21% translucent obsidian glass matching bottom nav bar
                    Color(0x55090C16)  // ~33% translucent deep glass tint matching bottom nav bar
                )
            )
        )
        .drawBehind {
            // Upper curvature glare & lens refraction highlight
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.14f),
                    0.50f to Color.White.copy(alpha = 0.03f),
                    1.0f to Color.Transparent
                ),
                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.50f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
            )
        }
        .border(
            width = if (isFocused) 1.5.dp else 1.dp,
            brush = specularRim,
            shape = shape
        )
        .onFocusChanged { isFocused = it.isFocused }
}

@Composable
fun LoginScreen(
    viewModel: PlatformViewModel,
    hazeState: HazeState? = null,
    onAuthSuccess: () -> Unit,
    onSwitchToSignUp: () -> Unit,
    onOpenLegal: (LegalTab) -> Unit = {},
    textColor: Color = Color.White,
    mutedTextColor: Color = Color(0xFFCBD5E1)
) {
    val isAuthLoading by viewModel.isAuthLoading.collectAsState(initial = false)

    
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
                viewModel.showError("Google Login Failed: Missing ID Token")
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                12501 -> {
                    // User cancelled Google Sign-In prompt
                    android.util.Log.d("AuthScreen", "Google Sign-In cancelled by user")
                }
                12502 -> {
                    // Sign-in currently in progress
                    viewModel.showError("Google Sign-In session busy. Please tap again.")
                }
                else -> {
                    viewModel.showError("Google Login Failed: Code ${e.statusCode}")
                }
            }
        } catch (e: Exception) {
            viewModel.showError("Google Login Failed: ${e.message}")
        }
    }
    // isAuthLoading is already defined in RegistrationScreen
    

    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf("") }
        var resetStatus by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Enter your email address to receive a password reset link.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    if (resetStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(resetStatus, color = if (resetStatus.contains("sent", true)) Color.Green else Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetPassword(resetEmail) { success, message ->
                        resetStatus = message
                    }
                }) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = emailOrPhone,
            onValueChange = { emailOrPhone = it },
            modifier = Modifier
                .fillMaxWidth()
                .glassTextBox(hazeState = hazeState)
                .testTag("email_input"),
            label = { Text("Email or Phone", color = mutedTextColor) },
            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_profile), contentDescription = null, tint = textColor) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = textColor,
                unfocusedLabelColor = mutedTextColor
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .glassTextBox(hazeState = hazeState)
                .testTag("password_input"),
            label = { Text("Password", color = mutedTextColor) },
            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_lock), contentDescription = null, tint = textColor) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = textColor,
                unfocusedLabelColor = mutedTextColor
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Forgot Password?",
            style = MaterialTheme.typography.bodyMedium.copy(color = textColor.copy(alpha = 0.90f), fontWeight = FontWeight.Normal),
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 20.dp, end = 4.dp)
                .clickable { showForgotPasswordDialog = true }
        )

        // Submit Button - Solid White with Black text
        Button(
            onClick = {
                val method = if (emailOrPhone.contains("@")) "email" else "phone"
                viewModel.login(emailOrPhone, password, method, onComplete = onAuthSuccess)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("submit_login_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            enabled = !isAuthLoading
        ) {
            if (isAuthLoading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text("Sign In", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // OR Divider
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.22f))
            Text("  OR  ", color = mutedTextColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.22f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        val context = androidx.compose.ui.platform.LocalContext.current
        var activityContext: android.content.Context = context
        while (activityContext is android.content.ContextWrapper) {
            if (activityContext is android.app.Activity) break
            activityContext = activityContext.baseContext
        }
        
        // Google Sign-In with 2025 Google Favicon
        AuthOptionButton(
            text = "Sign in with Google",
            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.google_favicon_2025),
            enabled = !isAuthLoading
        ) {
            try {
                val webClientId = context.getString(com.example.R.string.default_web_client_id)
                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(activityContext, gso)
                try {
                    googleSignInClient.signOut().addOnCompleteListener {
                        try {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        } catch (e: Exception) {
                            viewModel.showError("Google Sign In Error: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                }
            } catch (e: Exception) {
                viewModel.showError("Google Sign In Error: ${e.message}")
            }
        }
        
        AuthLegalConsentFootnote(onOpenLegal = onOpenLegal)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Don't have an account? Register with to continue",
            style = MaterialTheme.typography.bodyMedium.copy(color = textColor, fontWeight = FontWeight.Bold),
            modifier = Modifier.clickable { onSwitchToSignUp() }.padding(8.dp).align(Alignment.CenterHorizontally).testTag("toggle_auth_mode")
        )
    }
}

@Composable
fun RegistrationScreen(
    viewModel: PlatformViewModel,
    hazeState: HazeState? = null,
    onAuthSuccess: () -> Unit,
    onSwitchToLogin: () -> Unit,
    onOpenLegal: (LegalTab) -> Unit = {},
    textColor: Color = Color.White,
    mutedTextColor: Color = Color(0xFFCBD5E1)
) {
    val isAuthLoading by viewModel.isAuthLoading.collectAsState(initial = false)

    
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
                viewModel.showError("Google Login Failed: Missing ID Token")
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                12501 -> {
                    // User cancelled Google Sign-In prompt
                    android.util.Log.d("AuthScreen", "Google Sign-In cancelled by user")
                }
                12502 -> {
                    // Sign-in currently in progress
                    viewModel.showError("Google Sign-In session busy. Please tap again.")
                }
                else -> {
                    viewModel.showError("Google Login Failed: Code ${e.statusCode}")
                }
            }
        } catch (e: Exception) {
            viewModel.showError("Google Login Failed: ${e.message}")
        }
    }
    // isAuthLoading is already defined in RegistrationScreen
    

    var username by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var isAgeConfirmed by remember { mutableStateOf(false) }
    var isStateCompliant by remember { mutableStateOf(true) }
    var isTermsAccepted by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier
                .fillMaxWidth()
                .glassTextBox(hazeState = hazeState)
                .testTag("reg_username_input"),
            label = { Text("Username", color = mutedTextColor) },
            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_profile), contentDescription = null, tint = textColor) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = textColor,
                unfocusedLabelColor = mutedTextColor
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = emailOrPhone,
            onValueChange = { emailOrPhone = it },
            modifier = Modifier
                .fillMaxWidth()
                .glassTextBox(hazeState = hazeState)
                .testTag("reg_email_input"),
            label = { Text("Email or Phone", color = mutedTextColor) },
            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_mail), contentDescription = null, tint = textColor) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = textColor,
                unfocusedLabelColor = mutedTextColor
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .glassTextBox(hazeState = hazeState)
                .testTag("reg_password_input"),
            label = { Text("Password", color = mutedTextColor) },
            leadingIcon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(com.example.R.drawable.ic_iconsax_lock), contentDescription = null, tint = textColor) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = textColor,
                unfocusedLabelColor = mutedTextColor
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = referralCode,
            onValueChange = { referralCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() } },
            modifier = Modifier
                .fillMaxWidth()
                .glassTextBox(hazeState = hazeState)
                .testTag("reg_referral_input"),
            label = { Text("Referral Code (Optional)", color = mutedTextColor) },
            placeholder = { Text("e.g. VRX-NAME-9999", color = mutedTextColor.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = textColor) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = textColor,
                unfocusedLabelColor = mutedTextColor
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Statutory & COPPA Compliance Gate (Full-screen clean layout, no heavy window container)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            // Mandatory Age Gate (18+ / COPPA Compliance)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAgeConfirmed = !isAgeConfirmed }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = isAgeConfirmed,
                    onCheckedChange = { isAgeConfirmed = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF22C55E),
                        uncheckedColor = Color(0xFFEF4444)
                    )
                )
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "I confirm I am 18 years of age or older",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "Mandatory statutory verification for skill-based tournaments (COPPA & IT Rules).",
                        fontSize = 10.sp,
                        color = mutedTextColor
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // State Skill-Gaming Jurisdiction Warranty
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isStateCompliant = !isStateCompliant }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = isStateCompliant,
                    onCheckedChange = { isStateCompliant = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8))
                )
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "I am not a resident of restricted states",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = "Assam, Odisha, Telangana, Nagaland, Sikkim, Andhra Pradesh.",
                        fontSize = 10.sp,
                        color = mutedTextColor
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Terms of Service & Privacy & DMCA Safe Harbor
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isTermsAccepted = !isTermsAccepted }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = isTermsAccepted,
                    onCheckedChange = { isTermsAccepted = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "I agree to Terms of Service, Privacy Policy & Safe Harbor IP guidelines.",
                    fontSize = 11.sp,
                    color = mutedTextColor
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "Terms of Service",
                    fontSize = 10.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onOpenLegal(LegalTab.TERMS) }
                )
                Text("•", fontSize = 10.sp, color = mutedTextColor)
                Text(
                    text = "Privacy Policy",
                    fontSize = 10.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onOpenLegal(LegalTab.PRIVACY) }
                )
                Text("•", fontSize = 10.sp, color = mutedTextColor)
                Text(
                    text = "Refunds & Escrow",
                    fontSize = 10.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onOpenLegal(LegalTab.REFUNDS) }
                )
            }
        }

        // Submit Button - Solid White with Black Text
        Button(
            onClick = {
                if (!isAgeConfirmed) {
                    viewModel.showError("Age Verification Required: You must certify you are 18+ to create an account.")
                    return@Button
                }
                if (!isStateCompliant) {
                    viewModel.showError("Jurisdiction Notice: Real-money contests are restricted in your state.")
                    return@Button
                }
                if (!isTermsAccepted) {
                    viewModel.showError("Please accept Terms of Service & Privacy Policy to continue.")
                    return@Button
                }
                val method = if (emailOrPhone.contains("@")) "email" else "phone"
                viewModel.register(
                    username = username,
                    phoneOrEmail = emailOrPhone,
                    passwordHash = password,
                    loginMethod = method,
                    referralCode = referralCode,
                    onComplete = { if (it) onAuthSuccess() }
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("submit_register_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            enabled = !isAuthLoading && isAgeConfirmed && isStateCompliant && isTermsAccepted
        ) {
            if (isAuthLoading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text("Create Account (18+ Verified)", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // OR Divider
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.22f))
            Text("  OR  ", color = mutedTextColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.22f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        val context = androidx.compose.ui.platform.LocalContext.current
        var activityContext: android.content.Context = context
        while (activityContext is android.content.ContextWrapper) {
            if (activityContext is android.app.Activity) break
            activityContext = activityContext.baseContext
        }
        
        // Google Sign-In Button with 2025 Google Favicon
        AuthOptionButton(
            text = "Register with Google",
            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.google_favicon_2025),
            enabled = !isAuthLoading
        ) {
            try {
                val webClientId = context.getString(com.example.R.string.default_web_client_id)
                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(activityContext, gso)
                try {
                    googleSignInClient.signOut().addOnCompleteListener {
                        try {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        } catch (e: Exception) {
                            viewModel.showError("Google Sign In Error: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                }
            } catch (e: Exception) {
                viewModel.showError("Google Sign In Error: ${e.message}")
            }
        }
        
        AuthLegalConsentFootnote(onOpenLegal = onOpenLegal)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Already have an account? Sign in with to continue",
            style = MaterialTheme.typography.bodyMedium.copy(color = textColor, fontWeight = FontWeight.Bold),
            modifier = Modifier.clickable { onSwitchToLogin() }.padding(8.dp).align(Alignment.CenterHorizontally).testTag("toggle_auth_mode")
        )
    }
}

@Composable
fun AuthLegalConsentFootnote(
    onOpenLegal: (LegalTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_untitledui_shield_tick),
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Skill Gaming • 18+ Only • DPDP Compliant",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF38BDF8)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "By continuing, you accept ",
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1)
            )
            Text(
                text = "Terms",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                modifier = Modifier.clickable { onOpenLegal(LegalTab.TERMS) }
            )
            Text(
                text = " • ",
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1)
            )
            Text(
                text = "Privacy",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                modifier = Modifier.clickable { onOpenLegal(LegalTab.PRIVACY) }
            )
            Text(
                text = " • ",
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1)
            )
            Text(
                text = "Fair Play",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                modifier = Modifier.clickable { onOpenLegal(LegalTab.FAIR_PLAY) }
            )
        }
    }
}

@Composable
fun AuthOptionButton(
    text: String,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("google_login_button"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0x33000000),
            contentColor = Color.White
        )
    ) {
        if (painter != null) {
            Image(painter = painter, contentDescription = null, modifier = Modifier.size(22.dp))
        } else if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp))
    }
}


