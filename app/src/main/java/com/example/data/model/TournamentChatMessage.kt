package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

/**
 * Real-time Tournament Lobby Chat Message Entity
 * Backed by Firebase Firestore for live synchronization across squad members and match competitors.
 */
@Serializable
@IgnoreExtraProperties
data class TournamentChatMessage(
    var id: String = "",
    var tournamentId: String = "",
    var senderId: String = "",
    var senderName: String = "",
    var senderAvatar: String = "",
    var senderTeam: String = "",
    var senderSlotNumber: Int? = null,
    var text: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var isSystemMessage: Boolean = false,
    var messageType: String = "TEXT", // "TEXT", "TACTICAL", "ANNOUNCEMENT", "SLOT_CLAIM"
    var reactionCount: Int = 0
)
