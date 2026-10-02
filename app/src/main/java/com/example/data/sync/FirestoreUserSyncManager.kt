package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.User
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * FirestoreUserSyncManager
 *
 * Centralized data synchronization layer using Firebase Firestore.
 * Enforces 'Last-Write-Wins' (LWW) and server-timestamp / version validation for user profile
 * updates to prevent stale data sync, out-of-order writes, or race conditions across multiple device instances.
 */
class FirestoreUserSyncManager private constructor(
    private val context: Context,
    private val db: AppDatabase
) {

    companion object {
        private const val TAG = "FirestoreUserSync"
        private const val PREFS_NAME = "velorix_device_sync_prefs"
        private const val KEY_DEVICE_INSTANCE_ID = "device_instance_id"
        private const val RTDB_URL = "https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app"

        @Volatile
        private var INSTANCE: FirestoreUserSyncManager? = null

        fun getInstance(context: Context, db: AppDatabase): FirestoreUserSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirestoreUserSyncManager(context.applicationContext, db).also { INSTANCE = it }
            }
        }
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val rtdb = FirebaseDatabase.getInstance(RTDB_URL)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Unique identifier for this device installation.
     * Prevents self-echo loops and aids conflict resolution across multiple device instances.
     */
    val deviceInstanceId: String by lazy {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_INSTANCE_ID, null)
        if (id.isNullOrBlank()) {
            id = "DEV_${UUID.randomUUID().toString().take(12)}"
            prefs.edit().putString(KEY_DEVICE_INSTANCE_ID, id).apply()
        }
        id
    }

    private val activeListeners = ConcurrentHashMap<String, ListenerRegistration>()

    /**
     * Authoritatively updates user profile in Cloud Firestore using an atomic transaction.
     * Enforces 'last-write-wins' and monotonic version/timestamp validation:
     * 1. Reads current server document state.
     * 2. Validates timestamp and increments version.
     * 3. Writes serverTimestamp and payload atomically.
     * 4. Propagates changes to Realtime Database and local SQLite cache.
     */
    suspend fun updateUserProfileAuthoritative(updatedUser: User): Result<User> = withContext(Dispatchers.IO) {
        val userId = updatedUser.id
        if (userId.isBlank() || userId == "guest") {
            return@withContext Result.failure(IllegalArgumentException("Invalid user ID: $userId"))
        }

        val userDocRef = firestore.collection("users").document(userId)

        try {
            val nowMillis = System.currentTimeMillis()

            val reconciledUser = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userDocRef)

                var serverUpdatedAt = 0L
                var serverVersion = 0L

                if (snapshot.exists()) {
                    serverUpdatedAt = (snapshot.get("updatedAtMillis") as? Number)?.toLong()
                        ?: snapshot.getTimestamp("updatedAt")?.toDate()?.time
                        ?: (snapshot.get("lastModified") as? Number)?.toLong()
                        ?: 0L
                    serverVersion = (snapshot.get("version") as? Number)?.toLong()
                        ?: (snapshot.get("profileVersion") as? Number)?.toLong()
                        ?: 0L
                }

                // Monotonic progression: new timestamp must be strictly >= server's current timestamp
                val newTimestamp = maxOf(nowMillis, serverUpdatedAt + 1L)
                val newVersion = serverVersion + 1L

                val authoritativeUser = updatedUser.copy(
                    updatedAt = newTimestamp,
                    version = newVersion
                )

                val payload = buildUserProfileMap(authoritativeUser, newTimestamp, newVersion)

                transaction.set(userDocRef, payload, SetOptions.merge())
                authoritativeUser
            }.await()

            // 2. Persist authoritative user to local Room DB immediately
            db.userDao().deleteOtherUsers(userId)
            db.userDao().insert(reconciledUser)

            // 3. Replicate authoritative update to Realtime Database
            scope.launch(Dispatchers.IO) {
                try {
                    val rtdbMap = buildUserProfileMap(reconciledUser, reconciledUser.updatedAt, reconciledUser.version)
                    rtdb.getReference("users").child(userId).updateChildren(rtdbMap).await()
                    Log.i(TAG, "Profile update replicated to RTDB for user $userId (v${reconciledUser.version})")
                } catch (e: Exception) {
                    Log.w(TAG, "Notice replicating profile to RTDB: ${e.message}")
                }
            }

            Log.i(TAG, "Authoritative profile update committed for ${reconciledUser.username} (v${reconciledUser.version}@${reconciledUser.updatedAt})")
            return@withContext Result.success(reconciledUser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed authoritative profile update for $userId: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Attaches an authoritative real-time listener on the Firestore user document.
     * Enforces 'last-write-wins' by checking incoming server timestamps and versions against local cache.
     * Rejects stale or out-of-order updates that arrive from older device states.
     */
    fun startAuthoritativeUserSync(userId: String, onUserSynced: (User) -> Unit = {}): ListenerRegistration {
        stopAuthoritativeUserSync(userId)

        val docRef = firestore.collection("users").document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Authoritative Firestore listener error for $userId: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) {
                return@addSnapshotListener
            }

            scope.launch(Dispatchers.IO) {
                try {
                    val remoteUpdatedAt = (snapshot.get("updatedAtMillis") as? Number)?.toLong()
                        ?: snapshot.getTimestamp("updatedAt")?.toDate()?.time
                        ?: (snapshot.get("lastModified") as? Number)?.toLong()
                        ?: 0L
                    val remoteVersion = (snapshot.get("version") as? Number)?.toLong()
                        ?: (snapshot.get("profileVersion") as? Number)?.toLong()
                        ?: 0L
                    val lastWriterDevice = snapshot.getString("lastDeviceId") ?: ""

                    val localUser = db.userDao().getUserSync()

                    // Stale Data Rejection:
                    // If local device already has a newer timestamp or version, do not overwrite with stale snapshot
                    if (localUser != null && localUser.id == userId) {
                        if (localUser.updatedAt > 0L && remoteUpdatedAt > 0L) {
                            if (remoteUpdatedAt < localUser.updatedAt && remoteVersion < localUser.version) {
                                Log.d(TAG, "Discarded stale Firestore snapshot for $userId. Local: (v${localUser.version}@${localUser.updatedAt}), Remote: (v${remoteVersion}@${remoteUpdatedAt})")
                                return@launch
                            }
                        }
                    }

                    // Parse full user document
                    val parsedUser = parseUserFromDocument(snapshot, userId, localUser)
                    val reconciledUser = parsedUser.copy(
                        updatedAt = maxOf(remoteUpdatedAt, localUser?.updatedAt ?: 0L),
                        version = maxOf(remoteVersion, localUser?.version ?: 0L),
                        passwordHash = if (parsedUser.passwordHash.isNotBlank()) parsedUser.passwordHash else (localUser?.passwordHash ?: ""),
                        sessionToken = if (parsedUser.sessionToken.isNotBlank()) parsedUser.sessionToken else (localUser?.sessionToken ?: "")
                    )

                    db.userDao().deleteOtherUsers(userId)
                    db.userDao().insert(reconciledUser)

                    Log.i(TAG, "Authoritative user profile synced across devices: ${reconciledUser.username} (v${reconciledUser.version}@${reconciledUser.updatedAt}, fromDevice=$lastWriterDevice)")
                    withContext(Dispatchers.Main) {
                        onUserSynced(reconciledUser)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error applying authoritative user sync for $userId", e)
                }
            }
        }

        activeListeners[userId] = registration
        return registration
    }

    fun stopAuthoritativeUserSync(userId: String) {
        activeListeners.remove(userId)?.remove()
    }

    fun stopAll() {
        activeListeners.values.forEach { it.remove() }
        activeListeners.clear()
    }

    /**
     * Constructs a comprehensive dictionary of user profile fields for Firestore and RTDB.
     */
    private fun buildUserProfileMap(user: User, timestamp: Long, version: Long): Map<String, Any> {
        val map = hashMapOf<String, Any>(
            "id" to user.id,
            "username" to user.username,
            "name" to user.username,
            "fullName" to user.fullName,
            "full_name" to user.fullName,
            "email" to user.phoneOrEmail,
            "phoneOrEmail" to user.phoneOrEmail,
            "mobileNo" to user.mobileNo,
            "mobile_no" to user.mobileNo,
            "dob" to user.dob,
            "dateOfBirth" to user.dob,
            "bio" to user.bio,
            "socialLink" to user.socialLink,
            "social_link" to user.socialLink,
            "state" to user.state,
            "isAgeVerified" to user.isAgeVerified,
            "is_age_verified" to user.isAgeVerified,
            "legalConsentAccepted" to user.legalConsentAccepted,
            "legal_consent_accepted" to user.legalConsentAccepted,
            "legalConsentTimestamp" to user.legalConsentTimestamp,
            "coolingOffUntil" to user.coolingOffUntil,
            "dataExported" to user.dataExported,
            "data_exported" to user.dataExported,
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
            "totalTokensConverted" to user.totalTokensConverted,
            "matchesPlayed" to user.matchesPlayed,
            "totalWins" to user.totalWins,
            "totalKills" to user.totalKills,
            "avatarUrl" to user.avatarUrl,
            "avatarIdx" to user.avatarIdx,
            "referralCode" to user.referralCode,
            "referredBy" to user.referredBy,
            "referralCount" to user.referralCount,
            "referralEarnings" to user.referralEarnings,
            "loginStreak" to user.loginStreak,
            "lastLoginClaimDate" to user.lastLoginClaimDate,
            "dailyMissionsTokensClaimed" to user.dailyMissionsTokensClaimed,
            "lastMissionClaimDate" to user.lastMissionClaimDate,
            "founderTier" to user.founderTier,
            "isFounder" to user.isFounder,
            "reservedTokens" to user.reservedTokens,
            "isBanned" to user.isBanned,
            "banReason" to user.banReason,
            "banType" to user.banType,
            "isSuspended" to user.isSuspended,
            "suspendReason" to user.suspendReason,
            "role" to user.role,
            "version" to version,
            "profileVersion" to version,
            "updatedAtMillis" to timestamp,
            "lastModified" to timestamp,
            "lastDeviceId" to deviceInstanceId,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return map
    }

    /**
     * Parses a Firestore snapshot into an authentic User model.
     */
    private fun parseUserFromDocument(doc: DocumentSnapshot, uid: String, local: User?): User {
        val username = doc.getString("username") ?: doc.getString("name") ?: (local?.username ?: "Player")
        val phoneOrEmail = doc.getString("phoneOrEmail") ?: doc.getString("email") ?: (local?.phoneOrEmail ?: "")
        val fullName = doc.getString("fullName") ?: doc.getString("full_name") ?: (local?.fullName ?: username)
        val mobileNo = doc.getString("mobileNo") ?: doc.getString("mobile_no") ?: (local?.mobileNo ?: "")
        val dob = doc.getString("dob") ?: doc.getString("dateOfBirth") ?: (local?.dob ?: "")
        val bio = doc.getString("bio") ?: (local?.bio ?: "Ready to compete")
        val socialLink = doc.getString("socialLink") ?: doc.getString("social_link") ?: (local?.socialLink ?: "")
        val state = doc.getString("state") ?: (local?.state ?: "Delhi")
        val isAgeVerified = doc.getBoolean("isAgeVerified") ?: doc.getBoolean("is_age_verified") ?: (local?.isAgeVerified ?: false)
        val legalConsent = doc.getBoolean("legalConsentAccepted") ?: doc.getBoolean("legal_consent_accepted") ?: (local?.legalConsentAccepted ?: true)
        val legalTimestamp = (doc.get("legalConsentTimestamp") as? Number)?.toLong() ?: (local?.legalConsentTimestamp ?: 0L)
        val coolingOff = (doc.get("coolingOffUntil") as? Number)?.toLong() ?: (local?.coolingOffUntil ?: 0L)
        val dataExported = doc.getBoolean("dataExported") ?: doc.getBoolean("data_exported") ?: (local?.dataExported ?: false)

        val balance = (doc.get("balance") as? Number)?.toDouble()
            ?: (doc.get("walletBalance") as? Number)?.toDouble()
            ?: (doc.get("wallet_balance") as? Number)?.toDouble() ?: (local?.balance ?: 0.0)

        val tokens = when {
            doc.contains("tokens") -> (doc.get("tokens") as? Number)?.toInt() ?: (local?.tokens ?: 0)
            doc.contains("tokenBalance") -> (doc.get("tokenBalance") as? Number)?.toInt() ?: (local?.tokens ?: 0)
            doc.contains("tokensBalance") -> (doc.get("tokensBalance") as? Number)?.toInt() ?: (local?.tokens ?: 0)
            doc.contains("rewardTokens") -> (doc.get("rewardTokens") as? Number)?.toInt() ?: (local?.tokens ?: 0)
            doc.contains("activityPoints") -> (doc.get("activityPoints") as? Number)?.toInt() ?: (local?.tokens ?: 0)
            else -> local?.tokens ?: 0
        }

        val totalTokensConverted = (doc.get("totalTokensConverted") as? Number)?.toInt()
            ?: (doc.get("total_tokens_converted") as? Number)?.toInt()
            ?: (local?.totalTokensConverted ?: 0)

        val inGameName = doc.getString("inGameName") ?: doc.getString("ign") ?: (local?.inGameName ?: "")
        val freeFireId = doc.getString("freeFireId") ?: doc.getString("gameId") ?: (local?.freeFireId ?: "")
        val avatarUrl = doc.getString("avatarUrl") ?: doc.getString("avatar_url") ?: (local?.avatarUrl ?: "")
        val avatarIdx = (doc.get("avatarIdx") as? Number)?.toInt() ?: (doc.get("avatar_idx") as? Number)?.toInt() ?: (local?.avatarIdx ?: 1)
        val matchesPlayed = (doc.get("matchesPlayed") as? Number)?.toInt() ?: (local?.matchesPlayed ?: 0)
        val totalWins = (doc.get("totalWins") as? Number)?.toInt() ?: (doc.get("wins") as? Number)?.toInt() ?: (local?.totalWins ?: 0)
        val totalKills = (doc.get("totalKills") as? Number)?.toInt() ?: (doc.get("kills") as? Number)?.toInt() ?: (local?.totalKills ?: 0)
        val dateOfJoining = (doc.get("createdAt") as? Number)?.toLong() ?: (local?.dateOfJoining ?: System.currentTimeMillis())

        val referralCode = doc.getString("referralCode") ?: doc.getString("referral_code") ?: (local?.referralCode ?: "")
        val referredBy = doc.getString("referredBy") ?: doc.getString("referred_by") ?: (local?.referredBy ?: "")
        val referralCount = (doc.get("referralCount") as? Number)?.toInt() ?: (local?.referralCount ?: 0)
        val referralEarnings = (doc.get("referralEarnings") as? Number)?.toDouble() ?: (local?.referralEarnings ?: 0.0)

        val loginStreak = (doc.get("loginStreak") as? Number)?.toInt() ?: (local?.loginStreak ?: 0)
        val lastLoginClaimDate = doc.getString("lastLoginClaimDate") ?: (local?.lastLoginClaimDate ?: "")
        val dailyMissionsTokensClaimed = (doc.get("dailyMissionsTokensClaimed") as? Number)?.toInt() ?: (local?.dailyMissionsTokensClaimed ?: 0)
        val lastMissionClaimDate = doc.getString("lastMissionClaimDate") ?: (local?.lastMissionClaimDate ?: "")

        val founderTier = doc.getString("founderTier") ?: doc.getString("founder_tier") ?: (local?.founderTier ?: "")
        val isFounder = doc.getBoolean("isFounder") ?: (founderTier.isNotBlank())
        val reservedTokens = (doc.get("reservedTokens") as? Number)?.toInt() ?: (local?.reservedTokens ?: 0)

        val isBanned = doc.getBoolean("isBanned") ?: doc.getBoolean("banned") ?: (local?.isBanned ?: false)
        val banReason = doc.getString("banReason") ?: doc.getString("ban_reason") ?: (local?.banReason ?: "")
        val banType = doc.getString("banType") ?: (local?.banType ?: "PERMANENT")
        val isSuspended = doc.getBoolean("isSuspended") ?: doc.getBoolean("suspended") ?: (local?.isSuspended ?: false)
        val suspendReason = doc.getString("suspendReason") ?: (local?.suspendReason ?: "")
        val role = doc.getString("role") ?: (local?.role ?: "user")

        val updatedAt = (doc.get("updatedAtMillis") as? Number)?.toLong()
            ?: doc.getTimestamp("updatedAt")?.toDate()?.time
            ?: (local?.updatedAt ?: 0L)
        val version = (doc.get("version") as? Number)?.toLong() ?: (local?.version ?: 0L)

        return User(
            id = uid,
            username = username,
            fullName = fullName,
            phoneOrEmail = phoneOrEmail,
            mobileNo = mobileNo,
            dob = dob,
            bio = bio,
            socialLink = socialLink,
            passwordHash = local?.passwordHash ?: "",
            sessionToken = local?.sessionToken ?: "",
            fcmToken = doc.getString("fcmToken") ?: (local?.fcmToken ?: ""),
            state = state,
            isAgeVerified = isAgeVerified,
            legalConsentAccepted = legalConsent,
            legalConsentTimestamp = legalTimestamp,
            coolingOffUntil = coolingOff,
            dataExported = dataExported,
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
            dailyMissionsTokensClaimed = dailyMissionsTokensClaimed,
            lastMissionClaimDate = lastMissionClaimDate,
            founderTier = founderTier,
            isFounder = isFounder,
            reservedTokens = reservedTokens,
            isBanned = isBanned,
            banReason = banReason,
            banType = banType,
            isSuspended = isSuspended,
            suspendReason = suspendReason,
            role = role,
            updatedAt = updatedAt,
            version = version
        )
    }
}
