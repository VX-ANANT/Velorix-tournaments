package com.example.data.repository

import android.util.Log
import com.example.data.model.TournamentChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TournamentChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "TournamentChatRepo"
        private const val COLLECTION_TOURNAMENTS = "tournaments"
        private const val SUBCOLLECTION_CHAT = "chat_messages"

        val TACTICAL_PRESETS = listOf(
            "🎙️ Join Voice Chat / Discord",
            "🎯 Squad ready for the match!",
            "📍 Drop strategy: Play safe early",
            "⚡ Rush & hold high ground",
            "🔑 Room ID & Pass available?",
            "🛡️ Need 1 more teammate for slot!",
            "🔥 Let's get that Chicken Dinner / Booyah!"
        )
    }

    /**
     * Observes real-time chat messages for a specific tournament lobby.
     */
    fun observeMessages(tournamentId: String): Flow<List<TournamentChatMessage>> = callbackFlow {
        if (tournamentId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var listener: ListenerRegistration? = null
        try {
            val query = firestore.collection(COLLECTION_TOURNAMENTS)
                .document(tournamentId)
                .collection(SUBCOLLECTION_CHAT)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limitToLast(150)

            listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to lobby chat for $tournamentId: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            val msg = doc.toObject(TournamentChatMessage::class.java)
                            msg?.apply { id = doc.id }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse chat message ${doc.id}: ${e.message}")
                            null
                        }
                    }

                    // If completely empty on initial load, emit a welcoming lobby guidance message
                    if (messages.isEmpty()) {
                        val welcomeMsg = TournamentChatMessage(
                            id = "welcome_${tournamentId}",
                            tournamentId = tournamentId,
                            senderId = "system",
                            senderName = "VeloRix Lobby Bot",
                            senderAvatar = "",
                            senderTeam = "ADMIN",
                            text = "👋 Welcome to the match lobby! Coordinate with teammates, plan drop locations, and verify room credentials here.",
                            timestamp = System.currentTimeMillis() - 60000,
                            isSystemMessage = true,
                            messageType = "ANNOUNCEMENT"
                        )
                        trySend(listOf(welcomeMsg))
                    } else {
                        trySend(messages)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing lobby chat observer for $tournamentId", e)
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
            Log.d(TAG, "Removed lobby chat listener for $tournamentId")
        }
    }

    /**
     * Sends a text or tactical message to the tournament's real-time Firestore chat subcollection.
     */
    suspend fun sendMessage(
        tournamentId: String,
        senderId: String,
        senderName: String,
        senderAvatar: String,
        senderTeam: String,
        senderSlotNumber: Int?,
        text: String,
        messageType: String = "TEXT"
    ): Result<String> {
        return try {
            val trimmedText = text.trim()
            if (trimmedText.isEmpty()) {
                return Result.failure(IllegalArgumentException("Message content cannot be empty"))
            }

            val messageId = UUID.randomUUID().toString()
            val messageData = hashMapOf(
                "id" to messageId,
                "tournamentId" to tournamentId,
                "senderId" to senderId,
                "senderName" to senderName.ifBlank { "Player" },
                "senderAvatar" to senderAvatar,
                "senderTeam" to senderTeam,
                "senderSlotNumber" to senderSlotNumber,
                "text" to trimmedText,
                "timestamp" to System.currentTimeMillis(),
                "isSystemMessage" to (messageType == "ANNOUNCEMENT"),
                "messageType" to messageType,
                "reactionCount" to 0
            )

            firestore.collection(COLLECTION_TOURNAMENTS)
                .document(tournamentId)
                .collection(SUBCOLLECTION_CHAT)
                .document(messageId)
                .set(messageData)
                .await()

            Log.i(TAG, "Successfully sent lobby chat message to $tournamentId from $senderName")
            Result.success(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send chat message for tournament $tournamentId: ${e.message}", e)
            Result.failure(e)
        }
    }
}
