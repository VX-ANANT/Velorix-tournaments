package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

object OwnerAlertManager {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val firestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            null
        }
    }

    private val rtdb by lazy {
        try {
            FirebaseDatabase.getInstance("https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app")
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Inspects incoming user messages and automatically triggers a silent alert to the owner/admin
     * if the user reports a bug, wallet delay (> 1-2 hours), payout discrepancy, or error.
     */
    fun checkAndTriggerAutoAlert(
        userMessage: String,
        userContextSummary: String?
    ) {
        val query = userMessage.lowercase()
        val isPayoutIssue = ("withdraw" in query || "payout" in query || "money" in query || "wallet" in query || "balance" in query || "upi" in query || "utr" in query) &&
                ("hour" in query || "hr" in query || "delay" in query || "not received" in query || "pending" in query || "stuck" in query || "failed" in query || "problem" in query)

        val isBugOrError = "bug" in query || "error" in query || "crash" in query || "glitch" in query || "not working" in query || "problem" in query || "cheater" in query || "hacker" in query || "report" in query || "room id" in query || "password" in query

        if (isPayoutIssue || isBugOrError) {
            val alertType = if (isPayoutIssue) "WALLET_PAYOUT_DELAY" else "GEMINI_ASSISTANT_REPORT"
            sendOwnerAlert(
                alertType = alertType,
                userQuery = userMessage,
                userContext = userContextSummary ?: "Anonymous / Unauthenticated Player"
            )
        }
    }

    /**
     * Silently dispatches an alert payload to the owner's WhatsApp/Webhook, RTDB /reports, & Firestore collection
     * so it immediately appears in the Admin Panel without interrupting the player's chat flow.
     */
    fun sendOwnerAlert(
        alertType: String,
        userQuery: String,
        userContext: String
    ) {
        val ownerPhone = try { BuildConfig.OWNER_WHATSAPP_NUMBER } catch (e: Throwable) { "" }
        val ownerWebhook = try { BuildConfig.OWNER_ALERT_WEBHOOK } catch (e: Throwable) { "" }

        val timestamp = System.currentTimeMillis()
        val incidentTimeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
        val reportId = "rep_ai_${timestamp}_${UUID.randomUUID().toString().take(6)}"

        // Extract user id / username / contact from context if present
        val uidRegex = Regex("""UID:\s*([^\s,\]]+)""").find(userContext)
        val extractedUid = uidRegex?.groupValues?.get(1) ?: "guest"
        val nameRegex = Regex("""User:\s*([^\s,(]+)""").find(userContext)
        val extractedName = nameRegex?.groupValues?.get(1) ?: "Player"
        val contactRegex = Regex("""Contact:\s*([^\s,\]]+)""").find(userContext)
        val extractedContact = contactRegex?.groupValues?.get(1) ?: ""

        val alertData = hashMapOf<String, Any>(
            "id" to reportId,
            "userId" to extractedUid,
            "userName" to extractedName,
            "userContact" to extractedContact,
            "contactInfo" to extractedContact,
            "alertType" to alertType,
            "category" to if (alertType.contains("WALLET")) "PAYMENT_WITHDRAW" else "GEMINI_AUTO_LOGGED",
            "issueCategory" to if (alertType.contains("WALLET")) "PAYMENT_WITHDRAW" else "GEMINI_AUTO_LOGGED",
            "title" to "AI Assistant Alert: ${userQuery.take(35)}...",
            "description" to userQuery,
            "message" to userQuery,
            "incidentTime" to incidentTimeStr,
            "userQuery" to userQuery,
            "userContext" to userContext,
            "targetOwnerPhone" to (if (ownerPhone.isNotBlank() && ownerPhone != "DEFAULT_OWNER_WHATSAPP_NUMBER") ownerPhone else "Configured via Secrets"),
            "timestamp" to timestamp,
            "createdAt" to timestamp,
            "updatedAt" to timestamp,
            "source" to "GEMINI_ASSISTANT",
            "status" to "PENDING",
            "priority" to if (alertType.contains("WALLET")) "HIGH" else "NORMAL"
        )

        // 1. Log alert to Firestore under 'reports', 'support_tickets', and 'admin_alerts'
        firestore?.let { db ->
            try {
                db.collection("reports").document(reportId).set(alertData)
                db.collection("support_tickets").document(reportId).set(alertData)
                db.collection("admin_alerts").document(reportId).set(alertData)
                if (extractedUid.isNotBlank() && extractedUid != "guest") {
                    db.collection("users").document(extractedUid).collection("reports").document(reportId).set(alertData)
                }
            } catch (e: Throwable) {
                Log.e("OwnerAlertManager", "Error recording Firestore admin alert", e)
            }
        }

        // 2. Log alert to Realtime Database under '/reports', '/support_tickets'
        rtdb?.let { db ->
            try {
                db.getReference("reports").child(reportId).setValue(alertData)
                db.getReference("support_tickets").child(reportId).setValue(alertData)
                if (extractedUid.isNotBlank() && extractedUid != "guest") {
                    db.getReference("users").child(extractedUid).child("reports").child(reportId).setValue(alertData)
                }
            } catch (e: Throwable) {
                Log.e("OwnerAlertManager", "Error recording RTDB admin report", e)
            }
        }

        // 3. Dispatch HTTP Webhook / WhatsApp API Gateway if webhook URL or server endpoint is configured
        if (ownerWebhook.isNotBlank() && ownerWebhook != "DEFAULT_OWNER_ALERT_WEBHOOK" && ownerWebhook.startsWith("http")) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val jsonPayload = """
                        {
                            "event": "OWNER_BUG_ALERT",
                            "alertType": "$alertType",
                            "reportId": "$reportId",
                            "ownerPhone": "${alertData["targetOwnerPhone"]}",
                            "userQuery": "${userQuery.replace("\"", "\\\"")}",
                            "userContext": "${userContext.replace("\"", "\\\"")}",
                            "timestamp": $timestamp
                        }
                    """.trimIndent()

                    val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url(ownerWebhook)
                        .post(requestBody)
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        Log.i("OwnerAlertManager", "Webhook alert dispatched to server. Code: ${response.code}")
                    }
                } catch (e: Throwable) {
                    Log.e("OwnerAlertManager", "Failed to dispatch webhook alert to owner server", e)
                }
            }
        }
    }
}

