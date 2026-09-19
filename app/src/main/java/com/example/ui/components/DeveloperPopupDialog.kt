package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.R
import com.example.util.UpiPaymentManager

/**
 * DeveloperPopupDialog.kt
 *
 * Professional modal popup window featuring:
 * - Real-time Gaussian window background blur (API 31+) & subtle darkened scrim
 * - Centered modal window layout (not full-screen) with smooth spring scaling
 * - Deep Navy Blue logo background canvas with precision border rings
 * - Authentic developer profile credentials & direct interaction vectors
 * - Touch-outside dismissal & Android system back-press integration
 */
@Composable
fun DeveloperPopupDialog(
    onDismissRequest: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    val appName = "VeloRix Tournaments"
    val appVersion = BuildConfig.VERSION_NAME

    val instagramHandle = "@anant_sgh"
    val xHandle = "@Anant__sgh"
    val githubRepo = "VX-ANANT/Velorix-tournaments"
    val upiId = UpiPaymentManager.PRIMARY_UPI_ID

    val instagramUrl = "https://instagram.com/anant_sgh"
    val twitterUrl = "https://x.com/Anant__sgh"
    val githubUrl = "https://github.com/VX-ANANT/Velorix-tournaments"
    val patreonUrl = "https://patreon.com/Anant_sgh"
    val discordUrl = "https://discord.gg/ghxrpQAAC2"

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not launch link", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(text: String, label: String = "UPI ID") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard: $text", Toast.LENGTH_SHORT).show()
    }

    // High-end Cybernetic Obsidian + Navy Palette (Refined, Zero Tacky Colors)
    val modalBackground = Color(0xF80E131A)
    val cardSectionBg = Color(0xFF131923)
    val cardSectionBorder = Color(0xFF1E2636)
    val iconSquareBg = Color(0xFF18202D)
    val dividerColor = Color(0xFF1A2230)
    val sectionHeaderColor = Color(0xFFCBD5E1)
    val primaryTextColor = Color(0xFFF8FAFC)
    val secondaryTextColor = Color(0xFF94A3B8)
    val arrowColor = Color(0xFF64748B)

    // Dedicated Deep Navy Blue Palette for Logo Emblem
    val navyLogoBg = Color(0xFF0A192F) // Deep Authority Navy Blue
    val navyLogoOuterRing = Color(0xFF0F2644)
    val navyLogoBorder = Color(0xFF1E3A5F)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Hardware Gaussian Background Blur behind Dialog Window (Android 12+)
        ApplyDialogWindowBlur(dimAmount = 0.55f, blurRadius = 110)

        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            isVisible = true
        }

        // Full-screen click-outside dismiss container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x40000000))
                .systemBarsPadding()
                .padding(horizontal = 18.dp, vertical = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismissRequest()
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.90f,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = 380f
                            )
                        ),
                exit = fadeOut(animationSpec = tween(150)) +
                        scaleOut(targetScale = 0.92f, animationSpec = tween(150))
            ) {
                // Centered Modal Window Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 410.dp)
                        .heightIn(max = 680.dp)
                        .shadow(
                            elevation = 32.dp,
                            shape = RoundedCornerShape(26.dp),
                            ambientColor = Color(0x99000000),
                            spotColor = Color(0xDD000000)
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x38FFFFFF),
                                        Color(0x14FFFFFF),
                                        Color(0x06FFFFFF)
                                    )
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Prevent outside dismiss when clicking inside the window
                        },
                    shape = RoundedCornerShape(26.dp),
                    color = modalBackground
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        // HyperOS Subtle Top Handle Accent
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 38.dp, height = 4.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0x33FFFFFF))
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // 1. BRAND HEADER WITH DEEP NAVY BLUE LOGO CANVAS
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Concentric Deep Navy Blue Emblem Frame
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(navyLogoOuterRing)
                                    .border(1.5.dp, navyLogoBorder, CircleShape)
                                    .padding(6.dp)
                                    .clip(CircleShape)
                                    .background(navyLogoBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.velorix_logo_image),
                                    contentDescription = "VeloRix Emblem",
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = appName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Monospaced Version Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Text(
                                    text = appVersion,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 2. DEVELOPER & COMMUNITY SECTION
                        Text(
                            text = "Developer & Source",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = sectionHeaderColor,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardSectionBg,
                            border = BorderStroke(1.dp, cardSectionBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ProfessionalDialogRow(
                                    drawableRes = R.drawable.ic_github,
                                    title = "GitHub Repository",
                                    subtitle = githubRepo,
                                    iconBg = iconSquareBg,
                                    primaryTextColor = primaryTextColor,
                                    secondaryTextColor = secondaryTextColor,
                                    arrowColor = arrowColor,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(githubUrl)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                ProfessionalDialogRow(
                                    drawableRes = R.drawable.ic_discord,
                                    title = "Discord Community",
                                    subtitle = "discord.gg/ghxrpQAAC2",
                                    iconBg = iconSquareBg,
                                    primaryTextColor = primaryTextColor,
                                    secondaryTextColor = secondaryTextColor,
                                    arrowColor = arrowColor,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(discordUrl)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                ProfessionalDialogRow(
                                    drawableRes = R.drawable.ic_x_twitter,
                                    title = "X (Twitter)",
                                    subtitle = xHandle,
                                    iconBg = iconSquareBg,
                                    primaryTextColor = primaryTextColor,
                                    secondaryTextColor = secondaryTextColor,
                                    arrowColor = arrowColor,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(twitterUrl)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                ProfessionalDialogRow(
                                    drawableRes = R.drawable.ic_instagram,
                                    title = "Instagram",
                                    subtitle = instagramHandle,
                                    iconBg = iconSquareBg,
                                    primaryTextColor = primaryTextColor,
                                    secondaryTextColor = secondaryTextColor,
                                    arrowColor = arrowColor,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(instagramUrl)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3. PROJECT BACKING SECTION
                        Text(
                            text = "Backing & Support",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = sectionHeaderColor,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardSectionBg,
                            border = BorderStroke(1.dp, cardSectionBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ProfessionalDialogRow(
                                    drawableRes = R.drawable.ic_patreon,
                                    title = "Patreon Backing",
                                    subtitle = "patreon.com/Anant_sgh",
                                    iconBg = iconSquareBg,
                                    primaryTextColor = primaryTextColor,
                                    secondaryTextColor = secondaryTextColor,
                                    arrowColor = arrowColor,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        openUrl(patreonUrl)
                                    }
                                )

                                HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                                ProfessionalDialogRow(
                                    drawableRes = R.drawable.ic_upi_logo,
                                    title = "Direct UPI (India)",
                                    subtitle = upiId,
                                    iconBg = iconSquareBg,
                                    primaryTextColor = primaryTextColor,
                                    secondaryTextColor = secondaryTextColor,
                                    arrowColor = arrowColor,
                                    iconTint = null,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        copyToClipboard(upiId, "UPI ID")
                                        val upiUri = Uri.parse("upi://pay?pa=$upiId&pn=${Uri.encode("Anant (VeloRix)")}&cu=INR")
                                        val intent = Intent(Intent.ACTION_VIEW, upiUri).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 4. ACTION CONTROLS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Star Repo Secondary Button
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = cardSectionBg,
                                border = BorderStroke(1.dp, cardSectionBorder),
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    openUrl(githubUrl)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.StarOutline,
                                        contentDescription = "Star Repo",
                                        tint = primaryTextColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Star Repo",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = primaryTextColor
                                    )
                                }
                            }

                            // Dismiss Primary Button
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    onDismissRequest()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF1F5F9),
                                    contentColor = Color(0xFF0F172A)
                                )
                            ) {
                                Text(
                                    text = "Continue",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalDialogRow(
    title: String,
    subtitle: String,
    iconBg: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    arrowColor: Color,
    onClick: () -> Unit,
    vectorIcon: ImageVector? = null,
    drawableRes: Int? = null,
    iconTint: Color? = primaryTextColor
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            if (vectorIcon != null) {
                Icon(
                    imageVector = vectorIcon,
                    contentDescription = title,
                    tint = primaryTextColor,
                    modifier = Modifier.size(20.dp)
                )
            } else if (drawableRes != null) {
                if (iconTint != null) {
                    Icon(
                        painter = painterResource(drawableRes),
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(drawableRes),
                        contentDescription = title,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryTextColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Navigate",
            tint = arrowColor,
            modifier = Modifier.size(16.dp)
        )
    }
}
