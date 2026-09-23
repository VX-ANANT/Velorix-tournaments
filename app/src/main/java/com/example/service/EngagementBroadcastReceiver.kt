package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class EngagementBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("EngagementReceiver", "Periodic engagement alarm received")
        try {
            val prefs = context.getSharedPreferences("velorix_engagement_prefs", Context.MODE_PRIVATE)
            val lastTime = prefs.getLong("last_engagement_notif_time", 0L)
            val now = System.currentTimeMillis()
            val minInterval = 60 * 60 * 1000L // 1 hour minimum gap

            if (now - lastTime >= minInterval) {
                NotificationHelper.showPeriodicEngagementNotification(context)
                prefs.edit().putLong("last_engagement_notif_time", now).apply()
            }

            // Reschedule next alarm for continuous background engagement (every 1.5 - 2 hours)
            EngagementNotificationScheduler.schedulePeriodicEngagement(context)
        } catch (e: Exception) {
            Log.e("EngagementReceiver", "Error processing periodic engagement: ${e.message}", e)
        }
    }
}
