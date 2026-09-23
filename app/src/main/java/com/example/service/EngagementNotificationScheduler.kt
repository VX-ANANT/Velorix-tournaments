package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object EngagementNotificationScheduler {
    private const val ENGAGEMENT_REQUEST_CODE = 445566
    private const val INTERVAL_MILLIS = 90 * 60 * 1000L // 1.5 hours (within the 1 to 2 hour requested window)

    /**
     * Schedules periodic engagement notification via AlarmManager.
     */
    fun schedulePeriodicEngagement(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, EngagementBroadcastReceiver::class.java)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ENGAGEMENT_REQUEST_CODE,
                intent,
                flags
            )

            val triggerTime = System.currentTimeMillis() + INTERVAL_MILLIS

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("EngagementScheduler", "Periodic engagement notification scheduled for ${INTERVAL_MILLIS / 60000} mins")
        } catch (e: Exception) {
            Log.e("EngagementScheduler", "Failed to schedule engagement notification", e)
        }
    }

    /**
     * Checks when the app is foregrounded or opened, and delivers an engagement alert if
     * 1 to 2 hours have passed since the previous notification.
     */
    fun checkAndTriggerInAppEngagement(context: Context) {
        try {
            val prefs = context.getSharedPreferences("velorix_engagement_prefs", Context.MODE_PRIVATE)
            val lastTime = prefs.getLong("last_engagement_notif_time", 0L)
            val now = System.currentTimeMillis()
            val minInterval = 60 * 60 * 1000L // 1 hour

            if (now - lastTime >= minInterval) {
                NotificationHelper.showPeriodicEngagementNotification(context)
                prefs.edit().putLong("last_engagement_notif_time", now).apply()
            }
        } catch (e: Exception) {
            Log.e("EngagementScheduler", "Error in checkAndTriggerInAppEngagement", e)
        }
    }
}
