package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Tournament
import dev.chrisbanes.haze.HazeState

/**
 * Cloned directly from BitChord MiniPlayer structure in the context of esports matches.
 * Floating frosted glass pill that docks directly above the FloatingBottomBar.
 */
@Composable
fun MiniTournamentPlayer(
    tournament: Tournament?,
    onExpand: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isLoading: Boolean = false,
) {
    AnimatedVisibility(
        visible = tournament != null,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut(),
        modifier = modifier
    ) {
        if (tournament == null) return@AnimatedVisibility

        val shape = RoundedCornerShape(percent = 50)
        val container = MaterialTheme.colorScheme.surface
        val useGlass = LocalLiquidGlassEnabled.current && isGlassSupported()

        Box(
            modifier = Modifier
                .padding(horizontal = BAR_GUTTER)
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (useGlass && LocalAppBackdrop.current != null) {
                        Modifier.liquidGlass(shape = shape)
                    } else if (hazeState != null) {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            shape = shape,
                            style = bitChordGlassStyle(container, blurRadius = 16.dp, alpha = 0.20f),
                        )
                    } else {
                        Modifier.background(container.copy(alpha = 0.40f))
                    }
                )
                .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
                .clickable(onClick = onExpand)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Artwork / Thumbnail
                val context = LocalContext.current
                val cleanUrl = tournament.imageUrl.trim().trim('"', '\'')
                val model = if (cleanUrl.isNotBlank() && cleanUrl != "null") cleanUrl else null

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (model != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(model)
                                .crossfade(true)
                                .build(),
                            contentDescription = tournament.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Title & Subtitle Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tournament.title.ifBlank { "Live Tournament" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            tournament.status.equals("live", true) -> "🔴 LIVE NOW • ${tournament.mapType.ifBlank { "Custom" }}"
                            tournament.dateTimeStr.isNotBlank() -> "Starts ${tournament.dateTimeStr} • ${tournament.filledSlots}/${tournament.maxSlots} Joined"
                            else -> "${tournament.format} • ${tournament.filledSlots}/${tournament.maxSlots} Slots"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Action Glyph / Status
                if (isLoading) {
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    IconButton(
                        onClick = onActionClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Open Lobby",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
