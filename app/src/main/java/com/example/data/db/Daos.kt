package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LeaderboardPlayer
import com.example.data.model.Tournament
import com.example.data.model.Transaction
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments")
    fun getAll(): Flow<List<Tournament>>

    @Query("SELECT * FROM tournaments")
    suspend fun getAllSync(): List<Tournament>

    @Query("SELECT * FROM tournaments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Tournament?

    @Query("SELECT COUNT(*) FROM tournaments")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tournament: Tournament)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tournaments: List<Tournament>)
    
    @Update
    suspend fun update(tournament: Tournament)

    @Query("DELETE FROM tournaments WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tournaments")
    suspend fun clearAll()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getUser(): Flow<User?>
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUserSync(): User?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Update
    suspend fun update(user: User)
    
    @Query("DELETE FROM users")
    suspend fun clearAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllSync(): List<Transaction>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)
    
    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard ORDER BY rank ASC")
    fun getAll(): Flow<List<LeaderboardPlayer>>

    @Query("SELECT * FROM leaderboard")
    suspend fun getAllSync(): List<LeaderboardPlayer>

    @Query("SELECT COUNT(*) FROM leaderboard")
    suspend fun getCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<LeaderboardPlayer>)
    
    @Query("DELETE FROM leaderboard")
    suspend fun clearAll()
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT query FROM search_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT 5")
    fun getRecentSearches(userId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistory: com.example.data.model.SearchHistory)

    @Query("DELETE FROM search_history WHERE userId = :userId")
    suspend fun clearHistory(userId: String)
}

@Dao
interface MatchStatDao {
    @Query("SELECT * FROM match_stats ORDER BY timestamp DESC")
    fun getAll(): Flow<List<com.example.data.model.MatchStat>>
    
    @Query("SELECT * FROM match_stats")
    suspend fun getAllSync(): List<com.example.data.model.MatchStat>

    @Query("SELECT * FROM match_stats WHERE userId = :userId ORDER BY timestamp DESC")
    fun getByUserId(userId: String): Flow<List<com.example.data.model.MatchStat>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stat: com.example.data.model.MatchStat)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stats: List<com.example.data.model.MatchStat>)
    
    @Query("DELETE FROM match_stats")
    suspend fun clearAll()
}


@Dao
interface MissionDao {
    @Query("SELECT * FROM missions")
    fun getAllMissions(): Flow<List<com.example.data.model.Mission>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(missions: List<com.example.data.model.Mission>)

    @Update
    suspend fun update(mission: com.example.data.model.Mission)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mission: com.example.data.model.Mission)

    @Query("DELETE FROM missions")
    suspend fun deleteAll()
}

@Dao
interface AppNotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<com.example.data.model.AppNotification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: com.example.data.model.AppNotification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<com.example.data.model.AppNotification>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("DELETE FROM notifications WHERE id LIKE 'notif_welcome%' OR id LIKE 'notif_reminder_sample%' OR id LIKE 'notif_result_sample%'")
    suspend fun cleanupMockNotifications()
}

@Dao
interface BannerDao {
    @Query("SELECT * FROM banners WHERE active = 1 ORDER BY `order` ASC")
    fun getActiveBanners(): Flow<List<com.example.data.model.Banner>>

    @Query("SELECT * FROM banners ORDER BY `order` ASC")
    fun getAllBanners(): Flow<List<com.example.data.model.Banner>>

    @Query("SELECT * FROM banners")
    suspend fun getAllSync(): List<com.example.data.model.Banner>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(banner: com.example.data.model.Banner)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(banners: List<com.example.data.model.Banner>)

    @Query("DELETE FROM banners WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM banners")
    suspend fun clearAll()
}

@Dao
interface TournamentParticipantDao {
    @Query("SELECT * FROM tournament_participants WHERE tournamentId = :tournamentId ORDER BY slotNumber ASC")
    fun getParticipantsByTournament(tournamentId: String): Flow<List<com.example.data.model.TournamentParticipant>>

    @Query("SELECT * FROM tournament_participants WHERE tournamentId = :tournamentId AND userId = :userId LIMIT 1")
    fun getParticipant(tournamentId: String, userId: String): Flow<com.example.data.model.TournamentParticipant?>

    @Query("SELECT * FROM tournament_participants WHERE tournamentId = :tournamentId AND userId = :userId LIMIT 1")
    suspend fun getParticipantSync(tournamentId: String, userId: String): com.example.data.model.TournamentParticipant?

    @Query("SELECT * FROM tournament_participants WHERE userId = :userId ORDER BY registeredAt DESC")
    fun getMyTickets(userId: String): Flow<List<com.example.data.model.TournamentParticipant>>

    @Query("SELECT * FROM tournament_participants WHERE userId = :userId AND registeredAt >= :sinceTimestamp")
    suspend fun getParticipantsSince(userId: String, sinceTimestamp: Long): List<com.example.data.model.TournamentParticipant>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(participant: com.example.data.model.TournamentParticipant)

    @Query("DELETE FROM tournament_participants WHERE tournamentId = :tournamentId AND userId = :userId")
    suspend fun delete(tournamentId: String, userId: String)

    @Query("DELETE FROM tournament_participants")
    suspend fun clearAll()
}

@Dao
interface UserReportDao {
    @Query("SELECT * FROM user_reports WHERE userId = :userId ORDER BY createdAt DESC")
    fun getReportsByUser(userId: String): Flow<List<com.example.data.model.UserReport>>

    @Query("SELECT * FROM user_reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<com.example.data.model.UserReport>>

    @Query("SELECT * FROM user_reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: String): com.example.data.model.UserReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: com.example.data.model.UserReport)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<com.example.data.model.UserReport>)

    @Query("DELETE FROM user_reports WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM user_reports WHERE userId = :userId")
    suspend fun clearForUser(userId: String)

    @Query("UPDATE user_reports SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())
}



