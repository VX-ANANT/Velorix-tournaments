/**
 * Models.kt
 * 
 * Defines the core data structures (Entities) for the application.
 * These classes represent the local SQLite database tables using Room annotations.
 * 
 * Responsibilities:
 * - Define User, Tournament, Match, Participant, and Transaction models.
 * - Provide JSON serialization support via kotlinx.serialization.
 */
package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNames
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "tournaments")
@IgnoreExtraProperties
data class Tournament(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var game: String = "", // "BGMI" or "Free Fire"
    @JsonNames("prizePool", "prize_pool") var prizePool: Double = 0.0,
    @JsonNames("entryFee", "entry_fee") var entryFee: Double = 0.0,
    @JsonNames("maxSlots", "max_slots") var maxSlots: Int = 100,
    @JsonNames("filledSlots", "filled_slots") var filledSlots: Int = 0,
    var joined: Boolean = false,
    @JsonNames("dateTimeStr", "date_time_str") var dateTimeStr: String = "", // "Today at 8:00 PM"
    @JsonNames("mapType", "map_type") var mapType: String = "", // "Erangel", "Bermuda", "Miramar"
    var perspective: String = "", // "TPP" or "FPP"
    @JsonNames("bannerIdx", "banner_idx") var bannerIdx: Int = 1, // 1, 2, 3
    @JsonNames("roomId", "room_id") var roomId: String = "",
    @JsonNames("roomPassword", "room_password") var roomPassword: String = "",
    @JsonNames("rank1Prize", "rank_1_prize") var rank1Prize: Double = 0.0,
    @JsonNames("rank2Prize", "rank_2_prize") var rank2Prize: Double = 0.0,
    @JsonNames("rank3Prize", "rank_3_prize") var rank3Prize: Double = 0.0,
    @JsonNames("rank4To10Prize", "rank_4_10_prize") var rank4To10Prize: Double = 0.0,
    @JsonNames("killBounty", "kill_bounty") var killBounty: Double = 0.0,
    var format: String = "SOLO", // "SOLO", "DUO", "SQUAD", "1v1", "2v2", "3v3", "4v4"
    var status: String = "UPCOMING", // "UPCOMING", "LIVE", "COMPLETED", "CANCELLED"
    var rules: String = "",

    @JsonNames("matchCategory", "match_category", "category")
    var matchCategory: String = "BATTLE_ROYALE", // "BATTLE_ROYALE", "CLASH_SQUAD", "LONE_WOLF", "SPECIAL_MODE"

    @JsonNames("matchMode", "match_mode", "mode")
    var matchMode: String = "PER_KILL", // "PER_KILL", "SURVIVAL", "HEADSHOT_ONLY", "BODY_DAMAGE_ON", "SNIPER_ONLY", "LIMITED_AMMO", "UNLIMITED_AMMO", "PISTOL_ONLY"

    @JsonNames("customRuleBadge", "custom_rule_badge", "ruleBadge", "badge", "ruleHighlight")
    var customRuleBadge: String = "" // e.g. "ONLY HEADSHOT (NO BODY)", "PER KILL ₹50", "1v1 PRO DUEL"
) {
    val isFull: Boolean
        get() = filledSlots >= maxSlots

    val displayCategoryBadge: String
        get() {
            if (customRuleBadge.isNotBlank()) return customRuleBadge
            return when (matchCategory.uppercase()) {
                "CLASH_SQUAD", "CS" -> when (matchMode.uppercase()) {
                    "HEADSHOT_ONLY", "ONLY_HEAD" -> "HEADSHOT ONLY (NO BODY)"
                    "BODY_DAMAGE_ON", "ALL_WEAPONS" -> "ALL WEAPONS & BODY DMG"
                    "SNIPER_ONLY" -> "SNIPER ONLY DUEL"
                    "LIMITED_AMMO" -> "LIMITED AMMO TACTICAL"
                    "UNLIMITED_AMMO" -> "UNLIMITED AMMO RUSH"
                    "PISTOL_ONLY" -> "DESERT EAGLE ONLY"
                    else -> "CLASH SQUAD $format"
                }
                "LONE_WOLF" -> when (format.uppercase()) {
                    "2V2", "DUO" -> "LONE WOLF 2v2 COMBAT"
                    else -> "LONE WOLF 1v1 PRO DUEL"
                }
                "SPECIAL_MODE", "CUSTOM" -> when (matchMode.uppercase()) {
                    "RUSHER_VS_SNIPER" -> "RUSHER VS SNIPER"
                    "SPEED_WAR" -> "SPEED 200% RUSH"
                    else -> "SPECIAL TACTICAL"
                }
                else -> when (matchMode.uppercase()) {
                    "PER_KILL", "PER_KILL_DOMINATION" -> if (killBounty > 0) "₹${killBounty.toInt()} PER KILL" else "PER-KILL DOMINATION"
                    "SURVIVAL", "SURVIVAL_WWCD", "WWCD" -> "SURVIVAL / WWCD PRIORITY"
                    else -> if (killBounty > 0) "₹${killBounty.toInt()} PER KILL" else "$format BATTLE ROYALE"
                }
            }
        }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "transactions")
@IgnoreExtraProperties
data class Transaction(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var userId: String = "",
    var type: String = "ADD_FUNDS", // "ADD_FUNDS", "WITHDRAWAL", "ENTRY_FEE", "WINNINGS"
    var amount: Double = 0.0,
    var detail: String = "", // e.g. "BGMI Solo Battle Royale Entry" or "Withdrawn to UPI"
    var timestamp: Long = 0, // Using default of 0 as System.currentTimeMillis() requires custom serializer, though we can just not rely on it in default
    @JsonNames("isPositive", "is_positive") var isPositive: Boolean = true,
    var status: String = "SUCCESS" // "SUCCESS", "PENDING", "REJECTED"
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "users")
@IgnoreExtraProperties
data class User(
    @PrimaryKey var id: String = "",
    var username: String = "Player",
    @JsonNames("phoneOrEmail", "phone_or_email") var phoneOrEmail: String = "",
    var balance: Double = 0.0,
    @JsonNames("avatarIdx", "avatar_idx") var avatarIdx: Int = 1,
    @JsonNames("passwordHash", "password_hash") var passwordHash: String = "",
    @JsonNames("sessionToken", "session_token") var sessionToken: String = "",
    var bio: String = "Ready for battle",
    @JsonNames("socialLink", "social_link") var socialLink: String = "",
    @JsonNames("dataExported", "data_exported") var dataExported: Boolean = false,
    @JsonNames("avatarUrl", "avatar_url") var avatarUrl: String = "",
    @JsonNames("freeFireId", "free_fire_id") var freeFireId: String = "",
    @JsonNames("inGameName", "in_game_name") var inGameName: String = "",
    @JsonNames("dateOfJoining", "date_of_joining") var dateOfJoining: Long = 0L,
    @JsonNames("matchesPlayed", "matches_played") var matchesPlayed: Int = 0,
    @JsonNames("totalKills", "total_kills") var totalKills: Int = 0,
    @JsonNames("totalWins", "total_wins") var totalWins: Int = 0,
    @JsonNames("fcmToken", "fcm_token") var fcmToken: String = "",
    var fullName: String = "",
    var dob: String = "",
    var mobileNo: String = "",
    var referralCode: String = "",
    var referredBy: String = "",
    @JsonNames("referralCount", "referral_count") var referralCount: Int = 0,
    @JsonNames("referralEarnings", "referral_earnings") var referralEarnings: Double = 0.0,
    var tokens: Int = 0,
    var loginStreak: Int = 0,
    @JsonNames("lastLoginClaimDate", "last_login_claim_date") var lastLoginClaimDate: String = "",
    @JsonNames("totalTokensConverted", "total_tokens_converted") var totalTokensConverted: Int = 0,
    @JsonNames("founderTier", "founder_tier") var founderTier: String = "",
    @JsonNames("isFounder", "is_founder") var isFounder: Boolean = false,
    @JsonNames("reservedTokens", "reserved_tokens") var reservedTokens: Int = 0,
    @JsonNames("isBanned", "is_banned", "banned") var isBanned: Boolean = false,
    @JsonNames("banReason", "ban_reason") var banReason: String = "",
    @JsonNames("banType", "ban_type") var banType: String = "PERMANENT",
    @JsonNames("bannedAt", "banned_at") var bannedAt: Long = 0L,
    @JsonNames("banExpiresAt", "ban_expires_at") var banExpiresAt: Long = 0L,
    @JsonNames("isSuspended", "is_suspended", "suspended") var isSuspended: Boolean = false,
    @JsonNames("suspendReason", "suspend_reason") var suspendReason: String = "",
    @JsonNames("suspensionExpiresAt", "suspension_expires_at") var suspensionExpiresAt: Long = 0L,
    @JsonNames("role", "adminRole") var role: String = "user",
    var state: String = "Delhi",
    var isAgeVerified: Boolean = false,
    var legalConsentAccepted: Boolean = true,
    var legalConsentTimestamp: Long = 0L,
    var coolingOffUntil: Long = 0L
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "match_stats")
@IgnoreExtraProperties
data class MatchStat(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var tournamentId: String = "",
    var tournamentTitle: String = "Tournament Match",
    var game: String = "Free Fire",
    var userId: String = "",
    var matchNo: String = "Match #1",
    var position: Int = 0,
    var kills: Int = 0,
    var winnings: Double = 0.0,
    var tokensEarned: Int = 0,
    var timestamp: Long = 0,
    var status: String = "COMPLETED"
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "leaderboard")
@IgnoreExtraProperties
data class LeaderboardPlayer(
    @PrimaryKey var rank: Int,
    var username: String,
    @JsonNames("totalWinnings", "total_winnings") var totalWinnings: Double,
    var tokens: Int,
    @JsonNames("avatarIdx", "avatar_idx") var avatarIdx: Int,
    @JsonNames("avatarUrl", "avatar_url") var avatarUrl: String = ""
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "search_history", primaryKeys = ["userId", "query"])
@IgnoreExtraProperties
data class SearchHistory(
    var userId: String = "guest",
    var query: String = "",
    var timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class LiveMatchUpdate(
    var tournamentId: String,
    var alivePlayers: Int,
    var totalPlayers: Int,
    var topPlayer: String,
    var topKills: Int,
    var matchPhase: String
)


@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "banners")
@IgnoreExtraProperties
data class Banner(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var subtitle: String = "",
    var imageUrl: String = "",
    @JsonNames("badgeText", "badge_text") var badgeText: String = "FEATURED",
    @JsonNames("actionType", "action_type") var actionType: String = "MATCH", // "MATCH", "WALLET", "SUPPORT", "MISSIONS", "LINK", "ANNOUNCEMENT", "RULES"
    @JsonNames("targetId", "target_id") var targetId: String = "",
    var order: Int = 0,
    var active: Boolean = true,
    @JsonNames("ctaText", "cta_text") var ctaText: String = "EXPLORE NOW",
    @JsonNames("gradientTheme", "gradient_theme") var gradientTheme: String = "CYAN_PURPLE", // "CYAN_PURPLE", "EMERALD_TEAL", "AMBER_ORANGE", "CRIMSON_DARK", "DEEP_VIOLET", "SAPPHIRE_BLUE", "DARK_ONYX"
    var description: String = "",
    var terms: String = "",
    @JsonNames("validUntil", "valid_until", "expiryDate", "expiry_date") var validUntil: String = "",
    var category: String = "FEATURED"
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@androidx.room.Entity(tableName = "missions")
@IgnoreExtraProperties
data class Mission(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var description: String = "",
    var target: Int = 1,
    var progress: Int = 0,
    @JsonNames("rewardCurrency", "reward_currency") var rewardCurrency: Double = 20.0,
    @JsonNames("isCompleted", "is_completed") var isCompleted: Boolean = false,
    @JsonNames("isClaimed", "is_claimed") var isClaimed: Boolean = false,
    var category: String = "DAILY" // "DAILY", "CHALLENGE", "SPECIAL"
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "notifications")
@IgnoreExtraProperties
data class AppNotification(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var message: String = "",
    var type: String = "GENERAL", // "TOURNAMENT_REMINDER", "MATCH_RESULT", "MATCH_UPDATE", "PRIZE_ANNOUNCEMENT", "GENERAL"
    var timestamp: Long = System.currentTimeMillis(),
    var isRead: Boolean = false,
    var tournamentId: String = "",
    var tournamentTitle: String = "",
    var roomId: String = "",
    var roomPassword: String = "",
    var kills: Int = 0,
    var position: Int = 0,
    var winnings: Double = 0.0,
    var timeRemaining: String = ""
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "tournament_participants")
@IgnoreExtraProperties
data class TournamentParticipant(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var tournamentId: String = "",
    var userId: String = "",
    var username: String = "",
    var inGameName: String = "",
    var characterId: String = "",
    var slotNumber: Int = 1,
    var teamName: String = "",
    var registeredAt: Long = System.currentTimeMillis(),
    var ticketCode: String = ""
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "user_reports")
@IgnoreExtraProperties
data class UserReport(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var userId: String = "",
    var userName: String = "",
    var userContact: String = "",
    var inGameName: String = "",
    var category: String = "GENERAL_SUPPORT", // "PAYMENT_DEPOSIT", "PAYMENT_WITHDRAW", "ROOM_CREDENTIALS", "CHEATER_HACKER", "APP_BUG_CRASH", "ACCOUNT_ISSUE", "GENERAL_SUPPORT"
    var title: String = "",
    var description: String = "",
    var incidentTime: String = "",
    var relatedId: String = "", // e.g. Tournament ID, Match ID, Transaction ID
    var status: String = "PENDING", // "PENDING", "INVESTIGATING", "RESOLVED", "REJECTED", "CANCELLED"
    var priority: String = "NORMAL", // "LOW", "NORMAL", "HIGH", "URGENT"
    var source: String = "MANUAL_FORM", // "MANUAL_FORM", "GEMINI_ASSISTANT", "AUTO_ALERT"
    var adminReply: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@IgnoreExtraProperties
data class SystemAppConfig(
    @JsonNames("isMaintenance", "is_maintenance", "maintenance", "maintenance_mode", "maintenanceMode") 
    var isMaintenance: Boolean = false,
    
    @JsonNames("maintenanceTitle", "maintenance_title", "title") 
    var maintenanceTitle: String = "Scheduled System Maintenance",
    
    @JsonNames("maintenanceMessage", "maintenance_message", "message", "reason") 
    var maintenanceMessage: String = "We are currently upgrading our tournament servers and matchmaking network. Services will be restored shortly.",
    
    @JsonNames("maintenanceUntil", "maintenance_until", "estimated_end", "estimatedEnd", "eta") 
    var maintenanceUntil: String = "Expected Back Shortly",
    
    @JsonNames("allowAdminBypass", "allow_admin_bypass", "adminBypass") 
    var allowAdminBypass: Boolean = true,
    
    @JsonNames("isForceUpdate", "is_force_update", "force_update", "forceUpdate") 
    var isForceUpdate: Boolean = false,
    
    @JsonNames("minRequiredVersion", "min_required_version", "min_version", "minVersion") 
    var minRequiredVersion: String = "1.0.0",
    
    @JsonNames("minRequiredVersionCode", "min_version_code") 
    var minRequiredVersionCode: Int = 1,
    
    @JsonNames("updateTitle", "update_title") 
    var updateTitle: String = "Critical Update Available",
    
    @JsonNames("updateMessage", "update_message") 
    var updateMessage: String = "A mandatory new version of VeloRix Tournaments is required to compete in matches and access secure wallets.",
    
    @JsonNames("updateUrl", "update_url", "apk_url", "apkUrl", "playstore_url") 
    var updateUrl: String = "https://velorix.esports/download",
    
    @JsonNames("changelog", "update_changelog", "notes") 
    var changelog: String = "• Server stability & latency improvements\n• Enhanced anti-cheat & emulator detection\n• Fast wallet payouts and instant room access",
    
    @JsonNames("supportEmail", "support_email") 
    var supportEmail: String = "service.veloxyra@gmail.com",
    
    @JsonNames("supportWhatsApp", "support_whatsapp") 
    var supportWhatsApp: String = "+919876543210",
    
    @JsonNames("emergencyNotice", "emergency_notice", "banner_notice") 
    var emergencyNotice: String = "",

    @JsonNames("vpnRestrictionEnabled", "vpn_restriction_enabled", "vpn_blocked", "block_vpn")
    var vpnRestrictionEnabled: Boolean = true,

    @JsonNames("showDeveloperModal", "show_developer_modal", "developer_modal_visible", "dev_window_enabled")
    var showDeveloperModal: Boolean = true,

    @JsonNames("showBanners", "show_banners", "banners_enabled", "bannersEnabled", "isBannersEnabled")
    var showBanners: Boolean = false
)

enum class SituationPreviewType {
    NONE,
    BANNED,
    MAINTENANCE,
    SUSPENDED,
    FORCE_UPDATE,
    VPN_BLOCKED
}




