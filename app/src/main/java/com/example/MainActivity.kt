package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.togetherWith
import androidx.navigation.compose.*
import androidx.navigation.navDeepLink
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.PlatformRepository
import com.example.data.model.SituationPreviewType
import com.example.data.model.SystemAppConfig
import com.example.ui.screens.*
import com.example.ui.screens.situations.*
import com.example.ui.theme.MyApplicationTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.example.ui.theme.CyberpunkYellow
import com.example.ui.viewmodel.PlatformViewModel
import com.entrig.sdk.Entrig
import com.entrig.sdk.models.EntrigConfig

class MainActivity : ComponentActivity() {
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        try {
            Entrig.onRequestPermissionsResult(requestCode, grantResults)
        } catch(e: Throwable) {
            android.util.Log.e("Entrig", "Entrig onRequestPermissionsResult failed", e)
        }
    }
    private fun enforceHighRefreshRate() {
        try {
            // 1. Get default display reliably across all Android OS versions
            val currentDisplay: android.view.Display? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                this.display ?: (getSystemService(android.content.Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager)?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }

            // 2. Select maximum supported display refresh rate mode (120Hz / 144Hz / 90Hz) using public Android APIs
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && currentDisplay != null) {
                val supportedModes = currentDisplay.supportedModes
                val maxRefreshMode = supportedModes.filter { it.refreshRate >= 89f }.maxByOrNull { it.refreshRate }
                    ?: supportedModes.maxByOrNull { it.refreshRate }

                val targetRate = maxRefreshMode?.refreshRate ?: 120f
                val params = window.attributes
                if (maxRefreshMode != null) {
                    params.preferredDisplayModeId = maxRefreshMode.modeId
                }
                params.preferredRefreshRate = targetRate
                window.attributes = params
            }
        } catch (e: Throwable) {
            android.util.Log.d("HighRefreshRate", "120 FPS configuration handled: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        enforceHighRefreshRate()
    }

    override fun onStart() {
        super.onStart()
        enforceHighRefreshRate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        enforceHighRefreshRate()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enforceHighRefreshRate()
        }
    }

    @OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        
        val isEmulator = EnvUtils.isEmu()
        try {
            if (!isEmulator) {
                Entrig.setOnForegroundNotificationListener { notification ->
                    android.util.Log.d("Entrig", "Foreground notification: ${notification.title}")
                    // Manually show the notification in foreground to ensure it appears in the emulator
                    val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    val channelId = "entrig_test_channel"
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val channel = android.app.NotificationChannel(channelId, "Test Channel", android.app.NotificationManager.IMPORTANCE_HIGH)
                        notificationManager.createNotificationChannel(channel)
                    }
                    val builder = androidx.core.app.NotificationCompat.Builder(this@MainActivity, channelId)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(notification.title)
                        .setContentText(notification.body ?: "Test body")
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                    notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
                }
                Entrig.setOnNotificationOpenedListener { notification ->
                    android.util.Log.d("Entrig", "Notification opened: ${notification.title}")
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("Entrig", "Entrig setup listeners failed", e)
        }
        enableEdgeToEdge()
        enforceHighRefreshRate()

        val viewModel: PlatformViewModel by viewModels()
        setContent {
            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    android.util.Log.d("FCM", "Notification permission granted.")
                }
            }
            LaunchedEffect(Unit) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    when (androidx.core.content.ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.POST_NOTIFICATIONS)) {
                        android.content.pm.PackageManager.PERMISSION_GRANTED -> {}
                        else -> permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                try {
                    val isEmu = com.example.EnvUtils.isEmu()
                    val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                    val statusCode = availability.isGooglePlayServicesAvailable(this@MainActivity)
                    if (!isEmu && statusCode == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                        val messaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()
                        messaging.isAutoInitEnabled = true

                        val fcmPrefs = this@MainActivity.getSharedPreferences("velorix_fcm_prefs", android.content.Context.MODE_PRIVATE)
                        val cachedToken = fcmPrefs.getString("fcm_token_cached", null)
                        val isSubscribed = fcmPrefs.getBoolean("topic_all_users_subscribed", false)

                        if (!cachedToken.isNullOrBlank()) {
                            android.util.Log.d("FCM_TOKEN", "Using cached FCM Registration Token")
                            viewModel.updateFcmToken(cachedToken)
                            if (!isSubscribed) {
                                messaging.subscribeToTopic("all_users").addOnCompleteListener { subTask ->
                                    if (subTask.isSuccessful) {
                                        fcmPrefs.edit().putBoolean("topic_all_users_subscribed", true).apply()
                                        android.util.Log.d("FCM", "Subscribed to all_users topic")
                                    }
                                }
                            }
                        } else {
                            messaging.token.addOnCompleteListener { task ->
                                try {
                                    if (task.isSuccessful && !task.result.isNullOrBlank()) {
                                        val token = task.result
                                        android.util.Log.d("FCM_TOKEN", "FCM Registration Token: $token")
                                        fcmPrefs.edit().putString("fcm_token_cached", token).apply()
                                        viewModel.updateFcmToken(token)

                                        if (!isSubscribed) {
                                            messaging.subscribeToTopic("all_users").addOnCompleteListener { subTask ->
                                                if (subTask.isSuccessful) {
                                                    fcmPrefs.edit().putBoolean("topic_all_users_subscribed", true).apply()
                                                    android.util.Log.d("FCM", "Subscribed to all_users topic")
                                                }
                                            }
                                        }
                                    } else {
                                        val ex = task.exception
                                        val errMsg = ex?.message.orEmpty()
                                        android.util.Log.w("FCM_TOKEN", "FCM token registration status: $errMsg")
                                    }
                                } catch (e: Throwable) {
                                    android.util.Log.d("FCM_TOKEN", "Token handler exception: ${e.message}")
                                }
                            }
                        }
                    } else {
                        try {
                            com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled = false
                        } catch (_: Throwable) {}
                        android.util.Log.d("FCM", "Skipping FCM push sync on emulator or Play Services unavailable (code: $statusCode)")
                    }
                } catch (e: Throwable) {
                    android.util.Log.d("FCM", "FCM initialization handled gracefully: ${e.message}")
                }

                try {
                    com.google.firebase.installations.FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            android.util.Log.d("VelorixTest", "Firebase Installation ID (FID): ${task.result}")
                        }
                    }
                } catch (e: Throwable) {
                    android.util.Log.w("VelorixTest", "Firebase Installations skipped: ${e.message}")
                }
            }
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                val context = LocalContext.current
                val dbError by viewModel.dbErrorDialog.collectAsStateWithLifecycle()
                var currentTabState by remember { mutableStateOf("home") }
                val user by viewModel.userState.collectAsStateWithLifecycle()
                LaunchedEffect(user?.id) {
                    val isEmuEnv = EnvUtils.isEmu()

                    val userId = user?.id
                    if (userId != null) {
                        try {
                            if (!isEmuEnv) {
                                Entrig.register(userId.toString(), this@MainActivity) { success, error ->
                                    if (success) {
                                        android.util.Log.d("Entrig", "Registered Entrig for user: $userId")
                                    } else {
                                        android.util.Log.e("Entrig", "Entrig register failed: $error")
                                    }
                                }
                            } else {
                                android.util.Log.d("Entrig", "Skipping Entrig register on emulator")
                            }
                        } catch (e: Throwable) {
                            android.util.Log.e("Entrig", "Entrig register crashed", e)
                        }
                    } else {
                        try {
                            if (!isEmuEnv) {
                                Entrig.unregister()
                            }
                        } catch (e: Throwable) {}
                    }
                }
                if (dbError != null) {
                    com.example.ui.components.VeloRixGlassAlertDialog(
                        onDismissRequest = { viewModel.clearDbError() },
                        title = { androidx.compose.material3.Text("Firebase Backend Error", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White) },
                        text = { androidx.compose.material3.Text(dbError!!, color = androidx.compose.ui.graphics.Color(0xFFE2E8F0)) },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { viewModel.clearDbError() }) {
                                androidx.compose.material3.Text("OK", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    )
                }
                // Live Toast collector for instant actions feedback
                LaunchedEffect(Unit) {
                    viewModel.toastMessage.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
                val isBanned by viewModel.isVpnBanned.collectAsStateWithLifecycle()
                val isVpnActive by viewModel.isVpnActive.collectAsStateWithLifecycle()
                val systemConfig by viewModel.systemConfig.collectAsStateWithLifecycle()
                val situationPreview by viewModel.situationPreview.collectAsStateWithLifecycle()
                val isVpnDetected by viewModel.isVpnDetected.collectAsStateWithLifecycle()
                val showDailyDeveloperPopup by viewModel.showDailyDeveloperPopup.collectAsStateWithLifecycle()
                var showAdminSituationSheet by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.checkVpnStatus()
                }

                LaunchedEffect(isLoggedIn, user) {
                    if (isLoggedIn && user != null) {
                        viewModel.checkAndTriggerDailyDeveloperPopup()
                    }
                }

                val isAdmin = user?.role?.contains("admin", ignoreCase = true) == true ||
                              user?.phoneOrEmail?.contains("admin", ignoreCase = true) == true ||
                              user?.phoneOrEmail.equals("anantisback47@gmail.com", ignoreCase = true)

                val activeSituation: SituationPreviewType = remember(situationPreview, user?.isBanned, user?.isSuspended, systemConfig.isMaintenance, systemConfig.isForceUpdate, isBanned, isVpnDetected, isAdmin) {
                    if (situationPreview != SituationPreviewType.NONE) {
                        situationPreview
                    } else if (user?.isBanned == true) {
                        SituationPreviewType.BANNED
                    } else if (user?.isSuspended == true) {
                        SituationPreviewType.SUSPENDED
                    } else if (systemConfig.isMaintenance && !isAdmin) {
                        SituationPreviewType.MAINTENANCE
                    } else if (systemConfig.isForceUpdate && !isAdmin) {
                        SituationPreviewType.FORCE_UPDATE
                    } else if ((systemConfig.vpnRestrictionEnabled || isBanned) && isVpnDetected && !isAdmin) {
                        SituationPreviewType.VPN_BLOCKED
                    } else {
                        SituationPreviewType.NONE
                    }
                }

                // Global Background
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (activeSituation != SituationPreviewType.NONE) {
                            when (activeSituation) {
                                SituationPreviewType.BANNED -> {
                                    BannedScreen(
                                        viewModel = viewModel,
                                        user = user,
                                        onLogout = {
                                            viewModel.logout()
                                            navController.navigate("auth") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        },
                                        onAdminBypass = {
                                            viewModel.setSituationPreview(SituationPreviewType.NONE)
                                            showAdminSituationSheet = true
                                        }
                                    )
                                }
                                SituationPreviewType.MAINTENANCE -> {
                                    MaintenanceScreen(
                                        viewModel = viewModel,
                                        config = systemConfig,
                                        isAdmin = isAdmin,
                                        onAdminBypass = {
                                            viewModel.setSituationPreview(SituationPreviewType.NONE)
                                            showAdminSituationSheet = true
                                        }
                                    )
                                }
                                SituationPreviewType.SUSPENDED -> {
                                    SuspendedScreen(
                                        viewModel = viewModel,
                                        user = user,
                                        onLogout = {
                                            viewModel.logout()
                                            navController.navigate("auth") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        },
                                        onAdminBypass = {
                                            viewModel.setSituationPreview(SituationPreviewType.NONE)
                                            showAdminSituationSheet = true
                                        }
                                    )
                                }
                                SituationPreviewType.FORCE_UPDATE -> {
                                    ForceUpdateScreen(
                                        viewModel = viewModel,
                                        config = systemConfig,
                                        isAdmin = isAdmin,
                                        onAdminBypass = {
                                            viewModel.setSituationPreview(SituationPreviewType.NONE)
                                            showAdminSituationSheet = true
                                        }
                                    )
                                }
                                SituationPreviewType.VPN_BLOCKED -> {
                                    VpnBlockedScreen(
                                        viewModel = viewModel,
                                        isAdmin = isAdmin,
                                        onAdminBypass = {
                                            viewModel.setSituationPreview(SituationPreviewType.NONE)
                                            showAdminSituationSheet = true
                                        }
                                    )
                                }
                                SituationPreviewType.NONE -> Unit
                            }
                        } else {
                            androidx.navigation.compose.NavHost(
                        navController = navController, 
                        startDestination = "splash",
                        enterTransition = { androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 400f), initialOffsetX = { it }) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) },
                        exitTransition = { androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 400f), targetOffsetX = { -it / 3 }) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) },
                        popEnterTransition = { androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 400f), initialOffsetX = { -it / 3 }) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) },
                        popExitTransition = { androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 400f), targetOffsetX = { it }) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) }
                    ) {
                                                                composable("splash") {
                        SplashScreen(
                            viewModel = viewModel,
                            onNavigateToAuth = {
                                navController.navigate("auth") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            },
                            onNavigateToHome = {
                                if (viewModel.hasCompletedOnboarding.value) {
                                    navController.navigate("main") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("onboarding") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                    composable("auth") {
                        AuthScreen(
                            viewModel = viewModel,
                            onAuthSuccess = {
                                // Existing account login always proceeds directly to main dashboard
                                navController.navigate("main") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            },
                            onRegisterSuccess = {
                                // Newly created account ALWAYS goes directly to registration setup pages
                                navController.navigate("onboarding") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("onboarding") {
                        OnboardingScreen(
                            viewModel = viewModel,
                            onComplete = {
                                navController.navigate("main") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("main") {
                        val hazeState = remember { HazeState() }
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding(),
                            containerColor = Color.Transparent,
                            bottomBar = {
                                if (currentTabState != "support") {
                                    GlassBottomBar(
                                        currentTab = currentTabState,
                                        onTabSelected = { currentTabState = it },
                                        hazeState = hazeState
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .haze(
                                        state = hazeState,
                                        style = HazeStyle(
                                            tint = Color.Transparent,
                                            blurRadius = 16.dp,
                                            noiseFactor = 0f
                                        )
                                    )
                                    .padding(
                                        top = innerPadding.calculateTopPadding()
                                        // bottom padding removed to allow content to scroll behind the glass bottom bar
                                    )
                            ) {
                                androidx.compose.animation.AnimatedContent(
                                    targetState = currentTabState,
                                    label = "tab_switch_animation",
                                    transitionSpec = {
                                        (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) + androidx.compose.animation.scaleIn(initialScale = 0.95f)) togetherWith (
                                            androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) + androidx.compose.animation.scaleOut(targetScale = 1.05f)
                                        )
                                    }
                                ) { targetTab ->
                                    when (targetTab) {
                                        "home" -> {
                                            HomeScreen(
                                                viewModel = viewModel,
                                                onNavigateToTournament = { id ->
                                                    navController.navigate("details/$id")
                                                },
                                                onNavigateToWallet = {
                                                    currentTabState = "wallet"
                                                },
                                                onNavigateToProfile = {
                                                    currentTabState = "profile"
                                                },
                                                onNavigateToSupport = {
                                                    currentTabState = "support"
                                                },
                                                onNavigateToNotifications = {
                                                    navController.navigate("notifications")
                                                },
                                                onNavigateToLeaderboard = {
                                                    currentTabState = "leaderboard"
                                                }
                                            )
                                        }
                                        "matches" -> {
                                            MatchesScreen(
                                                viewModel = viewModel,
                                                onNavigateToTournament = { id ->
                                                    navController.navigate("details/$id")
                                                }
                                            )
                                        }
                                        "leaderboard" -> {
                                            LeaderboardScreen(viewModel = viewModel)
                                        }
                                        "wallet" -> {
                                            WalletScreen(viewModel = viewModel)
                                        }
                                        "profile" -> {
                                            ProfileScreen(
                                                viewModel = viewModel,
                                                onLogout = {
                                                    navController.navigate("auth") {
                                                        popUpTo("main") { inclusive = true }
                                                    }
                                                },
                                                onNavigateToSupport = {
                                                    currentTabState = "support"
                                                },
                                                onNavigateToNotifications = {
                                                    navController.navigate("notifications")
                                                },
                                                onNavigateToSettings = {
                                                    navController.navigate("settings")
                                                },
                                                onNavigateToAbout = {
                                                    navController.navigate("about")
                                                },
                                                onOpenAdminSituations = {
                                                    showAdminSituationSheet = true
                                                }
                                            )
                                        }
                                        "support" -> {
                                            com.example.ui.screens.CustomerSupportScreen(
                                                platformViewModel = viewModel,
                                                onNavigateBack = { currentTabState = "home" }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 4. TOURNAMENT COMPREHENSIVE DETAIL DISPLAY PANEL
                    composable(
                        "details/{tournamentId}",
                        deepLinks = listOf(navDeepLink { uriPattern = "velorixtournaments://details/{tournamentId}" }),
                        enterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(300))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(250))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(300))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(250))
                        }
                    ) { backStackEntry ->
                        val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: ""
                        TournamentDetailsScreen(
                            viewModel = viewModel,
                            tournamentId = tournamentId,
                            onNavigateBack = {
                                navController.navigateUp()
                            }
                        )
                    }

                    // 5. NOTIFICATION CENTER SCREEN
                    composable(
                        "notifications",
                        enterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(300))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(250))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(300))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(250))
                        }
                    ) {
                        com.example.ui.screens.NotificationCenterScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.navigateUp()
                            },
                            onNavigateToTournament = { tourneyId ->
                                navController.navigate("details/$tourneyId")
                            }
                        )
                    }

                    // 6. DEDICATED SETTINGS SCREEN
                    composable(
                        "settings",
                        enterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(300))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(250))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(300))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(250))
                        }
                    ) {
                        com.example.ui.screens.SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.navigateUp()
                            },
                            onNavigateToSupport = {
                                navController.navigate("main")
                                currentTabState = "support"
                            },
                            onOpenAdminSituations = {
                                showAdminSituationSheet = true
                            },
                            onNavigateToLegal = { tab ->
                                navController.navigate("legal_compliance/${tab.name}")
                            }
                        )
                    }

                    // 7. DEDICATED ABOUT SCREEN (MINIMAL XIAOMI & VERCEL STYLE)
                    composable(
                        "about",
                        enterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(300))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(250))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(300))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(250))
                        }
                    ) {
                        com.example.ui.screens.AboutScreen(
                            onNavigateBack = {
                                navController.navigateUp()
                            },
                            onNavigateToLegal = { tab ->
                                navController.navigate("legal_compliance/${tab.name}")
                            }
                        )
                    }

                    // 8. DEDICATED LEGAL & COMPLIANCE PAGE (FULL-PAGE MINIMALIST ARCHITECTURE)
                    composable(
                        "legal_compliance/{tabName}",
                        enterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(300))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(250))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(tween(300))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(250))
                        }
                    ) { backStackEntry ->
                        val tabName = backStackEntry.arguments?.getString("tabName") ?: "TERMS"
                        val initialTab = try {
                            com.example.ui.components.LegalTab.valueOf(tabName)
                        } catch (_: Exception) {
                            com.example.ui.components.LegalTab.TERMS
                        }
                        com.example.ui.screens.LegalComplianceScreen(
                            initialTab = initialTab,
                            onNavigateBack = {
                                navController.navigateUp()
                            }
                        )
                    }
                    } // End of NavHost
                        } // End of activeSituation == NONE check

                        // Once-per-day developer highlight & support popup (ONLY shown after login, NEVER over auth/splash/onboarding)
                        val isUserReadyForPopup = isLoggedIn && user != null && currentRoute != "auth" && currentRoute != "splash" && currentRoute != "onboarding"
                        if (showDailyDeveloperPopup && isUserReadyForPopup) {
                            com.example.ui.components.DeveloperPopupDialog(
                                onDismissRequest = {
                                    viewModel.dismissDailyDeveloperPopup()
                                },
                                onOpenSettings = {
                                    viewModel.dismissDailyDeveloperPopup()
                                    navController.navigate("about")
                                }
                            )
                        }

                        InAppNotificationOverlay(
                            onNotificationClick = {
                                navController.navigate("notifications")
                            }
                        )

                        if (showAdminSituationSheet) {
                            AdminSituationTesterSheet(
                                viewModel = viewModel,
                                user = user,
                                systemConfig = systemConfig,
                                currentPreview = situationPreview,
                                onDismiss = { showAdminSituationSheet = false }
                            )
                        }
                    }
                }
        }
    }
}
@Composable
fun InAppNotificationOverlay(
    onNotificationClick: () -> Unit = {}
) {
    val notification by com.example.service.NotificationEventBus.events.collectAsStateWithLifecycle(initialValue = null)
    var currentNotification by remember { mutableStateOf<com.example.service.NotificationEventBus.NotificationEvent?>(null) }
    LaunchedEffect(notification) {
        if (notification != null) {
            currentNotification = notification
            kotlinx.coroutines.delay(4000)
            if (currentNotification == notification) {
                currentNotification = null
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        androidx.compose.animation.AnimatedVisibility(
            visible = currentNotification != null,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            currentNotification?.let { notif ->
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentNotification = null
                            onNotificationClick()
                        },
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Notification",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = notif.title,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notif.body,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val pillTabs = listOf(
        Triple("home", "Home", androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = R.drawable.ic_iconsax_home)),
        Triple("matches", "Matches", androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = R.drawable.ic_iconsax_matches)),
        Triple("leaderboard", "Ranks", Icons.Rounded.Leaderboard),
        Triple("wallet", "Wallet", androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = R.drawable.ic_iconsax_wallet))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The Glass Pill
        Row(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .hazeChild(
                    state = hazeState,
                    shape = RoundedCornerShape(36.dp)
                )
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0x8009090B), Color(0x4018181B))))
                .border(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pillTabs.forEach { (route, label, icon) ->
                val isSelected = currentTab == route
                
                val weight by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    ), label = "tabWeight"
                )

                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(36.dp))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onTabSelected(route) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow Effect Behind Icon
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSelected,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.8f),
                            modifier = Modifier.size(if (isSelected) 26.dp else 24.dp)
                        )
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isSelected,
                            enter = androidx.compose.animation.expandVertically(expandFrom = Alignment.Top) + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Top) + androidx.compose.animation.fadeOut()
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // The Separate Circle Button (Profile)
        val isProfileSelected = currentTab == "profile"
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .hazeChild(
                    state = hazeState,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0x8009090B), Color(0x4018181B))))
                .border(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onTabSelected("profile") }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Glow Effect
            androidx.compose.animation.AnimatedVisibility(
                visible = isProfileSelected,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
            Icon(
                imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = R.drawable.ic_iconsax_profile),
                contentDescription = "Profile",
                tint = if (isProfileSelected) Color.White else Color.Gray.copy(alpha = 0.8f),
                modifier = Modifier.size(if (isProfileSelected) 28.dp else 24.dp)
            )
        }
    }
}
}

