/**
 * AppDatabase.kt
 * 
 * Room Database configuration and initialization.
 * 
 * Responsibilities:
 * - Define the database schema and version.
 * - Provide DAO (Data Access Object) instances for database operations.
 * - Manage a singleton instance of the Room database to prevent memory leaks.
 */
package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AppNotification
import com.example.data.model.Banner
import com.example.data.model.LeaderboardPlayer
import com.example.data.model.SearchHistory
import com.example.data.model.Tournament
import com.example.data.model.TournamentParticipant
import com.example.data.model.Transaction
import com.example.data.model.User
import com.example.data.model.MatchStat
import com.example.data.model.Mission
import com.example.data.model.UserReport

@Database(
    entities = [Tournament::class, User::class, Transaction::class, LeaderboardPlayer::class, SearchHistory::class, MatchStat::class, Mission::class, AppNotification::class, TournamentParticipant::class, Banner::class, UserReport::class],
    version = 26,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun matchStatDao(): MatchStatDao
    abstract fun missionDao(): MissionDao
    abstract fun appNotificationDao(): AppNotificationDao
    abstract fun tournamentParticipantDao(): TournamentParticipantDao
    abstract fun bannerDao(): BannerDao
    abstract fun userReportDao(): UserReportDao



    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "velorix_database_v2"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
