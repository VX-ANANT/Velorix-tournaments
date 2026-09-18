/**
 * MyFirebaseMessagingService.kt
 * 
 * Background service to handle Firebase Cloud Messaging (FCM).
 * 
 * Responsibilities:
 * - Receive push notifications from the backend / admin panel in real-time.
 * - Display distinct high-priority notifications for:
 *   1. Tournament start times & room credentials reminders.
 *   2. Real-time match updates & scheduling announcements.
 *   3. Prize pool announcements, winner celebrations & payout credits.
 * - Broadcast in-app alerts via NotificationEventBus when the app is in foreground.
 * - Update the FCM registration token in Firebase Realtime Database.
 */
package com.example.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Refreshed token: $token")

        try {
            val fcmPrefs = applicationContext.getSharedPreferences("velorix_fcm_prefs", android.content.Context.MODE_PRIVATE)
            fcmPrefs.edit().putString("fcm_token_cached", token).apply()
        } catch (e: Throwable) {
            Log.d("FCM_TOKEN", "Prefs cache handled: ${e.message}")
        }

        // Update the token in the repository
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = com.example.data.repository.RepositoryManager.getInstance(applicationContext).repository
                repository.updateFcmToken(token)
            } catch (e: Exception) {
                Log.e("FCM_TOKEN", "Failed to update token in backend", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("FCM_MSG", "From: ${remoteMessage.from}, data: ${remoteMessage.data}, notif: ${remoteMessage.notification?.title}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val type = (data["type"] ?: data["action"] ?: data["category"] ?: "").uppercase()
        val title = data["title"] ?: notification?.title ?: "VeloRix Esports"
        val body = data["body"] ?: data["message"] ?: notification?.body ?: ""
        val tourneyId = data["tournamentId"] ?: data["tournament_id"] ?: data["id"] ?: ""
        val imageUrl = data["image"] ?: data["imageUrl"] ?: data["icon"] ?: notification?.imageUrl?.toString() ?: ""

        when {
            // 1. Tournament Start Times & Reminders
            type == "TOURNAMENT_START" || type == "TOURNAMENT_REMINDER" || type == "START_TIME" || type == "ROOM_ALERT" -> {
                val matchTitle = data["tournamentTitle"] ?: data["title"] ?: notification?.title ?: "Esports Tournament"
                val roomId = data["roomId"] ?: data["room_id"] ?: ""
                val roomPass = data["roomPassword"] ?: data["room_password"] ?: data["roomPass"] ?: ""
                val timeRemaining = data["timeRemaining"] ?: data["startsIn"] ?: "15 minutes"
                NotificationHelper.showTournamentStartingNotification(
                    context = this,
                    tournamentTitle = matchTitle,
                    roomId = roomId,
                    roomPass = roomPass,
                    timeRemaining = timeRemaining,
                    tournamentId = tourneyId
                )
            }

            // 2. Real-time Match Updates & Schedule Changes
            type == "MATCH_UPDATE" || type == "MATCH_STATUS" || type == "SCHEDULE_UPDATE" || type == "ROOM_READY" -> {
                val matchTitle = data["tournamentTitle"] ?: data["title"] ?: notification?.title ?: "Match Update"
                val message = data["message"] ?: data["body"] ?: notification?.body ?: "Live match status has been updated."
                val statusType = data["status"] ?: "LIVE"
                NotificationHelper.showMatchUpdateNotification(
                    context = this,
                    tournamentTitle = matchTitle,
                    updateMessage = message,
                    statusType = statusType,
                    tournamentId = tourneyId
                )
            }

            // 3. Prize Announcements & Distributions
            type == "PRIZE_ANNOUNCEMENT" || type == "PRIZE_POOL" || type == "WINNER_ANNOUNCEMENT" || type == "PRIZE_PAYOUT" -> {
                val matchTitle = data["tournamentTitle"] ?: data["title"] ?: notification?.title ?: "Grand Championship"
                val winnerInfo = data["winner"] ?: data["winnerName"] ?: data["winners"] ?: ""
                val prizePoolText = data["prizePool"] ?: data["prize"] ?: data["winnings"] ?: ""
                val customMsg = data["message"] ?: data["body"] ?: notification?.body ?: ""
                NotificationHelper.showPrizeAnnouncementNotification(
                    context = this,
                    tournamentTitle = matchTitle,
                    winnerInfo = winnerInfo,
                    prizePoolText = prizePoolText,
                    customMessage = customMsg,
                    tournamentId = tourneyId
                )
            }

            // 4. Individual Match Result & Winnings
            type == "MATCH_RESULT" || type == "RESULT" -> {
                val matchTitle = data["tournamentTitle"] ?: data["title"] ?: notification?.title ?: "Match Results"
                val pos = data["position"]?.toIntOrNull() ?: data["rank"]?.toIntOrNull() ?: 1
                val kills = data["kills"]?.toIntOrNull() ?: 0
                val winnings = data["winnings"]?.toDoubleOrNull() ?: data["prize"]?.toDoubleOrNull() ?: 0.0
                NotificationHelper.showMatchResultNotification(
                    context = this,
                    tournamentTitle = matchTitle,
                    position = pos,
                    kills = kills,
                    winnings = winnings,
                    tournamentId = tourneyId
                )
            }

            // 5. General announcements / Default
            else -> {
                NotificationHelper.showGeneralNotification(
                    context = this,
                    title = title,
                    messageBody = if (body.isNotBlank()) body else "Check out the latest live esports tournaments and match updates!",
                    tournamentId = tourneyId,
                    imageUrl = imageUrl
                )
            }
        }
    }
}
