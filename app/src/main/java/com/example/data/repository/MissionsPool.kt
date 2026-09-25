package com.example.data.repository

import com.example.data.model.Mission
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * MissionsPool.kt
 *
 * Provides a robust, multi-tier procedural repository of Daily, Weekly, and Monthly
 * tactical esports missions with balanced, strictly limited rewards and server-side cap protection.
 *
 * Daily Cap: 100 Tokens / day
 * Reset Schedule:
 * - Daily: 12:00 AM IST
 * - Weekly: Monday 12:00 AM IST
 * - Monthly: 1st of month 12:00 AM IST
 */
object MissionsPool {

    const val DAILY_MISSION_REWARD_CAP_TOKENS = 100

    fun getTodayIstDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return sdf.format(Date())
    }

    fun getCurrentIstWeekKey(): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.ENGLISH, "%d-W%02d", year, week)
    }

    fun getCurrentIstMonthKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return sdf.format(Date())
    }

    fun getDailyTimeRemainingFormatted(): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val now = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 24)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val diff = (cal.timeInMillis - now).coerceAtLeast(0)
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60
        return "${hours}h ${minutes}m"
    }

    fun getWeeklyTimeRemainingFormatted(): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val now = cal.timeInMillis
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val diff = (cal.timeInMillis - now).coerceAtLeast(0)
        val days = diff / (1000 * 60 * 60 * 24)
        val hours = (diff / (1000 * 60 * 60)) % 24
        return "${days}d ${hours}h"
    }

    fun getMonthlyTimeRemainingFormatted(): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val now = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val diff = (cal.timeInMillis - now).coerceAtLeast(0)
        val days = diff / (1000 * 60 * 60 * 24)
        return "${days}d left"
    }

    /**
     * Generates standard active 5 Daily Missions for the given day key.
     */
    fun generateDailyMissions(dayKey: String, isLoginClaimed: Boolean): List<Mission> {
        return listOf(
            Mission(
                id = "m_daily_login_${dayKey}",
                title = "Daily Check-In Streak",
                description = "Open Velorix daily to maintain your operational streak and receive bonus combat tokens.",
                target = 1,
                progress = 1,
                rewardCurrency = 10.0,
                isCompleted = true,
                isClaimed = isLoginClaimed,
                category = "DAILY",
                periodKey = dayKey,
                badge = "STREAK",
                actionType = "LOGIN"
            ),
            Mission(
                id = "m_daily_play_match_${dayKey}",
                title = "Combat Deployment",
                description = "Join and deploy in 1 verified Esports tournament match today.",
                target = 1,
                progress = 0,
                rewardCurrency = 15.0,
                isCompleted = false,
                isClaimed = false,
                category = "DAILY",
                periodKey = dayKey,
                badge = "COMBAT",
                actionType = "MATCH_PLAY"
            ),
            Mission(
                id = "m_daily_kills_${dayKey}",
                title = "Target Neutralization",
                description = "Eliminate 2 or more opponents across your tournament games.",
                target = 2,
                progress = 0,
                rewardCurrency = 15.0,
                isCompleted = false,
                isClaimed = false,
                category = "DAILY",
                periodKey = dayKey,
                badge = "COMBAT",
                actionType = "KILL_COUNT"
            ),
            Mission(
                id = "m_daily_top_placement_${dayKey}",
                title = "Top 5 Survivor",
                description = "Secure a Top 5 placement in any active tournament match.",
                target = 1,
                progress = 0,
                rewardCurrency = 20.0,
                isCompleted = false,
                isClaimed = false,
                category = "DAILY",
                periodKey = dayKey,
                badge = "SURVIVAL",
                actionType = "TOP_RANK"
            ),
            Mission(
                id = "m_daily_squad_share_${dayKey}",
                title = "Squad Recruiter",
                description = "Copy or share your unique referral code with teammates to earn dynamic 10-15% commission.",
                target = 1,
                progress = 0,
                rewardCurrency = 10.0,
                isCompleted = false,
                isClaimed = false,
                category = "DAILY",
                periodKey = dayKey,
                badge = "COMMUNITY",
                actionType = "REFERRAL_SHARE"
            )
        )
    }

    /**
     * Generates active 5 Weekly Missions for the given week key.
     */
    fun generateWeeklyMissions(weekKey: String): List<Mission> {
        return listOf(
            Mission(
                id = "m_weekly_matches_${weekKey}",
                title = "Weekly Warrior",
                description = "Participate in 5 Esports tournament matches this week.",
                target = 5,
                progress = 0,
                rewardCurrency = 30.0,
                isCompleted = false,
                isClaimed = false,
                category = "WEEKLY",
                periodKey = weekKey,
                badge = "COMBAT",
                actionType = "MATCH_PLAY"
            ),
            Mission(
                id = "m_weekly_kills_${weekKey}",
                title = "Sharpshooter Elite",
                description = "Accumulate 10 confirmed eliminations across all weekly matches.",
                target = 10,
                progress = 0,
                rewardCurrency = 35.0,
                isCompleted = false,
                isClaimed = false,
                category = "WEEKLY",
                periodKey = weekKey,
                badge = "COMBAT",
                actionType = "KILL_COUNT"
            ),
            Mission(
                id = "m_weekly_victory_${weekKey}",
                title = "Victory Royale",
                description = "Achieve Rank #1 Champion in any registered tournament match.",
                target = 1,
                progress = 0,
                rewardCurrency = 40.0,
                isCompleted = false,
                isClaimed = false,
                category = "WEEKLY",
                periodKey = weekKey,
                badge = "SURVIVAL",
                actionType = "TOP_RANK"
            ),
            Mission(
                id = "m_weekly_converter_${weekKey}",
                title = "Token Liquidity",
                description = "Exchange mission tokens into playable VT Tokens in your wallet.",
                target = 1,
                progress = 0,
                rewardCurrency = 25.0,
                isCompleted = false,
                isClaimed = false,
                category = "WEEKLY",
                periodKey = weekKey,
                badge = "CHALLENGE",
                actionType = "WALLET_CONVERT"
            ),
            Mission(
                id = "m_weekly_streak_${weekKey}",
                title = "5-Day Loyalty",
                description = "Maintain an uninterrupted 5-day check-in streak this week.",
                target = 5,
                progress = 0,
                rewardCurrency = 30.0,
                isCompleted = false,
                isClaimed = false,
                category = "WEEKLY",
                periodKey = weekKey,
                badge = "STREAK",
                actionType = "LOGIN"
            )
        )
    }

    /**
     * Generates active 5 Monthly Missions for the given month key.
     */
    fun generateMonthlyMissions(monthKey: String): List<Mission> {
        return listOf(
            Mission(
                id = "m_monthly_veteran_${monthKey}",
                title = "Season Veteran",
                description = "Deploy in 20 total Esports tournament matches this month.",
                target = 20,
                progress = 0,
                rewardCurrency = 60.0,
                isCompleted = false,
                isClaimed = false,
                category = "MONTHLY",
                periodKey = monthKey,
                badge = "COMBAT",
                actionType = "MATCH_PLAY"
            ),
            Mission(
                id = "m_monthly_kills_${monthKey}",
                title = "Centurion Slayer",
                description = "Eliminate 35 total enemies across all competitive matches this month.",
                target = 35,
                progress = 0,
                rewardCurrency = 75.0,
                isCompleted = false,
                isClaimed = false,
                category = "MONTHLY",
                periodKey = monthKey,
                badge = "COMBAT",
                actionType = "KILL_COUNT"
            ),
            Mission(
                id = "m_monthly_podium_${monthKey}",
                title = "Podium Dominance",
                description = "Finish in the Top 3 podium across 3 different tournament matches.",
                target = 3,
                progress = 0,
                rewardCurrency = 80.0,
                isCompleted = false,
                isClaimed = false,
                category = "MONTHLY",
                periodKey = monthKey,
                badge = "SURVIVAL",
                actionType = "TOP_RANK"
            ),
            Mission(
                id = "m_monthly_leaderboard_${monthKey}",
                title = "Leaderboard Contender",
                description = "Climb to the top leaderboard standings with verified tournament earnings.",
                target = 1,
                progress = 0,
                rewardCurrency = 50.0,
                isCompleted = false,
                isClaimed = false,
                category = "MONTHLY",
                periodKey = monthKey,
                badge = "RANK",
                actionType = "LEADERBOARD_VIEW"
            ),
            Mission(
                id = "m_monthly_patron_${monthKey}",
                title = "Squad Master",
                description = "Refer 2 active squadmates who join and play tournament matches.",
                target = 2,
                progress = 0,
                rewardCurrency = 70.0,
                isCompleted = false,
                isClaimed = false,
                category = "MONTHLY",
                periodKey = monthKey,
                badge = "COMMUNITY",
                actionType = "REFERRAL_SHARE"
            )
        )
    }
}
