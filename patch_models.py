import re
with open("app/src/main/java/com/example/data/model/Models.kt", "r") as f:
    text = f.read()

target_user = """@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = "",
    val username: String = "Player",
    @JsonNames("phoneOrEmail", "phone_or_email") val phoneOrEmail: String = "",
    val balance: Double = 0.0,
    @JsonNames("avatarIdx", "avatar_idx") val avatarIdx: Int = 1,
    @JsonNames("passwordHash", "password_hash") val passwordHash: String = "",
    @JsonNames("sessionToken", "session_token") val sessionToken: String = "",
    val bio: String = "Ready for battle",
    @JsonNames("socialLink", "social_link") val socialLink: String = "",
    @JsonNames("dataExported", "data_exported") val dataExported: Boolean = false,
    @JsonNames("avatarUrl", "avatar_url") val avatarUrl: String = "",
    @JsonNames("freeFireId", "free_fire_id") val freeFireId: String = "",
    @JsonNames("inGameName", "in_game_name") val inGameName: String = "",
    @JsonNames("dateOfJoining", "date_of_joining") val dateOfJoining: Long = 0L,
    @JsonNames("matchesPlayed", "matches_played") val matchesPlayed: Int = 0,
    @JsonNames("totalKills", "total_kills") val totalKills: Int = 0,
    @JsonNames("totalWins", "total_wins") val totalWins: Int = 0,
    @JsonNames("fcmToken", "fcm_token") val fcmToken: String = ""
)"""

repl_user = """@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = "",
    val username: String = "Player",
    @JsonNames("phoneOrEmail", "phone_or_email") val phoneOrEmail: String = "",
    val balance: Double = 0.0,
    @JsonNames("avatarIdx", "avatar_idx") val avatarIdx: Int = 1,
    @JsonNames("passwordHash", "password_hash") val passwordHash: String = "",
    @JsonNames("sessionToken", "session_token") val sessionToken: String = "",
    val bio: String = "Ready for battle",
    @JsonNames("socialLink", "social_link") val socialLink: String = "",
    @JsonNames("dataExported", "data_exported") val dataExported: Boolean = false,
    @JsonNames("avatarUrl", "avatar_url") val avatarUrl: String = "",
    @JsonNames("freeFireId", "free_fire_id") val freeFireId: String = "",
    @JsonNames("inGameName", "in_game_name") val inGameName: String = "",
    @JsonNames("dateOfJoining", "date_of_joining") val dateOfJoining: Long = 0L,
    @JsonNames("matchesPlayed", "matches_played") val matchesPlayed: Int = 0,
    @JsonNames("totalKills", "total_kills") val totalKills: Int = 0,
    @JsonNames("totalWins", "total_wins") val totalWins: Int = 0,
    @JsonNames("fcmToken", "fcm_token") val fcmToken: String = "",
    val fullName: String = "",
    val dob: String = "",
    val mobileNo: String = "",
    val referralCode: String = "",
    val referredBy: String = "",
    val tokens: Int = 0,
    val loginStreak: Int = 0
)"""

text = text.replace(target_user, repl_user)
with open("app/src/main/java/com/example/data/model/Models.kt", "w") as f:
    f.write(text)
