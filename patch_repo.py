import re

with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "r") as f:
    content = f.read()

replacement1 = """    suspend fun fetchDataFromServer(force: Boolean = false): Boolean {
        val isEmulator = android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("Emulator") || android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.HARDWARE.contains("ranchu")
        if (isEmulator) {
            android.util.Log.d("PlatformRepository", "Skipping fetch on emulator to prevent broker errors.")
            return true
        }
        val currentTime = System.currentTimeMillis()"""

content = re.sub(
    r"    suspend fun fetchDataFromServer\(force: Boolean = false\): Boolean \{\s*val currentTime = System\.currentTimeMillis\(\)",
    replacement1,
    content,
    flags=re.DOTALL
)

replacement2 = """    suspend fun observeLeaderboardRealtime() {
        val isEmulator = android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("Emulator") || android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.HARDWARE.contains("ranchu")
        if (isEmulator) {
            android.util.Log.d("PlatformRepository", "Skipping leaderboard stream on emulator to prevent broker errors.")
            return
        }
        try {"""

content = re.sub(
    r"    suspend fun observeLeaderboardRealtime\(\) \{\s*try \{",
    replacement2,
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "w") as f:
    f.write(content)

