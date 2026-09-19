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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.LegalTab
import com.example.ui.components.stretchOverscroll
import com.example.util.UpiPaymentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AboutScreen.kt
 *
 * Professional, top-to-bottom minimalist design matching Xiaomi HyperOS & Vercel aesthetics:
 * - Pure AMOLED Black background (#000000) with subtle obsidian cards (#0A0A0A)
 * - Restrained, clean typography and smooth micro-interactions
 * - Professional Contributor & Lead Developer card (no cheesy/cluttered frames)
 * - Dedicated navigation to Legal & Compliance page
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLegal: (LegalTab) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Developer & Social Links
    val githubUrl = "https://github.com/VX-ANANT/Velorix-tournaments"
    val instagramUrl = "https://instagram.com/anant_sgh"
    val twitterUrl = "https://x.com/Anant__sgh"
    val patreonUrl = "https://patreon.com/Anant_sgh"
    val discordUrl = "https://discord.gg/ghxrpQAAC2"
    val upiId = UpiPaymentManager.PRIMARY_UPI_ID

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
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    // Xiaomi & Vercel Minimalist Theme Tokens
    val screenBg = Color(0xFF000000)
    val cardBg = Color(0xFF0A0A0A)
    val cardBorder = Color(0xFF1B1B1B)
    val dividerColor = Color(0xFF161616)
    val sectionHeaderColor = Color(0xFF737373)
    val primaryTextColor = Color(0xFFEDEDED)
    val secondaryTextColor = Color(0xFF8A8A8A)

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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryTextColor,
                        letterSpacing = (-0.3).sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onNavigateBack()
                        }
                    ) {
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
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
        ) {
            // 1. TOP-TO-BOTTOM HERO: LOGO, APP TITLE, VERSION PILL
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(350)) +
                        slideInVertically(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { -20 })
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Clean Xiaomi/Vercel Minimal Icon
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF0F0F0F))
                                .border(1.dp, Color(0xFF262626), RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.velorix_logo_image),
                                contentDescription = "VeloRix App Logo",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "VELORIX",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = primaryTextColor
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Esports Infrastructure & Tournament Engine",
                            fontSize = 12.sp,
                            color = secondaryTextColor,
                            letterSpacing = (-0.1).sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Clean Minimal Badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF141414))
                                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "v${com.example.BuildConfig.VERSION_NAME}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFD4D4D8)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF101814))
                                    .border(1.dp, Color(0xFF1E3A2B), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "PROD RELEASE",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4ADE80)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 2. LEAD DEVELOPER & CONTRIBUTOR SECTION (PROFESSIONAL TOP-TO-BOTTOM CARD)
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(350, delayMillis = 60)) +
                        slideInVertically(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { 20 })
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(text = "AUTHOR & CREATOR")

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Author Profile Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Professional circular avatar with clean border
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color(0xFF333333), CircleShape)
                                            .clickable {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                Toast.makeText(context, "Anant — Creator & Lead Architect", Toast.LENGTH_SHORT).show()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.developer_pfp),
                                            contentDescription = "Anant Profile Picture",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Anant Singh",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = primaryTextColor,
                                                letterSpacing = (-0.2).sp
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF1F1F1F))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "CREATOR",
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFA1A1A6)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "Lead Architect & Engine Developer",
                                            fontSize = 12.sp,
                                            color = secondaryTextColor,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                // GitHub / Source Repository
                                MinimalRowItem(
                                    drawableRes = R.drawable.ic_github,
                                    title = "GitHub Repository",
                                    subtitle = "VX-ANANT / Velorix-tournaments",
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(githubUrl)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                // Instagram
                                MinimalRowItem(
                                    drawableRes = R.drawable.ic_instagram,
                                    title = "Instagram",
                                    subtitle = "@anant_sgh",
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(instagramUrl)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                // X (Twitter)
                                MinimalRowItem(
                                    drawableRes = R.drawable.ic_x_twitter,
                                    title = "X (Twitter)",
                                    subtitle = "@Anant__sgh",
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(twitterUrl)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 3. STATUTORY COMPLIANCE & LEGAL JURISPRUDENCE (CLEAN FULL-PAGE ACCESS)
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(350, delayMillis = 120)) +
                        slideInVertically(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { 25 })
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(text = "LEGAL & COMPLIANCE")

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                MinimalRowItem(
                                    drawableRes = R.drawable.ic_legal_gavel,
                                    title = "Terms of Service",
                                    subtitle = "Game of Skill Covenant & Participation Rules",
                                    hasArrow = true,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        onNavigateToLegal(LegalTab.TERMS)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                MinimalRowItem(
                                    drawableRes = R.drawable.ic_untitledui_shield_tick,
                                    title = "Data Sovereignty & Privacy",
                                    subtitle = "DPDP Act 2023 Compliance & Zero Telemetry Sharing",
                                    hasArrow = true,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        onNavigateToLegal(LegalTab.PRIVACY)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                MinimalRowItem(
                                    drawableRes = R.drawable.ic_fair_play_swords,
                                    title = "Sentinel Anti-Cheat Protocol",
                                    subtitle = "Competitive Parity, Emulators & Zero-Tolerance Policy",
                                    hasArrow = true,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        onNavigateToLegal(LegalTab.FAIR_PLAY)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                MinimalRowItem(
                                    drawableRes = R.drawable.ic_refund_receipt,
                                    title = "Escrow & Payout Arbitration",
                                    subtitle = "Wallet Escrow, Instant UPI Liquidation & Tax Deductions",
                                    hasArrow = true,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        onNavigateToLegal(LegalTab.REFUNDS)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                MinimalRowItem(
                                    drawableRes = R.drawable.ic_age_18_badge,
                                    title = "Operative Welfare & 18+ Gate",
                                    subtitle = "Mandatory 18+ Age Gating & National Helplines",
                                    hasArrow = true,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        onNavigateToLegal(LegalTab.RESPONSIBLE)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                MinimalRowItem(
                                    drawableRes = R.drawable.ic_untitledui_shield,
                                    title = "Statutory Jurisprudence",
                                    subtitle = "PROG Act 2025, MeitY PROG Rules 2026 & OGAI",
                                    hasArrow = true,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        onNavigateToLegal(LegalTab.LEGAL_STATUS)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 4. SUPPORT & APPRECIATION
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(350, delayMillis = 180)) +
                        slideInVertically(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { 30 })
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(text = "SUPPORT & DONATE")

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                MinimalRowItem(
                                    drawableRes = R.drawable.ic_patreon,
                                    title = "Patreon",
                                    subtitle = "patreon.com/Anant_sgh",
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(patreonUrl)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                MinimalRowItem(
                                    drawableRes = R.drawable.ic_upi_logo,
                                    title = "UPI Direct",
                                    subtitle = upiId,
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

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 5. COMMUNITY
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(350, delayMillis = 240)) +
                        slideInVertically(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), initialOffsetY = { 35 })
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(text = "COMMUNITY")

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                MinimalRowItem(
                                    drawableRes = R.drawable.ic_discord,
                                    title = "Discord Community",
                                    subtitle = "discord.gg/ghxrpQAAC2",
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(discordUrl)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // UPI QR Dialog
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

// ---------------- REUSABLE MINIMALIST COMPONENTS ----------------

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFF737373),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun MinimalRowItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    drawableRes: Int? = null,
    iconTint: Color? = Color(0xFFEDEDED),
    hasArrow: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (drawableRes != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF141414)),
                contentAlignment = Alignment.Center
            ) {
                if (iconTint != null) {
                    Icon(
                        painter = painterResource(drawableRes),
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(drawableRes),
                        contentDescription = title,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFEDEDED),
                letterSpacing = (-0.2).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF8A8A8A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (hasArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF444444),
                modifier = Modifier.size(12.dp)
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
        containerColor = Color(0xFF0A0A0A),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_upi_logo),
                    contentDescription = "UPI",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "UPI Support",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEDEDED),
                    fontSize = 17.sp
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
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFFFFFF)
                )

                Spacer(modifier = Modifier.height(14.dp))

                qrBitmap?.let { bmp ->
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp),
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
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141414)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Scan with any UPI application",
                    fontSize = 12.sp,
                    color = Color(0xFF737373),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onPayViaApp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEDEDED),
                    contentColor = Color(0xFF000000)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Open UPI App", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onCopy) {
                Text("Copy UPI ID", color = Color(0xFFEDEDED), fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }
    )
}
