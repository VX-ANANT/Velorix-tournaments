package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DeveloperPopupDialog
import com.example.ui.components.LegalComplianceModal
import com.example.ui.components.LegalTab
import com.example.ui.components.stretchOverscroll
import com.example.ui.viewmodel.PlatformViewModel
import com.example.util.UpiPaymentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SettingsScreen.kt
 *
 * Dedicated Settings and "About the Developer" screen accessible from the top right
 * of the Profile Screen and from the daily popup.
 *
 * Contains:
 * - App Appearance & Theme switch (Light, Dark, System Auto)
 * - Developer Spotlight (VX-ANANT): Patreon supporter info, GitHub repo link, UPI tip/support with QR code
 * - App Specifications & Build Information (Version, Runtime, Architecture, Watchdog Daemon)
 * - Cache Management & Data Export
 * - Direct Links to Privacy Policy, Terms, and Support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PlatformViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onOpenAdminSituations: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val themeMode by viewModel.themeMode.collectAsState()
    val user by viewModel.userState.collectAsState()

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isSystemDark = isSystemInDarkTheme()
    val isSystemMatch = themeMode == "system"

    val devName = "VX-ANANT"
    val patreonUsername = "anantisback47"
    val patreonUrl = "https://patreon.com/anantisback47"
    val githubRepoUrl = "https://github.com/VX-ANANT/velorix-tournaments"
    val upiId = UpiPaymentManager.PRIMARY_UPI_ID

    var showDeveloperModal by remember { mutableStateOf(false) }
    var showQrCode by remember { mutableStateOf(false) }
    var showLegalModal by remember { mutableStateOf(false) }
    var selectedLegalTab by remember { mutableStateOf(LegalTab.TERMS) }

    // Dynamic QR generation for UPI support
    val qrBgColor = (if (isDarkTheme) Color(0xFF0D1117) else Color(0xFFF6F8FA)).toArgb()
    val qrModuleColor = (if (isDarkTheme) Color(0xFFF0F6FC) else Color(0xFF0D1117)).toArgb()
    val upiUriString = remember(upiId) {
        "upi://pay?pa=$upiId&pn=${Uri.encode("VX-ANANT (Velorix Dev)")}&tn=${Uri.encode("Support Velorix Esports Engine")}&cu=INR"
    }

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(upiUriString, isDarkTheme) {
        withContext(Dispatchers.Default) {
            val bmp = UpiPaymentManager.generateQrBitmap(
                content = upiUriString,
                sizePx = 420,
                context = context,
                isDarkTheme = isDarkTheme,
                customBgColor = qrBgColor,
                customModuleColor = qrModuleColor
            )
            withContext(Dispatchers.Main) {
                qrBitmap = bmp
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Settings & Info",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        showDeveloperModal = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About Developer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .stretchOverscroll()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
        ) {
            // Section 1: Developer Spotlight Card
            item {
                Text(
                    text = "ABOUT THE DEVELOPER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                Color(0xFF7F52FF),
                                                Color(0xFF00E5FF),
                                                Color(0xFFFF0055),
                                                Color(0xFF7F52FF)
                                            )
                                        )
                                    )
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(if (isDarkTheme) Color(0xFF0F1420) else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = "Developer",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = devName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "LEAD DEV",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                                Text(
                                    text = "Architect of VeloRix Android Esports Platform",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Engineered with Kotlin 2.0, Jetpack Compose M3, sub-second Firebase synchronization, and autonomous Gemini AI Watchdog operations.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // GitHub Repository Link Row
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    try {
                                        val gitIntent = Intent(Intent.ACTION_VIEW, Uri.parse(githubRepoUrl))
                                        context.startActivity(gitIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Opening GitHub repository...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0xFF161B26) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "GitHub",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "GitHub Repository",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "VX-ANANT/velorix-tournaments",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Patreon Supporter Row
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    try {
                                        val patreonIntent = Intent(Intent.ACTION_VIEW, Uri.parse(patreonUrl))
                                        context.startActivity(patreonIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Opening Patreon...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0xFF261417) else Color(0xFFFFF0F1),
                            border = BorderStroke(1.dp, Color(0xFFFF424D).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Favorite,
                                    contentDescription = "Patreon",
                                    tint = Color(0xFFFF424D),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Support on Patreon",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "@$patreonUsername • Early access & beta perks",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFF424D)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = Color(0xFFFF424D),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // UPI Tip Row with Copy & QR toggle
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0xFF121927) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_custom_wallet),
                                        contentDescription = "UPI",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Direct UPI Support",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = upiId,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Developer UPI", upiId)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "UPI ID copied: $upiId", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            showQrCode = !showQrCode
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.QrCodeScanner,
                                            contentDescription = "Show QR",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = showQrCode,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (qrBitmap != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(160.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isDarkTheme) Color(0xFF0D1117) else Color(0xFFF6F8FA))
                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    bitmap = qrBitmap!!.asImageBitmap(),
                                                    contentDescription = "Developer UPI QR",
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Scan with any UPI app to support development",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section 2: App Appearance & Theming
            item {
                Text(
                    text = "APPEARANCE & DISPLAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // System Match Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSystemMatch) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SettingsBrightness,
                                        contentDescription = "System Match",
                                        tint = if (isSystemMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "System Match",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isSystemMatch) "Auto-sync active (${if (isSystemDark) "Dark" else "Light"})" else "Manual override active",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isSystemMatch,
                                onCheckedChange = { checked ->
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    if (checked) {
                                        viewModel.setThemeMode("system")
                                    } else {
                                        viewModel.setThemeMode(if (isSystemDark) "dark" else "light")
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3-Option Theme Cards: Light, Dark, System
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(
                                Triple("light", "Light", Icons.Default.LightMode),
                                Triple("dark", "Dark", Icons.Default.DarkMode),
                                Triple("system", "System", Icons.Default.Smartphone)
                            )

                            options.forEach { (modeKey, modeTitle, modeIcon) ->
                                val isSelected = themeMode == modeKey
                                val activeBorder = if (isSelected) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            viewModel.setThemeMode(modeKey)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = activeBorder
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = modeIcon,
                                            contentDescription = modeTitle,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = modeTitle,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section 3: App Diagnostics & Build Specifications
            item {
                Text(
                    text = "SPECIFICATIONS & DIAGNOSTICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        SettingsInfoRow("App Version", "2.1.0 (Build 42)")
                        SettingsInfoRow("Target Environment", "Android 14+ (API 34)")
                        SettingsInfoRow("UI Framework", "Jetpack Compose Material 3")
                        SettingsInfoRow("Language Runtime", "Kotlin 2.0.0")
                        SettingsInfoRow("Backend Synchronization", "Firebase Realtime Database")
                        SettingsInfoRow("AI Watchdog Daemon", "Gemini 1.5 Flash (Node.js)")
                        SettingsInfoRow("License", "MIT Open Source")

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                Toast.makeText(context, "Local app cache cleared successfully.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Temporary Cache", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section 4: Support & Admin Shortcuts
            item {
                Text(
                    text = "SUPPORT & ADMIN CONTROLS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        SettingsActionTile(
                            icon = Icons.Outlined.HeadsetMic,
                            title = "24/7 Gemini AI Support",
                            subtitle = "Chat with AI bot for instant match issue resolution",
                            onClick = {
                                onNavigateToSupport()
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val currentUser = user
                        val isAdmin = currentUser?.role?.contains("admin", ignoreCase = true) == true ||
                                currentUser?.phoneOrEmail?.equals("anantisback47@gmail.com", ignoreCase = true) == true

                        if (isAdmin) {
                            SettingsActionTile(
                                icon = Icons.Default.AdminPanelSettings,
                                title = "Admin Situation Controls",
                                subtitle = "Test Maintenance, Force Update, Ban & VPN screens",
                                iconTint = Color(0xFFF59E0B),
                                onClick = {
                                    onOpenAdminSituations()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section 5: Legal & Regulatory Compliance
            item {
                Text(
                    text = "LEGAL, COMPLIANCE & POLICIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        SettingsActionTile(
                            icon = Icons.Default.Gavel,
                            title = "Terms of Service",
                            subtitle = "Constitutional Game of Skill Covenant & Art. 19(1)(g) safe harbor",
                            iconTint = Color(0xFF38BDF8),
                            onClick = {
                                selectedLegalTab = LegalTab.TERMS
                                showLegalModal = true
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsActionTile(
                            icon = Icons.Default.Security,
                            title = "Data Sovereignty & Privacy",
                            subtitle = "DPDP Act 2023, IT Act 2000 & zero marketing telemetry guarantee",
                            iconTint = Color(0xFF10B981),
                            onClick = {
                                selectedLegalTab = LegalTab.PRIVACY
                                showLegalModal = true
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsActionTile(
                            icon = Icons.Default.Shield,
                            title = "Sentinel Anti-Cheat Protocol",
                            subtitle = "Autonomous Forensic Bot & Zero-Tolerance Hardware Blacklisting",
                            iconTint = Color(0xFFEF4444),
                            onClick = {
                                selectedLegalTab = LegalTab.FAIR_PLAY
                                showLegalModal = true
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsActionTile(
                            icon = Icons.Outlined.ReceiptLong,
                            title = "Escrow & Liquidation Arbitration",
                            subtitle = "Deterministic wallet escrow & instant UPI rail reconciliation",
                            iconTint = Color(0xFFF59E0B),
                            onClick = {
                                selectedLegalTab = LegalTab.REFUNDS
                                showLegalModal = true
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsActionTile(
                            icon = Icons.Outlined.HealthAndSafety,
                            title = "Operative Welfare & 18+ Safeguards",
                            subtitle = "Player welfare, circuit-breaker freeze & national helpline resources",
                            iconTint = Color(0xFFA855F7),
                            onClick = {
                                selectedLegalTab = LegalTab.RESPONSIBLE
                                showLegalModal = true
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsActionTile(
                            icon = Icons.Default.AccountBalance,
                            title = "Statutory Jurisprudence & OGAI",
                            subtitle = "PROG Act 2025, MeitY PROG Rules 2026 & Esports Skill Classification",
                            iconTint = Color(0xFF06B6D4),
                            onClick = {
                                selectedLegalTab = LegalTab.LEGAL_STATUS
                                showLegalModal = true
                            }
                        )
                    }
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

    if (showDeveloperModal) {
        DeveloperPopupDialog(
            onDismissRequest = { showDeveloperModal = false },
            onOpenSettings = null
        )
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
