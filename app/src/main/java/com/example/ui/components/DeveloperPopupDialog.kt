package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.util.UpiPaymentManager

/**
 * DeveloperPopupDialog.kt
 *
 * Pixel-perfect subtle UI matching the user's reference screenshots,
 * with VX-ANANT's genuine developer credentials and links:
 * - App: VeloRix (v2.1.0)
 * - Instagram: @vx_anant
 * - X (Twitter): @vx_anant
 * - GitHub: VX-ANANT
 * - Buy Me a Coffee: buymeacoffee.com/anantisback47
 * - Patreon: patreon.com/anantisback47
 * - UPI: veloxyra.anant@fam
 * - Discord: discord.gg/velorix
 * - Star Repo: github.com/VX-ANANT/velorix-tournaments
 */
@Composable
fun DeveloperPopupDialog(
    onDismissRequest: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    val appName = "VeloRix"
    val appVersion = "2.1.0"

    val devHandle = "@vx_anant"
    val githubUser = "VX-ANANT"
    val patreonUser = "anantisback47"
    val upiId = UpiPaymentManager.PRIMARY_UPI_ID // veloxyra.anant@fam

    val instagramUrl = "https://instagram.com/vx_anant"
    val twitterUrl = "https://x.com/vx_anant"
    val githubUrl = "https://github.com/VX-ANANT/velorix-tournaments"
    val buyMeCoffeeUrl = "https://buymeacoffee.com/anantisback47"
    val patreonUrl = "https://patreon.com/anantisback47"
    val discordUrl = "https://discord.gg/velorix"

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

    fun copyToClipboard(text: String, label: String = "UPI ID") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Saved successfully: $text", Toast.LENGTH_SHORT).show()
    }

    // Palette extracted directly from reference screenshots
    val cardBg = Color(0xFF1E1718)
    val cardBorder = Color(0xFF2A2022)
    val iconSquareBg = Color(0xFF2C2224)
    val dividerColor = Color(0xFF261D1F)
    val sectionHeaderColor = Color(0xFFEDE4E5)
    val primaryTextColor = Color(0xFFF3ECEE)
    val secondaryTextColor = Color(0xFFA69E9F)
    val arrowColor = Color(0xFF8F888A)
    val pillBg = Color(0xFF2B2224)
    val pillTextColor = Color(0xFFCCC5C6)
    val continueBtnBg = Color(0xFFF8A5A5) // Soft peach/coral button from screenshot
    val continueBtnText = Color(0xFF1C1314)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(vertical = 12.dp)
            ) {
                // 1. TOP CARD: Centered App Identity & Concentric Logo
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Concentric Rings Logo
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .border(3.5.dp, Color(0xFFE84438), CircleShape)
                                .padding(5.dp)
                                .clip(CircleShape)
                                .border(3.5.dp, Color.White, CircleShape)
                                .padding(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE84438)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.velorix_logo_image),
                                contentDescription = "VeloRix Logo",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = appName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(pillBg)
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = appVersion,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = pillTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 2. SECTION: Follow Developer
                Text(
                    text = "Follow Developer",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = sectionHeaderColor,
                    modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SubtlePopupRow(
                            drawableRes = R.drawable.ic_instagram,
                            title = "Instagram",
                            subtitle = devHandle,
                            iconBg = iconSquareBg,
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            arrowColor = arrowColor,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                openUrl(instagramUrl)
                            }
                        )

                        HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                        SubtlePopupRow(
                            drawableRes = R.drawable.ic_x_twitter,
                            title = "X (Twitter)",
                            subtitle = devHandle,
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

                        SubtlePopupRow(
                            drawableRes = R.drawable.ic_github,
                            title = "GitHub",
                            subtitle = githubUser,
                            iconBg = iconSquareBg,
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            arrowColor = arrowColor,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                openUrl(githubUrl)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 3. SECTION: Support VeloRix
                Text(
                    text = "Support VeloRix",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = sectionHeaderColor,
                    modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SubtlePopupRow(
                            vectorIcon = Icons.Outlined.Coffee,
                            title = "Buy Me a Coffee",
                            subtitle = "buymeacoffee.com/$patreonUser",
                            iconBg = iconSquareBg,
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            arrowColor = arrowColor,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                openUrl(buyMeCoffeeUrl)
                            }
                        )

                        HorizontalDivider(thickness = 0.8.dp, color = dividerColor)

                        SubtlePopupRow(
                            drawableRes = R.drawable.ic_patreon,
                            title = "Patreon",
                            subtitle = "patreon.com/$patreonUser",
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

                        SubtlePopupRow(
                            drawableRes = R.drawable.ic_bhim_upi,
                            title = "UPI",
                            subtitle = upiId,
                            iconBg = iconSquareBg,
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            arrowColor = arrowColor,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                copyToClipboard(upiId, "UPI ID")
                                val upiUri = Uri.parse("upi://pay?pa=$upiId&pn=${Uri.encode("VX-ANANT (VeloRix Dev)")}&cu=INR")
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

                Spacer(modifier = Modifier.height(22.dp))

                // 4. SECTION: Community
                Text(
                    text = "Community",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = sectionHeaderColor,
                    modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SubtlePopupRow(
                            drawableRes = R.drawable.ic_discord,
                            title = "Discord",
                            subtitle = "discord.gg/velorix",
                            iconBg = iconSquareBg,
                            primaryTextColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            arrowColor = arrowColor,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                openUrl(discordUrl)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // 5. BUTTONS: Star the Repo & Continue
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
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
                            contentDescription = "Star",
                            tint = primaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Star the Repo",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = primaryTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = continueBtnBg,
                        contentColor = continueBtnText
                    )
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SubtlePopupRow(
    title: String,
    subtitle: String,
    iconBg: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    arrowColor: Color,
    onClick: () -> Unit,
    vectorIcon: ImageVector? = null,
    drawableRes: Int? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            if (vectorIcon != null) {
                Icon(
                    imageVector = vectorIcon,
                    contentDescription = title,
                    tint = primaryTextColor,
                    modifier = Modifier.size(22.dp)
                )
            } else if (drawableRes != null) {
                Icon(
                    painter = painterResource(drawableRes),
                    contentDescription = title,
                    tint = primaryTextColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryTextColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Go",
            tint = arrowColor,
            modifier = Modifier.size(18.dp)
        )
    }
}
