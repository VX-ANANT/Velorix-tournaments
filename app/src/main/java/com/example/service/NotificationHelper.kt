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
    const val OFFICIAL_SENDER = "service.veloxyra@gmail.com"

    const val CHANNEL_TOURNAMENT_REMINDERS = "velorix_tournament_reminders"
    const val CHANNEL_MATCH_UPDATES = "velorix_match_updates"
    const val CHANNEL_PRIZE_ANNOUNCEMENTS = "velorix_prize_announcements"
    const val CHANNEL_TOURNAMENT_REGISTRATIONS = "velorix_tournament_registrations"
    const val CHANNEL_ENGAGEMENT = "velorix_engagement_alerts"
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

            val regChannel = NotificationChannel(
                CHANNEL_TOURNAMENT_REGISTRATIONS,
                "Tournament Registrations & Enrollments",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Confirmation when you join a tournament and alerts for open registrations"
                enableVibration(true)
            }

            val engagementChannel = NotificationChannel(
                CHANNEL_ENGAGEMENT,
                "Daily Esports & Activity Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic reminders about active scrims, bonus tokens, and leaderboards"
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Esports Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General platform news, events, and community updates"
            }

            notificationManager.createNotificationChannels(
                listOf(reminderChannel, matchChannel, prizeChannel, regChannel, engagementChannel, generalChannel)
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
            .setSubText(OFFICIAL_SENDER)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$tournamentTitle is starting in $timeRemaining!\n\n" +
                    "$roomInfo\n\n" +
                    "Join the custom room right now to secure your slot and avoid disqualification.\n\n" +
                    "Official Dispatch from: $OFFICIAL_SENDER"
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
            .setSubText(OFFICIAL_SENDER)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Tournament: $tournamentTitle\n" +
                    "Status: $statusType\n\n" +
                    "$updateMessage\n\n" +
                    "Official Dispatch from: $OFFICIAL_SENDER"
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
            .setSubText(OFFICIAL_SENDER)
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
            .setSubText(OFFICIAL_SENDER)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Official match results for $tournamentTitle:\n\n" +
                    "• Outcome: $rankStr\n" +
                    "• Total Kills: $kills\n" +
                    "• Winnings: $winningsText\n\n" +
                    "Official Dispatch from: $OFFICIAL_SENDER"
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
            .setSubText(OFFICIAL_SENDER)
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

    /**
     * Alert when a tournament is successfully joined.
     */
    fun showTournamentJoinedNotification(
        context: Context,
        tournamentTitle: String,
        slotNumber: Int,
        startTime: String = "",
        entryFee: Double = 0.0,
        tournamentId: String = ""
    ) {
        createNotificationChannels(context)
        val pendingIntent = createPendingIntent(context)

        val title = "Tournament Registration Confirmed!"
        val slotText = if (slotNumber > 0) " (Slot #$slotNumber)" else ""
        val timeText = if (startTime.isNotBlank()) " | Scheduled: $startTime" else ""
        val content = "You have secured your place in $tournamentTitle$slotText$timeText. Entry fee: ₹${entryFee.toInt()}."

        NotificationEventBus.postEvent(title, content)

        persistNotificationToDb(
            context,
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = "Registration Confirmed! You are booked for '$tournamentTitle'$slotText. Match begins at $startTime. Custom Room ID & Password will be released 15 minutes prior to match launch.",
                type = "TOURNAMENT_JOINED",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                tournamentId = tournamentId,
                tournamentTitle = tournamentTitle
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TOURNAMENT_REGISTRATIONS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(OFFICIAL_SENDER)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You have successfully enrolled in '$tournamentTitle'!\n\n" +
                    "• Slot Assigned: ${if (slotNumber > 0) "#$slotNumber" else "Auto-Allocated"}\n" +
                    "• Match Start Time: ${if (startTime.isNotBlank()) startTime else "Check Tournament Schedule"}\n" +
                    "• Room Credentials: Will be broadcasted 15 mins before kick-off\n\n" +
                    "Official Dispatch from: $OFFICIAL_SENDER"
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
     * Alert when Room ID and Password are added or released to joined players.
     */
    fun showRoomCredentialsNotification(
        context: Context,
        tournamentTitle: String,
        roomId: String,
        roomPass: String,
        tournamentId: String = ""
    ) {
        createNotificationChannels(context)
        val pendingIntent = createPendingIntent(context)

        val title = "Room ID & Password Released!"
        val content = "Room ID: $roomId | Password: $roomPass. Join the Free Fire custom room immediately!"

        NotificationEventBus.postEvent(title, "$tournamentTitle: Room ID: $roomId | Password: $roomPass")

        persistNotificationToDb(
            context,
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = "Room credentials for '$tournamentTitle' have been broadcasted.\nRoom ID: $roomId\nPassword: $roomPass\nJoin the custom room in Free Fire right away.",
                type = "ROOM_CREDENTIALS",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                tournamentId = tournamentId,
                tournamentTitle = tournamentTitle,
                roomId = roomId,
                roomPassword = roomPass
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TOURNAMENT_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(OFFICIAL_SENDER)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Custom room credentials for '$tournamentTitle' are now LIVE:\n\n" +
                    "🔑 Room ID: $roomId\n" +
                    "🔒 Password: $roomPass\n\n" +
                    "Please open Free Fire, navigate to Custom Room, enter these credentials, and occupy your designated slot. Avoid sharing credentials outside your squad.\n\n" +
                    "Broadcasted by: $OFFICIAL_SENDER"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    /**
     * Alert when tournament parameters, schedule, or rules are updated.
     */
    fun showTournamentUpdatedNotification(
        context: Context,
        tournamentTitle: String,
        updateDetails: String,
        tournamentId: String = ""
    ) {
        createNotificationChannels(context)
        val pendingIntent = createPendingIntent(context)

        val title = "Tournament Schedule Updated: $tournamentTitle"
        val content = updateDetails

        NotificationEventBus.postEvent(title, content)

        persistNotificationToDb(
            context,
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = "Tournament '$tournamentTitle' has been updated: $updateDetails",
                type = "TOURNAMENT_UPDATED",
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
            .setSubText(OFFICIAL_SENDER)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Tournament '$tournamentTitle' has received an official update:\n\n" +
                    "$updateDetails\n\n" +
                    "Check your match pass in the app for latest details.\n" +
                    "Official Dispatch from: $OFFICIAL_SENDER"
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
     * Alert when a tournament is cancelled, confirming automatic refund to wallet.
     */
    fun showTournamentCancelledNotification(
        context: Context,
        tournamentTitle: String,
        refundAmount: Double = 0.0,
        reason: String = "",
        tournamentId: String = ""
    ) {
        createNotificationChannels(context)
        val pendingIntent = createPendingIntent(context)

        val title = "Tournament Cancelled: $tournamentTitle"
        val refundText = if (refundAmount > 0) " ₹${refundAmount.toInt()} entry fee refunded to wallet." else " Free registration cancelled."
        val reasonText = if (reason.isNotBlank()) " Reason: $reason." else ""
        val content = "This tournament has been cancelled.$reasonText$refundText"

        NotificationEventBus.postEvent(title, content)

        persistNotificationToDb(
            context,
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = "Notice: Tournament '$tournamentTitle' has been cancelled.$reasonText 100% of your entry fee ($refundAmount VT) has been safely refunded to your balance.",
                type = "TOURNAMENT_CANCELLED",
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
            .setSubText(OFFICIAL_SENDER)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Notice: Tournament '$tournamentTitle' has been cancelled.\n\n" +
                    "${if (reason.isNotBlank()) "Reason: $reason\n" else ""}" +
                    "Financial Settlement: 100% of your entry fee ($refundAmount VT) has been refunded back to your wallet balance.\n\n" +
                    "Support Desk: $OFFICIAL_SENDER"
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
     * Alert users when a new upcoming tournament is open for registration.
     */
    fun showUpcomingRegistrationNotification(
        context: Context,
        tournamentTitle: String,
        gameMode: String = "Free Fire",
        prizePoolText: String = "",
        entryFee: Double = 0.0,
        tournamentId: String = ""
    ) {
        createNotificationChannels(context)
        val pendingIntent = createPendingIntent(context)

        val title = "Registrations Open: $tournamentTitle"
        val feeText = if (entryFee > 0) "Entry: ₹${entryFee.toInt()}" else "Free Entry"
        val prizeText = if (prizePoolText.isNotBlank()) " | Pool: $prizePoolText" else ""
        val content = "New $gameMode tournament is open! $feeText$prizeText. Secure your slot now!"

        NotificationEventBus.postEvent(title, content)

        persistNotificationToDb(
            context,
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = "Registrations are now officially OPEN for '$tournamentTitle'! Game: $gameMode, $feeText$prizeText. Slots are filling up fast.",
                type = "REGISTRATION_OPEN",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                tournamentId = tournamentId,
                tournamentTitle = tournamentTitle
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TOURNAMENT_REGISTRATIONS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(OFFICIAL_SENDER)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "⚡ New Tournament Enrollment Live!\n\n" +
                    "• Event: $tournamentTitle\n" +
                    "• Discipline: $gameMode\n" +
                    "• Entry: $feeText\n" +
                    "${if (prizePoolText.isNotBlank()) "• Prize Pool: $prizePoolText\n" else ""}" +
                    "Tap to select your custom slot before capacity is reached.\n\n" +
                    "Official Dispatch from: $OFFICIAL_SENDER"
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
     * Periodic re-engagement alert sent every hour or two to attract users back to the app.
     */
    fun showPeriodicEngagementNotification(context: Context) {
        createNotificationChannels(context)
        val pendingIntent = createPendingIntent(context)

        val engagementMessages = listOf(
            Pair(
                "Cash Scrims & Tournaments Live!",
                "Daily Free Fire custom scrims are live with instant cash prizes. Enter the arena and claim your glory!"
            ),
            Pair(
                "Daily Login Streak Bonus Waiting!",
                "Don't lose your login streak! Open VeloRix now to claim free bonus tokens and climb the rank tier."
            ),
            Pair(
                "New Match Slots Open!",
                "Fresh competitive brackets have been posted. Select your custom slot before the room fills up!"
            ),
            Pair(
                "Bounty Leaderboards Active!",
                "Top fraggers are racking up bounty rewards today. Jump into a squad match and take the lead!"
            ),
            Pair(
                "Weekend Championship Registration!",
                "High-roller prize pools are now live on VeloRix. Check out today's featured matches!"
            )
        )

        val selected = engagementMessages.random()
        val title = selected.first
        val body = selected.second

        NotificationEventBus.postEvent(title, body)

        persistNotificationToDb(
            context,
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = "$body\n\nOfficial Dispatch from: $OFFICIAL_SENDER",
                type = "ENGAGEMENT",
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ENGAGEMENT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(OFFICIAL_SENDER)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$body\n\n" +
                    "Compete fairly, play sharp, and earn real rewards.\n" +
                    "Official Dispatch from: $OFFICIAL_SENDER"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(778899, notification)
    }
}
