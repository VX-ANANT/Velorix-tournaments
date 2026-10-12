package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.Tournament
import dev.chrisbanes.haze.HazeState

/**
 * Cloned directly from BitChord GlassNavBar pattern.
 * Coordinates the FloatingBottomBar (with separated profile tab) and the MiniPlayer into a unified liquid glass dock.
 */
@Composable
fun GlassNavBar(
    tabs: List<BottomTab>,
    currentRoute: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    profileTab: BottomTab? = null,
    userAvatarUrl: String? = null,
    activeTournament: Tournament? = null,
    onExpandTournament: (Tournament) -> Unit = {},
    onActionTournament: (Tournament) -> Unit = {},
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Optional Active Tournament mini player floating over tabs
            if (activeTournament != null) {
                MiniTournamentPlayer(
                    tournament = activeTournament,
                    onExpand = { onExpandTournament(activeTournament) },
                    onActionClick = { onActionTournament(activeTournament) },
                    hazeState = hazeState,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Floating Bottom Bar with Separated Profile Section
            FloatingBottomBar(
                tabs = tabs,
                currentRoute = currentRoute,
                onTabSelected = onTabSelected,
                hazeState = hazeState,
                profileTab = profileTab,
                userAvatarUrl = userAvatarUrl,
            )
        }
    }
}
