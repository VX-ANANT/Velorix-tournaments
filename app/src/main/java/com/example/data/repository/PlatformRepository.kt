/**
 * PlatformRepository.kt
 * 
 * The Single Source of Truth for the application's data.
 * 
 * Connected directly to Firebase Realtime Database:
 * https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app
 * 
 * Synchronizes with Room SQLite locally and Firebase RTDB in real time so the Admin Panel
 * and User App are 100% in sync.
 */
package com.example.data.repository

import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.AppNotification
import com.example.data.model.Banner
import com.example.data.model.LeaderboardPlayer
import com.example.data.model.Tournament
import com.example.data.model.Transaction
import com.example.data.model.User
import com.example.data.model.Mission
import com.example.data.model.UserReport
import com.example.data.api.OwnerAlertManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class PlatformRepository(
    private val db: AppDatabase,
    private val listenerManager: RepositoryManager? = null
) {

    companion object {
        private const val TAG = "PlatformRepository"
        private const val RTDB_URL = "https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app"

        fun getInstance(context: android.content.Context): PlatformRepository {
            return RepositoryManager.getInstance(context).repository
        }

        fun getTodayIstDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            return sdf.format(Date())
        }

        fun getYesterdayIstDate(): String {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            return sdf.format(cal.time)
        }
    }

    var dbErrorCallback: ((String) -> Unit)? = null
    
    // Realtime Database root reference
    private val rtdb: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance(RTDB_URL)
    }

    private val usersRef get() = rtdb.getReference("users")
    private val tournamentsRef get() = rtdb.getReference("tournaments")
    private val transactionsRef get() = rtdb.getReference("transactions")
    private val withdrawRequestsRef get() = rtdb.getReference("withdraw_requests")
    private val depositRequestsRef get() = rtdb.getReference("deposit_requests")
    private val walletRequestsRef get() = rtdb.getReference("wallet_requests")
    private val registrationsRef get() = rtdb.getReference("registrations")
    private val tournamentRegistrationsRef get() = rtdb.getReference("tournament_registrations")
    private val auditLogsRef get() = rtdb.getReference("audit_logs")
    private val verifiedBankDepositsRef get() = rtdb.getReference("verified_bank_deposits")
    private val processedUtrsRef get() = rtdb.getReference("processed_utrs")
    private val deletionRequestsRef get() = rtdb.getReference("account_deletion_requests")
    private val leaderboardRef get() = rtdb.getReference("leaderboard")
    private val bannersRef get() = rtdb.getReference("banners")
    private val missionsRef get() = rtdb.getReference("missions")
    private val userMissionsRef get() = rtdb.getReference("user_missions")
    private val notificationsRef get() = rtdb.getReference("notifications")
    private val reportsRef get() = rtdb.getReference("reports")
    private val supportTicketsRef get() = rtdb.getReference("support_tickets")

    private var lastFetchTime: Long = 0
    private val FETCH_COOLDOWN_MS = 10000L // 10s cooldown

    private val _isFirebaseConnected = kotlinx.coroutines.flow.MutableStateFlow(true)
    val isFirebaseConnected: kotlinx.coroutines.flow.StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private val _isSyncing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSyncing: kotlinx.coroutines.flow.StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncedTimestamp = kotlinx.coroutines.flow.MutableStateFlow(System.currentTimeMillis())
    val lastSyncedTimestamp: kotlinx.coroutines.flow.StateFlow<Long> = _lastSyncedTimestamp.asStateFlow()

    val user: Flow<User?> = db.userDao().getUser()
    suspend fun getUserSync(): User? = db.userDao().getUserSync()
    val tournaments: Flow<List<Tournament>> = db.tournamentDao().getAll()
    val transactions: Flow<List<Transaction>> = db.transactionDao().getAll()
    val matchStats: Flow<List<com.example.data.model.MatchStat>> = db.matchStatDao().getAll()
    val leaderboard: Flow<List<LeaderboardPlayer>> = db.leaderboardDao().getAll()
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchHistory: Flow<List<String>> = user.flatMapLatest { u ->
        val uid = u?.id?.takeIf { it.isNotBlank() } ?: "guest"
        db.searchHistoryDao().getRecentSearches(uid)
    }
    val missions: Flow<List<Mission>> = db.missionDao().getAllMissions()
    val banners: Flow<List<Banner>> = db.bannerDao().getActiveBanners()
    val notifications: Flow<List<AppNotification>> = db.appNotificationDao().getAllNotifications()
    val unreadNotificationCount: Flow<Int> = db.appNotificationDao().getUnreadCount()
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userReports: Flow<List<com.example.data.model.UserReport>> = user.flatMapLatest { u ->
        val uid = u?.id?.takeIf { it.isNotBlank() } ?: "guest"
        db.userReportDao().getReportsByUser(uid)
    }

    private val _systemConfig = kotlinx.coroutines.flow.MutableStateFlow(com.example.data.model.SystemAppConfig())
    val systemConfig: kotlinx.coroutines.flow.StateFlow<com.example.data.model.SystemAppConfig> = _systemConfig.asStateFlow()

    private val _situationPreview = kotlinx.coroutines.flow.MutableStateFlow(com.example.data.model.SituationPreviewType.NONE)
    val situationPreview: kotlinx.coroutines.flow.StateFlow<com.example.data.model.SituationPreviewType> = _situationPreview.asStateFlow()

    fun setSituationPreview(type: com.example.data.model.SituationPreviewType) {
        _situationPreview.value = type
    }


    init {
        startConnectionMonitoring()
        CoroutineScope(Dispatchers.IO).launch {
            cleanupAllMockData()
            initializeMissions()
        }
        startRealtimeTournamentsSync()
        startRealtimeLeaderboardSync()
        startRealtimeBannersSync()
        startRealtimeMissionsSync()
        startRealtimeNotificationsSync()
        startSystemConfigSync()
    }

    private fun startConnectionMonitoring() {
        try {
            val connectedRef = rtdb.getReference(".info/connected")
            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    _isFirebaseConnected.value = connected
                    if (connected) {
                        _lastSyncedTimestamp.value = System.currentTimeMillis()
                    }
                    Log.d(TAG, "Firebase Realtime DB Connection Status: connected=$connected")
                }

                override fun onCancelled(error: DatabaseError) {
                    _isFirebaseConnected.value = false
                    Log.w(TAG, "Firebase connection status observer cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up Firebase .info/connected listener: ${e.message}")
        }
    }

    suspend fun cleanupAllMockData() {
        cleanupLegacyTournaments()
        cleanupMockBanners()
        cleanupLegacyLeaderboard()
        cleanupMockNotifications()
        cleanupMockMatchStats()
        reconcileUserCareerStats()
    }

    private suspend fun reconcileUserCareerStats() {
        try {
            val currentUser = getUserSync() ?: return
            val allStats = db.matchStatDao().getAllSync().filter { !isMockMatchStat(it.id, it.tournamentTitle) }
            val realCount = allStats.size
            val realWins = allStats.count { it.position == 1 }
            val realKills = allStats.sumOf { it.kills }
            if (currentUser.matchesPlayed != realCount || currentUser.totalWins != realWins || currentUser.totalKills != realKills) {
                val reconciled = currentUser.copy(
                    matchesPlayed = realCount,
                    totalWins = realWins,
                    totalKills = realKills
                )
                db.userDao().update(reconciled)
                syncUserToRealtimeDb(reconciled)
                Log.i(TAG, "Reconciled career stats: played=$realCount (was ${currentUser.matchesPlayed}), wins=$realWins, kills=$realKills")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice reconciling user career stats: ${e.message}")
        }
    }

    private suspend fun cleanupMockMatchStats() {
        try {
            val allStats = db.matchStatDao().getAllSync()
            val hasMocks = allStats.any { isMockMatchStat(it.id, it.tournamentTitle) }
            if (hasMocks) {
                db.matchStatDao().clearAll()
                Log.i(TAG, "Purged mock match stats from local database")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Legacy match stats cleanup error: ${e.message}")
        }
    }

    private suspend fun cleanupMockBanners() {
        try {
            val allBanners = db.bannerDao().getAllSync()
            for (b in allBanners) {
                if (isMockBanner(b.id, b.title)) {
                    db.bannerDao().delete(b.id)
                    Log.i(TAG, "Purged mock banner from local database: ${b.id} - ${b.title}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up legacy mock banners: ${e.message}")
        }
    }

    private suspend fun cleanupMockNotifications() {
        try {
            db.appNotificationDao().cleanupMockNotifications()
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up mock notifications: ${e.message}")
        }
    }

    suspend fun insertNotification(notification: AppNotification) {
        try {
            db.appNotificationDao().insert(notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting notification", e)
        }
    }

    suspend fun markNotificationAsRead(id: String) {
        try {
            db.appNotificationDao().markAsRead(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
        }
    }

    suspend fun markAllNotificationsAsRead() {
        try {
            db.appNotificationDao().markAllAsRead()
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all notifications as read", e)
        }
    }

    suspend fun deleteNotification(id: String) {
        try {
            db.appNotificationDao().delete(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification", e)
        }
    }

    suspend fun clearAllNotifications() {
        try {
            db.appNotificationDao().clearAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing notifications", e)
        }
    }

    /**
     * Starts Realtime listener for broadcast and user-specific notifications from RTDB and Firestore.
     */
    fun startRealtimeNotificationsSync() {
        val notifListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val list = mutableListOf<AppNotification>()
                        for (child in snapshot.children) {
                            val parsed = parseNotificationFromSnapshot(child)
                            if (parsed != null) {
                                list.add(parsed)
                            }
                        }
                        if (list.isNotEmpty()) {
                            db.appNotificationDao().insertAll(list)
                            Log.i(TAG, "Realtime synced ${list.size} notification(s) from Firebase RTDB")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice processing RTDB notifications: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Firebase RTDB notifications sync notice: ${error.message}")
            }
        }

        if (listenerManager != null) {
            listenerManager.registerValueEventListener(RepositoryManager.KEY_NOTIFICATIONS, notificationsRef, notifListener)
        } else {
            notificationsRef.addValueEventListener(notifListener)
        }

        // Firestore broadcast notifications sync
        try {
            FirebaseFirestore.getInstance().collection("notifications")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val list = mutableListOf<AppNotification>()
                            for (doc in snapshot.documents) {
                                val parsed = parseNotificationFromFirestoreDoc(doc)
                                if (parsed != null) {
                                    list.add(parsed)
                                }
                            }
                            if (list.isNotEmpty()) {
                                db.appNotificationDao().insertAll(list)
                                Log.i(TAG, "Realtime synced ${list.size} notification(s) from Firestore")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Notice processing Firestore notifications: ${e.message}")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up Firestore notifications sync: ${e.message}")
        }
    }

    private fun parseNotificationFromSnapshot(child: DataSnapshot): AppNotification? {
        try {
            val id = child.key ?: child.child("id").getValue(String::class.java) ?: return null
            if (id.startsWith("notif_welcome") || id.startsWith("notif_reminder_sample") || id.startsWith("notif_result_sample")) return null
            val title = child.child("title").getValue(String::class.java) ?: "Velorix Esports"
            val message = child.child("message").getValue(String::class.java)
                ?: child.child("body").getValue(String::class.java)
                ?: child.child("text").getValue(String::class.java)
                ?: ""
            val type = child.child("type").getValue(String::class.java) ?: "GENERAL"
            val timestamp = (child.child("timestamp").value as? Number)?.toLong()
                ?: (child.child("createdAt").value as? Number)?.toLong()
                ?: System.currentTimeMillis()
            val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
            val tournamentId = child.child("tournamentId").getValue(String::class.java) ?: ""
            val tournamentTitle = child.child("tournamentTitle").getValue(String::class.java) ?: ""
            val roomId = child.child("roomId").getValue(String::class.java) ?: ""
            val roomPassword = child.child("roomPassword").getValue(String::class.java) ?: ""
            val kills = (child.child("kills").value as? Number)?.toInt() ?: 0
            val position = (child.child("position").value as? Number)?.toInt() ?: 0
            val winnings = (child.child("winnings").value as? Number)?.toDouble() ?: 0.0
            val timeRemaining = child.child("timeRemaining").getValue(String::class.java) ?: ""

            return AppNotification(
                id = id,
                title = title,
                message = message,
                type = type,
                timestamp = timestamp,
                isRead = isRead,
                tournamentId = tournamentId,
                tournamentTitle = tournamentTitle,
                roomId = roomId,
                roomPassword = roomPassword,
                kills = kills,
                position = position,
                winnings = winnings,
                timeRemaining = timeRemaining
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing notification from RTDB: ${e.message}")
            return null
        }
    }

    private fun parseNotificationFromFirestoreDoc(doc: DocumentSnapshot): AppNotification? {
        try {
            val id = doc.id
            if (id.startsWith("notif_welcome") || id.startsWith("notif_reminder_sample") || id.startsWith("notif_result_sample")) return null
            val title = doc.getString("title") ?: "Velorix Esports"
            val message = doc.getString("message") ?: doc.getString("body") ?: doc.getString("text") ?: ""
            val type = doc.getString("type") ?: "GENERAL"
            val timestamp = (doc.get("timestamp") as? Number)?.toLong()
                ?: (doc.get("createdAt") as? Number)?.toLong()
                ?: System.currentTimeMillis()
            val isRead = doc.getBoolean("isRead") ?: false
            val tournamentId = doc.getString("tournamentId") ?: ""
            val tournamentTitle = doc.getString("tournamentTitle") ?: ""
            val roomId = doc.getString("roomId") ?: ""
            val roomPassword = doc.getString("roomPassword") ?: ""
            val kills = (doc.get("kills") as? Number)?.toInt() ?: 0
            val position = (doc.get("position") as? Number)?.toInt() ?: 0
            val winnings = (doc.get("winnings") as? Number)?.toDouble() ?: 0.0
            val timeRemaining = doc.getString("timeRemaining") ?: ""

            return AppNotification(
                id = id,
                title = title,
                message = message,
                type = type,
                timestamp = timestamp,
                isRead = isRead,
                tournamentId = tournamentId,
                tournamentTitle = tournamentTitle,
                roomId = roomId,
                roomPassword = roomPassword,
                kills = kills,
                position = position,
                winnings = winnings,
                timeRemaining = timeRemaining
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing notification from Firestore: ${e.message}")
            return null
        }
    }

    private fun DataSnapshot.getStringField(vararg keys: String, default: String = ""): String {
        for (k in keys) {
            val v = child(k).getValue(String::class.java)
            if (!v.isNullOrBlank()) return v
        }
        return default
    }

    private fun parseUserReportFromSnapshot(child: DataSnapshot): UserReport? {
        try {
            val id = child.key ?: child.child("id").getValue(String::class.java) ?: return null
            val userId = child.getStringField("userId", "user_id")
            val userName = child.getStringField("userName", "user_name", "username", default = "Player")
            val userContact = child.getStringField("userContact", "contactInfo", "user_contact")
            val inGameName = child.getStringField("inGameName", "in_game_name", "ign")
            val category = child.getStringField("category", "issueCategory", "type", default = "GENERAL_SUPPORT")
            val title = child.getStringField("title", "subject")
            val description = child.getStringField("description", "message", "details")
            val incidentTime = child.getStringField("incidentTime", "timeOfIncident", "incident_time")
            val relatedId = child.getStringField("relatedId", "matchId", "tournamentId", "transactionId")
            val status = child.getStringField("status", "reportStatus", default = "PENDING").uppercase()
            val priority = child.getStringField("priority", default = "NORMAL").uppercase()
            val source = child.getStringField("source", default = "MANUAL_FORM")
            val adminReply = child.getStringField("adminReply", "admin_reply", "resolution")
            val createdAt = (child.child("createdAt").value as? Number)?.toLong()
                ?: (child.child("timestamp").value as? Number)?.toLong()
                ?: System.currentTimeMillis()
            val updatedAt = (child.child("updatedAt").value as? Number)?.toLong()
                ?: (child.child("lastUpdated").value as? Number)?.toLong()
                ?: createdAt

            return UserReport(
                id = id,
                userId = userId,
                userName = userName,
                userContact = userContact,
                inGameName = inGameName,
                category = category,
                title = title,
                description = description,
                incidentTime = incidentTime,
                relatedId = relatedId,
                status = status,
                priority = priority,
                source = source,
                adminReply = adminReply,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing user report from RTDB snapshot: ${e.message}")
            return null
        }
    }

    private fun parseUserReportFromFirestoreDoc(doc: DocumentSnapshot): UserReport? {
        try {
            val id = doc.id
            val userId = doc.getString("userId") ?: doc.getString("user_id") ?: ""
            val userName = doc.getString("userName") ?: doc.getString("user_name") ?: doc.getString("username") ?: ""
            val userContact = doc.getString("userContact") ?: doc.getString("contactInfo") ?: doc.getString("user_contact") ?: ""
            val inGameName = doc.getString("inGameName") ?: doc.getString("in_game_name") ?: doc.getString("ign") ?: ""
            val category = doc.getString("category") ?: doc.getString("issueCategory") ?: doc.getString("type") ?: "GENERAL_SUPPORT"
            val title = doc.getString("title") ?: doc.getString("subject") ?: ""
            val description = doc.getString("description") ?: doc.getString("message") ?: doc.getString("details") ?: ""
            val incidentTime = doc.getString("incidentTime") ?: doc.getString("timeOfIncident") ?: doc.getString("incident_time") ?: ""
            val relatedId = doc.getString("relatedId") ?: doc.getString("matchId") ?: doc.getString("tournamentId") ?: doc.getString("transactionId") ?: ""
            val status = (doc.getString("status") ?: doc.getString("reportStatus") ?: "PENDING").uppercase()
            val priority = (doc.getString("priority") ?: "NORMAL").uppercase()
            val source = doc.getString("source") ?: "MANUAL_FORM"
            val adminReply = doc.getString("adminReply") ?: doc.getString("admin_reply") ?: doc.getString("resolution") ?: ""
            val createdAt = (doc.get("createdAt") as? Number)?.toLong()
                ?: (doc.get("timestamp") as? Number)?.toLong()
                ?: System.currentTimeMillis()
            val updatedAt = (doc.get("updatedAt") as? Number)?.toLong()
                ?: (doc.get("lastUpdated") as? Number)?.toLong()
                ?: createdAt

            return UserReport(
                id = id,
                userId = userId,
                userName = userName,
                userContact = userContact,
                inGameName = inGameName,
                category = category,
                title = title,
                description = description,
                incidentTime = incidentTime,
                relatedId = relatedId,
                status = status,
                priority = priority,
                source = source,
                adminReply = adminReply,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing user report from Firestore doc: ${e.message}")
            return null
        }
    }


    fun isMockTournament(id: String, title: String): Boolean {
        val lowerId = id.trim().lowercase()
        val lowerTitle = title.trim().lowercase()

        // Blank, empty, or generic placeholder titles are mock/invalid entries
        if (lowerTitle.isBlank() ||
            lowerTitle == "tournament" ||
            lowerTitle == "match" ||
            lowerTitle == "untitled" ||
            lowerTitle == "test" ||
            lowerTitle == "sample" ||
            lowerTitle == "mock" ||
            lowerTitle == "demo"
        ) {
            return true
        }

        val mockIds = setOf(
            "tourney_ff_1", "tourney_ff_2", "tourney_bgmi_1", "tourney_ff_3",
            "tourney_1", "tourney_2", "tourney_3", "tourney_4",
            "mock_1", "mock_2", "sample_1", "sample_2",
            "tourney_sample", "tournament_mock", "mock_tourney_1",
            "test", "demo", "sample", "mock", "tournament"
        )
        if (lowerId in mockIds ||
            lowerId.startsWith("mock") ||
            lowerId.startsWith("sample") ||
            lowerId.startsWith("dummy") ||
            lowerId.startsWith("test_tourney") ||
            lowerId.startsWith("demo_tourney") ||
            lowerId.startsWith("tourney_ff_") ||
            lowerId.startsWith("tourney_bgmi_")
        ) {
            return true
        }
        val mockKeywords = listOf(
            "bermuda solo survival",
            "erangel squad championship",
            "clash squad open",
            "pro league finals",
            "sunday showdown",
            "mock tournament",
            "sample match",
            "dummy match",
            "test tournament",
            "demo tournament",
            "sample tournament",
            "placeholder"
        )
        return mockKeywords.any { lowerTitle.contains(it) }
    }

    fun isMockBanner(id: String, title: String): Boolean {
        val lowerId = id.trim().lowercase()
        val lowerTitle = title.trim().lowercase()
        val mockIds = setOf(
            "banner_bgmi_pro", "banner_ff_clash", "banner_deposit_bonus", "banner_referral_rewards",
            "banner_1", "banner_2", "banner_3", "banner_4", "banner_mock", "banner_sample"
        )
        if (lowerId in mockIds ||
            lowerId.startsWith("mock") ||
            lowerId.startsWith("sample") ||
            lowerId.startsWith("dummy") ||
            lowerId.startsWith("test_banner")
        ) {
            return true
        }
        val mockKeywords = listOf(
            "mock banner",
            "sample banner",
            "dummy banner",
            "test banner",
            "demo banner"
        )
        return mockKeywords.any { lowerTitle.contains(it) }
    }

    fun isMockMatchStat(id: String, tournamentTitle: String): Boolean {
        val lowerId = id.trim().lowercase()
        val lowerTitle = tournamentTitle.trim().lowercase()
        val mockIds = setOf("stat_1", "stat_2", "stat_3", "mock_stat_1", "sample_stat_1", "match_stat_1", "match_stat_2", "match_stat_3")
        if (lowerId in mockIds || lowerId.startsWith("mock_") || lowerId.startsWith("sample_") || lowerId.startsWith("dummy_") || lowerId.startsWith("match_stat_")) {
            return true
        }
        val mockKeywords = listOf("bermuda solo survival", "erangel squad championship", "clash squad open", "pro league finals", "sunday showdown", "mock", "sample")
        return mockKeywords.any { lowerTitle.contains(it) }
    }

    fun isMockLeaderboardPlayer(username: String): Boolean {
        val dummyNames = setOf("Anant (You)", "ToxicShadow_FF", "ApexPredator99", "HeadshotKing", "PhantomSniper", "VortexWarrior", "AlphaStriker", "CrimsonGhost")
        val lower = username.trim().lowercase()
        return username in dummyNames || lower.startsWith("mock_") || lower.startsWith("sample_") || lower.startsWith("dummy_")
    }

    private suspend fun cleanupLegacyTournaments() {
        try {
            val all = db.tournamentDao().getAllSync()
            for (t in all) {
                if (isMockTournament(t.id, t.title)) {
                    db.tournamentDao().delete(t.id)
                    Log.i(TAG, "Cleared mock tournament from local database: ${t.id} - ${t.title}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Legacy tournament cleanup error: ${e.message}")
        }
    }

    private suspend fun cleanupLegacyLeaderboard() {
        try {
            val all = db.leaderboardDao().getAllSync()
            val hasDummies = all.any { isMockLeaderboardPlayer(it.username) }
            if (hasDummies) {
                db.leaderboardDao().clearAll()
                Log.i(TAG, "Purged mock leaderboard entries from local database")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Legacy leaderboard cleanup error: ${e.message}")
        }
    }

    suspend fun seedDefaultTournamentsIfEmpty() {
        // No-op: Only display real tournaments created in Firebase Realtime Database / Admin Panel
    }

    suspend fun seedDefaultLeaderboardIfEmpty() {
        // No-op: Mock leaderboard data removed. Real rankings will be unlocked in the Stable Release.
    }

    suspend fun seedDefaultDataIfEmpty() {
        // No-op: Only 100% live data synced from Firebase Realtime Database and Cloud Firestore. No mock data is seeded.
    }

    /**
     * Realtime listener for Tournaments added, edited, or deleted in Admin Panel.
     * Managed via RepositoryManager to prevent memory leaks and ghost updates.
     * Syncs from both Firebase Realtime Database and Cloud Firestore tournaments collection.
     */
    fun startRealtimeTournamentsSync() {
        startFirestoreTournamentsSync()
        
        val tournamentListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _isSyncing.value = true
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: db.userDao().getUserSync()?.id
                        val list = mutableListOf<Tournament>()
                        for (child in snapshot.children) {
                            val parsed = parseTournamentFromDataSnapshot(child, currentUserId)
                            if (parsed != null && !isMockTournament(parsed.id, parsed.title)) {
                                list.add(parsed)
                            }
                        }
                        val validIds = list.map { it.id }.toSet()
                        val localTournaments = db.tournamentDao().getAllSync()
                        for (t in localTournaments) {
                            if (t.id !in validIds || isMockTournament(t.id, t.title)) {
                                db.tournamentDao().delete(t.id)
                            }
                        }
                        if (list.isNotEmpty()) {
                            db.tournamentDao().insertAll(list)
                            Log.i(TAG, "Realtime synced ${list.size} tournament(s) from Firebase RTDB tournaments")
                        }
                        _lastSyncedTimestamp.value = System.currentTimeMillis()
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice processing RTDB tournaments: ${e.message}")
                    } finally {
                        _isSyncing.value = false
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Firebase Realtime DB tournament sync notice: ${error.message}.")
            }
        }

        if (listenerManager != null) {
            listenerManager.registerValueEventListener(RepositoryManager.KEY_TOURNAMENTS, tournamentsRef, tournamentListener)
        } else {
            tournamentsRef.addValueEventListener(tournamentListener)
        }
    }

    private fun parseTournamentFromDataSnapshot(child: DataSnapshot, currentUserId: String?): Tournament? {
        try {
            val id = child.key ?: child.getStringSafe("id", "tournamentId", defaultValue = "")
            if (id.isBlank()) return null

            val title = child.getStringSafe("title", "tournamentName").ifEmpty {
                child.getStringSafe("name", "matchTitle").ifEmpty {
                    child.getStringSafe("tourneyTitle", "matchName").ifEmpty {
                        child.getStringSafe("title_str", defaultValue = "")
                    }
                }
            }

            if (title.isBlank() || isMockTournament(id, title)) {
                return null
            }

            val rawGame = child.getStringSafe("game", "gameType").ifEmpty {
                child.getStringSafe("gameName", "game_type").ifEmpty {
                    child.getStringSafe("selectedGame", "category").ifEmpty {
                        "Free Fire"
                    }
                }
            }
            val game = when {
                rawGame.contains("bgmi", ignoreCase = true) || rawGame.contains("pubg", ignoreCase = true) || rawGame.contains("battleground", ignoreCase = true) -> "BGMI"
                rawGame.contains("free", ignoreCase = true) || rawGame.contains("ff", ignoreCase = true) -> "Free Fire"
                else -> rawGame
            }

            val prizePool = child.getDoubleSafe("prizePool", "prize_pool", defaultValue = -1.0).let {
                if (it >= 0) it else child.getDoubleSafe("prize", "totalPrize", defaultValue = -1.0).let { p2 ->
                    if (p2 >= 0) p2 else child.getDoubleSafe("prizeMoney", "winningPrize", defaultValue = 0.0)
                }
            }

            val entryFee = child.getDoubleSafe("entryFee", "entry_fee", defaultValue = -1.0).let {
                if (it >= 0) it else child.getDoubleSafe("fee", "matchFee", defaultValue = -1.0).let { f2 ->
                    if (f2 >= 0) f2 else child.getDoubleSafe("joiningFee", "price", defaultValue = 0.0)
                }
            }

            val maxSlots = child.getIntSafe("maxSlots", "max_slots", defaultValue = 0).let {
                if (it > 0) it else child.getIntSafe("totalSlots", "total_slots", defaultValue = 0).let { s2 ->
                    if (s2 > 0) s2 else child.getIntSafe("maxPlayers", "maxParticipants", defaultValue = 100)
                }
            }

            val rawParticipantsCount = if (child.hasChild("participants")) child.child("participants").childrenCount.toInt() else 0
            val rawJoinedCount = if (child.hasChild("joinedPlayerIds")) child.child("joinedPlayerIds").childrenCount.toInt() else 0
            val filledSlots = child.getIntSafe("filledSlots", "filled_slots", defaultValue = -1).let {
                if (it >= 0) it else child.getIntSafe("currentParticipants", "joinedCount", defaultValue = -1).let { c2 ->
                    if (c2 >= 0) c2 else maxOf(rawParticipantsCount, rawJoinedCount)
                }
            }

            val dateTimeStr = child.getStringSafe("dateTimeStr", "date_time_str").ifEmpty {
                child.getStringSafe("startTime", "matchTime").ifEmpty {
                    child.getStringSafe("schedule", "time").ifEmpty {
                        child.getStringSafe("dateTime", "date").ifEmpty {
                            "Starting Soon"
                        }
                    }
                }
            }

            val mapType = child.getStringSafe("mapType", "map_type").ifEmpty {
                child.getStringSafe("map", "mapName").ifEmpty {
                    if (game == "BGMI") "Erangel" else "Bermuda"
                }
            }

            val perspective = child.getStringSafe("perspective", "perspective_type").ifEmpty {
                child.getStringSafe("mode", "viewType").ifEmpty {
                    child.getStringSafe("type", defaultValue = "TPP")
                }
            }

            val bannerIdx = child.getIntSafe("bannerIdx", "banner_idx", defaultValue = 1).let {
                if (it in 1..4) it else 1
            }

            val roomId = child.getStringSafe("roomId", "room_id").ifEmpty {
                child.getStringSafe("customRoomId", "roomCode").ifEmpty {
                    child.child("roomDetails").getStringSafe("roomId", "room_id").ifEmpty {
                        child.child("room").getStringSafe("roomId", "room_id")
                    }
                }
            }

            val roomPassword = child.getStringSafe("roomPassword", "room_password").ifEmpty {
                child.getStringSafe("customRoomPassword", "password").ifEmpty {
                    child.getStringSafe("pass", defaultValue = "").ifEmpty {
                        child.child("roomDetails").getStringSafe("roomPassword", "password").ifEmpty {
                            child.child("room").getStringSafe("roomPassword", "password")
                        }
                    }
                }
            }

            val isJoined = if (!currentUserId.isNullOrBlank()) {
                child.child("participants").hasChild(currentUserId) ||
                child.child("joinedPlayerIds").hasChild(currentUserId) ||
                child.child("joinedUsers").hasChild(currentUserId) ||
                child.child("players").hasChild(currentUserId) ||
                child.child("registeredUsers").hasChild(currentUserId) ||
                (0..100).any { slot -> child.child("slots").child(slot.toString()).child("userId").getValue(String::class.java) == currentUserId }
            } else false

            val format = child.getStringSafe("format", "matchFormat").ifEmpty {
                child.getStringSafe("teamType", defaultValue = "SOLO")
            }
            val status = child.getStringSafe("status", "matchStatus").ifEmpty {
                child.getStringSafe("state", defaultValue = "UPCOMING")
            }
            val rules = child.getStringSafe("rules", "matchRules")
            val rank1Prize = child.getDoubleSafe("rank1Prize", "rank_1_prize", defaultValue = 0.0)
            val rank2Prize = child.getDoubleSafe("rank2Prize", "rank_2_prize", defaultValue = 0.0)
            val rank3Prize = child.getDoubleSafe("rank3Prize", "rank_3_prize", defaultValue = 0.0)
            val rank4To10Prize = child.getDoubleSafe("rank4To10Prize", "rank_4_10_prize", defaultValue = 0.0)
            val killBounty = child.getDoubleSafe("killBounty", "kill_bounty", defaultValue = 0.0)

            return Tournament(
                id = id,
                title = title,
                game = game,
                prizePool = prizePool,
                entryFee = entryFee,
                maxSlots = maxSlots,
                filledSlots = filledSlots,
                joined = isJoined,
                dateTimeStr = dateTimeStr,
                mapType = mapType,
                perspective = perspective,
                bannerIdx = bannerIdx,
                roomId = roomId,
                roomPassword = roomPassword,
                rank1Prize = rank1Prize,
                rank2Prize = rank2Prize,
                rank3Prize = rank3Prize,
                rank4To10Prize = rank4To10Prize,
                killBounty = killBounty,
                format = format,
                status = status,
                rules = rules
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing tournament from RTDB: ${e.message}")
            return null
        }
    }

    private fun parseTournamentFromFirestoreDoc(doc: DocumentSnapshot, currentUserId: String?): Tournament? {
        try {
            val id = doc.id
            if (id.isBlank()) return null

            val title = doc.getString("title")
                ?: doc.getString("tournamentName")
                ?: doc.getString("name")
                ?: doc.getString("matchTitle")
                ?: doc.getString("tourneyTitle")
                ?: doc.getString("title_str")
                ?: doc.getString("matchName")
                ?: ""

            if (title.isBlank() || isMockTournament(id, title)) {
                return null
            }

            val rawGame = doc.getString("game")
                ?: doc.getString("gameType")
                ?: doc.getString("gameName")
                ?: doc.getString("game_type")
                ?: doc.getString("selectedGame")
                ?: doc.getString("category")
                ?: "Free Fire"
            val game = when {
                rawGame.contains("bgmi", ignoreCase = true) || rawGame.contains("pubg", ignoreCase = true) || rawGame.contains("battleground", ignoreCase = true) -> "BGMI"
                rawGame.contains("free", ignoreCase = true) || rawGame.contains("ff", ignoreCase = true) -> "Free Fire"
                else -> rawGame
            }

            val prizePool = when (val v = doc.get("prizePool") ?: doc.get("prize_pool") ?: doc.get("prize") ?: doc.get("totalPrize") ?: doc.get("prizeMoney") ?: doc.get("winningPrize") ?: doc.get("winnings") ?: doc.get("pool")) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val entryFee = when (val v = doc.get("entryFee") ?: doc.get("entry_fee") ?: doc.get("fee") ?: doc.get("matchFee") ?: doc.get("joiningFee") ?: doc.get("price") ?: doc.get("cost")) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val maxSlots = when (val v = doc.get("maxSlots") ?: doc.get("max_slots") ?: doc.get("totalSlots") ?: doc.get("total_slots") ?: doc.get("maxPlayers") ?: doc.get("maxParticipants") ?: doc.get("capacity")) {
                is Number -> v.toInt()
                is String -> v.toIntOrNull() ?: 100
                else -> 100
            }

            val participantsMap = doc.get("participants") as? Map<*, *>
            val joinedIdsList = doc.get("joinedPlayerIds") as? List<*>
            val joinedUsersList = doc.get("joinedUsers") as? List<*>
            val rawParticipantsCount = maxOf(participantsMap?.size ?: 0, maxOf(joinedIdsList?.size ?: 0, joinedUsersList?.size ?: 0))

            val filledSlots = when (val v = doc.get("filledSlots") ?: doc.get("filled_slots") ?: doc.get("currentParticipants") ?: doc.get("joinedCount") ?: doc.get("totalJoined") ?: doc.get("participantsCount")) {
                is Number -> v.toInt()
                is String -> v.toIntOrNull() ?: rawParticipantsCount
                else -> rawParticipantsCount
            }

            val dateTimeStr = doc.getString("dateTimeStr")
                ?: doc.getString("date_time_str")
                ?: doc.getString("startTime")
                ?: doc.getString("matchTime")
                ?: doc.getString("schedule")
                ?: doc.getString("time")
                ?: doc.getString("date")
                ?: doc.getString("dateTime")
                ?: doc.getString("matchDate")
                ?: "Starting Soon"

            val mapType = doc.getString("mapType")
                ?: doc.getString("map_type")
                ?: doc.getString("map")
                ?: doc.getString("mapName")
                ?: if (game == "BGMI") "Erangel" else "Bermuda"

            val perspective = doc.getString("perspective")
                ?: doc.getString("perspective_type")
                ?: doc.getString("mode")
                ?: doc.getString("viewType")
                ?: doc.getString("type")
                ?: "TPP"

            val bannerIdx = when (val v = doc.get("bannerIdx") ?: doc.get("banner_idx") ?: doc.get("banner")) {
                is Number -> v.toInt().coerceIn(1, 4)
                is String -> (v.toIntOrNull() ?: 1).coerceIn(1, 4)
                else -> 1
            }

            val roomId = doc.getString("roomId")
                ?: doc.getString("room_id")
                ?: doc.getString("customRoomId")
                ?: doc.getString("roomCode")
                ?: (doc.get("roomDetails") as? Map<*, *>)?.get("roomId") as? String
                ?: (doc.get("roomDetails") as? Map<*, *>)?.get("room_id") as? String
                ?: (doc.get("room") as? Map<*, *>)?.get("roomId") as? String
                ?: ""

            val roomPassword = doc.getString("roomPassword")
                ?: doc.getString("room_password")
                ?: doc.getString("customRoomPassword")
                ?: doc.getString("password")
                ?: doc.getString("pass")
                ?: (doc.get("roomDetails") as? Map<*, *>)?.get("roomPassword") as? String
                ?: (doc.get("roomDetails") as? Map<*, *>)?.get("password") as? String
                ?: (doc.get("room") as? Map<*, *>)?.get("roomPassword") as? String
                ?: (doc.get("room") as? Map<*, *>)?.get("password") as? String
                ?: ""

            val isJoined = if (!currentUserId.isNullOrBlank()) {
                val slotsMap = doc.get("slots") as? Map<*, *>
                participantsMap?.containsKey(currentUserId) == true ||
                joinedIdsList?.contains(currentUserId) == true ||
                joinedUsersList?.contains(currentUserId) == true ||
                slotsMap?.values?.any { (it as? Map<*, *>)?.get("userId") == currentUserId } == true
            } else false

            val format = doc.getString("format") ?: doc.getString("matchFormat") ?: doc.getString("teamType") ?: "SOLO"
            val status = doc.getString("status") ?: doc.getString("matchStatus") ?: doc.getString("state") ?: "UPCOMING"
            val rules = doc.getString("rules") ?: doc.getString("matchRules") ?: ""
            val rank1Prize = when (val v = doc.get("rank1Prize") ?: doc.get("rank_1_prize")) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val rank2Prize = when (val v = doc.get("rank2Prize") ?: doc.get("rank_2_prize")) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val rank3Prize = when (val v = doc.get("rank3Prize") ?: doc.get("rank_3_prize")) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val rank4To10Prize = when (val v = doc.get("rank4To10Prize") ?: doc.get("rank_4_10_prize")) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val killBounty = when (val v = doc.get("killBounty") ?: doc.get("kill_bounty")) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            return Tournament(
                id = id,
                title = title,
                game = game,
                prizePool = prizePool,
                entryFee = entryFee,
                maxSlots = maxSlots,
                filledSlots = filledSlots,
                joined = isJoined,
                dateTimeStr = dateTimeStr,
                mapType = mapType,
                perspective = perspective,
                bannerIdx = bannerIdx,
                roomId = roomId,
                roomPassword = roomPassword,
                rank1Prize = rank1Prize,
                rank2Prize = rank2Prize,
                rank3Prize = rank3Prize,
                rank4To10Prize = rank4To10Prize,
                killBounty = killBounty,
                format = format,
                status = status,
                rules = rules
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing Firestore tournament: ${e.message}")
            return null
        }
    }

    private fun startFirestoreTournamentsSync() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            
            firestore.collection("tournaments")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore tournaments listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: db.userDao().getUserSync()?.id
                            val list = mutableListOf<Tournament>()
                            if (snapshot != null && !snapshot.isEmpty) {
                                for (doc in snapshot.documents) {
                                    val parsed = parseTournamentFromFirestoreDoc(doc, currentUserId)
                                    if (parsed != null && !isMockTournament(parsed.id, parsed.title)) {
                                        list.add(parsed)
                                    }
                                }
                            }
                            val localTournaments = db.tournamentDao().getAllSync()
                            for (t in localTournaments) {
                                if (isMockTournament(t.id, t.title)) {
                                    db.tournamentDao().delete(t.id)
                                }
                            }
                            if (list.isNotEmpty()) {
                                db.tournamentDao().insertAll(list)
                                Log.i(TAG, "Realtime synced ${list.size} tournament(s) from Firestore tournaments")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing Firestore tournaments", e)
                        }
                    }
                }

            firestore.collection("matches")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore matches listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: db.userDao().getUserSync()?.id
                            val list = mutableListOf<Tournament>()
                            if (snapshot != null && !snapshot.isEmpty) {
                                for (doc in snapshot.documents) {
                                    val parsed = parseTournamentFromFirestoreDoc(doc, currentUserId)
                                    if (parsed != null && !isMockTournament(parsed.id, parsed.title)) {
                                        list.add(parsed)
                                    }
                                }
                            }
                            val localTournaments = db.tournamentDao().getAllSync()
                            for (t in localTournaments) {
                                if (isMockTournament(t.id, t.title)) {
                                    db.tournamentDao().delete(t.id)
                                }
                            }
                            if (list.isNotEmpty()) {
                                db.tournamentDao().insertAll(list)
                                Log.i(TAG, "Realtime synced ${list.size} tournament(s) from Firestore matches")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing Firestore matches", e)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Firestore tournaments sync", e)
        }
    }

    /**
     * Realtime listener for Leaderboard from Realtime Database.
     * Managed via RepositoryManager.
     */
    fun startRealtimeLeaderboardSync() {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    val players = mutableListOf<LeaderboardPlayer>()
                    for (child in snapshot.children) {
                        try {
                            val rank = child.getIntSafe("rank", null, players.size + 1)
                            val username = child.getStringSafe("username", "name").ifEmpty { "Warrior" }
                            if (isMockLeaderboardPlayer(username)) continue
                            val totalWinnings = child.getDoubleSafe("totalWinnings", "total_winnings")
                            val tokens = child.getIntSafe("tokens", "activityPoints", 0)
                            val avatarIdx = child.getIntSafe("avatarIdx", "avatar_idx", 1)
                            val avatarUrl = child.getStringSafe("avatarUrl")

                            players.add(LeaderboardPlayer(rank, username, totalWinnings, tokens, avatarIdx, avatarUrl))
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing leaderboard player: ${e.message}")
                        }
                    }
                    db.leaderboardDao().clearAll()
                    if (players.isNotEmpty()) {
                        db.leaderboardDao().insertAll(players)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Leaderboard sync notice: ${error.message}.")
            }
        }

        if (listenerManager != null) {
            listenerManager.registerValueEventListener(RepositoryManager.KEY_LEADERBOARD, leaderboardRef, listener)
        } else {
            leaderboardRef.addValueEventListener(listener)
        }
    }

    private fun parseBannerFromSnapshot(child: DataSnapshot): Banner? {
        try {
            val bId = child.key ?: child.child("id").getValue(String::class.java) ?: return null
            val title = child.child("title").getValue(String::class.java) ?: ""
            if (isMockBanner(bId, title)) {
                return null
            }
            val subtitle = child.child("subtitle").getValue(String::class.java) ?: ""
            val imageUrl = child.child("imageUrl").getValue(String::class.java)
                ?: child.child("image_url").getValue(String::class.java)
                ?: ""
            val badgeText = child.child("badgeText").getValue(String::class.java)
                ?: child.child("badge_text").getValue(String::class.java)
                ?: "FEATURED"
            val actionType = child.child("actionType").getValue(String::class.java)
                ?: child.child("action_type").getValue(String::class.java)
                ?: "MATCH"
            val targetId = child.child("targetId").getValue(String::class.java)
                ?: child.child("target_id").getValue(String::class.java)
                ?: ""
            val order = child.getIntSafe("order", null, 0)
            val active = child.child("active").getValue(Boolean::class.java) ?: true
            val ctaText = child.child("ctaText").getValue(String::class.java)
                ?: child.child("cta_text").getValue(String::class.java)
                ?: "EXPLORE NOW"
            val gradientTheme = child.child("gradientTheme").getValue(String::class.java)
                ?: child.child("gradient_theme").getValue(String::class.java)
                ?: "CYAN_PURPLE"
            val description = child.child("description").getValue(String::class.java)
                ?: child.child("details").getValue(String::class.java)
                ?: child.child("content").getValue(String::class.java)
                ?: ""
            val terms = child.child("terms").getValue(String::class.java)
                ?: child.child("rules").getValue(String::class.java)
                ?: child.child("terms_and_conditions").getValue(String::class.java)
                ?: ""
            val validUntil = child.child("validUntil").getValue(String::class.java)
                ?: child.child("valid_until").getValue(String::class.java)
                ?: child.child("expiryDate").getValue(String::class.java)
                ?: child.child("expiry_date").getValue(String::class.java)
                ?: ""
            val category = child.child("category").getValue(String::class.java)
                ?: child.child("bannerType").getValue(String::class.java)
                ?: child.child("banner_type").getValue(String::class.java)
                ?: "FEATURED"

            return Banner(
                id = bId,
                title = title,
                subtitle = subtitle,
                imageUrl = imageUrl,
                badgeText = badgeText,
                actionType = actionType,
                targetId = targetId,
                order = order,
                active = active,
                ctaText = ctaText,
                gradientTheme = gradientTheme,
                description = description,
                terms = terms,
                validUntil = validUntil,
                category = category
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing banner from snapshot: ${e.message}")
            return null
        }
    }

    /**
     * Starts Realtime sync for top promotional banners strictly from Firestore banners collection (and RTDB mirror).
     * Only real banners added from the admin panel / Firestore are displayed; NO mock data is seeded or shown.
     */
    fun startRealtimeBannersSync() {
        startFirestoreBannersSync()
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (!snapshot.exists()) {
                            db.bannerDao().clearAll()
                            return@launch
                        }
                        val list = mutableListOf<Banner>()
                        for (child in snapshot.children) {
                            val parsed = parseBannerFromSnapshot(child)
                            if (parsed != null && parsed.active) {
                                list.add(parsed)
                            }
                        }
                        db.bannerDao().clearAll()
                        if (list.isNotEmpty()) {
                            db.bannerDao().insertAll(list)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing banners from RTDB: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Banners sync cancelled: ${error.message}")
            }
        }
        bannersRef.addValueEventListener(listener)
    }

    private fun startFirestoreBannersSync() {
        try {
            FirebaseFirestore.getInstance().collection("banners")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Firestore banners listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            if (snapshot == null || snapshot.isEmpty) {
                                db.bannerDao().clearAll()
                                Log.i(TAG, "No promotional banners found in Firestore collection.")
                                return@launch
                            }
                            val list = snapshot.documents.mapNotNull { doc ->
                                val bId = doc.id
                                val title = doc.getString("title") ?: ""
                                if (isMockBanner(bId, title)) {
                                    return@mapNotNull null
                                }
                                val subtitle = doc.getString("subtitle") ?: ""
                                val imageUrl = doc.getString("imageUrl") ?: doc.getString("image_url") ?: ""
                                val badgeText = doc.getString("badgeText") ?: doc.getString("badge_text") ?: "FEATURED"
                                val actionType = doc.getString("actionType") ?: doc.getString("action_type") ?: "MATCH"
                                val targetId = doc.getString("targetId") ?: doc.getString("target_id") ?: ""
                                val order = doc.getLong("order")?.toInt() ?: 0
                                val active = doc.getBoolean("active") ?: true
                                val ctaText = doc.getString("ctaText") ?: doc.getString("cta_text") ?: "EXPLORE NOW"
                                val gradientTheme = doc.getString("gradientTheme") ?: doc.getString("gradient_theme") ?: "CYAN_PURPLE"
                                val description = doc.getString("description") ?: doc.getString("details") ?: doc.getString("content") ?: ""
                                val terms = doc.getString("terms") ?: doc.getString("rules") ?: doc.getString("terms_and_conditions") ?: ""
                                val validUntil = doc.getString("validUntil") ?: doc.getString("valid_until") ?: doc.getString("expiryDate") ?: doc.getString("expiry_date") ?: ""
                                val category = doc.getString("category") ?: doc.getString("bannerType") ?: doc.getString("banner_type") ?: "FEATURED"

                                Banner(
                                    id = bId,
                                    title = title,
                                    subtitle = subtitle,
                                    imageUrl = imageUrl,
                                    badgeText = badgeText,
                                    actionType = actionType,
                                    targetId = targetId,
                                    order = order,
                                    active = active,
                                    ctaText = ctaText,
                                    gradientTheme = gradientTheme,
                                    description = description,
                                    terms = terms,
                                    validUntil = validUntil,
                                    category = category
                                )
                            }.filter { it.active }

                            db.bannerDao().clearAll()
                            if (list.isNotEmpty()) {
                                db.bannerDao().insertAll(list)
                                Log.i(TAG, "Synced ${list.size} dynamic promotional banners from Firestore")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing Firestore banners", e)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Firestore banners sync", e)
        }
    }

    /**
     * Submits player satisfaction feedback for a broadcast announcement banner.
     * Stored in Firebase RTDB /banner_feedback and Cloud Firestore.
     */
    suspend fun submitBannerSatisfaction(bannerId: String, reaction: String, feedbackNote: String = ""): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val localUser = db.userDao().getUserSync() ?: user.firstOrNull()
                val uId = localUser?.id ?: FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
                val uName = localUser?.username ?: "Player"
                val data = mapOf(
                    "userId" to uId,
                    "userName" to uName,
                    "reaction" to reaction,
                    "feedbackNote" to feedbackNote,
                    "timestamp" to System.currentTimeMillis()
                )
                rtdb.getReference("banner_feedback").child(bannerId).child(uId).setValue(data).await()
                try {
                    FirebaseFirestore.getInstance().collection("banner_feedback")
                        .document("${bannerId}_${uId}")
                        .set(data + mapOf("bannerId" to bannerId)).await()
                } catch (_: Exception) {}
                Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to submit banner feedback: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Starts Realtime sync for dynamic missions table from RTDB /missions.
     */
    fun startRealtimeMissionsSync() {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val userItem = db.userDao().getUserSync() ?: user.firstOrNull()
                        val today = getTodayIstDate()
                        val currentLocal = db.missionDao().getAllMissions().firstOrNull() ?: emptyList()
                        val localDaily = currentLocal.find { it.id == "m_daily_checkin" }
                        val isDailyClaimed = (userItem?.lastLoginClaimDate == today) || (localDaily?.isClaimed == true)

                        val list = mutableListOf<Mission>()

                        if (snapshot.exists()) {
                            for (child in snapshot.children) {
                                val mId = child.key ?: child.getStringSafe("id").ifEmpty { continue }
                                val title = child.getStringSafe("title").ifEmpty { "Mission" }
                                val desc = child.getStringSafe("description")
                                val target = child.getIntSafe("target", null, 1)
                                val reward = child.getDoubleSafe("rewardCurrency", "reward").let { if (it > 0.0) it else child.getDoubleSafe("reward_currency", null, 20.0) }
                                val category = child.getStringSafe("category").ifEmpty { "DAILY" }

                                val local = currentLocal.find { it.id == mId }
                                val isClaimed = if (mId == "m_daily_checkin") isDailyClaimed else (local?.isClaimed ?: false)
                                val progress = if (mId == "m_daily_checkin") 1 else (local?.progress ?: 0)
                                val isCompleted = if (mId == "m_daily_checkin") true else (progress >= target)

                                list.add(
                                    Mission(
                                        id = mId,
                                        title = title,
                                        description = desc,
                                        target = target,
                                        progress = progress,
                                        rewardCurrency = reward,
                                        isCompleted = isCompleted,
                                        isClaimed = isClaimed,
                                        category = category
                                    )
                                )
                            }
                        }

                        // Also sync live missions from Firestore if present
                        try {
                            val fsDocs = FirebaseFirestore.getInstance().collection("missions").get().await()
                            for (doc in fsDocs.documents) {
                                val mId = doc.id
                                if (list.any { it.id == mId }) continue
                                val title = doc.getString("title") ?: "Mission"
                                val desc = doc.getString("description") ?: ""
                                val target = (doc.get("target") as? Number)?.toInt() ?: 1
                                val reward = (doc.get("rewardCurrency") as? Number)?.toDouble()
                                    ?: (doc.get("reward") as? Number)?.toDouble() ?: 20.0
                                val category = doc.getString("category") ?: "DAILY"

                                val local = currentLocal.find { it.id == mId }
                                val isClaimed = if (mId == "m_daily_checkin") isDailyClaimed else (local?.isClaimed ?: false)
                                val progress = if (mId == "m_daily_checkin") 1 else (local?.progress ?: 0)
                                val isCompleted = if (mId == "m_daily_checkin") true else (progress >= target)

                                list.add(
                                    Mission(
                                        id = mId,
                                        title = title,
                                        description = desc,
                                        target = target,
                                        progress = progress,
                                        rewardCurrency = reward,
                                        isCompleted = isCompleted,
                                        isClaimed = isClaimed,
                                        category = category
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Firestore missions live sync notice: ${e.message}")
                        }

                        // Merge backend missions with essential daily engagement missions
                        val defaultMissions = getDefaultMissions(isDailyClaimed)
                        val mergedList = mutableListOf<Mission>()
                        mergedList.addAll(list)
                        for (dm in defaultMissions) {
                            if (mergedList.none { it.id == dm.id }) {
                                val existing = currentLocal.find { it.id == dm.id }
                                mergedList.add(
                                    dm.copy(
                                        progress = existing?.progress ?: dm.progress,
                                        isCompleted = existing?.isCompleted ?: dm.isCompleted,
                                        isClaimed = if (dm.id == "m_daily_checkin") isDailyClaimed else (existing?.isClaimed ?: dm.isClaimed)
                                    )
                                )
                            }
                        }

                        db.missionDao().deleteAll()
                        db.missionDao().insertAll(mergedList)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing live missions: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Missions sync cancelled: ${error.message}")
            }
        }
        missionsRef.addValueEventListener(listener)
    }

    /**
     * Starts Realtime listener for the logged-in User profile from RTDB /users/$userId.
     */
    fun startRealtimeUserSync(userId: String) {
        if (userId.isBlank()) return
        val userQuery = usersRef.child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val fetchedUser = parseUserFromSnapshot(snapshot, userId)
                        val local = db.userDao().getUserSync()
                        val mergedUser = if (local != null && local.id == userId) {
                            fetchedUser.copy(
                                loginStreak = maxOf(fetchedUser.loginStreak, local.loginStreak),
                                lastLoginClaimDate = if (fetchedUser.lastLoginClaimDate.isNotBlank()) fetchedUser.lastLoginClaimDate else local.lastLoginClaimDate,
                                totalTokensConverted = maxOf(fetchedUser.totalTokensConverted, local.totalTokensConverted),
                                balance = if (fetchedUser.balance > 0.0) fetchedUser.balance else local.balance,
                                tokens = maxOf(fetchedUser.tokens, local.tokens)
                            )
                        } else {
                            fetchedUser
                        }
                        db.userDao().clearAll()
                        db.userDao().insert(mergedUser)
                        Log.d(TAG, "Realtime user profile updated: ${mergedUser.username}, tokens: ${mergedUser.tokens}, balance: ${mergedUser.balance}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating user in realtime: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "User realtime sync cancelled: ${error.message}")
            }
        }

        if (listenerManager != null) {
            listenerManager.registerValueEventListener(RepositoryManager.KEY_USER_PROFILE, userQuery, listener)
        } else {
            userQuery.addValueEventListener(listener)
        }

        // Firestore real-time sync for User Profile
        try {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .addSnapshotListener { doc, error ->
                    if (error != null || doc == null || !doc.exists()) return@addSnapshotListener
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val fsTokens = (doc.get("tokens") as? Number)?.toInt()
                                ?: (doc.get("tokenBalance") as? Number)?.toInt()
                                ?: (doc.get("tokensBalance") as? Number)?.toInt()
                                ?: (doc.get("rewardTokens") as? Number)?.toInt()
                                ?: (doc.get("activityPoints") as? Number)?.toInt() ?: 0
                            val fsBalance = (doc.get("balance") as? Number)?.toDouble()
                                ?: (doc.get("walletBalance") as? Number)?.toDouble()
                                ?: (doc.get("wallet_balance") as? Number)?.toDouble() ?: 0.0
                            val fsStreak = (doc.get("loginStreak") as? Number)?.toInt() ?: 0
                            val fsLastClaim = doc.getString("lastLoginClaimDate") ?: ""
                            val fsTotalConverted = (doc.get("totalTokensConverted") as? Number)?.toInt()
                                ?: (doc.get("total_tokens_converted") as? Number)?.toInt()
                                ?: (doc.get("tokensConverted") as? Number)?.toInt() ?: 0
                            val fsFounder = doc.getBoolean("isFounder") ?: false
                            val fsTier = doc.getString("founderTier") ?: ""
                            val fsReserved = (doc.get("reservedTokens") as? Number)?.toInt() ?: 0
                            val fsBanned = doc.getBoolean("isBanned") ?: doc.getBoolean("banned") ?: doc.getBoolean("is_banned") ?: (doc.getString("status")?.equals("BANNED", ignoreCase = true) ?: false)
                            val fsBanReason = doc.getString("banReason") ?: doc.getString("ban_reason") ?: doc.getString("reason") ?: ""
                            val fsBanType = doc.getString("banType") ?: doc.getString("ban_type") ?: "PERMANENT"
                            val fsSuspended = doc.getBoolean("isSuspended") ?: doc.getBoolean("suspended") ?: doc.getBoolean("is_suspended") ?: (doc.getString("status")?.equals("SUSPENDED", ignoreCase = true) ?: false)
                            val fsSuspendReason = doc.getString("suspendReason") ?: doc.getString("suspend_reason") ?: ""
                            val fsRole = doc.getString("role") ?: doc.getString("adminRole") ?: ""

                            val local = db.userDao().getUserSync()
                            if (local != null && local.id == userId) {
                                val updated = local.copy(
                                    balance = if (fsBalance > 0.0 || local.balance == 0.0) fsBalance else local.balance,
                                    tokens = if (fsTokens > 0 || local.tokens == 0) fsTokens else local.tokens,
                                    loginStreak = maxOf(local.loginStreak, fsStreak),
                                    lastLoginClaimDate = if (fsLastClaim.isNotBlank()) fsLastClaim else local.lastLoginClaimDate,
                                    totalTokensConverted = maxOf(local.totalTokensConverted, fsTotalConverted),
                                    isFounder = fsFounder || local.isFounder,
                                    founderTier = if (fsTier.isNotBlank()) fsTier else local.founderTier,
                                    reservedTokens = maxOf(local.reservedTokens, fsReserved),
                                    isBanned = fsBanned || local.isBanned,
                                    banReason = if (fsBanReason.isNotBlank()) fsBanReason else local.banReason,
                                    banType = if (fsBanType.isNotBlank()) fsBanType else local.banType,
                                    isSuspended = fsSuspended || local.isSuspended,
                                    suspendReason = if (fsSuspendReason.isNotBlank()) fsSuspendReason else local.suspendReason,
                                    role = if (fsRole.isNotBlank()) fsRole else local.role
                                )
                                db.userDao().update(updated)
                                Log.d(TAG, "User profile updated from Firestore real-time listener")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Notice updating user from Firestore snapshot: ${e.message}")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up Firestore user listener: ${e.message}")
        }

        // Realtime sync for Banned Users node
        try {
            rtdb.getReference("banned_users").child(userId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(banSnap: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val local = db.userDao().getUserSync()
                        if (local != null && local.id == userId) {
                            if (banSnap.exists()) {
                                val reason = banSnap.child("banReason").value?.toString()
                                    ?: banSnap.child("reason").value?.toString()
                                    ?: "Account banned by administrator for policy violation."
                                val banType = banSnap.child("banType").value?.toString() ?: "PERMANENT"
                                db.userDao().update(local.copy(isBanned = true, banReason = reason, banType = banType))
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up banned_users listener: ${e.message}")
        }

        // Realtime sync for Suspended Users node
        try {
            rtdb.getReference("suspended_users").child(userId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(suspendSnap: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val local = db.userDao().getUserSync()
                        if (local != null && local.id == userId) {
                            if (suspendSnap.exists()) {
                                val reason = suspendSnap.child("suspendReason").value?.toString()
                                    ?: suspendSnap.child("reason").value?.toString()
                                    ?: "Account temporarily under security review."
                                db.userDao().update(local.copy(isSuspended = true, suspendReason = reason))
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up suspended_users listener: ${e.message}")
        }

        // Realtime sync for User's Transactions & Withdrawal Requests from Admin updates
        try {
            val txQuery = transactionsRef.orderByChild("userId").equalTo(userId)
            val txListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val list = mutableListOf<Transaction>()
                            for (child in snapshot.children) {
                                val tx = child.getValue(Transaction::class.java)
                                if (tx != null) {
                                    list.add(tx)
                                }
                            }
                            if (list.isNotEmpty()) {
                                db.transactionDao().insertAll(list)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing transactions in realtime", e)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Transactions sync cancelled: ${error.message}")
                }
            }
            txQuery.addValueEventListener(txListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up user transactions listener", e)
        }

        // Realtime sync for user-specific notifications from RTDB /users/$userId/notifications
        try {
            val userNotifsRef = usersRef.child(userId).child("notifications")
            val userNotifsListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val list = mutableListOf<AppNotification>()
                            for (child in snapshot.children) {
                                val parsed = parseNotificationFromSnapshot(child)
                                if (parsed != null) {
                                    list.add(parsed)
                                }
                            }
                            if (list.isNotEmpty()) {
                                db.appNotificationDao().insertAll(list)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Notice processing user RTDB notifications: ${e.message}")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "User RTDB notifications listener cancelled: ${error.message}")
                }
            }
            if (listenerManager != null) {
                listenerManager.registerValueEventListener(RepositoryManager.KEY_USER_NOTIFICATIONS, userNotifsRef, userNotifsListener)
            } else {
                userNotifsRef.addValueEventListener(userNotifsListener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up user notifications listener: ${e.message}")
        }

        // Firestore user-specific notifications
        try {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("notifications")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val list = mutableListOf<AppNotification>()
                            for (doc in snapshot.documents) {
                                val parsed = parseNotificationFromFirestoreDoc(doc)
                                if (parsed != null) {
                                    list.add(parsed)
                                }
                            }
                            if (list.isNotEmpty()) {
                                db.appNotificationDao().insertAll(list)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Notice processing user Firestore notifications: ${e.message}")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up user Firestore notifications: ${e.message}")
        }

        // Realtime listener for Deposit Requests approval / rejection from Admin Panel
        try {
            val depositQuery = depositRequestsRef.orderByChild("userId").equalTo(userId)
            val depositListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            for (child in snapshot.children) {
                                val status = child.getStringSafe("status", "requestStatus", defaultValue = "PENDING").uppercase()
                                val txId = child.getStringSafe("transactionId", "txId")
                                val amount = child.getDoubleSafe("amount", "depositAmount", defaultValue = 0.0)
                                if (txId.isNotBlank() && (status == "APPROVED" || status == "SUCCESS" || status == "REJECTED" || status == "DECLINED")) {
                                    val mappedStatus = if (status == "APPROVED" || status == "SUCCESS") "SUCCESS" else "REJECTED"
                                    val currentTxs = db.transactionDao().getAll().firstOrNull() ?: emptyList()
                                    val tx = currentTxs.find { it.id == txId }
                                    if (tx != null && tx.status != mappedStatus) {
                                        db.transactionDao().insert(tx.copy(status = mappedStatus))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Notice checking deposit requests status: ${e.message}")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Deposit requests listener cancelled: ${error.message}")
                }
            }
            if (listenerManager != null) {
                listenerManager.registerValueEventListener(RepositoryManager.KEY_DEPOSIT_REQUESTS, depositQuery, depositListener)
            } else {
                depositQuery.addValueEventListener(depositListener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up deposit requests listener: ${e.message}")
        }

        // Realtime listener for Withdrawal Requests approval / rejection from Admin Panel
        try {
            val withdrawQuery = withdrawRequestsRef.orderByChild("userId").equalTo(userId)
            val withdrawListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            for (child in snapshot.children) {
                                val status = child.getStringSafe("status", "requestStatus", defaultValue = "PENDING").uppercase()
                                val txId = child.getStringSafe("transactionId", "txId")
                                val amount = child.getDoubleSafe("amount", "withdrawAmount", defaultValue = 0.0)
                                if (txId.isNotBlank() && (status == "APPROVED" || status == "SUCCESS" || status == "REJECTED" || status == "DECLINED")) {
                                    val mappedStatus = if (status == "APPROVED" || status == "SUCCESS") "SUCCESS" else "REJECTED"
                                    val currentTxs = db.transactionDao().getAll().firstOrNull() ?: emptyList()
                                    val tx = currentTxs.find { it.id == txId }
                                    if (tx != null && tx.status != mappedStatus) {
                                        db.transactionDao().insert(tx.copy(status = mappedStatus))
                                        // If rejected, refund balance if not already refunded
                                        if (mappedStatus == "REJECTED" && amount > 0) {
                                            val u = db.userDao().getUserSync()
                                            if (u != null) {
                                                val refunded = u.copy(balance = u.balance + amount)
                                                db.userDao().update(refunded)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Notice checking withdraw requests status: ${e.message}")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Withdraw requests listener cancelled: ${error.message}")
                }
            }
            if (listenerManager != null) {
                listenerManager.registerValueEventListener(RepositoryManager.KEY_WITHDRAW_REQUESTS, withdrawQuery, withdrawListener)
            } else {
                withdrawQuery.addValueEventListener(withdrawListener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up withdraw requests listener: ${e.message}")
        }

        // Firestore real-time sync for transactions and withdrawals
        try {
            FirebaseFirestore.getInstance().collection("transactions")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val txList = mutableListOf<Transaction>()
                            for (doc in snapshot.documents) {
                                val id = doc.id
                                val type = doc.getString("type") ?: "MISC"
                                val amount = doc.getDouble("amount") ?: doc.getLong("amount")?.toDouble() ?: 0.0
                                val detail = doc.getString("detail") ?: ""
                                val isPositive = doc.getBoolean("isPositive") ?: (amount >= 0)
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val status = doc.getString("status") ?: "SUCCESS"
                                txList.add(
                                    Transaction(
                                        id = id,
                                        userId = userId,
                                        type = type,
                                        amount = amount,
                                        detail = detail,
                                        isPositive = isPositive,
                                        timestamp = timestamp,
                                        status = status
                                    )
                                )
                            }
                            if (txList.isNotEmpty()) {
                                db.transactionDao().insertAll(txList)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating transactions from Firestore", e)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Firestore transactions listener", e)
        }

        // Realtime sync for User's Support & Issue Reports
        startRealtimeUserReportsSync(userId)
    }

    /**
     * Starts Realtime listener for reports filed by or belonging to this user.
     * Synchronizes status updates, admin resolution notes, and replies from Admin Panel.
     */
    fun startRealtimeUserReportsSync(userId: String) {
        val reportsQuery = reportsRef.orderByChild("userId").equalTo(userId)
        val reportsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val list = mutableListOf<UserReport>()
                        for (child in snapshot.children) {
                            val parsed = parseUserReportFromSnapshot(child)
                            if (parsed != null) {
                                list.add(parsed)
                            }
                        }
                        if (list.isNotEmpty()) {
                            db.userReportDao().insertAll(list)
                            Log.i(TAG, "Realtime synced ${list.size} report(s) from Firebase RTDB")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Notice processing RTDB user reports: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "User reports listener cancelled: ${error.message}")
            }
        }

        if (listenerManager != null) {
            listenerManager.registerValueEventListener(RepositoryManager.KEY_USER_REPORTS, reportsQuery, reportsListener)
        } else {
            reportsQuery.addValueEventListener(reportsListener)
        }

        // Firestore real-time sync for user reports
        try {
            FirebaseFirestore.getInstance().collection("reports")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val list = mutableListOf<UserReport>()
                            for (doc in snapshot.documents) {
                                val parsed = parseUserReportFromFirestoreDoc(doc)
                                if (parsed != null) {
                                    list.add(parsed)
                                }
                            }
                            if (list.isNotEmpty()) {
                                db.userReportDao().insertAll(list)
                                Log.i(TAG, "Realtime synced ${list.size} report(s) from Firestore")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Notice processing Firestore user reports: ${e.message}")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up Firestore reports listener: ${e.message}")
        }
    }

    /**
     * Submits a new user report / problem ticket to Room database, Firebase Realtime Database,
     * Cloud Firestore, and triggers Admin / Owner notification alert.
     */
    suspend fun submitUserReport(
        category: String,
        title: String,
        description: String,
        contactInfo: String,
        incidentTime: String = "",
        relatedId: String = "",
        priority: String = "NORMAL",
        source: String = "MANUAL_FORM"
    ): Result<UserReport> = withContext(Dispatchers.IO) {
        try {
            val userItem = db.userDao().getUserSync() ?: user.firstOrNull()
            val userId = userItem?.id ?: FirebaseAuth.getInstance().currentUser?.uid ?: "guest_${UUID.randomUUID().toString().take(6)}"
            val userName = userItem?.username ?: "Player"
            val inGameName = userItem?.inGameName ?: userItem?.freeFireId ?: ""
            val reportId = "rep_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            val now = System.currentTimeMillis()

            val effectiveContact = if (contactInfo.isNotBlank()) contactInfo else (userItem?.phoneOrEmail ?: "")

            val report = UserReport(
                id = reportId,
                userId = userId,
                userName = userName,
                userContact = effectiveContact,
                inGameName = inGameName,
                category = category,
                title = title.ifBlank { "Report: $category" },
                description = description,
                incidentTime = incidentTime.ifBlank { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(now)) },
                relatedId = relatedId,
                status = "PENDING",
                priority = priority,
                source = source,
                adminReply = "",
                createdAt = now,
                updatedAt = now
            )

            // 1. Insert into local Room database immediately
            db.userReportDao().insert(report)

            // 2. Prepare payload for Firebase collections
            val reportMap = hashMapOf<String, Any>(
                "id" to report.id,
                "userId" to report.userId,
                "userName" to report.userName,
                "userContact" to report.userContact,
                "contactInfo" to report.userContact,
                "inGameName" to report.inGameName,
                "category" to report.category,
                "issueCategory" to report.category,
                "title" to report.title,
                "description" to report.description,
                "message" to report.description,
                "incidentTime" to report.incidentTime,
                "relatedId" to report.relatedId,
                "status" to report.status,
                "priority" to report.priority,
                "source" to report.source,
                "adminReply" to report.adminReply,
                "createdAt" to report.createdAt,
                "timestamp" to report.createdAt,
                "updatedAt" to report.updatedAt
            )

            // 3. Write to RTDB: /reports/$id, /support_tickets/$id, and /users/$userId/reports/$id
            try {
                reportsRef.child(reportId).setValue(reportMap)
                supportTicketsRef.child(reportId).setValue(reportMap)
                usersRef.child(userId).child("reports").child(reportId).setValue(reportMap)
            } catch (e: Exception) {
                Log.w(TAG, "Notice writing report to RTDB: ${e.message}")
            }

            // 4. Write to Cloud Firestore: reports, support_tickets collections
            try {
                val fs = FirebaseFirestore.getInstance()
                fs.collection("reports").document(reportId).set(reportMap)
                fs.collection("support_tickets").document(reportId).set(reportMap)
                fs.collection("users").document(userId).collection("reports").document(reportId).set(reportMap)
            } catch (e: Exception) {
                Log.w(TAG, "Notice writing report to Firestore: ${e.message}")
            }

            // 5. Send Alert to Admin/Owner
            try {
                OwnerAlertManager.sendOwnerAlert(
                    alertType = "USER_REPORT_${category.uppercase()}",
                    userQuery = "Title: ${report.title}\nCategory: $category\nIncident Time: ${report.incidentTime}\nDetails: $description\nContact: $effectiveContact\nRelated ID: $relatedId",
                    userContext = "User: $userName (UID: $userId, IGN: $inGameName, Contact: $effectiveContact, Source: $source)"
                )
            } catch (e: Exception) {
                Log.w(TAG, "Notice sending owner alert for report: ${e.message}")
            }

            Result.success(report)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit user report", e)
            Result.failure(e)
        }
    }

    /**
     * Cancels or withdraws a user's open report.
     */
    suspend fun cancelUserReport(reportId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.userReportDao().updateStatus(reportId, "CANCELLED")
            val updates = mapOf<String, Any>(
                "status" to "CANCELLED",
                "updatedAt" to System.currentTimeMillis()
            )
            reportsRef.child(reportId).updateChildren(updates)
            supportTicketsRef.child(reportId).updateChildren(updates)
            val userItem = db.userDao().getUserSync()
            if (userItem != null) {
                usersRef.child(userItem.id).child("reports").child(reportId).updateChildren(updates)
            }
            try {
                val fs = FirebaseFirestore.getInstance()
                fs.collection("reports").document(reportId).update(updates)
                fs.collection("support_tickets").document(reportId).update(updates)
            } catch (e: Exception) {
                Log.w(TAG, "Firestore cancel report notice: ${e.message}")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling user report", e)
            false
        }
    }

    /**
     * Deletes a report locally and removes/marks it from Firebase.
     */
    suspend fun deleteUserReport(reportId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.userReportDao().delete(reportId)
            reportsRef.child(reportId).removeValue()
            supportTicketsRef.child(reportId).removeValue()
            val userItem = db.userDao().getUserSync()
            if (userItem != null) {
                usersRef.child(userItem.id).child("reports").child(reportId).removeValue()
            }
            try {
                val fs = FirebaseFirestore.getInstance()
                fs.collection("reports").document(reportId).delete()
                fs.collection("support_tickets").document(reportId).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore delete report notice: ${e.message}")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user report", e)
            false
        }
    }

    /**
     * Stops realtime user profile sync.
     */
    fun stopRealtimeUserSync() {

        listenerManager?.removeValueEventListener(RepositoryManager.KEY_USER_PROFILE)
    }

    /**
     * Detaches all active Realtime Database listeners.
     */
    fun stopAllRealtimeSync() {
        listenerManager?.removeAllListeners()
    }

    /**
     * Cleanup resources when repository is destroyed.
     */
    fun cleanup() {
        stopAllRealtimeSync()
    }

    fun getDefaultMissions(isDailyClaimed: Boolean = false): List<Mission> {
        return listOf(
            Mission(
                id = "m_daily_checkin",
                title = "Daily Login & Streak",
                description = "Open the app daily to maintain your streak and claim free reward tokens",
                target = 1,
                progress = 1,
                rewardCurrency = 50.0,
                isCompleted = true,
                isClaimed = isDailyClaimed,
                category = "DAILY"
            ),
            Mission(
                id = "m_tournament_contender",
                title = "Tournament Contender",
                description = "Register and join any featured esports tournament match",
                target = 1,
                progress = 0,
                rewardCurrency = 100.0,
                isCompleted = false,
                isClaimed = false,
                category = "TOURNAMENTS"
            ),
            Mission(
                id = "m_profile_master",
                title = "Profile Master",
                description = "Customize your profile with your in-game name and avatar",
                target = 1,
                progress = 0,
                rewardCurrency = 30.0,
                isCompleted = false,
                isClaimed = false,
                category = "PROFILE"
            ),
            Mission(
                id = "m_support_explorer",
                title = "Help & Support Explorer",
                description = "Visit 24/7 Live Support or browse tournament rules and FAQs",
                target = 1,
                progress = 0,
                rewardCurrency = 20.0,
                isCompleted = false,
                isClaimed = false,
                category = "ENGAGEMENT"
            ),
            Mission(
                id = "m_leaderboard_explorer",
                title = "Leaderboard Scout",
                description = "Check the top rankers on the platform leaderboard",
                target = 1,
                progress = 0,
                rewardCurrency = 20.0,
                isCompleted = false,
                isClaimed = false,
                category = "EXPLORE"
            )
        )
    }

    suspend fun initializeMissions() {
        val userItem = db.userDao().getUserSync() ?: user.firstOrNull()
        val today = getTodayIstDate()
        val currentMissions = db.missionDao().getAllMissions().firstOrNull() ?: emptyList()
        val localDaily = currentMissions.find { it.id == "m_daily_checkin" }
        val isDailyClaimed = (userItem?.lastLoginClaimDate == today) || (localDaily?.isClaimed == true)

        if (currentMissions.isEmpty()) {
            val defaults = getDefaultMissions(isDailyClaimed)
            db.missionDao().insertAll(defaults)
            Log.i(TAG, "Initialized default daily missions (${defaults.size} missions)")
        } else {
            val defaults = getDefaultMissions(isDailyClaimed)
            for (d in defaults) {
                val existing = currentMissions.find { it.id == d.id }
                if (existing == null) {
                    db.missionDao().insert(d)
                } else if (d.id == "m_daily_checkin") {
                    if (existing.isClaimed != isDailyClaimed || existing.progress != 1) {
                        db.missionDao().update(existing.copy(progress = 1, isCompleted = true, isClaimed = isDailyClaimed))
                    }
                }
            }
        }
    }

    suspend fun updateMissionProgress(missionId: String, amount: Int = 1) {
        try {
            val currentMissions = db.missionDao().getAllMissions().firstOrNull() ?: return
            val mission = currentMissions.find { it.id == missionId } ?: return
            if (mission.isClaimed) return
            
            val newProgress = (mission.progress + amount).coerceAtMost(mission.target)
            val isNowCompleted = newProgress >= mission.target
            if (newProgress != mission.progress || isNowCompleted != mission.isCompleted) {
                val updatedMission = mission.copy(progress = newProgress, isCompleted = isNowCompleted)
                db.missionDao().update(updatedMission)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update mission progress for $missionId", e)
        }
    }

    suspend fun claimDailyLoginMission(): ClaimMissionResult = withContext(Dispatchers.IO) {
        val userItem = db.userDao().getUserSync() ?: user.firstOrNull() ?: return@withContext ClaimMissionResult.Failure("User profile not found")
        val today = getTodayIstDate()

        // 1. Check if daily login is already claimed for today
        if (userItem.lastLoginClaimDate == today) {
            return@withContext ClaimMissionResult.Failure("Daily login reward already claimed for today! Resets at 12:00 AM IST.")
        }

        val yesterday = getYesterdayIstDate()
        val isConsecutive = userItem.lastLoginClaimDate == yesterday
        val newStreak = if (isConsecutive) userItem.loginStreak + 1 else 1
        val rewardTokens = 20

        val updatedUser = userItem.copy(
            tokens = userItem.tokens + rewardTokens,
            loginStreak = newStreak,
            lastLoginClaimDate = today
        )

        val currentMissions = db.missionDao().getAllMissions().firstOrNull() ?: emptyList()
        val daily = currentMissions.find { it.id == "m_daily_checkin" }
        if (daily != null) {
            db.missionDao().update(daily.copy(progress = 1, isCompleted = true, isClaimed = true))
        }

        val newTx = Transaction(
            userId = userItem.id,
            type = "MISSION_REWARD",
            amount = rewardTokens.toDouble(),
            detail = "Daily Check-In (Day $newStreak Streak): +${rewardTokens} Tokens",
            isPositive = true,
            timestamp = System.currentTimeMillis()
        )

        try {
            db.userDao().update(updatedUser)
            db.transactionDao().insert(newTx)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating local database for daily check-in", e)
        }

        // Asynchronously synchronize reward to Realtime Database and Firestore
        CoroutineScope(Dispatchers.IO).launch {
            try {
                syncUserToRealtimeDb(updatedUser)
                usersRef.child(userItem.id).child("tokens").setValue(updatedUser.tokens)
                usersRef.child(userItem.id).child("tokenBalance").setValue(updatedUser.tokens)
                usersRef.child(userItem.id).child("tokensBalance").setValue(updatedUser.tokens)
                usersRef.child(userItem.id).child("rewardTokens").setValue(updatedUser.tokens)
                usersRef.child(userItem.id).child("reward_tokens").setValue(updatedUser.tokens)
                usersRef.child(userItem.id).child("activityPoints").setValue(updatedUser.tokens)
                usersRef.child(userItem.id).child("lastLoginClaimDate").setValue(today)
                usersRef.child(userItem.id).child("loginStreak").setValue(newStreak)
                transactionsRef.child(newTx.id).setValue(newTx)
                usersRef.child(userItem.id).child("transactions").child(newTx.id).setValue(newTx)
                userMissionsRef.child(userItem.id).child("m_daily_checkin").child("claimedDate").setValue(today)
            } catch (e: Exception) {
                Log.w(TAG, "Notice syncing daily check-in to RTDB: ${e.message}")
            }

            try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("transactions").document(newTx.id).set(newTx)
                firestore.collection("users").document(userItem.id).collection("transactions").document(newTx.id).set(newTx)
                firestore.collection("users").document(userItem.id).set(
                    mapOf(
                        "id" to userItem.id,
                        "last_claim_timestamp" to FieldValue.serverTimestamp(),
                        "tokens" to updatedUser.tokens,
                        "tokenBalance" to updatedUser.tokens,
                        "tokensBalance" to updatedUser.tokens,
                        "rewardTokens" to updatedUser.tokens,
                        "reward_tokens" to updatedUser.tokens,
                        "activityPoints" to updatedUser.tokens,
                        "loginStreak" to newStreak,
                        "lastLoginClaimDate" to today,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Notice syncing daily check-in to Firestore: ${e.message}")
            }
        }

        return@withContext ClaimMissionResult.Success("Claimed +$rewardTokens Tokens! Streak: $newStreak Days")
    }

    suspend fun claimMissionReward(mission: Mission): ClaimMissionResult = withContext(Dispatchers.IO) {
        val userItem = db.userDao().getUserSync() ?: user.firstOrNull() ?: return@withContext ClaimMissionResult.Failure("User profile not found")

        if (mission.id == "m_daily_checkin") {
            return@withContext claimDailyLoginMission()
        }

        if (mission.isClaimed) {
            return@withContext ClaimMissionResult.Failure("Mission reward already claimed!")
        }

        if (!mission.isCompleted) {
            return@withContext ClaimMissionResult.Failure("Mission requirements not completed yet")
        }

        val rewardTokens = mission.rewardCurrency.toInt()
        val updatedUser = userItem.copy(tokens = userItem.tokens + rewardTokens)
        val updatedMission = mission.copy(isClaimed = true)

        val newTx = Transaction(
            userId = userItem.id,
            type = "MISSION_REWARD",
            amount = rewardTokens.toDouble(),
            detail = "Claimed Mission: ${mission.title} (+${rewardTokens} Tokens)",
            isPositive = true,
            timestamp = System.currentTimeMillis()
        )

        try {
            db.userDao().update(updatedUser)
            db.missionDao().update(updatedMission)
            db.transactionDao().insert(newTx)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating local database for mission claim", e)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                syncUserToRealtimeDb(updatedUser)
                usersRef.child(userItem.id).child("tokens").setValue(updatedUser.tokens)
                usersRef.child(userItem.id).child("tokenBalance").setValue(updatedUser.tokens)
                usersRef.child(userItem.id).child("tokensBalance").setValue(updatedUser.tokens)
                usersRef.child(userItem.id).child("rewardTokens").setValue(updatedUser.tokens)
                usersRef.child(userItem.id).child("reward_tokens").setValue(updatedUser.tokens)
                usersRef.child(userItem.id).child("activityPoints").setValue(updatedUser.tokens)
                transactionsRef.child(newTx.id).setValue(newTx)
                usersRef.child(userItem.id).child("transactions").child(newTx.id).setValue(newTx)
                userMissionsRef.child(userItem.id).child(mission.id).child("isClaimed").setValue(true)
            } catch (e: Exception) {
                Log.w(TAG, "Notice syncing mission claim to RTDB: ${e.message}")
            }

            try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("transactions").document(newTx.id).set(newTx)
                firestore.collection("users").document(userItem.id).collection("transactions").document(newTx.id).set(newTx)
                firestore.collection("users").document(userItem.id).set(
                    mapOf(
                        "tokens" to updatedUser.tokens,
                        "tokenBalance" to updatedUser.tokens,
                        "tokensBalance" to updatedUser.tokens,
                        "rewardTokens" to updatedUser.tokens,
                        "reward_tokens" to updatedUser.tokens,
                        "activityPoints" to updatedUser.tokens,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                firestore.collection("user_missions")
                    .document(userItem.id)
                    .collection("missions")
                    .document(mission.id)
                    .set(
                        mapOf(
                            "missionId" to mission.id,
                            "isClaimed" to true,
                            "last_claim_timestamp" to FieldValue.serverTimestamp()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
            } catch (e: Exception) {
                Log.w(TAG, "Notice syncing mission claim to Firestore: ${e.message}")
            }
        }

        return@withContext ClaimMissionResult.Success("Claimed $rewardTokens Tokens for ${mission.title}!")
    }

    /**
     * Converts Tokens to VT Tokens (Wallet Balance) at 10 Tokens = 1 VT Token.
     */
    suspend fun convertTokensToVt(tokensToConvert: Int): ConvertResult {
        val userItem = db.userDao().getUserSync() ?: user.firstOrNull() ?: return ConvertResult.Failure("User not found")
        if (tokensToConvert < 10) {
            return ConvertResult.Failure("Minimum 10 Tokens required to convert (10 Tokens = 1 VT)")
        }
        if (userItem.tokens < tokensToConvert) {
            return ConvertResult.Failure("Insufficient Tokens. You have ${userItem.tokens} Tokens.")
        }

        val convertedVt = (tokensToConvert / 10).toDouble()
        val remainderTokens = tokensToConvert % 10
        val actualTokensDeducted = tokensToConvert - remainderTokens
        val newTotalTokensConverted = userItem.totalTokensConverted + actualTokensDeducted

        val updatedUser = userItem.copy(
            tokens = userItem.tokens - actualTokensDeducted,
            balance = userItem.balance + convertedVt,
            totalTokensConverted = newTotalTokensConverted
        )

        val newTx = Transaction(
            userId = userItem.id,
            type = "TOKEN_CONVERSION",
            amount = convertedVt,
            detail = "Converted $actualTokensDeducted Tokens -> $convertedVt VT (Rate: 10 Tokens = 1 VT)",
            isPositive = true,
            timestamp = System.currentTimeMillis()
        )

        try {
            db.userDao().update(updatedUser)
            db.transactionDao().insert(newTx)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    syncUserToRealtimeDb(updatedUser)
                    transactionsRef.child(newTx.id).setValue(newTx)
                    usersRef.child(userItem.id).child("transactions").child(newTx.id).setValue(newTx)
                    walletRequestsRef.child(newTx.id).setValue(newTx)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync token conversion to RTDB: ${e.message}")
                }

                try {
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("transactions").document(newTx.id).set(newTx)
                    firestore.collection("wallet_transactions").document(newTx.id).set(newTx)
                    firestore.collection("users").document(userItem.id)
                        .collection("transactions").document(newTx.id).set(newTx)
                    firestore.collection("users").document(userItem.id).set(
                        mapOf(
                            "balance" to updatedUser.balance,
                            "walletBalance" to updatedUser.balance,
                            "wallet_balance" to updatedUser.balance,
                            "tokens" to updatedUser.tokens,
                            "tokenBalance" to updatedUser.tokens,
                            "tokensBalance" to updatedUser.tokens,
                            "totalTokensConverted" to newTotalTokensConverted,
                            "total_tokens_converted" to newTotalTokensConverted,
                            "tokensConverted" to newTotalTokensConverted,
                            "lastTokenConversionAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync token conversion to Firestore: ${e.message}")
                }
            }
            return ConvertResult.Success("Successfully converted $actualTokensDeducted Tokens to $convertedVt VT Tokens!")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert tokens locally", e)
            return ConvertResult.Failure("Failed to convert tokens. Try again.")
        }
    }

    val liveMatchUpdates: Flow<Map<String, com.example.data.model.LiveMatchUpdate>> = kotlinx.coroutines.flow.flow {
        // Only emit authentic live match data; strictly avoid mock or simulated random player updates
        emit(emptyMap())
    }

    suspend fun syncUserMatchStatsFromRemote(userId: String) {
        try {
            cleanupMockMatchStats()
            val snapshot = rtdb.getReference("match_stats").child(userId).get().await()
            val realStats = mutableListOf<com.example.data.model.MatchStat>()
            if (snapshot.exists()) {
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    if (id.startsWith("match_stat_") || id.startsWith("mock_") || id.startsWith("sample_")) continue
                    val tId = child.child("tournamentId").value?.toString() ?: ""
                    val tTitle = child.child("tournamentTitle").value?.toString() ?: ""
                    val game = child.child("game").value?.toString() ?: "Free Fire"
                    val matchNo = child.child("matchNo").value?.toString() ?: "Match #1"
                    val position = (child.child("position").value as? Number)?.toInt() ?: 1
                    val kills = (child.child("kills").value as? Number)?.toInt() ?: 0
                    val winnings = (child.child("winnings").value as? Number)?.toDouble() ?: 0.0
                    val tokensEarned = (child.child("tokensEarned").value as? Number)?.toInt() ?: 0
                    val timestamp = (child.child("timestamp").value as? Number)?.toLong() ?: System.currentTimeMillis()
                    val status = child.child("status").value?.toString() ?: "COMPLETED"

                    realStats.add(
                        com.example.data.model.MatchStat(
                            id = id,
                            tournamentId = tId,
                            tournamentTitle = tTitle,
                            game = game,
                            userId = userId,
                            matchNo = matchNo,
                            position = position,
                            kills = kills,
                            winnings = winnings,
                            tokensEarned = tokensEarned,
                            timestamp = timestamp,
                            status = status
                        )
                    )
                }
                db.matchStatDao().clearAll()
                if (realStats.isNotEmpty()) {
                    db.matchStatDao().insertAll(realStats)
                }
            } else {
                db.matchStatDao().clearAll()
            }

            // Synchronize and reconcile the user's career statistics with real match data
            val currentUser = getUserSync()
            if (currentUser != null && currentUser.id == userId) {
                val realMatchesCount = realStats.size
                val realWinsCount = realStats.count { it.position == 1 }
                val realKillsCount = realStats.sumOf { it.kills }
                if (currentUser.matchesPlayed != realMatchesCount || currentUser.totalWins != realWinsCount || currentUser.totalKills != realKillsCount) {
                    val updatedUser = currentUser.copy(
                        matchesPlayed = realMatchesCount,
                        totalWins = realWinsCount,
                        totalKills = realKillsCount
                    )
                    db.userDao().update(updatedUser)
                    syncUserToRealtimeDb(updatedUser)
                    Log.i(TAG, "Reconciled match stats for user $userId: played=$realMatchesCount, wins=$realWinsCount, kills=$realKillsCount")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice syncing user match stats: ${e.message}")
        }
    }

    suspend fun saveSearchQuery(query: String) {
        val uid = getUserSync()?.id?.takeIf { it.isNotBlank() } ?: "guest"
        db.searchHistoryDao().insert(com.example.data.model.SearchHistory(userId = uid, query = query, timestamp = System.currentTimeMillis()))
        try {
            if (uid != "guest") {
                val safeKey = query.replace("/", "_").replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
                rtdb.getReference("search_history").child(uid).child(safeKey).setValue(
                    mapOf(
                        "query" to query,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing search history to RTDB", e)
        }
    }

    suspend fun clearSearchHistory() {
        val uid = getUserSync()?.id?.takeIf { it.isNotBlank() } ?: "guest"
        db.searchHistoryDao().clearHistory(uid)
        try {
            if (uid != "guest") {
                rtdb.getReference("search_history").child(uid).removeValue()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed clearing search history from RTDB", e)
        }
    }

    fun getTournamentById(id: String): Flow<Tournament?> {
        return tournaments.map { list -> list.find { it.id == id } }
    }

    suspend fun observeLeaderboardRealtime() {
        // Handled automatically via startRealtimeLeaderboardSync()
    }

    /**
     * Fetches user profile, tournaments, and transactions from Firebase Realtime Database.
     */
    suspend fun fetchDataFromServer(force: Boolean = false): Boolean {
        val currentTime = System.currentTimeMillis()
        if (!force && currentTime - lastFetchTime < FETCH_COOLDOWN_MS) {
            Log.d(TAG, "Fetch debounced (using cached data).")
            return true
        }
        lastFetchTime = currentTime
        
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser ?: return true
        val uid = firebaseUser.uid
        val email = firebaseUser.email ?: ""

        try {
            withContext(Dispatchers.IO) {
                // 1. Fetch User from Realtime Database
                try {
                    val userSnapshot = usersRef.child(uid).get().await()
                    if (userSnapshot.exists()) {
                        val fetchedUser = parseUserFromSnapshot(userSnapshot, uid)
                        db.userDao().clearAll()
                        db.userDao().insert(fetchedUser)
                        Log.i(TAG, "User $uid successfully synced from Realtime Database: ${fetchedUser.username}")
                    } else {
                        // Check if user exists in Firestore before creating a new one
                        var foundFirestoreUser: User? = null
                        try {
                            val fsDoc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                            if (fsDoc.exists()) {
                                val fsTokens = (fsDoc.get("tokens") as? Number)?.toInt()
                                    ?: (fsDoc.get("tokenBalance") as? Number)?.toInt()
                                    ?: (fsDoc.get("tokensBalance") as? Number)?.toInt()
                                    ?: (fsDoc.get("rewardTokens") as? Number)?.toInt()
                                    ?: (fsDoc.get("activityPoints") as? Number)?.toInt() ?: 0
                                val fsBalance = (fsDoc.get("balance") as? Number)?.toDouble()
                                    ?: (fsDoc.get("walletBalance") as? Number)?.toDouble()
                                    ?: (fsDoc.get("wallet_balance") as? Number)?.toDouble() ?: 0.0
                                val fsName = fsDoc.getString("username") ?: fsDoc.getString("name") ?: firebaseUser.displayName ?: email.substringBefore("@").ifBlank { "Player" }
                                foundFirestoreUser = User(
                                    id = uid,
                                    username = fsName,
                                    phoneOrEmail = fsDoc.getString("phoneOrEmail") ?: fsDoc.getString("email") ?: email,
                                    fullName = fsDoc.getString("fullName") ?: fsName,
                                    avatarUrl = fsDoc.getString("avatarUrl") ?: firebaseUser.photoUrl?.toString() ?: "",
                                    balance = fsBalance,
                                    tokens = fsTokens,
                                    avatarIdx = (fsDoc.get("avatarIdx") as? Number)?.toInt() ?: 1,
                                    dateOfJoining = (fsDoc.get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis()
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Firestore user lookup notice in fetch: ${e.message}")
                        }

                        val userToSet = foundFirestoreUser ?: User(
                            id = uid,
                            username = firebaseUser.displayName ?: if (email.isNotBlank()) email.substringBefore("@") else "Player",
                            phoneOrEmail = email,
                            fullName = firebaseUser.displayName ?: "",
                            avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                            balance = 0.0,
                            avatarIdx = 1,
                            dateOfJoining = System.currentTimeMillis()
                        )
                        syncUserToRealtimeDb(userToSet)
                        db.userDao().clearAll()
                        db.userDao().insert(userToSet)
                        Log.i(TAG, "User $uid initialized/synced in Realtime Database: ${userToSet.username} with tokens=${userToSet.tokens}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "User RTDB fetch notice: ${e.message}.")
                }

                // 2. Fetch User Transactions & Auto-reconcile Earned Tokens
                try {
                    val txsSnapshot = transactionsRef.orderByChild("userId").equalTo(uid).get().await()
                    val fetchedTxs = mutableListOf<Transaction>()
                    for (child in txsSnapshot.children) {
                        val tx = child.getValue(Transaction::class.java)
                        if (tx != null) {
                            fetchedTxs.add(tx)
                        }
                    }
                    if (fetchedTxs.isNotEmpty()) {
                        db.transactionDao().clearAll()
                        db.transactionDao().insertAll(fetchedTxs)

                        // If user converted tokens, ensure totalTokensConverted is tracked accurately
                        val currentUser = db.userDao().getUserSync()
                        if (currentUser != null) {
                            var totalConverted = 0
                            for (tx in fetchedTxs) {
                                val detail = tx.detail.lowercase()
                                val type = tx.type.uppercase()
                                if (type == "TOKEN_CONVERSION" || detail.contains("convert")) {
                                    val regexMatch = Regex("""converted\s+(\d+)\s*tokens?""", RegexOption.IGNORE_CASE).find(detail)
                                    val count = regexMatch?.groupValues?.get(1)?.toIntOrNull() ?: (tx.amount * 10).toInt()
                                    totalConverted += count
                                }
                            }
                            if (totalConverted > currentUser.totalTokensConverted) {
                                val reconciled = currentUser.copy(totalTokensConverted = totalConverted)
                                db.userDao().update(reconciled)
                                syncUserToRealtimeDb(reconciled)
                                Log.i(TAG, "Reconciled total converted tokens for user $uid to $totalConverted")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Transactions RTDB fetch notice: ${e.message}")
                }

                // 3. Sync real tournament match stats history
                syncUserMatchStatsFromRemote(uid)

                // 4. Fetch tournaments from all RTDB and Firestore sources
                try {
                    val fetchedTournaments = mutableListOf<Tournament>()
                    
                    // RTDB tournaments
                    try {
                        val rtdbTournamentsSnap = tournamentsRef.get().await()
                        for (child in rtdbTournamentsSnap.children) {
                            val parsed = parseTournamentFromDataSnapshot(child, uid)
                            if (parsed != null && !isMockTournament(parsed.id, parsed.title)) {
                                fetchedTournaments.add(parsed)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "RTDB tournaments fetch notice: ${e.message}")
                    }

                    // Firestore tournaments
                    try {
                        val fsTournamentsSnap = FirebaseFirestore.getInstance().collection("tournaments").get().await()
                        for (doc in fsTournamentsSnap.documents) {
                            val parsed = parseTournamentFromFirestoreDoc(doc, uid)
                            if (parsed != null && !isMockTournament(parsed.id, parsed.title)) {
                                fetchedTournaments.add(parsed)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Firestore tournaments fetch notice: ${e.message}")
                    }

                    val validIds = fetchedTournaments.map { it.id }.toSet()
                    val localTournaments = db.tournamentDao().getAllSync()
                    for (t in localTournaments) {
                        if (t.id !in validIds || isMockTournament(t.id, t.title)) {
                            db.tournamentDao().delete(t.id)
                        }
                    }
                    if (fetchedTournaments.isNotEmpty()) {
                        db.tournamentDao().insertAll(fetchedTournaments)
                        Log.i(TAG, "Explicitly fetched and saved ${fetchedTournaments.size} tournaments from remote")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Tournament remote fetch notice: ${e.message}")
                }

                // 5. Fetch Banners from RTDB & Firestore
                try {
                    val rtdbBannersSnap = bannersRef.get().await()
                    val bannerList = mutableListOf<Banner>()
                    for (child in rtdbBannersSnap.children) {
                        val parsed = parseBannerFromSnapshot(child)
                        if (parsed != null) bannerList.add(parsed)
                    }
                    val localBanners = db.bannerDao().getAllSync()
                    for (b in localBanners) {
                        if (isMockBanner(b.id, b.title)) {
                            db.bannerDao().delete(b.id)
                        }
                    }
                    if (bannerList.isNotEmpty()) {
                        db.bannerDao().insertAll(bannerList)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Banners remote fetch notice: ${e.message}")
                }

                // 6. Fetch Missions from RTDB & Firestore
                try {
                    val missionsSnap = missionsRef.get().await()
                    val missionList = mutableListOf<Mission>()
                    for (child in missionsSnap.children) {
                        val id = child.key ?: child.child("id").getValue(String::class.java) ?: continue
                        val title = child.child("title").getValue(String::class.java) ?: ""
                        val description = child.child("description").getValue(String::class.java) ?: ""
                        val target = (child.child("target").value as? Number)?.toInt() ?: 1
                        val rewardCurrency = (child.child("rewardCurrency").value as? Number)?.toDouble() ?: 20.0
                        val category = child.child("category").getValue(String::class.java) ?: "DAILY"
                        missionList.add(
                            Mission(
                                id = id,
                                title = title,
                                description = description,
                                target = target,
                                progress = 0,
                                rewardCurrency = rewardCurrency,
                                isCompleted = false,
                                isClaimed = false,
                                category = category
                            )
                        )
                    }
                    val currentMissions = db.missionDao().getAllMissions().firstOrNull() ?: emptyList()
                    val userItem = db.userDao().getUserSync() ?: user.firstOrNull()
                    val today = getTodayIstDate()
                    val localDaily = currentMissions.find { it.id == "m_daily_checkin" }
                    val isDailyClaimed = (userItem?.lastLoginClaimDate == today) || (localDaily?.isClaimed == true)

                    val defaultMissions = getDefaultMissions(isDailyClaimed)
                    val mergedMissions = mutableListOf<Mission>()
                    mergedMissions.addAll(missionList)
                    for (dm in defaultMissions) {
                        if (mergedMissions.none { it.id == dm.id }) {
                            val existing = currentMissions.find { it.id == dm.id }
                            mergedMissions.add(
                                dm.copy(
                                    progress = existing?.progress ?: dm.progress,
                                    isCompleted = existing?.isCompleted ?: dm.isCompleted,
                                    isClaimed = if (dm.id == "m_daily_checkin") isDailyClaimed else (existing?.isClaimed ?: dm.isClaimed)
                                )
                            )
                        }
                    }
                    db.missionDao().deleteAll()
                    db.missionDao().insertAll(mergedMissions)
                } catch (e: Exception) {
                    Log.w(TAG, "Missions remote fetch notice: ${e.message}")
                }

                // 7. Fetch Notifications from RTDB & Firestore
                try {
                    val notifList = mutableListOf<AppNotification>()
                    val rtdbNotifSnap = notificationsRef.get().await()
                    for (child in rtdbNotifSnap.children) {
                        val parsed = parseNotificationFromSnapshot(child)
                        if (parsed != null) notifList.add(parsed)
                    }
                    val userNotifSnap = usersRef.child(uid).child("notifications").get().await()
                    for (child in userNotifSnap.children) {
                        val parsed = parseNotificationFromSnapshot(child)
                        if (parsed != null) notifList.add(parsed)
                    }
                    if (notifList.isNotEmpty()) {
                        db.appNotificationDao().insertAll(notifList)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Notifications remote fetch notice: ${e.message}")
                }

                // 8. Fetch User Reports from RTDB & Firestore
                try {
                    val reportsList = mutableListOf<UserReport>()
                    val rtdbReportsSnap = reportsRef.orderByChild("userId").equalTo(uid).get().await()
                    for (child in rtdbReportsSnap.children) {
                        val parsed = parseUserReportFromSnapshot(child)
                        if (parsed != null) reportsList.add(parsed)
                    }
                    val userReportsSnap = usersRef.child(uid).child("reports").get().await()
                    for (child in userReportsSnap.children) {
                        val parsed = parseUserReportFromSnapshot(child)
                        if (parsed != null && reportsList.none { it.id == parsed.id }) reportsList.add(parsed)
                    }
                    if (reportsList.isNotEmpty()) {
                        db.userReportDao().insertAll(reportsList)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Reports remote fetch notice: ${e.message}")
                }

                // 9. Attach managed realtime listener for user updates
                startRealtimeUserSync(uid)
            }
            return true

        } catch (e: Throwable) {
            Log.w(TAG, "Notice while fetching remote data: ${e.message}")
            return true
        }
    }

    /**
     * Public helper to refresh all backend listeners and fetch the latest state immediately.
     */
    suspend fun refreshBackendSync(force: Boolean = true): Boolean {
        return fetchDataFromServer(force = force)
    }

    /**
     * Joins a tournament and syncs to Realtime Database /tournaments/$id/participants/$userId.
     */
    suspend fun joinTournament(tournamentId: String): JoinResult {
        val userItem = user.firstOrNull() ?: return JoinResult.Failure("User not found")
        val match = tournaments.firstOrNull()?.find { it.id == tournamentId } 
            ?: return JoinResult.Failure("Tournament not found")

        if (match.joined) return JoinResult.Failure("Already joined this tournament")
        if (match.isFull) return JoinResult.Failure("Tournament is full")
        if (userItem.balance < match.entryFee) return JoinResult.Failure("Insufficient balance. Please add funds!")

        val updatedUser = userItem.copy(
            balance = userItem.balance - match.entryFee
        )
        val updatedMatch = match.copy(joined = true, filledSlots = match.filledSlots + 1)
        val newTx = Transaction(
            userId = userItem.id,
            type = "ENTRY_FEE",
            amount = match.entryFee,
            detail = "Joined ${match.game}: ${match.title}",
            isPositive = false,
            timestamp = System.currentTimeMillis()
        )

        try {
            db.userDao().update(updatedUser)
            db.tournamentDao().update(updatedMatch)
            db.transactionDao().insert(newTx)
            updateMissionProgress("m_tournament_contender", 1)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Update user balance & stats in RTDB & Firestore
                    syncUserToRealtimeDb(updatedUser)
                    transactionsRef.child(newTx.id).setValue(newTx)

                    // Update tournament participant list in RTDB & Firestore
                    val participantData = mapOf(
                        "userId" to userItem.id,
                        "userUid" to userItem.id,
                        "username" to userItem.username,
                        "inGameName" to userItem.inGameName,
                        "freeFireId" to userItem.freeFireId,
                        "tournamentId" to tournamentId,
                        "tournamentTitle" to match.title,
                        "game" to match.game,
                        "joinedAt" to System.currentTimeMillis(),
                        "timestamp" to System.currentTimeMillis()
                    )
                    tournamentsRef.child(tournamentId).child("participants").child(userItem.id).setValue(participantData)
                    tournamentsRef.child(tournamentId).child("filledSlots").setValue(updatedMatch.filledSlots)
                    tournamentsRef.child(tournamentId).child("currentParticipants").setValue(updatedMatch.filledSlots)
                    tournamentRegistrationsRef.child("${tournamentId}_${userItem.id}").setValue(participantData)

                    // Firestore sync
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("tournaments").document(tournamentId)
                        .collection("participants").document(userItem.id).set(participantData)
                    firestore.collection("tournament_registrations")
                        .document("${tournamentId}_${userItem.id}").set(participantData)
                    firestore.collection("transactions").document(newTx.id).set(newTx)
                } catch (e: Exception) {
                    Log.e(TAG, "Syncing join tournament to backend failed: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Local join failed", e)
            return JoinResult.Failure("Database error. Try again.")
        }

        return JoinResult.Success("Successfully registered for ${match.title}! Room details will be visible here before the match.")
    }

    sealed class DepositResult {
        data class Success(val message: String) : DepositResult()
        data class Failure(val message: String) : DepositResult()
    }

    suspend fun submitDepositRequest(amount: Double, utrNumber: String = "", paymentRef: String = ""): DepositResult {
        val userItem = user.firstOrNull() ?: return DepositResult.Failure("User session expired. Please log in again.")
        if (amount <= 0) return DepositResult.Failure("Deposit amount must be greater than zero.")

        val cleanUtr = utrNumber.trim()
        if (cleanUtr.length != 12 || !cleanUtr.all { it.isDigit() }) {
            return DepositResult.Failure("Invalid UTR number. Please provide the 12-digit transaction ID from your UPI app.")
        }
        if (cleanUtr.toSet().size <= 1) {
            return DepositResult.Failure("Invalid UTR number. Dummy sequence detected.")
        }

        // Check if this UTR has already been submitted locally
        val existingLocalTx = transactions.firstOrNull()?.find { tx ->
            tx.detail.contains(cleanUtr)
        }
        if (existingLocalTx != null) {
            return DepositResult.Failure("This UTR number ($cleanUtr) has already been submitted and is ${existingLocalTx.status.lowercase()}. Duplicate submissions are prevented.")
        }

        // Check cloud backend for duplicate UTR submissions across the system
        try {
            val duplicateQuery = withTimeoutOrNull(2500) {
                depositRequestsRef.orderByChild("utrNumber").equalTo(cleanUtr).get().await()
            }
            if (duplicateQuery != null && duplicateQuery.exists() && duplicateQuery.childrenCount > 0) {
                return DepositResult.Failure("This UTR ($cleanUtr) was already recorded in our system. Each UPI transaction reference can only be submitted once.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Duplicate UTR cloud query check: ${e.message}")
        }

        // Check if n8n has already pre-verified this payment from bank/SMS/FamPay notification
        var isPreVerifiedByN8n = false
        try {
            val preVerifiedSnap = withTimeoutOrNull(2500) {
                verifiedBankDepositsRef.child(cleanUtr).get().await()
            }
            if (preVerifiedSnap != null && preVerifiedSnap.exists()) {
                val preAmount = preVerifiedSnap.child("amount").getValue(Double::class.java) ?: 0.0
                val claimed = preVerifiedSnap.child("claimed").getValue(Boolean::class.java) ?: false
                if (!claimed && Math.abs(preAmount - amount) < 0.01) {
                    isPreVerifiedByN8n = true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pre-verified bank deposit check: ${e.message}")
        }

        val depositReqId = "REQ-DEP-${System.currentTimeMillis()}-${(1000..9999).random()}"
        val initialStatus = if (isPreVerifiedByN8n) "SUCCESS" else "PENDING"
        val newTx = Transaction(
            id = depositReqId,
            userId = userItem.id,
            type = "DEPOSIT_REQUEST",
            amount = amount,
            detail = if (isPreVerifiedByN8n) "Deposit Request (UTR: $cleanUtr) - Auto-Verified" else "Deposit Request (UTR: $cleanUtr) - Pending Verification",
            isPositive = true,
            timestamp = System.currentTimeMillis(),
            status = initialStatus
        )

        val depositData = mapOf(
            "id" to depositReqId,
            "requestId" to depositReqId,
            "type" to "DEPOSIT",
            "requestType" to "DEPOSIT",
            "userId" to userItem.id,
            "userUid" to userItem.id,
            "username" to userItem.username,
            "fullName" to userItem.fullName,
            "email" to userItem.phoneOrEmail,
            "phoneOrEmail" to userItem.phoneOrEmail,
            "inGameName" to userItem.inGameName,
            "freeFireId" to userItem.freeFireId,
            "amount" to amount,
            "utrNumber" to cleanUtr,
            "paymentRef" to paymentRef.ifBlank { cleanUtr },
            "paymentMethod" to "UPI_MANUAL_UTR",
            "status" to initialStatus,
            "autoVerified" to isPreVerifiedByN8n,
            "timestamp" to System.currentTimeMillis(),
            "createdAt" to System.currentTimeMillis(),
            "transactionId" to newTx.id,
            "userBalanceBefore" to userItem.balance
        )

        try {
            db.transactionDao().insert(newTx)
            if (isPreVerifiedByN8n) {
                // Instantly credit user balance
                val updatedUser = userItem.copy(balance = userItem.balance + amount)
                db.userDao().insert(updatedUser)
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    transactionsRef.child(newTx.id).setValue(newTx)
                    depositRequestsRef.child(depositReqId).setValue(depositData)
                    walletRequestsRef.child(depositReqId).setValue(depositData)
                    if (isPreVerifiedByN8n) {
                        usersRef.child(userItem.id).child("balance").setValue(userItem.balance + amount)
                        verifiedBankDepositsRef.child(cleanUtr).child("claimed").setValue(true)
                        processedUtrsRef.child(cleanUtr).setValue(mapOf(
                            "userId" to userItem.id,
                            "amount" to amount,
                            "verifiedAt" to System.currentTimeMillis(),
                            "requestId" to depositReqId
                        ))
                    }

                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("deposit_requests").document(depositReqId).set(depositData)
                    firestore.collection("wallet_requests").document(depositReqId).set(depositData)
                    firestore.collection("transactions").document(newTx.id).set(newTx)
                    firestore.collection("users").document(userItem.id)
                        .collection("deposit_requests").document(depositReqId).set(depositData)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync deposit request to backend: ${e.message}")
                }
            }
            return if (isPreVerifiedByN8n) {
                DepositResult.Success("Payment Auto-Verified! VT ${amount.toInt()} has been instantly credited to your wallet.")
            } else {
                DepositResult.Success("Deposit Request Submitted! Your 12-digit UTR ($cleanUtr) is under review. VT ${amount.toInt()} will be credited after verification.")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to submit deposit request locally", e)
            return DepositResult.Failure("Failed to record deposit request. Please try again.")
        }
    }

    suspend fun addFunds(amount: Double, utrNumber: String = "", paymentRef: String = "") {
        submitDepositRequest(amount, utrNumber, paymentRef)
    }

    fun getWalletBreakdown(user: User?, transactions: List<Transaction>): WalletBreakdown {
        val total = (user?.balance ?: 0.0).coerceAtLeast(0.0)
        val txWinnings = transactions
            .filter { it.userId == user?.id && (it.type == "WINNINGS" || it.type == "MATCH_PRIZE" || it.detail.contains("Prize", ignoreCase = true) || it.detail.contains("Winnings", ignoreCase = true) || it.detail.contains("Bounty", ignoreCase = true)) }
            .filter { it.status == "SUCCESS" || it.status.isBlank() }
            .sumOf { it.amount }
        val txWithdrawn = transactions
            .filter { it.userId == user?.id && it.type == "WITHDRAWAL" && it.status != "REJECTED" && it.status != "FAILED" }
            .sumOf { it.amount }
        val netWinnings = maxOf(0.0, txWinnings - txWithdrawn).coerceAtMost(total)
        val deposited = maxOf(0.0, total - netWinnings)
        return WalletBreakdown(total = total, deposited = deposited, winnings = netWinnings)
    }

    suspend fun getWalletBreakdownSync(): WalletBreakdown {
        val currentUser = getUserSync()
        val txs = db.transactionDao().getAllSync()
        return getWalletBreakdown(currentUser, txs)
    }

    suspend fun withdrawFunds(amount: Double, upiId: String = ""): WithdrawResult {
        val userItem = user.firstOrNull() ?: return WithdrawResult.Failure("User info missing")
        if (amount <= 0) return WithdrawResult.Failure("Withdrawal amount must be greater than zero.")
        if (amount < 50.0) return WithdrawResult.Failure("Minimum withdrawal limit is VT 50.")
        if (amount > 10000.0) return WithdrawResult.Failure("Maximum single withdrawal limit is VT 10,000.")

        val targetUpi = if (upiId.isNotBlank()) upiId.trim() else userItem.phoneOrEmail.trim()
        if (targetUpi.isBlank() || !targetUpi.contains("@") || targetUpi.length < 5) {
            return WithdrawResult.Failure("Please enter a valid UPI ID (e.g. yourname@okhdfc or name@paytm).")
        }

        // Financial enforcement: Only tournament winnings are withdrawable
        val breakdown = getWalletBreakdown(userItem, db.transactionDao().getAllSync())
        if (amount > breakdown.winnings) {
            return WithdrawResult.Failure(
                "Cannot withdraw more than your tournament winnings (Available: VT ${breakdown.winnings.toInt()}). Deposited balance is reserved for tournament match fees."
            )
        }
        if (userItem.balance < amount) {
            return WithdrawResult.Failure("Insufficient wallet balance.")
        }

        val updatedUser = userItem.copy(balance = userItem.balance - amount)
        val newTx = Transaction(
            userId = userItem.id,
            type = "WITHDRAWAL",
            amount = amount,
            detail = "Withdrawal to UPI: $targetUpi",
            isPositive = false,
            timestamp = System.currentTimeMillis(),
            status = "PENDING"
        )

        val withdrawReqId = "REQ-WDR-${System.currentTimeMillis()}-${(1000..9999).random()}"
        val withdrawData = mapOf(
            "id" to withdrawReqId,
            "requestId" to withdrawReqId,
            "type" to "WITHDRAWAL",
            "requestType" to "WITHDRAWAL",
            "userId" to userItem.id,
            "userUid" to userItem.id,
            "username" to userItem.username,
            "fullName" to userItem.fullName,
            "email" to userItem.phoneOrEmail,
            "phoneOrEmail" to userItem.phoneOrEmail,
            "inGameName" to userItem.inGameName,
            "freeFireId" to userItem.freeFireId,
            "amount" to amount,
            "upiId" to targetUpi,
            "paymentMethod" to "UPI",
            "status" to "PENDING",
            "timestamp" to System.currentTimeMillis(),
            "createdAt" to System.currentTimeMillis(),
            "transactionId" to newTx.id,
            "userBalanceBefore" to userItem.balance,
            "userBalanceAfter" to updatedUser.balance
        )

        try {
            db.userDao().update(updatedUser)
            db.transactionDao().insert(newTx)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    syncUserToRealtimeDb(updatedUser)
                    transactionsRef.child(newTx.id).setValue(newTx)
                    withdrawRequestsRef.child(withdrawReqId).setValue(withdrawData)
                    walletRequestsRef.child(withdrawReqId).setValue(withdrawData)
                    usersRef.child(userItem.id).child("withdraw_requests").child(withdrawReqId).setValue(withdrawData)

                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("withdraw_requests").document(withdrawReqId).set(withdrawData)
                    firestore.collection("wallet_requests").document(withdrawReqId).set(withdrawData)
                    firestore.collection("transactions").document(newTx.id).set(newTx)
                    firestore.collection("users").document(userItem.id)
                        .collection("withdraw_requests").document(withdrawReqId).set(withdrawData)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync withdraw to backend: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Withdraw failed locally", e)
            return WithdrawResult.Failure("Database error. Try again.")
        }
        return WithdrawResult.Success("Withdrawal of VT ${amount.toInt()} initiated successfully. Status: PENDING admin review.")
    }

    /**
     * Guarantees a unique, collision-proof referral code across all users.
     * Checks Firestore and Realtime Database reservations before assigning.
     */
    suspend fun ensureUniqueReferralCode(user: User): String {
        if (user.referralCode.isNotBlank() && user.referralCode.startsWith("VRX-") && user.referralCode.length >= 10) {
            return user.referralCode
        }

        val cleanUser = user.username.filter { it.isLetterOrDigit() }.uppercase()
        val userPrefix = if (cleanUser.length >= 3) cleanUser.take(4).padEnd(4, 'X') else "USER"
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val firestore = FirebaseFirestore.getInstance()
        var uniqueCode = ""

        for (attempt in 1..10) {
            val entropy = (1..4).map { chars.random() }.joinToString("")
            val candidate = "VRX-$userPrefix-$entropy"

            try {
                val fsDoc = firestore.collection("referral_codes").document(candidate).get().await()
                val fsUid = fsDoc.getString("userId")
                val isAvailableInFs = !fsDoc.exists() || fsUid == user.id

                val rtdbRef = FirebaseDatabase.getInstance().getReference("referral_codes").child(candidate)
                val rtdbSnap = rtdbRef.get().await()
                val rtdbUid = rtdbSnap.child("userId").getValue(String::class.java)
                val isAvailableInRtdb = !rtdbSnap.exists() || rtdbUid == user.id

                if (isAvailableInFs && isAvailableInRtdb) {
                    uniqueCode = candidate
                    val reservationData = mapOf(
                        "code" to candidate,
                        "userId" to user.id,
                        "username" to user.username,
                        "createdAt" to System.currentTimeMillis()
                    )
                    firestore.collection("referral_codes").document(candidate).set(reservationData).await()
                    rtdbRef.setValue(reservationData).await()
                    break
                }
            } catch (e: Exception) {
                Log.w(TAG, "Checking candidate referral code $candidate: ${e.message}")
            }
        }

        if (uniqueCode.isBlank()) {
            val highEntropy = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
            uniqueCode = "VRX-$highEntropy"
            val reservationData = mapOf(
                "code" to uniqueCode,
                "userId" to user.id,
                "username" to user.username,
                "createdAt" to System.currentTimeMillis()
            )
            try {
                firestore.collection("referral_codes").document(uniqueCode).set(reservationData)
                FirebaseDatabase.getInstance().getReference("referral_codes").child(uniqueCode).setValue(reservationData)
            } catch (e: Exception) {
                Log.w(TAG, "Notice setting fallback reservation: ${e.message}")
            }
        }

        return uniqueCode
    }

    /**
     * Applies a referral code with strict enforcement:
     * - Prevents self-referral
     * - Prevents duplicate redemption (one per user)
     * - Verifies code existence in Firestore / RTDB
     * - Credits 50 tokens bonus to referee
     * - Credits 50 tokens + increments referralCount & referralEarnings to referrer
     * - Logs transactions and sends in-app notifications
     */
    suspend fun applyReferralCode(code: String): Result<String> {
        val referralClean = code.trim().uppercase()
        if (referralClean.isBlank() || referralClean.length < 4) {
            return Result.failure(Exception("Please enter a valid referral code."))
        }

        val currentUser = getUserSync() ?: return Result.failure(Exception("User not found. Please log in first."))

        if (currentUser.referralCode.isNotBlank() && referralClean.equals(currentUser.referralCode, ignoreCase = true)) {
            return Result.failure(Exception("You cannot redeem your own referral code!"))
        }

        if (currentUser.referredBy.isNotBlank()) {
            return Result.failure(Exception("You have already redeemed a referral code (${currentUser.referredBy})!"))
        }

        var referrerUid: String? = null
        var referrerUsername = "Friend"

        try {
            val firestore = FirebaseFirestore.getInstance()
            // 1. Check reservation in referral_codes collection
            val codeDoc = firestore.collection("referral_codes").document(referralClean).get().await()
            if (codeDoc.exists()) {
                referrerUid = codeDoc.getString("userId")
                referrerUsername = codeDoc.getString("username") ?: "Friend"
            }

            // 2. Check users collection by referralCode
            if (referrerUid == null) {
                val usersQuery = firestore.collection("users")
                    .whereEqualTo("referralCode", referralClean)
                    .limit(1)
                    .get()
                    .await()
                if (!usersQuery.isEmpty) {
                    val doc = usersQuery.documents.first()
                    referrerUid = doc.id
                    referrerUsername = doc.getString("username") ?: "Friend"
                }
            }

            // 3. Fallback to Realtime Database
            if (referrerUid == null) {
                val rtdbRef = FirebaseDatabase.getInstance().getReference("referral_codes").child(referralClean).get().await()
                if (rtdbRef.exists()) {
                    referrerUid = rtdbRef.child("userId").getValue(String::class.java)
                    referrerUsername = rtdbRef.child("username").getValue(String::class.java) ?: "Friend"
                } else {
                    val userSnap = usersRef.orderByChild("referralCode").equalTo(referralClean).get().await()
                    if (userSnap.exists()) {
                        for (child in userSnap.children) {
                            referrerUid = child.key
                            referrerUsername = child.child("username").getValue(String::class.java) ?: "Friend"
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error looking up referral code: ${e.message}")
        }

        if (referrerUid == null) {
            return Result.failure(Exception("Invalid referral code. No player found with code $referralClean."))
        }

        if (referrerUid == currentUser.id) {
            return Result.failure(Exception("You cannot redeem your own referral code!"))
        }

        val updatedUser = currentUser.copy(
            referredBy = referralClean,
            tokens = currentUser.tokens + 50
        )
        val refereeTx = Transaction(
            userId = currentUser.id,
            type = "REFERRAL_BONUS",
            amount = 50.0,
            detail = "Redeemed invite code $referralClean: +50 Bonus Tokens",
            isPositive = true,
            timestamp = System.currentTimeMillis(),
            status = "SUCCESS"
        )

        try {
            db.userDao().update(updatedUser)
            db.transactionDao().insert(refereeTx)
            syncUserToRealtimeDb(updatedUser)
            transactionsRef.child(refereeTx.id).setValue(refereeTx)

            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("transactions").document(refereeTx.id).set(refereeTx)
            firestore.collection("users").document(currentUser.id).set(
                mapOf(
                    "referredBy" to referralClean,
                    "tokens" to updatedUser.tokens
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )

            // Reward referrer: +50 Tokens, increment referralCount, add 50 to referralEarnings
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val refUid = referrerUid ?: return@launch
                    val refSnap = usersRef.child(refUid).get().await()
                    val curTokens = refSnap.getIntSafe("tokens", "activityPoints", 0)
                    val curCount = refSnap.getIntSafe("referralCount", "referral_count", 0)
                    val curEarnings = refSnap.getDoubleSafe("referralEarnings", "referral_earnings", 0.0)

                    usersRef.child(refUid).child("tokens").setValue(curTokens + 50)
                    usersRef.child(refUid).child("referralCount").setValue(curCount + 1)
                    usersRef.child(refUid).child("referralEarnings").setValue(curEarnings + 50.0)

                    firestore.collection("users").document(refUid).update(
                        "tokens", FieldValue.increment(50),
                        "referralCount", FieldValue.increment(1),
                        "referralEarnings", FieldValue.increment(50.0)
                    )

                    val referrerTx = Transaction(
                        userId = refUid,
                        type = "REFERRAL_REWARD",
                        amount = 50.0,
                        detail = "Referral Reward: ${currentUser.username} redeemed your code! (+50 Tokens)",
                        isPositive = true,
                        timestamp = System.currentTimeMillis(),
                        status = "SUCCESS"
                    )
                    transactionsRef.child(referrerTx.id).setValue(referrerTx)
                    firestore.collection("transactions").document(referrerTx.id).set(referrerTx)

                    val refNotif = AppNotification(
                        id = UUID.randomUUID().toString(),
                        title = "Referral Bonus Received! 🎉",
                        message = "${currentUser.username} joined using your code! +50 Tokens added to your wallet.",
                        type = "REFERRAL",
                        timestamp = System.currentTimeMillis()
                    )
                    sendAppNotification(refNotif)
                    firestore.collection("notifications").document(refNotif.id).set(refNotif)
                } catch (e: Exception) {
                    Log.w(TAG, "Notice updating referrer in cloud: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed applying referral code locally", e)
            return Result.failure(Exception("Failed to apply referral code. Please try again."))
        }

        return Result.success("Referral code applied! 50 bonus tokens added to your wallet.")
    }

    suspend fun recordReferralBonus(updatedUser: User, transaction: Transaction) {
        try {
            db.userDao().update(updatedUser)
            db.transactionDao().insert(transaction)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    syncUserToRealtimeDb(updatedUser)
                    transactionsRef.child(transaction.id).setValue(transaction)
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("transactions").document(transaction.id).set(transaction)
                    firestore.collection("users").document(updatedUser.id).set(
                        mapOf(
                            "referredBy" to updatedUser.referredBy,
                            "tokens" to updatedUser.tokens
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Notice syncing referral bonus to cloud: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to record referral bonus locally", e)
            throw Exception("Failed to apply referral code.")
        }
    }

    suspend fun updateProfile(updatedUser: User) {
        try {
            db.userDao().update(updatedUser)
            if (updatedUser.inGameName.isNotBlank() || updatedUser.freeFireId.isNotBlank()) {
                updateMissionProgress("m_profile_master", 1)
            }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    syncUserToRealtimeDb(updatedUser)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update profile on RTDB: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to update profile locally", e)
            throw Exception("Failed to update profile.")
        }
    }

    suspend fun updateAvatar(newAvatarUrl: String) {
        try {
            val userItem = user.firstOrNull() ?: getUserSync() ?: return
            val updatedUser = userItem.copy(avatarUrl = newAvatarUrl)
            db.userDao().update(updatedUser)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    usersRef.child(updatedUser.id).child("avatarUrl").setValue(newAvatarUrl)
                    usersRef.child(updatedUser.id).child("avatar_url").setValue(newAvatarUrl)
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    firestore.collection("users").document(updatedUser.id).update("avatarUrl", newAvatarUrl)
                } catch (e: Exception) {
                    Log.w(TAG, "Notice syncing avatar to cloud: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to update avatar locally", e)
            throw Exception("Failed to update avatar.")
        }
    }

    suspend fun updateFcmToken(token: String) {
        try {
            val userItem = user.firstOrNull() ?: return
            val updated = userItem.copy(fcmToken = token)
            db.userDao().update(updated)
            usersRef.child(userItem.id).child("fcmToken").setValue(token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update FCM token", e)
        }
    }

    suspend fun updateSessionToken(token: String) {
        val currentUser = user.firstOrNull()
        if (currentUser != null) {
            val newUser = currentUser.copy(sessionToken = token)
            try {
                db.userDao().update(newUser)
                usersRef.child(newUser.id).child("sessionToken").setValue(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update session token", e)
            }
        }
    }

    suspend fun ensureFirestoreUserDocument(firebaseUser: FirebaseUser): User {
        val uid = firebaseUser.uid
        val email = firebaseUser.email ?: ""
        val photoUrl = firebaseUser.photoUrl?.toString() ?: ""
        val displayName = firebaseUser.displayName ?: if (email.isNotBlank()) email.substringBefore("@") else "Player"

        var existingUser: User? = null
        try {
            val snapshot = usersRef.child(uid).get().await()
            if (snapshot.exists()) {
                existingUser = parseUserFromSnapshot(snapshot, uid)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking RTDB user for $uid", e)
        }

        // Also check Firestore if RTDB was empty or had 0 tokens
        try {
            val firestore = FirebaseFirestore.getInstance()
            val fsDoc = firestore.collection("users").document(uid).get().await()
            if (fsDoc.exists()) {
                val fsTokens = (fsDoc.get("tokens") as? Number)?.toInt()
                    ?: (fsDoc.get("tokenBalance") as? Number)?.toInt()
                    ?: (fsDoc.get("tokensBalance") as? Number)?.toInt()
                    ?: (fsDoc.get("activityPoints") as? Number)?.toInt() ?: 0
                val fsBalance = (fsDoc.get("balance") as? Number)?.toDouble()
                    ?: (fsDoc.get("walletBalance") as? Number)?.toDouble()
                    ?: (fsDoc.get("wallet_balance") as? Number)?.toDouble() ?: 0.0
                val fsStreak = (fsDoc.get("loginStreak") as? Number)?.toInt() ?: 0
                val fsLastClaim = fsDoc.getString("lastLoginClaimDate") ?: ""
                val fsTotalConverted = (fsDoc.get("totalTokensConverted") as? Number)?.toInt()
                    ?: (fsDoc.get("total_tokens_converted") as? Number)?.toInt()
                    ?: (fsDoc.get("tokensConverted") as? Number)?.toInt() ?: 0

                if (existingUser == null) {
                    existingUser = User(
                        id = uid,
                        username = fsDoc.getString("username") ?: displayName,
                        phoneOrEmail = fsDoc.getString("phoneOrEmail") ?: email,
                        fullName = fsDoc.getString("fullName") ?: displayName,
                        avatarUrl = fsDoc.getString("avatarUrl") ?: photoUrl,
                        balance = fsBalance,
                        tokens = fsTokens,
                        totalTokensConverted = fsTotalConverted,
                        avatarIdx = (fsDoc.get("avatarIdx") as? Number)?.toInt() ?: 1,
                        loginStreak = fsStreak,
                        lastLoginClaimDate = fsLastClaim,
                        dateOfJoining = (fsDoc.get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                } else {
                    existingUser = existingUser.copy(
                        tokens = if (existingUser.tokens > 0) existingUser.tokens else fsTokens,
                        balance = if (existingUser.balance > 0.0) existingUser.balance else fsBalance,
                        totalTokensConverted = maxOf(existingUser.totalTokensConverted, fsTotalConverted),
                        loginStreak = maxOf(existingUser.loginStreak, fsStreak),
                        lastLoginClaimDate = if (existingUser.lastLoginClaimDate.isNotBlank()) existingUser.lastLoginClaimDate else fsLastClaim
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking Firestore user for $uid: ${e.message}")
        }

        // Check local SQLite cache as well so we never erase locally earned tokens
        val localUser = db.userDao().getUserSync()
        if (localUser != null && localUser.id == uid) {
            if (existingUser == null) {
                existingUser = localUser
            } else {
                existingUser = existingUser.copy(
                    totalTokensConverted = maxOf(existingUser.totalTokensConverted, localUser.totalTokensConverted),
                    loginStreak = maxOf(existingUser.loginStreak, localUser.loginStreak),
                    lastLoginClaimDate = if (existingUser.lastLoginClaimDate.isNotBlank()) existingUser.lastLoginClaimDate else localUser.lastLoginClaimDate
                )
            }
        }

        val userToSave = existingUser?.copy(
            id = uid,
            phoneOrEmail = existingUser.phoneOrEmail.ifEmpty { email },
            avatarUrl = existingUser.avatarUrl.ifEmpty { photoUrl }
        ) ?: User(
            id = uid,
            username = displayName,
            phoneOrEmail = email,
            fullName = firebaseUser.displayName ?: "",
            avatarUrl = photoUrl,
            balance = 0.0,
            avatarIdx = 1,
            dateOfJoining = System.currentTimeMillis()
        )

        try {
            db.userDao().clearAll()
            db.userDao().insert(userToSave)
            syncUserToRealtimeDb(userToSave)
            Log.i(TAG, "User $uid synced to RTDB & SQLite with tokens=${userToSave.tokens}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user document for $uid", e)
        }

        return userToSave
    }

    suspend fun saveUserProfile(
        username: String,
        phoneOrEmail: String,
        passwordHash: String = "",
        sessionToken: String = "",
        referralCodeApplied: String = ""
    ) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser ?: return
        val uid = firebaseUser.uid
        val email = firebaseUser.email ?: ""
        val photoUrl = firebaseUser.photoUrl?.toString() ?: ""
        
        var currentUser = getUserSync()
        if (currentUser == null) {
            try {
                val snapshot = usersRef.child(uid).get().await()
                if (snapshot.exists()) {
                    val fetched = parseUserFromSnapshot(snapshot, uid)
                    db.userDao().insert(fetched)
                    currentUser = fetched
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed checking RTDB user", e)
            }
        }
        
        val fallbackUsername = if (!firebaseUser.displayName.isNullOrBlank()) {
            firebaseUser.displayName!!
        } else if (email.isNotBlank()) {
            email.substringBefore("@")
        } else {
            username
        }

        val referralClean = referralCodeApplied.trim().uppercase()
        val hasReferral = referralClean.isNotBlank()

        // Verify referral code against database if provided
        var verifiedReferrerUid: String? = null
        var isReferralValid = false

        if (hasReferral) {
            try {
                // 1. Check in Firestore
                val firestore = FirebaseFirestore.getInstance()
                val fsSnapshot = firestore.collection("users")
                    .whereEqualTo("referralCode", referralClean)
                    .limit(1)
                    .get()
                    .await()
                if (!fsSnapshot.isEmpty) {
                    val refDoc = fsSnapshot.documents.first()
                    if (refDoc.id != uid) {
                        verifiedReferrerUid = refDoc.id
                        isReferralValid = true
                    }
                }

                // 2. Fallback check in Realtime Database if not found in Firestore
                if (!isReferralValid) {
                    val rtdbTask = usersRef.orderByChild("referralCode").equalTo(referralClean).get()
                    val rtdbSnap = rtdbTask.await()
                    if (rtdbSnap.exists()) {
                        for (child in rtdbSnap.children) {
                            val candidateUid = child.key ?: continue
                            if (candidateUid != uid) {
                                verifiedReferrerUid = candidateUid
                                isReferralValid = true
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Referral code database verification warning: ${e.message}")
            }
        }

        val referralToSave = if (isReferralValid) referralClean else ""
        val initialTokens = if (isReferralValid) 50 else 0

        val candidateUser = if (currentUser != null) {
            currentUser.copy(
                id = uid,
                username = if (currentUser.username.isBlank() || currentUser.username.startsWith("Player_") || currentUser.username == "Player") fallbackUsername else currentUser.username, 
                phoneOrEmail = if (phoneOrEmail.isNotBlank()) phoneOrEmail else currentUser.phoneOrEmail.ifEmpty { email }, 
                avatarUrl = currentUser.avatarUrl.ifEmpty { photoUrl },
                passwordHash = passwordHash.ifEmpty { currentUser.passwordHash },
                sessionToken = sessionToken.ifEmpty { currentUser.sessionToken },
                referredBy = if (isReferralValid && currentUser.referredBy.isBlank()) referralToSave else currentUser.referredBy,
                tokens = if (isReferralValid && currentUser.referredBy.isBlank()) currentUser.tokens + 50 else currentUser.tokens
            )
        } else {
            User(
                id = uid,
                username = fallbackUsername, 
                phoneOrEmail = phoneOrEmail.ifEmpty { email }, 
                fullName = firebaseUser.displayName ?: "",
                avatarUrl = photoUrl,
                balance = 0.0, 
                tokens = initialTokens,
                avatarIdx = 1, 
                passwordHash = passwordHash, 
                sessionToken = sessionToken, 
                dateOfJoining = System.currentTimeMillis(),
                referredBy = referralToSave
            )
        }

        val guaranteedReferralCode = ensureUniqueReferralCode(candidateUser)
        val newUser = candidateUser.copy(referralCode = guaranteedReferralCode)
            
        try {
            db.userDao().clearAll()
            db.userDao().insert(newUser)
            if (isReferralValid) {
                val refTx = Transaction(
                    userId = uid,
                    type = "REFERRAL_BONUS",
                    amount = 50.0,
                    detail = "Registration Referral Bonus ($referralClean): +50 Tokens",
                    isPositive = true,
                    timestamp = System.currentTimeMillis()
                )
                db.transactionDao().insert(refTx)
                transactionsRef.child(refTx.id).setValue(refTx)

                // Sync transaction to Firestore with server timestamp
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("transactions").document(refTx.id).set(
                    mapOf(
                        "id" to refTx.id,
                        "userId" to uid,
                        "type" to "REFERRAL_BONUS",
                        "amount" to 50.0,
                        "detail" to refTx.detail,
                        "isPositive" to true,
                        "timestamp" to System.currentTimeMillis(),
                        "serverTimestamp" to FieldValue.serverTimestamp(),
                        "status" to "SUCCESS"
                    )
                )
            }
            syncUserToRealtimeDb(newUser)
            
            // Sync user to Firestore
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users").document(uid).set(
                mapOf(
                    "id" to uid,
                    "username" to newUser.username,
                    "phoneOrEmail" to newUser.phoneOrEmail,
                    "tokens" to newUser.tokens,
                    "balance" to newUser.balance,
                    "referralCode" to newUser.referralCode,
                    "referredBy" to newUser.referredBy,
                    "referralCount" to newUser.referralCount,
                    "referralEarnings" to newUser.referralEarnings,
                    "createdAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            Log.i(TAG, "User $uid profile saved to RTDB & Firestore successfully")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed saving user profile for $uid", e)
        }

        // If a verified referral code was applied, reward the verified referrer
        if (isReferralValid && verifiedReferrerUid != null) {
            val referrerUid = verifiedReferrerUid
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    // 1. Credit in Firestore
                    firestore.collection("users").document(referrerUid).update(
                        "tokens", FieldValue.increment(50),
                        "referralCount", FieldValue.increment(1),
                        "referralEarnings", FieldValue.increment(50.0)
                    )

                    // 2. Credit in Realtime Database
                    val referrerSnap = usersRef.child(referrerUid).get().await()
                    if (referrerSnap.exists()) {
                        val currentTokens = referrerSnap.getIntSafe("tokens", "activityPoints", 0)
                        val currentCount = referrerSnap.getIntSafe("referralCount", "referral_count", 0)
                        val currentEarnings = referrerSnap.getDoubleSafe("referralEarnings", "referral_earnings", 0.0)
                        usersRef.child(referrerUid).child("tokens").setValue(currentTokens + 50)
                        usersRef.child(referrerUid).child("referralCount").setValue(currentCount + 1)
                        usersRef.child(referrerUid).child("referralEarnings").setValue(currentEarnings + 50.0)
                    }
                    
                    val referrerTx = Transaction(
                        userId = referrerUid,
                        type = "REFERRAL_REWARD",
                        amount = 50.0,
                        detail = "Referral Reward: ${newUser.username} registered with your code! (+50 Tokens)",
                        isPositive = true,
                        timestamp = System.currentTimeMillis()
                    )
                    transactionsRef.child(referrerTx.id).setValue(referrerTx)
                    firestore.collection("transactions").document(referrerTx.id).set(
                        mapOf(
                            "id" to referrerTx.id,
                            "userId" to referrerUid,
                            "type" to "REFERRAL_REWARD",
                            "amount" to 50.0,
                            "detail" to referrerTx.detail,
                            "isPositive" to true,
                            "timestamp" to System.currentTimeMillis(),
                            "serverTimestamp" to FieldValue.serverTimestamp(),
                            "status" to "SUCCESS"
                        )
                    )

                    val notif = AppNotification(
                        id = "notif_ref_${System.currentTimeMillis()}",
                        title = "Referral Bonus Earned!",
                        message = "${newUser.username} just registered using your referral code! 50 bonus tokens have been credited to your account.",
                        type = "REFERRAL",
                        timestamp = System.currentTimeMillis()
                    )
                    notificationsRef.child(referrerUid).child(notif.id).setValue(notif)
                    firestore.collection("users").document(referrerUid)
                        .collection("notifications").document(notif.id).set(notif)
                } catch (e: Exception) {
                    Log.w(TAG, "Notice rewarding referrer: ${e.message}")
                }
            }
        }
    }

    suspend fun submitAccountDeletionRequest(reason: String, details: String) {
        val userItem = user.firstOrNull() ?: throw Exception("User not found locally")
        val reqId = UUID.randomUUID().toString()
        val deletionData = mapOf(
            "id" to reqId,
            "userId" to userItem.id,
            "username" to userItem.username,
            "phoneOrEmail" to userItem.phoneOrEmail,
            "reason" to reason,
            "details" to details,
            "requestedAt" to System.currentTimeMillis()
        )
        db.userDao().clearAll()
        db.transactionDao().clearAll()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                deletionRequestsRef.child(reqId).setValue(deletionData)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit deletion request", e)
            }
        }
    }

    suspend fun clearUserDatabase() {
        withContext(Dispatchers.IO) {
            db.userDao().clearAll()
            db.transactionDao().clearAll()
        }
    }

    /**
     * Writes comprehensive User data to Realtime Database at /users/$userId
     * with all key name variations so any Admin Panel schema will match perfectly!
     */
    private fun syncUserToRealtimeDb(user: User) {
        val userMap = hashMapOf<String, Any>(
            "id" to user.id,
            "username" to user.username,
            "name" to user.username,
            "email" to user.phoneOrEmail,
            "phoneOrEmail" to user.phoneOrEmail,
            "ign" to user.inGameName,
            "inGameName" to user.inGameName,
            "gameId" to user.freeFireId,
            "freeFireId" to user.freeFireId,
            "balance" to user.balance,
            "walletBalance" to user.balance,
            "wallet_balance" to user.balance,
            "tokens" to user.tokens,
            "tokenBalance" to user.tokens,
            "tokensBalance" to user.tokens,
            "activityPoints" to user.tokens,
            "totalTokensConverted" to user.totalTokensConverted,
            "total_tokens_converted" to user.totalTokensConverted,
            "tokensConverted" to user.totalTokensConverted,
            "matchesPlayed" to user.matchesPlayed,
            "totalWins" to user.totalWins,
            "totalKills" to user.totalKills,
            "wins" to user.totalWins,
            "kills" to user.totalKills,
            "isBanned" to user.isBanned,
            "banned" to user.isBanned,
            "banReason" to user.banReason,
            "banType" to user.banType,
            "bannedAt" to user.bannedAt,
            "banExpiresAt" to user.banExpiresAt,
            "isSuspended" to user.isSuspended,
            "suspended" to user.isSuspended,
            "suspendReason" to user.suspendReason,
            "suspensionExpiresAt" to user.suspensionExpiresAt,
            "role" to user.role,
            "avatarUrl" to user.avatarUrl,
            "avatarIdx" to user.avatarIdx,
            "bio" to user.bio,
            "referralCode" to user.referralCode,
            "referredBy" to user.referredBy,
            "referralCount" to user.referralCount,
            "referralEarnings" to user.referralEarnings,
            "loginStreak" to user.loginStreak,
            "lastLoginClaimDate" to user.lastLoginClaimDate,
            "createdAt" to if (user.dateOfJoining > 0) user.dateOfJoining else System.currentTimeMillis(),
            "lastLoginAt" to System.currentTimeMillis(),
            "fcmToken" to user.fcmToken,
            "founderTier" to user.founderTier,
            "registeredTierId" to user.founderTier,
            "isFounder" to user.isFounder,
            "reservedTokens" to user.reservedTokens
        )
        try {
            usersRef.child(user.id).updateChildren(userMap)
                .addOnFailureListener { e ->
                    Log.w(TAG, "Notice: Realtime Database user sync: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Notice: syncUserToRealtimeDb: ${e.message}")
        }

        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users").document(user.id).set(
                mapOf(
                    "id" to user.id,
                    "username" to user.username,
                    "name" to user.username,
                    "email" to user.phoneOrEmail,
                    "phoneOrEmail" to user.phoneOrEmail,
                    "ign" to user.inGameName,
                    "inGameName" to user.inGameName,
                    "gameId" to user.freeFireId,
                    "freeFireId" to user.freeFireId,
                    "balance" to user.balance,
                    "walletBalance" to user.balance,
                    "wallet_balance" to user.balance,
                    "tokens" to user.tokens,
                    "tokenBalance" to user.tokens,
                    "tokensBalance" to user.tokens,
                    "activityPoints" to user.tokens,
                    "totalTokensConverted" to user.totalTokensConverted,
                    "total_tokens_converted" to user.totalTokensConverted,
                    "tokensConverted" to user.totalTokensConverted,
                    "matchesPlayed" to user.matchesPlayed,
                    "totalWins" to user.totalWins,
                    "totalKills" to user.totalKills,
                    "avatarUrl" to user.avatarUrl,
                    "avatarIdx" to user.avatarIdx,
                    "bio" to user.bio,
                    "referralCode" to user.referralCode,
                    "referredBy" to user.referredBy,
                    "referralCount" to user.referralCount,
                    "referralEarnings" to user.referralEarnings,
                    "loginStreak" to user.loginStreak,
                    "lastLoginClaimDate" to user.lastLoginClaimDate,
                    "founderTier" to user.founderTier,
                    "isFounder" to user.isFounder,
                    "reservedTokens" to user.reservedTokens,
                    "isBanned" to user.isBanned,
                    "banned" to user.isBanned,
                    "banReason" to user.banReason,
                    "banType" to user.banType,
                    "isSuspended" to user.isSuspended,
                    "suspended" to user.isSuspended,
                    "suspendReason" to user.suspendReason,
                    "role" to user.role,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Notice: syncUserToFirestore: ${e.message}")
        }

        if (user.phoneOrEmail.equals("anantisback47@gmail.com", ignoreCase = true) || user.username.contains("admin", ignoreCase = true)) {
            try {
                rtdb.getReference("admins").child(user.id).setValue(
                    mapOf(
                        "role" to "super_admin",
                        "active" to true,
                        "email" to user.phoneOrEmail,
                        "grantedBy" to "system",
                        "grantedAt" to System.currentTimeMillis()
                    )
                )
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("admins").document(user.id).set(
                    mapOf(
                        "role" to "super_admin",
                        "active" to true,
                        "email" to user.phoneOrEmail,
                        "grantedBy" to "system",
                        "grantedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Notice admin registration: ${e.message}")
            }
        }
    }

    private fun DataSnapshot.getIntSafe(key: String, altKey: String? = null, defaultValue: Int = 0): Int {
        val child = this.child(key)
        if (child.exists() && child.value != null) {
            val v = child.value
            return when (v) {
                is Number -> v.toInt()
                is String -> v.toIntOrNull() ?: defaultValue
                is Boolean -> if (v) 1 else 0
                else -> defaultValue
            }
        }
        if (altKey != null) {
            val altChild = this.child(altKey)
            if (altChild.exists() && altChild.value != null) {
                val v = altChild.value
                return when (v) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: defaultValue
                    is Boolean -> if (v) 1 else 0
                    else -> defaultValue
                }
            }
        }
        return defaultValue
    }

    private fun DataSnapshot.getDoubleSafe(key: String, altKey: String? = null, defaultValue: Double = 0.0): Double {
        val child = this.child(key)
        if (child.exists() && child.value != null) {
            val v = child.value
            return when (v) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: defaultValue
                else -> defaultValue
            }
        }
        if (altKey != null) {
            val altChild = this.child(altKey)
            if (altChild.exists() && altChild.value != null) {
                val v = altChild.value
                return when (v) {
                    is Number -> v.toDouble()
                    is String -> v.toDoubleOrNull() ?: defaultValue
                    else -> defaultValue
                }
            }
        }
        return defaultValue
    }

    private fun DataSnapshot.getStringSafe(key: String, altKey: String? = null, defaultValue: String = ""): String {
        val child = this.child(key)
        if (child.exists() && child.value != null) {
            return child.value.toString()
        }
        if (altKey != null) {
            val altChild = this.child(altKey)
            if (altChild.exists() && altChild.value != null) {
                return altChild.value.toString()
            }
        }
        return defaultValue
    }

    private fun DataSnapshot.getLongSafe(key: String, altKey: String? = null, defaultValue: Long = 0L): Long {
        val child = this.child(key)
        if (child.exists() && child.value != null) {
            val v = child.value
            return when (v) {
                is Number -> v.toLong()
                is String -> v.toLongOrNull() ?: defaultValue
                else -> defaultValue
            }
        }
        if (altKey != null) {
            val altChild = this.child(altKey)
            if (altChild.exists() && altChild.value != null) {
                val v = altChild.value
                return when (v) {
                    is Number -> v.toLong()
                    is String -> v.toLongOrNull() ?: defaultValue
                    else -> defaultValue
                }
            }
        }
        return defaultValue
    }

    private fun parseUserFromSnapshot(snapshot: DataSnapshot, uid: String): User {
        val username = snapshot.getStringSafe("username", "name").ifEmpty { "Player" }
        val phoneOrEmail = snapshot.getStringSafe("phoneOrEmail", "email")
        val balance = snapshot.getDoubleSafe("balance", "walletBalance").let { if (it > 0.0) it else snapshot.getDoubleSafe("wallet_balance") }
        val tokens = snapshot.getIntSafe("tokens", "tokenBalance", 0)
            .let { if (it > 0) it else snapshot.getIntSafe("tokensBalance", "rewardTokens", 0) }
            .let { if (it > 0) it else snapshot.getIntSafe("reward_tokens", "activityPoints", 0) }
            .let { if (it > 0) it else snapshot.getIntSafe("coins", "points", 0) }
            .let { if (it > 0) it else snapshot.getIntSafe("token_balance", "tokens_balance", 0) }
        val totalTokensConverted = snapshot.getIntSafe("totalTokensConverted", "total_tokens_converted", 0).let { if (it > 0) it else snapshot.getIntSafe("tokensConverted", null, 0) }
        val inGameName = snapshot.getStringSafe("inGameName", "ign")
        val freeFireId = snapshot.getStringSafe("freeFireId", "gameId")
        val rawAvatarUrl = snapshot.getStringSafe("avatarUrl", "avatar_url")
            .ifEmpty { snapshot.getStringSafe("photoUrl", "photo_url") }
            .ifEmpty { snapshot.getStringSafe("picture") }
        val avatarUrl = if (rawAvatarUrl.startsWith("content://")) "" else rawAvatarUrl
        val avatarIdx = snapshot.getIntSafe("avatarIdx", "avatar_idx", 1)
        val matchesPlayed = snapshot.getIntSafe("matchesPlayed", "matches_played", 0)
        val totalWins = snapshot.getIntSafe("totalWins", "wins", 0)
        val totalKills = snapshot.getIntSafe("totalKills", "kills", 0)
        val dateOfJoining = snapshot.getLongSafe("createdAt", "dateOfJoining", System.currentTimeMillis())
        val rawRefCode = snapshot.getStringSafe("referralCode", "referral_code")
        val referralCode = if (rawRefCode.isNotBlank() && !rawRefCode.startsWith("VT")) {
            rawRefCode
        } else {
            val namePart = username.filter { it.isLetterOrDigit() }.take(4).uppercase().padEnd(4, 'X')
            val saltPart = uid.takeLast(4).uppercase().padEnd(4, '9')
            "VRX-$namePart-$saltPart"
        }
        val referralCount = snapshot.getIntSafe("referralCount", "referral_count", 0)
        val referralEarnings = snapshot.getDoubleSafe("referralEarnings", "referral_earnings", 0.0)
        val referredBy = snapshot.getStringSafe("referredBy", "referred_by")
        val loginStreak = snapshot.getIntSafe("loginStreak", "login_streak", 0)
        val lastLoginClaimDate = snapshot.getStringSafe("lastLoginClaimDate", "last_login_claim_date")
        val rawFounderTier = snapshot.getStringSafe("founderTier", "founder_tier", "registered_tier_id")
        val isAnantEmail = phoneOrEmail.equals("anantisback47@gmail.com", ignoreCase = true)
        val founderTier = if (rawFounderTier.isNotBlank()) rawFounderTier else if (isAnantEmail) "tier_1000" else ""
        val isFounder = snapshot.child("isFounder").getValue(Boolean::class.java) ?: (founderTier.isNotBlank() || isAnantEmail)
        val rawReserved = snapshot.getIntSafe("reservedTokens", "reserved_tokens", 0)
        val reservedTokens = if (rawReserved > 0) rawReserved else if (founderTier == "tier_1000" || isAnantEmail) 2000 else 0

        val isBanned = snapshot.child("isBanned").getValue(Boolean::class.java)
            ?: snapshot.child("banned").getValue(Boolean::class.java)
            ?: snapshot.child("is_banned").getValue(Boolean::class.java)
            ?: (snapshot.getStringSafe("status").equals("BANNED", ignoreCase = true))
        val banReason = snapshot.getStringSafe("banReason", "ban_reason").ifEmpty { snapshot.getStringSafe("reason") }
        val banType = snapshot.getStringSafe("banType", "ban_type").ifEmpty { "PERMANENT" }
        val bannedAt = snapshot.getLongSafe("bannedAt", "banned_at")
        val banExpiresAt = snapshot.getLongSafe("banExpiresAt", "ban_expires_at")

        val isSuspended = snapshot.child("isSuspended").getValue(Boolean::class.java)
            ?: snapshot.child("suspended").getValue(Boolean::class.java)
            ?: snapshot.child("is_suspended").getValue(Boolean::class.java)
            ?: (snapshot.getStringSafe("status").equals("SUSPENDED", ignoreCase = true))
        val suspendReason = snapshot.getStringSafe("suspendReason", "suspend_reason").ifEmpty { snapshot.getStringSafe("suspension_reason") }
        val suspensionExpiresAt = snapshot.getLongSafe("suspensionExpiresAt", "suspension_expires_at")
        val role = snapshot.getStringSafe("role", "adminRole").ifEmpty { if (isAnantEmail) "super_admin" else "user" }

        return User(
            id = uid,
            username = username,
            phoneOrEmail = phoneOrEmail,
            balance = balance,
            tokens = tokens,
            totalTokensConverted = totalTokensConverted,
            inGameName = inGameName,
            freeFireId = freeFireId,
            avatarUrl = avatarUrl,
            avatarIdx = avatarIdx,
            matchesPlayed = matchesPlayed,
            totalWins = totalWins,
            totalKills = totalKills,
            dateOfJoining = dateOfJoining,
            referralCode = referralCode,
            referredBy = referredBy,
            referralCount = referralCount,
            referralEarnings = referralEarnings,
            loginStreak = loginStreak,
            lastLoginClaimDate = lastLoginClaimDate,
            founderTier = founderTier,
            isFounder = isFounder,
            reservedTokens = reservedTokens,
            isBanned = isBanned,
            banReason = banReason,
            banType = banType,
            bannedAt = bannedAt,
            banExpiresAt = banExpiresAt,
            isSuspended = isSuspended,
            suspendReason = suspendReason,
            suspensionExpiresAt = suspensionExpiresAt,
            role = role
        )
    }

    /**
     * Registers and activates Founder Patron tier for user.
     */
    suspend fun registerFounderPass(tierId: String, tokensReward: Int, priceInr: Double, paymentRef: String = ""): Boolean {
        val userItem = user.firstOrNull() ?: getUserSync() ?: return false
        val updatedUser = userItem.copy(
            founderTier = tierId,
            isFounder = true,
            reservedTokens = tokensReward
        )
        db.userDao().update(updatedUser)
        syncUserToRealtimeDb(updatedUser)

        val txId = "TX-FOUNDER-${System.currentTimeMillis()}-${(1000..9999).random()}"
        val tx = Transaction(
            id = txId,
            userId = userItem.id,
            type = "FOUNDER_PASS",
            amount = priceInr,
            detail = "Founder Supporter Pass ($tierId) - $tokensReward Reserved Tokens",
            isPositive = false,
            timestamp = System.currentTimeMillis(),
            status = "SUCCESS"
        )
        try {
            db.transactionDao().insert(tx)
            transactionsRef.child(txId).setValue(tx)
            val notif = AppNotification(
                id = "notif_founder_${System.currentTimeMillis()}",
                title = "Founder Pass Activated!",
                message = "Congratulations! Your $tierId Founder Pass is officially activated with $tokensReward reserved launch tokens and lifetime perks.",
                type = "SYSTEM",
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            db.appNotificationDao().insert(notif)
            notificationsRef.child(notif.id).setValue(notif)
        } catch (e: Exception) {
            Log.w(TAG, "Notice: registerFounderPass sync: ${e.message}")
        }
        return true
    }

    /**
     * Retrieves participants for a tournament.
     */
    fun getParticipants(tournamentId: String): Flow<List<com.example.data.model.TournamentParticipant>> {
        return db.tournamentParticipantDao().getParticipantsByTournament(tournamentId)
    }

    /**
     * Retrieves current user's registration details for a tournament.
     */
    fun getMyParticipant(tournamentId: String, userId: String): Flow<com.example.data.model.TournamentParticipant?> {
        return db.tournamentParticipantDao().getParticipant(tournamentId, userId)
    }

    /**
     * Joins a tournament with a selected slot number and verified in-game details.
     */
    suspend fun joinTournamentWithSlot(
        tournamentId: String,
        slotNumber: Int,
        inGameName: String,
        characterId: String,
        teamName: String = ""
    ): JoinResult {
        val userItem = user.firstOrNull() ?: return JoinResult.Failure("User not found")
        val match = tournaments.firstOrNull()?.find { it.id == tournamentId }
            ?: return JoinResult.Failure("Tournament not found")

        if (match.joined) return JoinResult.Failure("Already joined this tournament")
        if (match.isFull) return JoinResult.Failure("Tournament slots are full")
        if (userItem.balance < match.entryFee) return JoinResult.Failure("Insufficient balance. Please top-up VT Tokens!")

        val effectiveCharId = characterId.ifBlank { userItem.freeFireId }.trim()
        if (effectiveCharId.isBlank()) {
            logRegistrationAudit(
                userId = userItem.id,
                gameId = effectiveCharId,
                tournamentId = tournamentId,
                status = "REJECTED_EMPTY",
                details = "Registration rejected: Empty Game ID provided."
            )
            return JoinResult.Failure("Invalid ID Format: Please enter your Game ID / Character UID.")
        }
        if (!effectiveCharId.matches(Regex("^[0-9]{8,12}$")) || effectiveCharId.toSet().size <= 1) {
            logRegistrationAudit(
                userId = userItem.id,
                gameId = effectiveCharId,
                tournamentId = tournamentId,
                status = "REJECTED_INVALID_FORMAT",
                details = "Registration rejected: Game ID '$effectiveCharId' does not meet the 8-12 numeric digits constraint."
            )
            return JoinResult.Failure("Invalid ID Format: Game ID must be an authentic 8-12 digit numeric player UID.")
        }

        // Generate unique ticket code
        val ticketCode = "TKT-${match.game.take(2).uppercase()}-${slotNumber}-${(1000..9999).random()}"

        val participant = com.example.data.model.TournamentParticipant(
            id = UUID.randomUUID().toString(),
            tournamentId = tournamentId,
            userId = userItem.id,
            username = userItem.username,
            inGameName = inGameName.ifBlank { userItem.inGameName.ifBlank { userItem.username } },
            characterId = effectiveCharId,
            slotNumber = slotNumber,
            teamName = teamName,
            registeredAt = System.currentTimeMillis(),
            ticketCode = ticketCode
        )

        val updatedUser = userItem.copy(
            balance = userItem.balance - match.entryFee,
            inGameName = if (inGameName.isNotBlank()) inGameName else userItem.inGameName,
            freeFireId = effectiveCharId
        )
        val updatedMatch = match.copy(joined = true, filledSlots = match.filledSlots + 1)
        val newTx = Transaction(
            userId = userItem.id,
            type = "ENTRY_FEE",
            amount = match.entryFee,
            detail = "Joined ${match.game}: ${match.title} (Slot #$slotNumber)",
            isPositive = false,
            timestamp = System.currentTimeMillis()
        )

        val registrationNotif = AppNotification(
            id = "notif_reg_${System.currentTimeMillis()}",
            title = "Registration Confirmed: ${match.title}",
            message = "You are booked in Slot #$slotNumber with Ticket $ticketCode. Room credentials will appear before match start.",
            type = "TOURNAMENT_REMINDER",
            timestamp = System.currentTimeMillis(),
            tournamentId = tournamentId,
            tournamentTitle = match.title
        )

        try {
            db.userDao().update(updatedUser)
            db.tournamentDao().update(updatedMatch)
            db.transactionDao().insert(newTx)
            db.tournamentParticipantDao().insert(participant)
            db.appNotificationDao().insert(registrationNotif)
            updateMissionProgress("m_tournament_contender", 1)

            // Log successful registration attempt to audit_logs
            logRegistrationAudit(
                userId = userItem.id,
                gameId = effectiveCharId,
                tournamentId = tournamentId,
                status = "SUCCESS",
                details = "Registered in Slot #$slotNumber for '${match.title}' (Ticket: $ticketCode)"
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    syncUserToRealtimeDb(updatedUser)
                    transactionsRef.child(newTx.id).setValue(newTx)

                    val participantData = mapOf(
                        "userId" to userItem.id,
                        "userUid" to userItem.id,
                        "username" to userItem.username,
                        "ign" to participant.inGameName,
                        "inGameName" to participant.inGameName,
                        "gameId" to effectiveCharId,
                        "characterId" to effectiveCharId,
                        "freeFireId" to effectiveCharId,
                        "slotNumber" to slotNumber,
                        "teamName" to teamName,
                        "ticketCode" to ticketCode,
                        "tournamentId" to tournamentId,
                        "tournamentTitle" to match.title,
                        "game" to match.game,
                        "registeredAt" to System.currentTimeMillis(),
                        "paymentTxId" to newTx.id,
                        "joinedAt" to System.currentTimeMillis(),
                        "timestamp" to System.currentTimeMillis()
                    )
                    registrationsRef.child(tournamentId).child(userItem.id).setValue(participantData)
                    tournamentsRef.child(tournamentId).child("participants").child(userItem.id).setValue(participantData)
                    tournamentsRef.child(tournamentId).child("slots").child(slotNumber.toString()).setValue(participantData)
                    tournamentsRef.child(tournamentId).child("filledSlots").setValue(updatedMatch.filledSlots)
                    tournamentRegistrationsRef.child("${tournamentId}_${userItem.id}").setValue(participantData)

                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("tournaments").document(tournamentId)
                        .collection("participants").document(userItem.id).set(participantData)
                    firestore.collection("tournament_registrations")
                        .document("${tournamentId}_${userItem.id}").set(participantData)
                    firestore.collection("transactions").document(newTx.id).set(newTx)
                } catch (e: Exception) {
                    Log.e(TAG, "Syncing join tournament with slot to backend failed: ${e.message}")
                }
            }
            return JoinResult.Success("Confirmed! Registered in Slot #$slotNumber (Ticket: $ticketCode)")
        } catch (e: Throwable) {
            Log.e(TAG, "Registration failed", e)
            logRegistrationAudit(
                userId = userItem.id,
                gameId = effectiveCharId,
                tournamentId = tournamentId,
                status = "FAILED",
                details = "Registration error: ${e.message}"
            )
            return JoinResult.Failure("Failed to book slot. Please try again.")
        }
    }

    /**
     * Helper function to log every registration attempt to the 'audit_logs' path,
     * capturing the timestamp, user ID, and the 'gameId' provided, so admins can track
     * and investigate any repeated failed or suspicious registration patterns.
     */
    fun logRegistrationAudit(
        userId: String,
        gameId: String,
        tournamentId: String,
        status: String,
        details: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timestamp = System.currentTimeMillis()
                val logId = "reg_audit_${System.currentTimeMillis()}_${(1000..9999).random()}"
                val auditEntry = mapOf(
                    "id" to logId,
                    "action" to "TOURNAMENT_REGISTRATION_ATTEMPT",
                    "performedBy" to userId,
                    "userId" to userId,
                    "targetUid" to userId,
                    "gameId" to gameId,
                    "tournamentId" to tournamentId,
                    "status" to status,
                    "details" to details,
                    "timestamp" to timestamp
                )
                auditLogsRef.child(logId).setValue(auditEntry)
                Log.d(TAG, "Audit log recorded: User $userId, GameID: $gameId, Status: $status")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write audit log: ${e.message}")
            }
        }
    }

    /**
     * Sends custom push notification to in-app notification center.
     */
    suspend fun sendAppNotification(notification: AppNotification) {
        try {
            db.appNotificationDao().insert(notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert notification", e)
        }
    }

    private fun startSystemConfigSync() {
        // 1. RTDB /app_config
        try {
            rtdb.getReference("app_config").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        updateSystemConfigFromSnapshot(snapshot)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "app_config listener error: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up app_config listener: ${e.message}")
        }

        // 2. RTDB /system_config
        try {
            rtdb.getReference("system_config").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        updateSystemConfigFromSnapshot(snapshot)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up system_config listener: ${e.message}")
        }

        // 3. RTDB /maintenance
        try {
            rtdb.getReference("maintenance").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val isMaint = when (val v = snapshot.value) {
                            is Boolean -> v
                            is String -> v.equals("true", true) || v.equals("on", true) || v.equals("1", true)
                            is Number -> v.toInt() == 1
                            else -> snapshot.child("enabled").getValue(Boolean::class.java)
                                ?: snapshot.child("is_maintenance").getValue(Boolean::class.java)
                                ?: snapshot.child("active").getValue(Boolean::class.java)
                                ?: snapshot.child("isMaintenance").getValue(Boolean::class.java) ?: false
                        }
                        val msg = snapshot.child("message").value?.toString()
                            ?: snapshot.child("reason").value?.toString()
                            ?: snapshot.child("maintenance_message").value?.toString() ?: ""
                        val eta = snapshot.child("estimated_end").value?.toString()
                            ?: snapshot.child("eta").value?.toString()
                            ?: snapshot.child("maintenance_until").value?.toString() ?: ""
                        val title = snapshot.child("title").value?.toString()
                            ?: snapshot.child("maintenance_title").value?.toString() ?: ""

                        _systemConfig.value = _systemConfig.value.copy(
                            isMaintenance = isMaint,
                            maintenanceTitle = if (title.isNotBlank()) title else _systemConfig.value.maintenanceTitle,
                            maintenanceMessage = if (msg.isNotBlank()) msg else _systemConfig.value.maintenanceMessage,
                            maintenanceUntil = if (eta.isNotBlank()) eta else _systemConfig.value.maintenanceUntil
                        )
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up maintenance listener: ${e.message}")
        }

        // 4. Firestore app_config/global
        try {
            FirebaseFirestore.getInstance().collection("app_config").document("global")
                .addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        val isMaint = doc.getBoolean("isMaintenance") ?: doc.getBoolean("is_maintenance") ?: doc.getBoolean("maintenance") ?: false
                        val title = doc.getString("maintenanceTitle") ?: doc.getString("maintenance_title") ?: ""
                        val msg = doc.getString("maintenanceMessage") ?: doc.getString("maintenance_message") ?: doc.getString("message") ?: ""
                        val eta = doc.getString("maintenanceUntil") ?: doc.getString("maintenance_until") ?: doc.getString("eta") ?: ""
                        val isForce = doc.getBoolean("isForceUpdate") ?: doc.getBoolean("force_update") ?: false
                        val minVer = doc.getString("minRequiredVersion") ?: doc.getString("min_version") ?: ""
                        val updUrl = doc.getString("updateUrl") ?: doc.getString("update_url") ?: ""
                        val changelog = doc.getString("changelog") ?: ""

                        _systemConfig.value = _systemConfig.value.copy(
                            isMaintenance = isMaint,
                            maintenanceTitle = if (title.isNotBlank()) title else _systemConfig.value.maintenanceTitle,
                            maintenanceMessage = if (msg.isNotBlank()) msg else _systemConfig.value.maintenanceMessage,
                            maintenanceUntil = if (eta.isNotBlank()) eta else _systemConfig.value.maintenanceUntil,
                            isForceUpdate = isForce,
                            minRequiredVersion = if (minVer.isNotBlank()) minVer else _systemConfig.value.minRequiredVersion,
                            updateUrl = if (updUrl.isNotBlank()) updUrl else _systemConfig.value.updateUrl,
                            changelog = if (changelog.isNotBlank()) changelog else _systemConfig.value.changelog
                        )
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting up Firestore app_config listener: ${e.message}")
        }
    }

    private fun updateSystemConfigFromSnapshot(snapshot: DataSnapshot) {
        val isMaint = snapshot.child("isMaintenance").getValue(Boolean::class.java)
            ?: snapshot.child("is_maintenance").getValue(Boolean::class.java)
            ?: snapshot.child("maintenance").getValue(Boolean::class.java)
            ?: snapshot.child("maintenance_mode").getValue(Boolean::class.java)
            ?: snapshot.child("maintenanceMode").getValue(Boolean::class.java) ?: false
        val title = snapshot.child("maintenanceTitle").value?.toString()
            ?: snapshot.child("maintenance_title").value?.toString()
            ?: snapshot.child("title").value?.toString() ?: ""
        val message = snapshot.child("maintenanceMessage").value?.toString()
            ?: snapshot.child("maintenance_message").value?.toString()
            ?: snapshot.child("message").value?.toString()
            ?: snapshot.child("reason").value?.toString() ?: ""
        val until = snapshot.child("maintenanceUntil").value?.toString()
            ?: snapshot.child("maintenance_until").value?.toString()
            ?: snapshot.child("estimated_end").value?.toString()
            ?: snapshot.child("eta").value?.toString() ?: ""
        val isForceUpdate = snapshot.child("isForceUpdate").getValue(Boolean::class.java)
            ?: snapshot.child("is_force_update").getValue(Boolean::class.java)
            ?: snapshot.child("force_update").getValue(Boolean::class.java)
            ?: snapshot.child("forceUpdate").getValue(Boolean::class.java) ?: false
        val minVersion = snapshot.child("minRequiredVersion").value?.toString()
            ?: snapshot.child("min_required_version").value?.toString()
            ?: snapshot.child("min_version").value?.toString() ?: ""
        val updateUrl = snapshot.child("updateUrl").value?.toString()
            ?: snapshot.child("update_url").value?.toString() ?: ""
        val changelog = snapshot.child("changelog").value?.toString() ?: ""

        _systemConfig.value = _systemConfig.value.copy(
            isMaintenance = isMaint,
            maintenanceTitle = if (title.isNotBlank()) title else _systemConfig.value.maintenanceTitle,
            maintenanceMessage = if (message.isNotBlank()) message else _systemConfig.value.maintenanceMessage,
            maintenanceUntil = if (until.isNotBlank()) until else _systemConfig.value.maintenanceUntil,
            isForceUpdate = isForceUpdate,
            minRequiredVersion = if (minVersion.isNotBlank()) minVersion else _systemConfig.value.minRequiredVersion,
            updateUrl = if (updateUrl.isNotBlank()) updateUrl else _systemConfig.value.updateUrl,
            changelog = if (changelog.isNotBlank()) changelog else _systemConfig.value.changelog
        )
    }

    suspend fun updateMaintenanceMode(enabled: Boolean, title: String = "", message: String = "", eta: String = "") {
        try {
            val map = mutableMapOf<String, Any>(
                "is_maintenance" to enabled,
                "isMaintenance" to enabled,
                "maintenance" to enabled,
                "maintenance_mode" to enabled
            )
            if (title.isNotBlank()) {
                map["maintenance_title"] = title
                map["title"] = title
            }
            if (message.isNotBlank()) {
                map["maintenance_message"] = message
                map["message"] = message
            }
            if (eta.isNotBlank()) {
                map["maintenance_until"] = eta
                map["estimated_end"] = eta
                map["eta"] = eta
            }

            rtdb.getReference("app_config").updateChildren(map)
            rtdb.getReference("maintenance").setValue(map)
            FirebaseFirestore.getInstance().collection("app_config").document("global").set(map, com.google.firebase.firestore.SetOptions.merge())

            _systemConfig.value = _systemConfig.value.copy(
                isMaintenance = enabled,
                maintenanceTitle = if (title.isNotBlank()) title else _systemConfig.value.maintenanceTitle,
                maintenanceMessage = if (message.isNotBlank()) message else _systemConfig.value.maintenanceMessage,
                maintenanceUntil = if (eta.isNotBlank()) eta else _systemConfig.value.maintenanceUntil
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating maintenance mode: ${e.message}")
        }
    }

    suspend fun toggleUserBan(userId: String, isBanned: Boolean, reason: String = "", banType: String = "PERMANENT") {
        try {
            val banMap = mapOf(
                "isBanned" to isBanned,
                "banned" to isBanned,
                "status" to if (isBanned) "BANNED" else "ACTIVE",
                "banReason" to reason,
                "ban_reason" to reason,
                "banType" to banType,
                "bannedAt" to if (isBanned) System.currentTimeMillis() else 0L
            )
            usersRef.child(userId).updateChildren(banMap)
            if (isBanned) {
                rtdb.getReference("banned_users").child(userId).setValue(banMap)
            } else {
                rtdb.getReference("banned_users").child(userId).removeValue()
            }
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .set(banMap, com.google.firebase.firestore.SetOptions.merge())

            val local = db.userDao().getUserSync()
            if (local != null && local.id == userId) {
                val updated = local.copy(
                    isBanned = isBanned,
                    banReason = reason,
                    banType = banType,
                    bannedAt = if (isBanned) System.currentTimeMillis() else 0L
                )
                db.userDao().update(updated)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling user ban: ${e.message}")
        }
    }

    suspend fun toggleUserSuspension(userId: String, isSuspended: Boolean, reason: String = "") {
        try {
            val suspendMap = mapOf(
                "isSuspended" to isSuspended,
                "suspended" to isSuspended,
                "status" to if (isSuspended) "SUSPENDED" else "ACTIVE",
                "suspendReason" to reason,
                "suspend_reason" to reason
            )
            usersRef.child(userId).updateChildren(suspendMap)
            if (isSuspended) {
                rtdb.getReference("suspended_users").child(userId).setValue(suspendMap)
            } else {
                rtdb.getReference("suspended_users").child(userId).removeValue()
            }
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .set(suspendMap, com.google.firebase.firestore.SetOptions.merge())

            val local = db.userDao().getUserSync()
            if (local != null && local.id == userId) {
                val updated = local.copy(
                    isSuspended = isSuspended,
                    suspendReason = reason
                )
                db.userDao().update(updated)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling user suspension: ${e.message}")
        }
    }

    suspend fun toggleForceUpdate(enabled: Boolean, minVersion: String = "2.0.0", updateUrl: String = "") {
        try {
            val map = mutableMapOf<String, Any>(
                "is_force_update" to enabled,
                "isForceUpdate" to enabled,
                "force_update" to enabled,
                "min_required_version" to minVersion,
                "min_version" to minVersion
            )
            if (updateUrl.isNotBlank()) {
                map["update_url"] = updateUrl
            }
            rtdb.getReference("app_config").updateChildren(map)
            FirebaseFirestore.getInstance().collection("app_config").document("global")
                .set(map, com.google.firebase.firestore.SetOptions.merge())

            _systemConfig.value = _systemConfig.value.copy(
                isForceUpdate = enabled,
                minRequiredVersion = minVersion
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling force update: ${e.message}")
        }
    }
}

sealed interface JoinResult {
    data class Success(val message: String) : JoinResult
    data class Failure(val message: String) : JoinResult
}

sealed interface WithdrawResult {
    data class Success(val message: String) : WithdrawResult
    data class Failure(val message: String) : WithdrawResult
}

sealed interface ConvertResult {
    data class Success(val message: String) : ConvertResult
    data class Failure(val message: String) : ConvertResult
}

sealed interface ClaimMissionResult {
    data class Success(val message: String) : ClaimMissionResult
    data class Failure(val message: String) : ClaimMissionResult
}

data class WalletBreakdown(
    val total: Double = 0.0,
    val deposited: Double = 0.0,
    val winnings: Double = 0.0
)

sealed interface ReferralResult {
    data class Success(val message: String) : ReferralResult
    data class Failure(val message: String) : ReferralResult
}
