package com.example.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.ui.theme.NeonGreen
import com.example.util.AvatarHelper

val PRESET_AVATARS = AvatarHelper.PRESET_AVATARS.map { it.url to it.name }

@Composable
fun UserAvatar(
    avatarUrl: String,
    username: String,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    showEditBadge: Boolean = false,
    animateGlow: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val isAnimated = animateGlow && size >= 64.dp

    val ringBrush = if (isAnimated) {
        val infiniteTransition = rememberInfiniteTransition(label = "avatarGlow")
        val glowColor by infiniteTransition.animateColor(
            initialValue = NeonGreen,
            targetValue = primaryColor,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowColor"
        )
        Brush.sweepGradient(listOf(glowColor, secondaryColor, glowColor))
    } else {
        remember(primaryColor, secondaryColor) {
            Brush.sweepGradient(listOf(primaryColor, secondaryColor, primaryColor))
        }
    }

    val resolvedModel = remember(avatarUrl) {
        AvatarHelper.resolveAvatarModel(context, avatarUrl)
    }

    Box(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing border ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(ringBrush)
                .padding(2.5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (resolvedModel.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(resolvedModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = "$username's Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size((size.value * 0.28f).dp.coerceAtLeast(14.dp)),
                                strokeWidth = 2.dp,
                                color = NeonGreen
                            )
                        }
                    },
                    success = {
                        SubcomposeAsyncImageContent()
                    },
                    error = {
                        AvatarInitialsFallback(username = username, size = size)
                    }
                )
            } else {
                AvatarInitialsFallback(username = username, size = size)
            }
        }

        if (showEditBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size.value * 0.32f).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Edit Profile Picture",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size((size.value * 0.18f).dp)
                )
            }
        }
    }
}

@Composable
fun AvatarInitialsFallback(
    username: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val initials = username.trim().take(2).uppercase().ifEmpty { "VR" }
        Text(
            text = initials,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
