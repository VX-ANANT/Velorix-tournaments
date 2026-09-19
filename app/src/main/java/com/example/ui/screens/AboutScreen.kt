package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.ui.components.stretchOverscroll
import com.example.util.UpiPaymentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AboutScreen.kt
 *
 * Implements the exact layout, card groupings, and styling as provided in the reference screenshots:
 * - Top back navigation with "About" title
 * - Large circular App Logo with concentric rings
 * - App Name ("VeloRix") with "2.1.0" and "RELEASE" badges
 * - CONTRIBUTORS: Horizontal row of circular profile avatars
 * - DEVELOPER: Grouped card with Website, Instagram, X (Twitter)
 * - SUPPORT: Grouped card with Buy Me a Coffee, Patreon, UPI
 * - COMMUNITY: Grouped card with Discord
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Developer & Links for Anant
    val websiteUrl = "https://github.com/VX-ANANT/Velorix-tournaments"
    val instagramUrl = "https://instagram.com/anant_sgh"
    val twitterUrl = "https://x.com/Anant__sgh"
    val githubUrl = "https://github.com/VX-ANANT/Velorix-tournaments"
    val patreonUrl = "https://patreon.com/Anant_sgh"
    val upiId = UpiPaymentManager.PRIMARY_UPI_ID // veloxyra.anant@fam
    val fallbackUpiId = UpiPaymentManager.PRIMARY_UPI_ID
    val discordUrl = "https://discord.gg/ghxrpQAAC2"

    var showUpiQrDialog by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(text: String, label: String = "Text") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Saved successfully", Toast.LENGTH_SHORT).show()
    }

    // Pure AMOLED Black Palette (0% Red/Warm Tint, Pure Obsidian Neutral Charcoal Surfaces)
    val screenBg = Color(0xFF000000)
    val cardBg = Color(0xFF0E0E11)
    val cardBorder = Color(0xFF1C1C22)
    val dividerColor = Color(0xFF18181D)
    val sectionHeaderColor = Color(0xFF8E8E93)
    val primaryTextColor = Color(0xFFFFFFFF)
    val secondaryTextColor = Color(0xFFA1A1A6)
    val pillBg = Color(0xFF151518)
    val pillTextColor = Color(0xFFE5E5EA)

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor
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
                            tint = primaryTextColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenBg
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .stretchOverscroll()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
        ) {
            // 1. APP LOGO, TITLE, & VERSION/RELEASE BADGES
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400)) +
                        slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { -30 })
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Big Circular App Logo with Concentric Rings (AMOLED Pure Black)
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color(0xFFE5E5EA), CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFFE5E5EA).copy(alpha = 0.35f), CircleShape)
                                .background(Color(0xFF0E0E11)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.velorix_logo_image),
                                contentDescription = "App Logo",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "VeloRix",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Version & Release Pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pillBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = com.example.BuildConfig.VERSION_NAME,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pillTextColor
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pillBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "RELEASE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pillTextColor,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. CONTRIBUTORS SECTION
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 80)) +
                        slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { 30 })
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "CONTRIBUTORS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = sectionHeaderColor,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Horizontal Row of Circular Contributor Avatars
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val contributors = listOf(
                                ContributorItem("Cat Mascot", Color(0xFFE8D7F1), "🐱"),
                                ContributorItem("Lead Dev", Color(0xFF1F2937), "👨‍💻"),
                                ContributorItem("Core", Color(0xFFEA580C), "🎨"),
                                ContributorItem("GitHub", Color(0xFF181717), "🐙"),
                                ContributorItem("Doodle", Color(0xFFF3F4F6), "😀"),
                                ContributorItem("Anime", Color(0xFF0284C7), "✨")
                            )

                            contributors.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, Color(0xFF222228), CircleShape)
                                        .background(item.bgColor)
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            Toast.makeText(context, item.name, Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.emoji,
                                        fontSize = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // 3. DEVELOPER SECTION
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(450, delayMillis = 140)) +
                        slideInVertically(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { 40 })
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "DEVELOPER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = sectionHeaderColor,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // GitHub / Repo
                                AboutRowItem(
                                    drawableRes = R.drawable.ic_github,
                                    title = "GitHub / Website",
                                    subtitle = "github.com/VX-ANANT/Velorix-tournaments",
                                    titleColor = primaryTextColor,
                                    subtitleColor = secondaryTextColor,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(githubUrl)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                // Instagram
                                AboutRowItem(
                                    drawableRes = R.drawable.ic_instagram,
                                    title = "Instagram",
                                    subtitle = "@anant_sgh",
                                    titleColor = primaryTextColor,
                                    subtitleColor = secondaryTextColor,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(instagramUrl)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                // X (Twitter)
                                AboutRowItem(
                                    drawableRes = R.drawable.ic_x_twitter,
                                    title = "X (Twitter)",
                                    subtitle = "@Anant__sgh",
                                    titleColor = primaryTextColor,
                                    subtitleColor = secondaryTextColor,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(twitterUrl)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // 4. SUPPORT SECTION
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(450, delayMillis = 200)) +
                        slideInVertically(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { 50 })
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "SUPPORT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = sectionHeaderColor,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Patreon
                                AboutRowItem(
                                    drawableRes = R.drawable.ic_patreon,
                                    title = "Patreon",
                                    subtitle = "patreon.com/Anant_sgh",
                                    titleColor = primaryTextColor,
                                    subtitleColor = secondaryTextColor,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(patreonUrl)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                // UPI
                                AboutRowItem(
                                    drawableRes = R.drawable.ic_upi_logo,
                                    title = "UPI",
                                    subtitle = "veloxyra.anant@fam",
                                    titleColor = primaryTextColor,
                                    subtitleColor = secondaryTextColor,
                                    iconTint = null,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        copyToClipboard(upiId, "UPI ID")
                                        showUpiQrDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // 5. COMMUNITY SECTION
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(450, delayMillis = 260)) +
                        slideInVertically(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { 60 })
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "COMMUNITY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = sectionHeaderColor,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Discord
                                AboutRowItem(
                                    drawableRes = R.drawable.ic_discord,
                                    title = "Discord",
                                    subtitle = "discord.gg/ghxrpQAAC2",
                                    titleColor = primaryTextColor,
                                    subtitleColor = secondaryTextColor,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(discordUrl)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // UPI QR & Pay Dialog when tapping UPI row
    if (showUpiQrDialog) {
        UpiSupportModal(
            upiId = upiId,
            onDismiss = { showUpiQrDialog = false },
            onCopy = { copyToClipboard(upiId, "UPI ID") },
            onPayViaApp = {
                val upiUri = Uri.parse("upi://pay?pa=$upiId&pn=${Uri.encode("Developer")}&cu=INR")
                val intent = Intent(Intent.ACTION_VIEW, upiUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "No UPI app found", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

private data class ContributorItem(
    val name: String,
    val bgColor: Color,
    val emoji: String
)

@Composable
private fun AboutRowItem(
    title: String,
    subtitle: String,
    titleColor: Color,
    subtitleColor: Color,
    onClick: () -> Unit,
    vectorIcon: ImageVector? = null,
    drawableRes: Int? = null,
    iconTint: Color? = titleColor
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (vectorIcon != null) {
                Icon(
                    imageVector = vectorIcon,
                    contentDescription = title,
                    tint = titleColor,
                    modifier = Modifier.size(22.dp)
                )
            } else if (drawableRes != null) {
                if (iconTint != null) {
                    Icon(
                        painter = painterResource(drawableRes),
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(drawableRes),
                        contentDescription = title,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(18.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UpiSupportModal(
    upiId: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onPayViaApp: () -> Unit
) {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val upiUriString = "upi://pay?pa=$upiId&pn=${Uri.encode("VX-ANANT (VeloRix)")}&tn=${Uri.encode("Support VeloRix Dev")}&cu=INR"

    LaunchedEffect(upiUriString) {
        withContext(Dispatchers.Default) {
            val bmp = UpiPaymentManager.generateQrBitmap(
                content = upiUriString,
                sizePx = 420,
                context = context,
                isDarkTheme = true,
                customBgColor = android.graphics.Color.WHITE,
                customModuleColor = android.graphics.Color.BLACK
            )
            withContext(Dispatchers.Main) {
                qrBitmap = bmp
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0E0E12),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_upi_logo),
                    contentDescription = "UPI",
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "UPI Support",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF),
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = upiId,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFE5E5EA)
                )

                Spacer(modifier = Modifier.height(16.dp))

                qrBitmap?.let { bmp ->
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "UPI QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } ?: run {
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF16161A)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Scan with any UPI app (GPay, PhonePe, Paytm)",
                    fontSize = 12.sp,
                    color = Color(0xFFA1A1A6),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onPayViaApp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFFFFF),
                    contentColor = Color(0xFF000000)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open UPI App", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCopy) {
                Text("Copy UPI", color = Color(0xFFFFFFFF), fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
