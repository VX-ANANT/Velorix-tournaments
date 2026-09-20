package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Banner
import com.example.ui.theme.GffDevanagariFontFamily
import com.example.ui.viewmodel.PlatformViewModel

/**
 * Minimalist Gradient Theme configuration helper.
 * Provides curated palette stops, luminous neon accents, and dark obsidian backings.
 */
data class BannerThemeStyle(
    val gradient: Brush,
    val heroGradient: Brush,
    val accentColor: Color,
    val accentSecondary: Color,
    val chipBackground: Color,
    val textColor: Color
)

fun getBannerThemeStyle(gradientTheme: String): BannerThemeStyle {
    return when (gradientTheme.uppercase()) {
        "AMBER_ORANGE", "AMBER_GOLD" -> BannerThemeStyle(
            gradient = Brush.linearGradient(
                listOf(
                    Color(0xFF261202),
                    Color(0xFF5C240A),
                    Color(0xFF8C320E)
                )
            ),
            heroGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFF5C240A),
                    Color(0xFF261202),
                    Color(0xFF140B05)
                )
            ),
            accentColor = Color(0xFFF59E0B),
            accentSecondary = Color(0xFFD97706),
            chipBackground = Color(0x33F59E0B),
            textColor = Color(0xFF1E1000)
        )
        "EMERALD_TEAL", "NEON_EMERALD" -> BannerThemeStyle(
            gradient = Brush.linearGradient(
                listOf(
                    Color(0xFF022018),
                    Color(0xFF064E3B),
                    Color(0xFF0F766E)
                )
            ),
            heroGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFF064E3B),
                    Color(0xFF022018),
                    Color(0xFF05120F)
                )
            ),
            accentColor = Color(0xFF10B981),
            accentSecondary = Color(0xFF059669),
            chipBackground = Color(0x3310B981),
            textColor = Color(0xFF002418)
        )
        "CRIMSON_DARK", "CRIMSON_FIRE" -> BannerThemeStyle(
            gradient = Brush.linearGradient(
                listOf(
                    Color(0xFF300606),
                    Color(0xFF681010),
                    Color(0xFF991B1B)
                )
            ),
            heroGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFF681010),
                    Color(0xFF300606),
                    Color(0xFF160404)
                )
            ),
            accentColor = Color(0xFFEF4444),
            accentSecondary = Color(0xFFDC2626),
            chipBackground = Color(0x33EF4444),
            textColor = Color.White
        )
        "DEEP_VIOLET", "PURPLE_NIGHT" -> BannerThemeStyle(
            gradient = Brush.linearGradient(
                listOf(
                    Color(0xFF160F30),
                    Color(0xFF3B1366),
                    Color(0xFF581C87)
                )
            ),
            heroGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFF3B1366),
                    Color(0xFF160F30),
                    Color(0xFF0B0718)
                )
            ),
            accentColor = Color(0xFFA855F7),
            accentSecondary = Color(0xFF8B5CF6),
            chipBackground = Color(0x33A855F7),
            textColor = Color.White
        )
        "SAPPHIRE_BLUE", "BLUE_COBALT" -> BannerThemeStyle(
            gradient = Brush.linearGradient(
                listOf(
                    Color(0xFF081A36),
                    Color(0xFF1E3A8A),
                    Color(0xFF2563EB)
                )
            ),
            heroGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFF1E3A8A),
                    Color(0xFF081A36),
                    Color(0xFF050E1E)
                )
            ),
            accentColor = Color(0xFF38BDF8),
            accentSecondary = Color(0xFF0284C7),
            chipBackground = Color(0x3338BDF8),
            textColor = Color(0xFF031A33)
        )
        "DARK_ONYX", "MINIMAL_DARK" -> BannerThemeStyle(
            gradient = Brush.linearGradient(
                listOf(
                    Color(0xFF12131A),
                    Color(0xFF20222C),
                    Color(0xFF2E313D)
                )
            ),
            heroGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFF20222C),
                    Color(0xFF12131A),
                    Color(0xFF0A0B0E)
                )
            ),
            accentColor = Color(0xFFE4E4E7),
            accentSecondary = Color(0xFFA1A1AA),
            chipBackground = Color(0x33A1A1AA),
            textColor = Color(0xFF12131A)
        )
        else -> BannerThemeStyle( // Default "CYAN_PURPLE" / "CYAN_NEON"
            gradient = Brush.linearGradient(
                listOf(
                    Color(0xFF0A1128),
                    Color(0xFF191F4D),
                    Color(0xFF312E81)
                )
            ),
            heroGradient = Brush.verticalGradient(
                listOf(
                    Color(0xFF1E1B4B),
                    Color(0xFF0A1128),
                    Color(0xFF050714)
                )
            ),
            accentColor = Color(0xFF00E5FF),
            accentSecondary = Color(0xFF8B5CF6),
            chipBackground = Color(0x3300E5FF),
            textColor = Color(0xFF031926)
        )
    }
}

/**
 * Full Interactive Banner Details & Satisfaction Review Dialog.
 */
@Composable
fun BannerDetailDialog(
    banner: Banner,
    platformViewModel: PlatformViewModel? = null,
    onNavigateToTournament: (String) -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToLeaderboard: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val style = getBannerThemeStyle(banner.gradientTheme)
    val scrollState = rememberScrollState()

    var userReaction by remember { mutableStateOf<String?>(null) }
    var isBookmarked by remember { mutableStateOf(false) }
    var isTermsExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        1.2.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                style.accentColor.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    )
                    .shadow(20.dp, RoundedCornerShape(26.dp), ambientColor = style.accentColor.copy(alpha = 0.25f)),
                color = Color(0xEE121620)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFA161B26),
                                    Color(0xF50D1017)
                                )
                            )
                        )
                        .verticalScroll(scrollState)
                ) {
                    // Hero Cover Header Section with Glass Tint
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        style.accentColor.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    ) {
                        if (banner.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = banner.imageUrl,
                                contentDescription = banner.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.40f),
                                                Color.Black.copy(alpha = 0.70f),
                                                Color(0xEE121620)
                                            )
                                        )
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                style.accentColor.copy(alpha = 0.22f),
                                                Color.Transparent
                                            ),
                                            radius = 500f
                                        )
                                    )
                            )
                        }

                        // Top Action Icons (Category Pill, Share, Close)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category / Badge Glass Pill
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = Color.White.copy(alpha = 0.10f),
                                border = BorderStroke(0.8.dp, Brush.horizontalGradient(
                                    listOf(
                                        style.accentColor.copy(alpha = 0.75f),
                                        Color.White.copy(alpha = 0.2f)
                                    )
                                ))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    val badgeIcon = when {
                                        banner.actionType.equals("MATCH", ignoreCase = true) -> Icons.Rounded.EmojiEvents
                                        banner.actionType.equals("WALLET", ignoreCase = true) -> Icons.Rounded.AccountBalanceWallet
                                        banner.actionType.equals("SUPPORT", ignoreCase = true) -> Icons.Rounded.SupportAgent
                                        banner.badgeText.contains("LIVE", ignoreCase = true) -> Icons.Rounded.LocalFireDepartment
                                        else -> Icons.Rounded.Stars
                                    }
                                    Icon(
                                        imageVector = badgeIcon,
                                        contentDescription = null,
                                        tint = style.accentColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = banner.badgeText.ifBlank { "OFFICIAL" }.uppercase(),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Bookmark Button
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        isBookmarked = !isBookmarked
                                        if (isBookmarked) {
                                            platformViewModel?.submitBannerSatisfaction(banner.id, "BOOKMARKED", "Saved announcement to bookmarks")
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(0.8.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (isBookmarked) style.accentColor else Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // Share Button
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        try {
                                            val shareText = buildString {
                                                append("${banner.title}\n\n")
                                                if (banner.subtitle.isNotBlank()) append("${banner.subtitle}\n\n")
                                                if (banner.description.isNotBlank()) append("${banner.description}\n\n")
                                                append("Check it out in the VeloRix Esports App!")
                                            }
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "Share Announcement")
                                            context.startActivity(shareIntent)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(0.8.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = "Share",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Close Button
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(0.8.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Title within Hero
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = banner.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                lineHeight = 22.sp
                            )
                            if (banner.subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = banner.subtitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Content Details Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Key Metadata Tags Row (Action Type, Target ID, Validity)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Action category pill
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(0.8.dp, style.accentColor.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "ACTION: ${banner.actionType.uppercase()}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = style.accentColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp)
                                )
                            }

                            // Validity Tag if available
                            if (banner.validUntil.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = Color(0x22F59E0B),
                                    border = BorderStroke(0.8.dp, Color(0x55F59E0B))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Schedule,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = banner.validUntil,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFCD34D)
                                        )
                                    }
                                }
                            }

                            if (banner.targetId.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = Color.White.copy(alpha = 0.06f),
                                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    Text(
                                        text = "#${banner.targetId.takeLast(8)}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Full Announcement Description / Content Card - Liquid Glass Style
                        val displayDescription = when {
                            banner.description.isNotBlank() -> banner.description
                            banner.subtitle.isNotBlank() -> banner.subtitle
                            else -> "Stay tuned with the latest broadcast event details, tournament prize distribution, and exclusive platform promotions on VeloRix Esports."
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(
                                0.8.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.20f),
                                        Color.White.copy(alpha = 0.04f)
                                    )
                                )
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Campaign,
                                        contentDescription = null,
                                        tint = style.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "BROADCAST DETAILS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.65f),
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = displayDescription,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        // Optional Terms & Conditions Accordion - Glass Surface
                        if (banner.terms.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { isTermsExpanded = !isTermsExpanded },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Gavel,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Rules & Terms of Participation",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isTermsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    AnimatedVisibility(visible = isTermsExpanded) {
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 8.dp))
                                            Text(
                                                text = banner.terms,
                                                fontSize = 11.5.sp,
                                                color = Color.White.copy(alpha = 0.7f),
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Primary Action CTA Button - Frosted Glass Gradient Pill
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onDismiss()
                                when (banner.actionType.uppercase()) {
                                    "MATCH" -> {
                                        if (banner.targetId.isNotBlank()) onNavigateToTournament(banner.targetId)
                                    }
                                    "WALLET" -> onNavigateToWallet()
                                    "SUPPORT" -> onNavigateToSupport()
                                    "LEADERBOARD" -> onNavigateToLeaderboard()
                                    "LINK" -> {
                                        if (banner.targetId.isNotBlank()) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(banner.targetId))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    else -> {
                                        if (banner.targetId.isNotBlank()) onNavigateToTournament(banner.targetId)
                                        else onNavigateToWallet()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(10.dp, RoundedCornerShape(percent = 50), ambientColor = style.accentColor.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(percent = 50),
                            color = Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        style.accentColor.copy(alpha = 0.85f),
                                        Color.White.copy(alpha = 0.35f)
                                    )
                                )
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                style.accentColor.copy(alpha = 0.28f),
                                                style.accentColor.copy(alpha = 0.15f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = banner.ctaText.ifBlank { "EXPLORE NOW" }.uppercase(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = style.accentColor,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = style.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // User Satisfaction / Review & Feedback Section
                        BannerSatisfactionReviewSection(
                            banner = banner,
                            style = style,
                            currentReaction = userReaction,
                            onReact = { reaction, note ->
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                userReaction = reaction
                                platformViewModel?.submitBannerSatisfaction(banner.id, reaction, note)
                            },
                            onOpenSupport = {
                                onDismiss()
                                onNavigateToSupport()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tailored Satisfaction & Review Component based on Banner Action Type and Category.
 */
@Composable
private fun BannerSatisfactionReviewSection(
    banner: Banner,
    style: BannerThemeStyle,
    currentReaction: String?,
    onReact: (String, String) -> Unit,
    onOpenSupport: () -> Unit
) {
    val satisfactionQuestion = when (banner.actionType.uppercase()) {
        "MATCH" -> "Are you satisfied with this tournament details & prize pool?"
        "WALLET" -> "Satisfied with this deposit bonus & reward value?"
        "SUPPORT" -> "Was this support announcement helpful?"
        "LINK", "ANNOUNCEMENT" -> "Was this broadcast update clear and helpful?"
        else -> "How satisfied are you with this announcement?"
    }

    val positiveLabel = when (banner.actionType.uppercase()) {
        "MATCH" -> "Registering / Excited"
        "WALLET" -> "Great Value Offer"
        else -> "Helpful & Clear"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(
            0.8.dp,
            Brush.verticalGradient(
                listOf(
                    style.accentColor.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.06f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.RateReview,
                    contentDescription = null,
                    tint = style.accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "COMMUNITY FEEDBACK",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = satisfactionQuestion,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (currentReaction == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Positive / Satisfied Button - Frosted Pill
                    Surface(
                        onClick = { onReact("SATISFIED", "User indicated satisfaction with banner") },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(percent = 50),
                        color = Color(0x2210B981),
                        border = BorderStroke(0.8.dp, Color(0x6610B981))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 9.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ThumbUp,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = positiveLabel,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399),
                                maxLines = 1
                            )
                        }
                    }

                    // Need Help Button - Frosted Pill
                    Surface(
                        onClick = { onReact("NEED_HELP", "User requested clarification or support") },
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(percent = 50),
                        color = Color(0x22F59E0B),
                        border = BorderStroke(0.8.dp, Color(0x66F59E0B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 9.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HelpOutline,
                                contentDescription = null,
                                tint = Color(0xFFFCD34D),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Need Help",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCD34D),
                                maxLines = 1
                            )
                        }
                    }

                    // Not Satisfied Button - Frosted Pill
                    Surface(
                        onClick = { onReact("NOT_SATISFIED", "User marked announcement as irrelevant") },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(percent = 50),
                        color = Color(0x22EF4444),
                        border = BorderStroke(0.8.dp, Color(0x44EF4444))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 9.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ThumbDown,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Irrelevant",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFCA5A5),
                                maxLines = 1
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = when (currentReaction) {
                        "SATISFIED" -> Color(0x1E10B981)
                        "NEED_HELP" -> Color(0x1EF59E0B)
                        else -> Color(0x1E64748B)
                    },
                    border = BorderStroke(
                        0.8.dp,
                        when (currentReaction) {
                            "SATISFIED" -> Color(0x5510B981)
                            "NEED_HELP" -> Color(0x55F59E0B)
                            else -> Color(0x5564748B)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (currentReaction == "SATISFIED") Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                                contentDescription = null,
                                tint = if (currentReaction == "SATISFIED") Color(0xFF34D399) else Color(0xFFFCD34D),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when (currentReaction) {
                                    "SATISFIED" -> "Feedback Submitted! Thank you for rating."
                                    "NEED_HELP" -> "Need help with this announcement?"
                                    else -> "Feedback noted. We'll improve upcoming broadcasts."
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (currentReaction == "NEED_HELP") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                onClick = onOpenSupport,
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF59E0B)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SupportAgent,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Ask Gemini 24/7 Support",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
