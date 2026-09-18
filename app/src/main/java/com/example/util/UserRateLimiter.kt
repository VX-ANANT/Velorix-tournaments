package com.example.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Robust client-side rate limiter and action throttle manager.
 * Protects critical user actions (tournament joins, deposits, withdrawals, OTP requests,
 * report submissions, avatar updates, pull-to-refresh) from rapid spamming, duplicate requests,
 * accidental double-taps, or quota abuse.
 */
object UserRateLimiter {

    // Action buckets configuration
    enum class ActionType(
        val minIntervalMs: Long,      // Minimum time between single taps
        val maxRequestsPerWindow: Int, // Max requests allowed in sliding window
        val windowDurationMs: Long,    // Window duration in ms
        val cooldownOnViolationMs: Long, // Extra cooldown applied if limit exceeded
        val humanLabel: String
    ) {
        // Tournament joins & slot booking: prevent double clicks or race conditions on entry fee
        TOURNAMENT_JOIN(
            minIntervalMs = 2_000L,
            maxRequestsPerWindow = 3,
            windowDurationMs = 30_000L,
            cooldownOnViolationMs = 10_000L,
            humanLabel = "Tournament Registration"
        ),

        // Wallet deposits (UTR submission): prevent submitting duplicate UTRs simultaneously
        WALLET_DEPOSIT(
            minIntervalMs = 4_000L,
            maxRequestsPerWindow = 3,
            windowDurationMs = 60_000L,
            cooldownOnViolationMs = 15_000L,
            humanLabel = "Wallet Deposit"
        ),

        // Wallet withdrawals: protect payout endpoint
        WALLET_WITHDRAWAL(
            minIntervalMs = 5_000L,
            maxRequestsPerWindow = 2,
            windowDurationMs = 60_000L,
            cooldownOnViolationMs = 20_000L,
            humanLabel = "Withdrawal Request"
        ),

        // Token conversion: prevent fast spam
        TOKEN_CONVERT(
            minIntervalMs = 2_500L,
            maxRequestsPerWindow = 3,
            windowDurationMs = 30_000L,
            cooldownOnViolationMs = 10_000L,
            humanLabel = "Token Exchange"
        ),

        // Support tickets / Reports: prevent ticket spam
        REPORT_SUBMISSION(
            minIntervalMs = 5_000L,
            maxRequestsPerWindow = 2,
            windowDurationMs = 60_000L,
            cooldownOnViolationMs = 25_000L,
            humanLabel = "Report Submission"
        ),

        // Auth OTP / Login requests: prevent SMS / auth API flooding
        AUTH_REQUEST(
            minIntervalMs = 3_000L,
            maxRequestsPerWindow = 3,
            windowDurationMs = 60_000L,
            cooldownOnViolationMs = 30_000L,
            humanLabel = "Authentication"
        ),

        // Profile updates & avatar changes
        PROFILE_UPDATE(
            minIntervalMs = 2_000L,
            maxRequestsPerWindow = 4,
            windowDurationMs = 30_000L,
            cooldownOnViolationMs = 10_000L,
            humanLabel = "Profile Update"
        ),

        // Pull-to-refresh: prevent aggressive API flooding
        REFRESH_SYNC(
            minIntervalMs = 2_000L,
            maxRequestsPerWindow = 5,
            windowDurationMs = 30_000L,
            cooldownOnViolationMs = 5_000L,
            humanLabel = "Sync Refresh"
        )
    }

    sealed class RateLimitResult {
        object Allowed : RateLimitResult()
        data class Denied(
            val reason: String,
            val waitSeconds: Int,
            val actionType: ActionType
        ) : RateLimitResult()
    }

    private data class ActionRecord(
        val timestamps: MutableList<Long> = mutableListOf(),
        var lockedUntilMs: Long = 0L
    )

    private val actionHistory = ConcurrentHashMap<String, ActionRecord>()

    /**
     * Checks whether an action for a specific key (or global action type) is permitted.
     * @param actionType The category of user action
     * @param identifier Optional entity ID (e.g. tournamentId or userId) to track per-entity or globally
     */
    @Synchronized
    fun checkAndRecord(actionType: ActionType, identifier: String = "global"): RateLimitResult {
        val now = System.currentTimeMillis()
        val key = "${actionType.name}:$identifier"
        val record = actionHistory.getOrPut(key) { ActionRecord() }

        // 1. Check if user is in an active lock/cooldown
        if (now < record.lockedUntilMs) {
            val remainingSec = (((record.lockedUntilMs - now) / 1000L) + 1L).toInt()
            return RateLimitResult.Denied(
                reason = "⏳ Rate limit: Please wait ${remainingSec}s before retrying ${actionType.humanLabel.lowercase()}.",
                waitSeconds = remainingSec,
                actionType = actionType
            )
        }

        // 2. Minimum tap-to-tap interval (debounce double-clicks)
        val lastTimestamp = record.timestamps.lastOrNull() ?: 0L
        val interval = now - lastTimestamp
        if (interval < actionType.minIntervalMs) {
            val remainingSec = (((actionType.minIntervalMs - interval) / 1000L) + 1L).toInt()
            return RateLimitResult.Denied(
                reason = "⏳ Please wait ${remainingSec}s before pressing again.",
                waitSeconds = remainingSec,
                actionType = actionType
            )
        }

        // 3. Sliding window limit
        val cutoff = now - actionType.windowDurationMs
        record.timestamps.removeAll { it < cutoff }

        if (record.timestamps.size >= actionType.maxRequestsPerWindow) {
            // Apply penalty cooldown
            record.lockedUntilMs = now + actionType.cooldownOnViolationMs
            val remainingSec = (actionType.cooldownOnViolationMs / 1000L).toInt()
            return RateLimitResult.Denied(
                reason = "⏳ Frequent requests detected. Cooldown active: please wait ${remainingSec}s.",
                waitSeconds = remainingSec,
                actionType = actionType
            )
        }

        // Record timestamp
        record.timestamps.add(now)
        return RateLimitResult.Allowed
    }

    /**
     * Resets rate limits for an action or identifier (useful on test bypass or logout)
     */
    fun reset(actionType: ActionType? = null, identifier: String? = null) {
        if (actionType == null && identifier == null) {
            actionHistory.clear()
        } else if (actionType != null && identifier != null) {
            actionHistory.remove("${actionType.name}:$identifier")
        } else if (actionType != null) {
            val prefix = "${actionType.name}:"
            actionHistory.keys.filter { it.startsWith(prefix) }.forEach { actionHistory.remove(it) }
        }
    }
}
