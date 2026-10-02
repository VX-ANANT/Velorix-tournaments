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
import com.example.data.db.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private suspend fun isUserJoinedTournament(tourneyId: String): Boolean {
        if (tourneyId.isBlank()) return false
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val localUser = db.userDao().getUserSync()
            val currentUid = localUser?.id ?: FirebaseAuth.getInstance().currentUser?.uid

            // 1. Check local Room database cache
            val localMatch = db.tournamentDao().getById(tourneyId)
            if (localMatch?.joined == true) {
                return true
            }

            // 2. Check local participant table
            if (!currentUid.isNullOrBlank()) {
                val participant = db.tournamentParticipantDao().getParticipantSync(tourneyId, currentUid)
                if (participant != null) {
                    return true
                }
            }

            // 3. Fallback check against Firebase Realtime Database
            if (!currentUid.isNullOrBlank()) {
                val snapshot = FirebaseDatabase.getInstance().reference
                    .child("tournaments")
                    .child(tourneyId)
                    .child("participants")
                    .child(currentUid)
                    .get()
                    .await()
                if (snapshot.exists()) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w("FCM_JOIN_CHECK", "Verification fallback error: ${e.message}")
        }
        return false
    }

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
            // 1. Tournament Joined / Registration Confirmed
            type == "TOURNAMENT_JOINED" || type == "REGISTRATION_CONFIRMED" || type == "JOIN_CONFIRMED" -> {
                val matchTitle = data["tournamentTitle"] ?: data["title"] ?: notification?.title ?: "Esports Tournament"
                val slot = data["slotNumber"]?.toIntOrNull() ?: data["slot"]?.toIntOrNull() ?: 0
                val startTime = data["startTime"] ?: data["schedule"] ?: ""
                val entryFee = data["entryFee"]?.toDoubleOrNull() ?: 0.0
                NotificationHelper.showTournamentJoinedNotification(
                    context = this,
                    tournamentTitle = matchTitle,
                    slotNumber = slot,
                    startTime = startTime,
                    entryFee = entryFee,
                    tournamentId = tourneyId
                )
            }

            // 2. Room ID & Password Added / Released (STRICT: ONLY JOINED PARTICIPANTS)
            type == "ROOM_CREDENTIALS" || type == "ROOM_RELEASED" || type == "ROOM_READY" || type == "ROOM_ALERT" -> {
                val matchTitle = data["tournamentTitle"] ?: data["title"] ?: notification?.title ?: "Esports Match"
                val roomId = data["roomId"] ?: data["room_id"] ?: ""
                val roomPass = data["roomPassword"] ?: data["room_password"] ?: data["roomPass"] ?: ""

                CoroutineScope(Dispatchers.IO).launch {
                    val isJoined = isUserJoinedTournament(tourneyId)
                    if (isJoined) {
                        NotificationHelper.showRoomCredentialsNotification(
                            context = applicationContext,
                            tournamentTitle = matchTitle,
                            roomId = roomId,
                            roomPass = roomPass,
                            tournamentId = tourneyId
                        )
                    } else {
                        Log.w(
                            "FCM_SECURITY",
                            "Suppressed room credentials for tournament '$tourneyId': User is NOT registered in this match."
                        )
                    }
                }
            }

            // 3. Tournament Start Times & Pre-Match Reminders
            type == "TOURNAMENT_START" || type == "TOURNAMENT_REMINDER" || type == "START_TIME" -> {
                val matchTitle = data["tournamentTitle"] ?: data["title"] ?: notification?.title ?: "Esports Tournament"
                val roomId = data["roomId"] ?: data["room_id"] ?: ""
                val roomPass = data["roomPassword"] ?: data["room_password"] ?: data["roomPass"] ?: ""
                val timeRemaining = data["timeRemaining"] ?: data["startsIn"] ?: "15 minutes"

                CoroutineScope(Dispatchers.IO).launch {
                    val isJoined = isUserJoinedTournament(tourneyId)
                    if (isJoined) {
                        NotificationHelper.showTournamentStartingNotification(
                            context = applicationContext,
                            tournamentTitle = matchTitle,
                            roomId = roomId,
                            roomPass = roomPass,
                            timeRemaining = timeRemaining,
                            tournamentId = tourneyId
                        )
                    } else {
                        // User is not registered in this tournament: show upcoming tournament reminder without private room credentials
                        NotificationHelper.showUpcomingRegistrationNotification(
                            context = applicationContext,
                            tournamentTitle = matchTitle,
                            gameMode = data["game"] ?: "Free Fire",
                            prizePoolText = data["prizePool"] ?: "",
                            entryFee = data["entryFee"]?.toDoubleOrNull() ?: 0.0,
                            tournamentId = tourneyId
                        )
                    }
                }
            }

            // 3.5. Real-Time Wallet & Financial Updates (Deposits, Withdrawals, Winnings, Refunds)
            type == "WALLET_UPDATE" || type == "WALLET_DEPOSIT" || type == "WALLET_WITHDRAWAL" ||
            type == "WALLET_CREDIT" || type == "WALLET_REFUND" || type == "DEPOSIT_CONFIRMED" ||
            type == "WITHDRAWAL_APPROVED" || type == "WINNINGS_CREDITED" || type == "WALLET" -> {
                val amount = data["amount"]?.toDoubleOrNull() ?: 0.0
                val newBalance = data["balance"]?.toDoubleOrNull() ?: data["newBalance"]?.toDoubleOrNull()
                val txId = data["txId"] ?: data["transactionId"] ?: ""
                val txType = data["txType"] ?: type

                NotificationHelper.showWalletUpdateNotification(
                    context = this,
                    title = title,
                    message = body,
                    amount = amount,
                    type = txType,
                    newBalance = newBalance,
                    txId = txId
                )

                // Trigger background refresh of wallet balances & transactions in local Room DB
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = com.example.data.repository.RepositoryManager.getInstance(applicationContext).repository
                        repository.fetchDataFromServer()
                    } catch (e: Exception) {
                        Log.d("FCM_WALLET", "Silent sync error: ${e.message}")
                    }
                }
            }

            // 4. Upcoming Tournament Registrations Open
            type == "REGISTRATION_OPEN" || type == "UPCOMING_TOURNAMENT" || type == "NEW_TOURNAMENT" -> {
                val matchTitle = data["tournamentTitle"] ?: data["title"] ?: notification?.title ?: "Championship Tournament"
                val gameMode = data["game"] ?: data["gameMode"] ?: "Free Fire"
                val prizePoolText = data["prizePool"] ?: data["prize"] ?: ""
                val entryFee = data["entryFee"]?.toDoubleOrNull() ?: 0.0
                NotificationHelper.showUpcomingRegistrationNotification(
                    context = this,
                    tournamentTitle = matchTitle,
                    gameMode = gameMode,
                    prizePoolText = prizePoolText,
                    entryFee = entryFee,
                    tournamentId = tourneyId
                )
            }

            // 5. Tournament Updated (Schedule / Rules / Map / Mode Change)
            type == "TOURNAMENT_UPDATED" || type == "SCHEDULE_UPDATE" || type == "SCHEDULE_CHANGE" -> {
                val matchTitle = data["tournamentTitle"] ?: data["title"] ?: notification?.title ?: "Tournament Notice"
                val details = data["updateDetails"] ?: data["message"] ?: data["body"] ?: notification?.body ?: "Tournament schedule or details have been updated."
                NotificationHelper.showTournamentUpdatedNotification(
                    context = this,
                    tournamentTitle = matchTitle,
                    updateDetails = details,
                    tournamentId = tourneyId
                )
            }

            // 6. Tournament Cancelled & Refund
            type == "TOURNAMENT_CANCELLED" || type == "CANCELLED" || type == "MATCH_CANCELLED" -> {
                val matchTitle = data["tournamentTitle"] ?: data["title"] ?: notification?.title ?: "Esports Tournament"
                val refund = data["refundAmount"]?.toDoubleOrNull() ?: data["refund"]?.toDoubleOrNull() ?: data["entryFee"]?.toDoubleOrNull() ?: 0.0
                val reason = data["reason"] ?: data["message"] ?: ""
                NotificationHelper.showTournamentCancelledNotification(
                    context = this,
                    tournamentTitle = matchTitle,
                    refundAmount = refund,
                    reason = reason,
                    tournamentId = tourneyId
                )
            }

            // 7. Periodic Engagement Alert
            type == "ENGAGEMENT" || type == "PERIODIC_ALERT" || type == "REENGAGE" -> {
                NotificationHelper.showPeriodicEngagementNotification(context = this)
            }

            // 8. Real-time Match Updates & In-Game Status
            type == "MATCH_UPDATE" || type == "MATCH_STATUS" -> {
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

            // 9. Prize Announcements & Distributions
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

            // 10. Individual Match Result & Winnings
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

            // 11. General announcements / Default
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
