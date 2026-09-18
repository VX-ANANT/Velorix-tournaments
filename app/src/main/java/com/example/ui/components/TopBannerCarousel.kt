package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Banner
import com.example.ui.viewmodel.PlatformViewModel
import kotlinx.coroutines.delay

/**
 * Dynamic top promotional banner component for the Dashboard that fetches real campaigns from Firestore/RTDB.
 *
 * Rules:
 * - If 0 banners exist in admin panel / database, nothing is rendered.
 * - When banners are added in admin panel, they are rendered with minimalist gradients matching the dark theme.
 * - Clicking a banner opens the BannerDetailDialog.
 */
@Composable
fun TopBannerCarousel(
    banners: List<Banner>,
    onNavigateToTournament: (String) -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToLeaderboard: () -> Unit = {},
    platformViewModel: PlatformViewModel? = null,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return

    val haptic = LocalHapticFeedback.current
    val pagerState = rememberPagerState(pageCount = { banners.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    var selectedBannerForDetail by remember { mutableStateOf<Banner?>(null) }

    // Auto-scroll loop every 5 seconds when not interacting
    LaunchedEffect(isDragged, banners.size) {
        if (banners.size > 1 && !isDragged) {
            while (true) {
                delay(5000)
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 600)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp),
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) { page ->
            val banner = banners[page]
            BannerCard(
                banner = banner,
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    selectedBannerForDetail = banner
                }
            )
        }

        // Adaptive pill page indicators
        if (banners.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(banners.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 6.dp,
                        animationSpec = tween(300),
                        label = "indicatorWidth"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(5.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                    )
                }
            }
        }
    }

    // Detail & Satisfaction Modal Dialog
    selectedBannerForDetail?.let { banner ->
        BannerDetailDialog(
            banner = banner,
            platformViewModel = platformViewModel,
            onNavigateToTournament = onNavigateToTournament,
            onNavigateToWallet = onNavigateToWallet,
            onNavigateToSupport = onNavigateToSupport,
            onNavigateToLeaderboard = onNavigateToLeaderboard,
            onDismiss = { selectedBannerForDetail = null }
        )
    }
}

@Composable
private fun BannerCard(
    banner: Banner,
    onClick: () -> Unit
) {
    val style = getBannerThemeStyle(banner.gradientTheme)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), ambientColor = style.accentColor.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(24.dp))
            .background(style.gradient)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        style.accentColor.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Background Image with gradient darkening scrim
        if (banner.imageUrl.isNotBlank()) {
            AsyncImage(
                model = banner.imageUrl,
                contentDescription = banner.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.88f),
                                Color.Black.copy(alpha = 0.60f),
                                style.accentColor.copy(alpha = 0.25f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Badge Pill & Validity Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, style.accentColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val icon = when {
                            banner.badgeText.contains("LIVE", ignoreCase = true) -> Icons.Rounded.LocalFireDepartment
                            banner.badgeText.contains("GEMINI", ignoreCase = true) || banner.badgeText.contains("AI", ignoreCase = true) -> Icons.Rounded.Bolt
                            else -> Icons.Rounded.Stars
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = style.accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = banner.badgeText.ifBlank { "FEATURED" }.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                if (banner.validUntil.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(0.8.dp, Color(0x66F59E0B), RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFFCD34D),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = banner.validUntil,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFCD34D)
                            )
                        }
                    }
                }
            }

            // Banner Title & Subtitle
            Column {
                Text(
                    text = banner.title,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 1,
                    lineHeight = 20.sp
                )
                if (banner.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = banner.subtitle,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 2,
                        lineHeight = 15.sp
                    )
                }
            }

            // Bottom CTA Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(style.accentColor)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = banner.ctaText.ifBlank { "EXPLORE" }.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = style.textColor,
                            letterSpacing = 0.5.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = style.textColor,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Text(
                    text = "Tap for details",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
