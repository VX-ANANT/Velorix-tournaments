package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.AppNotification
import com.example.data.repository.RepositoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object NotificationHelper {
    const val CHANNEL_TOURNAMENT_REMINDERS = "velorix_tournament_reminders"
    const val CHANNEL_MATCH_UPDATES = "velorix_match_updates"
    const val CHANNEL_PRIZE_ANNOUNCEMENTS = "velorix_prize_announcements"
    const val CHANNEL_GENERAL = "velorix_general_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_TOURNAMENT_REMINDERS,
                "Tournament Start Times & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for upcoming tournament start times with Room ID & Password details"
                enableVibration(true)
            }

            val matchChannel = NotificationChannel(
                CHANNEL_MATCH_UPDATES,
                "Real-time Match Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live match status, schedule changes, and custom room announcements"
                enableVibration(true)
            }

            val prizeChannel = NotificationChannel(
                CHANNEL_PRIZE_ANNOUNCEMENTS,
                "Prize Announcements & Payouts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tournament winner celebrations and prize credit announcements"
                enableVibration(true)
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Esports Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General platform news, events, and community updates"
            }

            notificationManager.createNotificationChannels(
                listOf(reminderChannel, matchChannel, prizeChannel, generalChannel)
            )
        }
    }

    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun persistNotificationToDb(context: Context, notification: AppNotification) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = RepositoryManager.getInstance(context).repository
                repository.insertNotification(notification)
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Failed to persist notification to DB", e)
            }
        }
    }

    /**
     * Alert users when a tournament is starting or about to begin with Room ID and Password credentials.
     */
    fun showTournamentStartingNotification(
        context: Context,
        tournamentTitle: String,
        roomId: String = "",
        roomPass: String = "",
        timeRemaining: String = "15 minutes",
        tournamentId: String = ""
    ) {
        createNotificationChannels(context)
        val pendingIntent = createPendingIntent(context)

        val roomInfo = if (roomId.isNotBlank()) "Room ID: $roomId | Pass: $roomPass" else "Room credentials live in app"
        val title = "Tournament Starting Soon: $tournamentTitle"
        val content = "$tournamentTitle is starting in $timeRemaining! $roomInfo"

        NotificationEventBus.postEvent(title, content)

        // Save to Notification Center history
        persistNotificationToDb(
            context,
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = "$tournamentTitle is starting in $timeRemaining. Room ID: ${roomId.ifBlank { "TBD" }}, Password: ${roomPass.ifBlank { "TBD" }}. Join now to secure your slot.",
                type = "TOURNAMENT_REMINDER",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                tournamentId = tournamentId,
                tournamentTitle = tournamentTitle,
                roomId = roomId,
                roomPassword = roomPass,
                timeRemaining = timeRemaining
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TOURNAMENT_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$tournamentTitle is starting in $timeRemaining!\n\n" +
                    "$roomInfo\n\n" +
                    "Join the custom room right now to secure your slot and avoid disqualification."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    /**
     * Real-time match updates (e.g. room ready, delay notice, final circle alert, server status).
     */
    fun showMatchUpdateNotification(
        context: Context,
        tournamentTitle: String,
        updateMessage: String,
        statusType: String = "LIVE",
        tournamentId: String = ""
    ) {
        createNotificationChannels(context)
        val pendingIntent = createPendingIntent(context)

        val title = "Match Update: $tournamentTitle"
        val content = updateMessage

        NotificationEventBus.postEvent(title, content)

        // Save to Notification Center history
        persistNotificationToDb(
            context,
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = updateMessage,
                type = "MATCH_UPDATE",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                tournamentId = tournamentId,
                tournamentTitle = tournamentTitle
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MATCH_UPDATES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Tournament: $tournamentTitle\n" +
                    "Status: $statusType\n\n" +
                    "$updateMessage"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    /**
     * Prize pool announcements, winner celebrations, and wallet rewards.
     */
    fun showPrizeAnnouncementNotification(
        context: Context,
        tournamentTitle: String,
        winnerInfo: String = "",
        prizePoolText: String = "",
        customMessage: String = "",
        tournamentId: String = ""
    ) {
        createNotificationChannels(context)
        val pendingIntent = createPendingIntent(context)

        val title = "Prize Announcement: $tournamentTitle"
        val content = if (customMessage.isNotBlank()) customMessage else "Prize distribution announced for $tournamentTitle! Total Pool: $prizePoolText"

        NotificationEventBus.postEvent(title, content)

        val bodyBuilder = StringBuilder()
        bodyBuilder.append("Tournament: $tournamentTitle\n")
        if (prizePoolText.isNotBlank()) bodyBuilder.append("Total Prize Pool: $prizePoolText\n")
        if (winnerInfo.isNotBlank()) bodyBuilder.append("Winners: $winnerInfo\n")
        if (customMessage.isNotBlank()) bodyBuilder.append("\n$customMessage\n")
        bodyBuilder.append("\nWinnings are automatically credited to player wallets.")

        // Save to Notification Center history
        persistNotificationToDb(
            context,
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = bodyBuilder.toString(),
                type = "PRIZE_ANNOUNCEMENT",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                tournamentId = tournamentId,
                tournamentTitle = tournamentTitle
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PRIZE_ANNOUNCEMENTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyBuilder.toString()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    /**
     * Individual match result notification for participating user.
     */
    fun showMatchResultNotification(
        context: Context,
        tournamentTitle: String,
        position: Int,
        kills: Int,
        winnings: Double,
        tournamentId: String = ""
    ) {
        createNotificationChannels(context)
        val pendingIntent = createPendingIntent(context)

        val rankStr = when (position) {
            1 -> "1st Place (Champion)"
            2 -> "2nd Place (Runner-up)"
            3 -> "3rd Place"
            else -> "#${position} Place"
        }

        val winningsText = if (winnings > 0) "Earned VT ${winnings.toInt()} credited to your wallet!" else "Better luck in the next battle!"
        val title = "Results Published: $tournamentTitle"
        val content = "You finished $rankStr with $kills kills! $winningsText"

        NotificationEventBus.postEvent(title, content)

        // Save to Notification Center history
        persistNotificationToDb(
            context,
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = "Official match results for $tournamentTitle. Outcome: $rankStr with $kills total kills. $winningsText",
                type = "MATCH_RESULT",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                tournamentId = tournamentId,
                tournamentTitle = tournamentTitle,
                position = position,
                kills = kills,
                winnings = winnings
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PRIZE_ANNOUNCEMENTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Official match results for $tournamentTitle:\n\n" +
                    "• Outcome: $rankStr\n" +
                    "• Total Kills: $kills\n" +
                    "• Winnings: $winningsText"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    private fun getBitmapFromUrl(urlString: String?): android.graphics.Bitmap? {
        if (urlString.isNullOrBlank()) return null
        return try {
            val url = java.net.URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            val input = connection.inputStream
            android.graphics.BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * General notification fallback.
     */
    fun showGeneralNotification(
        context: Context,
        title: String,
        messageBody: String,
        tournamentId: String = "",
        imageUrl: String = ""
    ) {
        createNotificationChannels(context)
        NotificationEventBus.postEvent(title, messageBody)

        val pendingIntent = createPendingIntent(context)

        // Save to Notification Center history
        persistNotificationToDb(
            context,
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = messageBody,
                type = "GENERAL",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                tournamentId = tournamentId
            )
        )

        val bitmap = getBitmapFromUrl(imageUrl)

        val builder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as android.graphics.Bitmap?)
                    .setSummaryText(messageBody)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    }
}
