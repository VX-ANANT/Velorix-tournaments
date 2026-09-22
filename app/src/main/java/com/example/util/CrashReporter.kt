package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CrashIncidentData(
    val incidentId: String,
    val timestamp: Long,
    val formattedTime: String,
    val exceptionName: String,
    val errorMessage: String,
    val sanitizedStackTrace: String,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
    val userId: String,
    val userEmail: String,
    val originalThrowable: Throwable? = null,
    val isReported: Boolean = false
)

object CrashReporter {
    private const val TAG = "CrashReporter"

    private val _activeFatalCrash = MutableStateFlow<CrashIncidentData?>(null)
    val activeFatalCrash: StateFlow<CrashIncidentData?> = _activeFatalCrash.asStateFlow()

    private var isInitialized = false
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(true)
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("device_model", "${Build.MANUFACTURER} ${Build.MODEL}")
            crashlytics.setCustomKey("android_os", Build.VERSION.RELEASE)
            crashlytics.setCustomKey("sentinel_defense", "active")
            Log.d(TAG, "Firebase Crashlytics initialized successfully.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize Firebase Crashlytics", e)
        }

        installGlobalExceptionHandler()
    }

    private fun installGlobalExceptionHandler() {
        if (defaultHandler == null) {
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                Log.e(TAG, "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}", throwable)
                try {
                    // Record directly to Crashlytics
                    FirebaseCrashlytics.getInstance().recordException(throwable)
                    FirebaseCrashlytics.getInstance().sendUnsentReports()
                } catch (_: Throwable) {}

                // Capture and sanitize incident to prevent memory or credential leak
                val incident = createIncident(throwable)
                _activeFatalCrash.value = incident

                // Don't kill process immediately if we can display the Sentinel crash dialog
                // If it's a non-UI thread or critical lifecycle thread, we forward after short pause
                if (thread.name.contains("main", ignoreCase = true)) {
                    // Attempt to keep Compose alive for report transmission
                    CoroutineScope(Dispatchers.Main).launch {
                        _activeFatalCrash.value = incident
                    }
                } else {
                    // Non-main thread exception
                    _activeFatalCrash.value = incident
                }
            }
        }
    }

    fun setUserInfo(userId: String?, email: String?) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            if (!userId.isNullOrBlank()) {
                crashlytics.setUserId(userId)
            }
            if (!email.isNullOrBlank()) {
                crashlytics.setCustomKey("user_email", email)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to set Crashlytics user info: ${e.message}")
        }
    }

    fun log(message: String) {
        try {
            FirebaseCrashlytics.getInstance().log(message)
            Log.i(TAG, "Crashlytics Log: $message")
        } catch (_: Throwable) {}
    }

    fun logException(
        throwable: Throwable,
        message: String? = null,
        customKeys: Map<String, String> = emptyMap()
    ) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            customKeys.forEach { (k, v) ->
                crashlytics.setCustomKey(k, v)
            }
            if (!message.isNullOrBlank()) {
                crashlytics.log(message)
            }
            crashlytics.recordException(throwable)
            Log.w(TAG, "Crashlytics recorded non-fatal exception: ${throwable.message}")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to record Crashlytics exception", e)
        }
    }

    fun sanitizeText(input: String): String {
        var clean = input
        // Mask JWT / Bearer tokens
        clean = clean.replace(Regex("Bearer\\s+[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*"), "Bearer [PROTECTED_TOKEN]")
        clean = clean.replace(Regex("eyJ[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*"), "[PROTECTED_JWT]")
        // Mask Passwords
        clean = clean.replace(Regex("(?i)(password|secret|apikey|token|auth)\\s*[:=]\\s*['\"][^'\"]+['\"]"), "$1=[PROTECTED]")
        // Mask Indian Phone numbers (10 digits)
        clean = clean.replace(Regex("(\\+91[\\-\\s]?)?[6-9]\\d{9}"), "[PROTECTED_PHONE]")
        // Mask 16-digit Card or Account numbers
        clean = clean.replace(Regex("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b"), "[PROTECTED_PAYMENT_NUM]")
        return clean
    }

    fun createIncident(throwable: Throwable, user: User? = null): CrashIncidentData {
        val now = System.currentTimeMillis()
        val randomPart = UUID.randomUUID().toString().take(6).uppercase()
        val incidentId = "INC-CRASH-$randomPart"
        val timeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(now))

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val rawStack = sw.toString()
        val sanitizedStack = sanitizeText(rawStack)
        val sanitizedMsg = sanitizeText(throwable.message ?: throwable.localizedMessage ?: "Unknown Runtime Exception")

        val authUser = FirebaseAuth.getInstance().currentUser
        val effectiveUserId = user?.id ?: authUser?.uid ?: "anonymous_operative"
        val effectiveEmail = user?.phoneOrEmail ?: authUser?.email ?: "not_authenticated"

        return CrashIncidentData(
            incidentId = incidentId,
            timestamp = now,
            formattedTime = formattedTime,
            exceptionName = throwable.javaClass.simpleName.ifEmpty { "RuntimeException" },
            errorMessage = sanitizedMsg,
            sanitizedStackTrace = sanitizedStack,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            userId = effectiveUserId,
            userEmail = effectiveEmail,
            originalThrowable = throwable,
            isReported = false
        )
    }

    fun triggerSimulatedCrash(user: User?, customMessage: String = "Test Simulated Exception: VeloRix Sentinel Diagnostic Verification"): CrashIncidentData {
        val simulatedException = IllegalStateException(customMessage)
        val incident = createIncident(simulatedException, user)
        // Log to Crashlytics
        logException(simulatedException, "Simulated test crash triggered by ${user?.phoneOrEmail ?: "test_user"}")
        _activeFatalCrash.value = incident
        return incident
    }

    fun dismissFatalCrash() {
        _activeFatalCrash.value = null
    }

    fun sendCrashReportToAdmin(
        incident: CrashIncidentData,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Log to Crashlytics
                val crashlytics = FirebaseCrashlytics.getInstance()
                crashlytics.setCustomKey("incident_id", incident.incidentId)
                crashlytics.setCustomKey("reporter_user_id", incident.userId)
                crashlytics.setCustomKey("reporter_email", incident.userEmail)
                crashlytics.setCustomKey("device", incident.deviceModel)
                crashlytics.log("Operative explicitly dispatched crash report ${incident.incidentId}")
                incident.originalThrowable?.let { crashlytics.recordException(it) }
                crashlytics.sendUnsentReports()

                // 2. Prepare Structured Admin Report Payload
                val reportPayload = hashMapOf<String, Any>(
                    "id" to incident.incidentId,
                    "incidentId" to incident.incidentId,
                    "category" to "APP_BUG_CRASH",
                    "issueCategory" to "APP_BUG_CRASH",
                    "title" to "Crash Report [${incident.exceptionName}] ${incident.incidentId}",
                    "description" to """
                        Sentinel Incident: ${incident.incidentId}
                        Exception: ${incident.exceptionName}
                        Error Message: ${incident.errorMessage}
                        Device: ${incident.deviceModel}
                        OS: ${incident.androidVersion}
                        App Version: ${incident.appVersion}
                        Timestamp: ${incident.formattedTime}
                        User ID: ${incident.userId}
                        Email: ${incident.userEmail}
                        
                        --- STACK TRACE (SANITIZED) ---
                        ${incident.sanitizedStackTrace.take(3000)}
                    """.trimIndent(),
                    "exceptionType" to incident.exceptionName,
                    "errorMessage" to incident.errorMessage,
                    "sanitizedStackTrace" to incident.sanitizedStackTrace.take(4000),
                    "deviceModel" to incident.deviceModel,
                    "osVersion" to incident.androidVersion,
                    "appVersion" to incident.appVersion,
                    "userId" to incident.userId,
                    "userContact" to incident.userEmail,
                    "userName" to "Operative_${incident.userId.takeLast(4)}",
                    "status" to "PENDING",
                    "priority" to "URGENT",
                    "source" to "FATAL_CRASH_SENTINEL_DIALOG",
                    "createdAt" to incident.timestamp,
                    "timestamp" to incident.timestamp,
                    "updatedAt" to incident.timestamp
                )

                // 3. Write to Realtime Database at /crash_reports and /reports for Admin Panel
                try {
                    val rtdb = FirebaseDatabase.getInstance()
                    rtdb.getReference("crash_reports").child(incident.incidentId).setValue(reportPayload)
                    rtdb.getReference("reports").child(incident.incidentId).setValue(reportPayload)
                    rtdb.getReference("admin_reports").child("crashes").child(incident.incidentId).setValue(reportPayload)
                    Log.d(TAG, "Sent crash report ${incident.incidentId} to RTDB")
                } catch (e: Exception) {
                    Log.w(TAG, "Notice writing to RTDB crash_reports: ${e.message}")
                }

                // 4. Write to Firestore collections
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("crash_reports").document(incident.incidentId).set(reportPayload)
                    firestore.collection("reports").document(incident.incidentId).set(reportPayload)
                    Log.d(TAG, "Sent crash report ${incident.incidentId} to Firestore")
                } catch (e: Exception) {
                    Log.w(TAG, "Notice writing to Firestore crash_reports: ${e.message}")
                }

                // Update active state to marked as reported
                _activeFatalCrash.value = incident.copy(isReported = true)

                CoroutineScope(Dispatchers.Main).launch {
                    onResult(true, "Crash report ${incident.incidentId} transmitted to Admin Panel & Crashlytics successfully.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit crash report", e)
                CoroutineScope(Dispatchers.Main).launch {
                    onResult(false, "Failed to transmit report: ${e.localizedMessage ?: "Network error"}")
                }
            }
        }
    }
}
