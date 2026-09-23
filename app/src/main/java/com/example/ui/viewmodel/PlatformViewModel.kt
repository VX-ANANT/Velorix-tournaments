/**
 * PlatformViewModel.kt
 * 
 * The central UI state manager, following the MVI/MVVM pattern.
 * 
 * Responsibilities:
 * - Hold UI state (e.g., user details, tournaments, loading states).
 * - Handle user intents (e.g., login, register, join tournament).
 * - Communicate with the PlatformRepository to fetch/update data.
 * - Expose state to Compose UI using StateFlow.
 */
package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Banner
import com.example.data.model.LeaderboardPlayer
import com.example.data.model.Tournament
import com.example.data.model.Transaction
import com.example.data.model.User
import com.example.data.model.UserReport
import com.example.data.repository.JoinResult
import com.example.data.repository.PlatformRepository
import com.example.data.repository.WithdrawResult
import com.example.data.repository.WalletBreakdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.example.data.repository.RepositoryManager
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.example.util.UserRateLimiter

class PlatformViewModel(application: Application) : AndroidViewModel(application) {

    private val repositoryManager = RepositoryManager.getInstance(application)
    private val repository = repositoryManager.repository
    private val auth by lazy { FirebaseAuth.getInstance() }
    val remoteConfig by lazy { Firebase.remoteConfig }

    // Mutex & In-Flight Tracking to guarantee zero double-joins
    private val joinMutex = kotlinx.coroutines.sync.Mutex()
    private val ongoingRegistrations = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val depositMutex = kotlinx.coroutines.sync.Mutex()
    private val ongoingDeposits = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    val userState: StateFlow<User?> = repository.user
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val transactions: StateFlow<List<Transaction>> = repository.transactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tournaments: StateFlow<List<Tournament>> = repository.tournaments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val walletBreakdown: StateFlow<WalletBreakdown> = combine(userState, transactions) { u, txs ->
        repository.getWalletBreakdown(u, txs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WalletBreakdown()
    )

    val matchStats: StateFlow<List<com.example.data.model.MatchStat>> = repository.matchStats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val leaderboard: StateFlow<List<LeaderboardPlayer>> = repository.leaderboard
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchHistory: StateFlow<List<String>> = repository.searchHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    val missions: StateFlow<List<com.example.data.model.Mission>> = repository.missions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val banners: StateFlow<List<Banner>> = repository.banners
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val notifications: StateFlow<List<com.example.data.model.AppNotification>> = repository.notifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unreadNotificationCount: StateFlow<Int> = repository.unreadNotificationCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val userReports: StateFlow<List<com.example.data.model.UserReport>> = repository.userReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isFirebaseConnected: StateFlow<Boolean> = repository.isFirebaseConnected

    val isSyncing: StateFlow<Boolean> = repository.isSyncing
    val lastSyncedTimestamp: StateFlow<Long> = repository.lastSyncedTimestamp

    val systemConfig: StateFlow<com.example.data.model.SystemAppConfig> = repository.systemConfig
    val situationPreview: StateFlow<com.example.data.model.SituationPreviewType> = repository.situationPreview

    private val _isVpnDetected = MutableStateFlow(false)
    val isVpnDetected: StateFlow<Boolean> = _isVpnDetected.asStateFlow()

    fun setSituationPreview(type: com.example.data.model.SituationPreviewType) {
        repository.setSituationPreview(type)
    }

    fun updateMaintenanceMode(enabled: Boolean, title: String = "", message: String = "", eta: String = "") {
        viewModelScope.launch {
            repository.updateMaintenanceMode(enabled, title, message, eta)
            showToast(if (enabled) "Maintenance mode ENABLED" else "Maintenance mode DISABLED")
        }
    }

    fun toggleUserBan(userId: String, isBanned: Boolean, reason: String = "", banType: String = "PERMANENT") {
        viewModelScope.launch {
            repository.toggleUserBan(userId, isBanned, reason, banType)
            showToast(if (isBanned) "Account SANCTIONED (Banned)" else "Account ban REVOKED")
        }
    }

    fun toggleUserSuspension(userId: String, isSuspended: Boolean, reason: String = "") {
        viewModelScope.launch {
            repository.toggleUserSuspension(userId, isSuspended, reason)
            showToast(if (isSuspended) "Account SUSPENDED" else "Account suspension LIFTED")
        }
    }

    fun toggleForceUpdate(enabled: Boolean, minVersion: String = "2.0.0", updateUrl: String = "") {
        viewModelScope.launch {
            repository.toggleForceUpdate(enabled, minVersion, updateUrl)
            showToast(if (enabled) "Force Update ACTIVATED" else "Force Update DEACTIVATED")
        }
    }

    fun toggleDeveloperModal(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleDeveloperModal(enabled)
            showToast(if (enabled) "Developer Modal ENABLED" else "Developer Modal DISABLED")
        }
    }

    fun toggleShowBanners(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleShowBanners(enabled)
            showToast(if (enabled) "In-App Banners ENABLED" else "In-App Banners DISABLED (Hidden)")
        }
    }

    fun saveBanner(banner: Banner, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.saveBanner(banner)
            if (res.isSuccess) {
                _toastMessage.emit("Banner published to Cloud successfully!")
                onResult(true)
            } else {
                _toastMessage.emit("Failed to publish banner: ${res.exceptionOrNull()?.message}")
                onResult(false)
            }
        }
    }

    fun deleteBanner(bannerId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.deleteBanner(bannerId)
            if (res.isSuccess) {
                _toastMessage.emit("Banner deleted successfully.")
                onResult(true)
            } else {
                _toastMessage.emit("Failed to delete banner.")
                onResult(false)
            }
        }
    }

    fun publishAnnouncementNotification(title: String, message: String, bannerId: String = "", onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.publishAnnouncementNotification(title, message, bannerId)
            if (res.isSuccess) {
                _toastMessage.emit("Announcement broadcast sent to all users!")
                onResult(true)
            } else {
                _toastMessage.emit("Failed to send announcement notification.")
                onResult(false)
            }
        }
    }

    fun checkVpnStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val detected = isVpnActive()
            _isVpnDetected.value = detected
        }
    }

    private fun isVpnActive(): Boolean {
        try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeNetwork = cm.activeNetwork
                if (activeNetwork != null) {
                    val caps = cm.getNetworkCapabilities(activeNetwork)
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        return true
                    }
                }
            }
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val name = iface.name.lowercase()
                if (iface.isUp && (name.contains("tun") || name.contains("ppp") || name.contains("p2p") || name.contains("tap") || name.contains("wg0") || name.contains("wg1"))) {
                    return true
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("PlatformViewModel", "VPN check exception: ${e.message}")
        }
        return false
    }
        
    private val _showConfetti = MutableStateFlow(false)
    val showConfetti: StateFlow<Boolean> = _showConfetti.asStateFlow()
    
    fun claimMission(mission: com.example.data.model.Mission) {
        viewModelScope.launch {
            val result = repository.claimMissionReward(mission)
            when (result) {
                is com.example.data.repository.ClaimMissionResult.Success -> {
                    _toastMessage.emit(result.message)
                    _showConfetti.value = true
                    kotlinx.coroutines.delay(3000)
                    _showConfetti.value = false
                }
                is com.example.data.repository.ClaimMissionResult.Failure -> {
                    _toastMessage.emit(result.message)
                }
            }
        }
    }

    fun claimDailyLogin() {
        viewModelScope.launch {
            val result = repository.claimDailyLoginMission()
            when (result) {
                is com.example.data.repository.ClaimMissionResult.Success -> {
                    _toastMessage.emit(result.message)
                    _showConfetti.value = true
                    kotlinx.coroutines.delay(3000)
                    _showConfetti.value = false
                }
                is com.example.data.repository.ClaimMissionResult.Failure -> {
                    _toastMessage.emit(result.message)
                }
            }
        }
    }

    fun convertTokensToVt(tokens: Int, onResult: (Boolean) -> Unit = {}) {
        val rateLimit = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.TOKEN_CONVERT)
        if (rateLimit is UserRateLimiter.RateLimitResult.Denied) {
            viewModelScope.launch { _toastMessage.emit(rateLimit.reason) }
            startActionCooldown("token_convert", rateLimit.waitSeconds)
            onResult(false)
            return
        }
        viewModelScope.launch {
            when (val result = repository.convertTokensToVt(tokens)) {
                is com.example.data.repository.ConvertResult.Success -> {
                    _toastMessage.emit(result.message)
                    _showConfetti.value = true
                    kotlinx.coroutines.delay(2500)
                    _showConfetti.value = false
                    onResult(true)
                }
                is com.example.data.repository.ConvertResult.Failure -> {
                    _toastMessage.emit(result.message)
                    onResult(false)
                }
            }
        }
    }

    fun recordSupportInteraction() {
        viewModelScope.launch {
            repository.updateMissionProgress("m_support_explorer", 1)
        }
    }

    fun recordLeaderboardView() {
        viewModelScope.launch {
            repository.updateMissionProgress("m_leaderboard_explorer", 1)
        }
    }

    val liveMatchUpdates: StateFlow<Map<String, com.example.data.model.LiveMatchUpdate>> = repository.liveMatchUpdates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun saveSearchQuery(query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch {
                repository.saveSearchQuery(query)
            }
        }
    }

    // Current selected tournament for details screen
    private val _selectedTournamentId = MutableStateFlow<String?>(null)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedTournament: StateFlow<Tournament?> = _selectedTournamentId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getTournamentById(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val _isCheckingAuth = MutableStateFlow(true)
    val isCheckingAuth: StateFlow<Boolean> = _isCheckingAuth.asStateFlow()

    private val _dbErrorDialog = MutableStateFlow<String?>(null)
    val dbErrorDialog: StateFlow<String?> = _dbErrorDialog.asStateFlow()

    fun clearDbError() {
        _dbErrorDialog.value = null
    }

    fun showError(msg: String) {
        _dbErrorDialog.value = msg
    }


    // Screen Loading/Swipe Refresh variables
    private val _isRefreshingHome = MutableStateFlow(false)
    val isRefreshingHome: StateFlow<Boolean> = _isRefreshingHome.asStateFlow()

    private val _isLoadingTournaments = MutableStateFlow(true)
    val isLoadingTournaments: StateFlow<Boolean> = _isLoadingTournaments.asStateFlow()

    private val _isRefreshingWallet = MutableStateFlow(false)
    val isRefreshingWallet: StateFlow<Boolean> = _isRefreshingWallet.asStateFlow()

    // Status notifications / Alerts
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Client-side Action Rate Limiting States
    private val _actionCooldownSeconds = MutableStateFlow<Map<String, Int>>(emptyMap())
    val actionCooldownSeconds: StateFlow<Map<String, Int>> = _actionCooldownSeconds.asStateFlow()

    private fun startActionCooldown(actionKey: String, seconds: Int) {
        viewModelScope.launch {
            for (i in seconds downTo 1) {
                _actionCooldownSeconds.value = _actionCooldownSeconds.value.toMutableMap().apply { put(actionKey, i) }
                kotlinx.coroutines.delay(1000L)
            }
            _actionCooldownSeconds.value = _actionCooldownSeconds.value.toMutableMap().apply { remove(actionKey) }
        }
    }

    fun getActionCooldown(actionKey: String): Int = _actionCooldownSeconds.value[actionKey] ?: 0

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.emit(message)
        }
    }

    private val prefs = application.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean("has_completed_onboarding", false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    // Daily Developer Popup tracking (shows once per calendar day on first app launch)
    private val _showDailyDeveloperPopup = MutableStateFlow(false)
    val showDailyDeveloperPopup: StateFlow<Boolean> = _showDailyDeveloperPopup.asStateFlow()

    fun checkAndTriggerDailyDeveloperPopup() {
        if (!_isLoggedIn.value || userState.value == null) {
            // Only trigger for authenticated users
            return
        }
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val lastShownDate = prefs.getString("last_dev_popup_date", "") ?: ""
        if (lastShownDate != todayStr) {
            _showDailyDeveloperPopup.value = true
        }
    }

    fun dismissDailyDeveloperPopup() {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        prefs.edit().putString("last_dev_popup_date", todayStr).apply()
        _showDailyDeveloperPopup.value = false
    }

    fun openDeveloperPopupManually() {
        _showDailyDeveloperPopup.value = true
    }

    fun checkAndSetOnboardingStatus(userItem: User?): Boolean {
        val hasCompletedFlag = prefs.getBoolean("has_completed_onboarding", false)
        val hasExistingAccount = userItem != null && (
            userItem.username.isNotBlank() ||
            userItem.fullName.isNotBlank() ||
            userItem.freeFireId.isNotBlank() ||
            userItem.inGameName.isNotBlank()
        )
        val isCompleted = hasCompletedFlag || hasExistingAccount
        _hasCompletedOnboarding.value = isCompleted
        prefs.edit().putBoolean("has_completed_onboarding", isCompleted).apply()
        return isCompleted
    }

    fun completeOnboarding(theme: String) {
        setThemeMode(theme)
        prefs.edit().putBoolean("has_completed_onboarding", true).apply()
        _hasCompletedOnboarding.value = true
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    fun refreshHomeData() {
        viewModelScope.launch {
            _isRefreshingHome.value = true
            _isLoadingTournaments.value = true
            try {
                withContext(Dispatchers.IO) {
                    repository.fetchDataFromServer(force = true)
                }
            } catch (e: Throwable) {
                android.util.Log.e("PlatformViewModel", "Error refreshing home data", e)
            } finally {
                _isLoadingTournaments.value = false
                _isRefreshingHome.value = false
            }
        }
    }

    val isAutoRefreshing = com.example.data.sync.AutoRefreshManager.getInstance(getApplication<Application>()).isAutoRefreshing
    val lastAutoRefreshedAt = com.example.data.sync.AutoRefreshManager.getInstance(getApplication<Application>()).lastRefreshTimestamp

    fun triggerAutoRefresh(force: Boolean = true, source: String = "manual") {
        com.example.data.sync.AutoRefreshManager.getInstance(getApplication<Application>()).triggerManualRefresh(force = force, source = source)
    }

    private val _isVpnBanned = MutableStateFlow(prefs.getBoolean("is_vpn_banned", false))
    val isVpnBanned: StateFlow<Boolean> = _isVpnBanned.asStateFlow()
    
    private val _vpnWarningCount = MutableStateFlow(prefs.getInt("vpn_warning_count", 0))
    val vpnWarningCount: StateFlow<Int> = _vpnWarningCount.asStateFlow()
    
    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    private fun checkVpn(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    private fun handleVpnDetection() {
        val currentCount = _vpnWarningCount.value
        if (currentCount >= 3) {
            _isVpnBanned.value = true
            prefs.edit().putBoolean("is_vpn_banned", true).apply()
            logout()
        } else {
            val newCount = currentCount + 1
            _vpnWarningCount.value = newCount
            prefs.edit().putInt("vpn_warning_count", newCount).apply()
            logout() // Disconnect user
            viewModelScope.launch {
                _toastMessage.emit("VPN Detected. Disconnected! Warning $newCount of 3.")
            }
        }
    }

    private val alertedTournaments = mutableSetOf<String>()

    private fun checkUpcomingMatches() {
        val now = java.util.Calendar.getInstance()
        val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(java.util.Calendar.MINUTE)

        tournaments.value.forEach { t ->
            if (t.joined && !alertedTournaments.contains(t.id)) {
                // Parse "Today at 8:00 PM" loosely
                try {
                    val timePart = t.dateTimeStr.split("at").lastOrNull()?.trim() ?: return@forEach
                    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
                    val date = sdf.parse(timePart)
                    if (date != null) {
                        val cal = java.util.Calendar.getInstance()
                        cal.time = date
                        val matchHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                        val matchMinute = cal.get(java.util.Calendar.MINUTE)
                        
                        val matchTimeInMinutes = matchHour * 60 + matchMinute
                        val currentTimeInMinutes = currentHour * 60 + currentMinute
                        
                        val diff = matchTimeInMinutes - currentTimeInMinutes
                        if (diff in 0..15 && t.dateTimeStr.contains("Today", ignoreCase = true)) {
                            alertedTournaments.add(t.id)
                            viewModelScope.launch {
                                _toastMessage.emit("Reminder: ${t.title} starts in $diff mins!")
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore parse errors
                }
            }
        }
    }
    
    init {
        val isEmulator = com.example.EnvUtils.isEmu()

        try {
            if (!isEmulator) {
                val configSettings = remoteConfigSettings {
                    minimumFetchIntervalInSeconds = 3600
                }
                remoteConfig.setConfigSettingsAsync(configSettings)
                remoteConfig.setDefaultsAsync(com.example.R.xml.remote_config_defaults)

                remoteConfig.fetchAndActivate()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            android.util.Log.d("RemoteConfig", "Config values fetched and activated: ${task.result}")
                        } else {
                            android.util.Log.e("RemoteConfig", "Error fetching Remote Config", task.exception)
                        }
                    }

                remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                    override fun onUpdate(configUpdate : ConfigUpdate) {
                        android.util.Log.d("RemoteConfig", "Updated keys: " + configUpdate.updatedKeys)
                        remoteConfig.activate().addOnCompleteListener { }
                    }

                    override fun onError(error : FirebaseRemoteConfigException) {
                        android.util.Log.w("RemoteConfig", "Config update error with code: " + error.code, error)
                    }
                })
            }
        } catch (e: Throwable) {
            android.util.Log.w("RemoteConfig", "RemoteConfig init skipped: ${e.message}")
        }

        viewModelScope.launch {
            repository.initializeMissions()
        }
        // Upcoming matches polling Coroutine
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                withContext(Dispatchers.Main) {
                    checkUpcomingMatches()
                }
                delay(60000) // check every minute
            }
        }
        
        // VPN Polling Coroutine
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val hasVpn = checkVpn(application.applicationContext)
                
                // State change from OFF to ON
                if (hasVpn && !_isVpnActive.value) {
                    _isVpnActive.value = true
                    withContext(Dispatchers.Main) {
                        handleVpnDetection()
                    }
                } else if (!hasVpn && _isVpnActive.value) {
                    _isVpnActive.value = false
                }
                delay(1000)
            }
        }

        repository.dbErrorCallback = { errorMsg ->
            _dbErrorDialog.value = errorMsg
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingTournaments.value = true
            try {
                repository.fetchDataFromServer()
            } catch (e: Throwable) {
                android.util.Log.e("PlatformViewModel", "Initial server fetch failed", e)
            } finally {
                _isLoadingTournaments.value = false
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeLeaderboardRealtime()
        }
        viewModelScope.launch {
            val isFirebaseAuthed = try {
                withContext(Dispatchers.IO) {
                    auth.currentUser != null
                }
            } catch (e: Exception) { false }
            
            if (isFirebaseAuthed) {
                _isLoggedIn.value = true
                prefs.edit().putBoolean("is_logged_in", true).apply()
                withContext(Dispatchers.IO) {
                    repository.fetchDataFromServer(force = true)
                }
                val userItem = repository.getUserSync()
                checkAndSetOnboardingStatus(userItem)
            } else {
                prefs.edit().putBoolean("is_logged_in", false).apply()
                _isLoggedIn.value = false
            }
            _isCheckingAuth.value = false
        }
    }

    fun selectTournament(id: String?) {
        _selectedTournamentId.value = id
    }

    fun refreshHome() {
        val rateLimit = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.REFRESH_SYNC, "home")
        if (rateLimit is UserRateLimiter.RateLimitResult.Denied) {
            viewModelScope.launch { _toastMessage.emit(rateLimit.reason) }
            return
        }
        viewModelScope.launch {
            _isRefreshingHome.value = true
            withContext(Dispatchers.IO) {
                repository.fetchDataFromServer(force = true)
            }
            _isRefreshingHome.value = false
            _toastMessage.emit("Tournaments & matches updated from database!")
        }
    }

    fun refreshWallet() {
        val rateLimit = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.REFRESH_SYNC, "wallet")
        if (rateLimit is UserRateLimiter.RateLimitResult.Denied) {
            viewModelScope.launch { _toastMessage.emit(rateLimit.reason) }
            return
        }
        viewModelScope.launch {
            _isRefreshingWallet.value = true
            withContext(Dispatchers.IO) {
                repository.fetchDataFromServer()
            }
            _isRefreshingWallet.value = false
            _toastMessage.emit("Wallet transactions synchronized.")
        }
    }


    fun updateFcmToken(token: String) {
        viewModelScope.launch {
            repository.updateFcmToken(token)
        }
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
            _toastMessage.emit("All notifications marked as read")
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
            _toastMessage.emit("Notification history cleared")
        }
    }


    /**
     * Sends a password reset email to the specified address.
     */
    fun resetPassword(email: String, onComplete: (Boolean, String) -> Unit) {
        if (email.isBlank()) {
            onComplete(false, "Please enter your email.")
            return
        }
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, "Password reset email sent. Check your inbox.")
                } else {
                    onComplete(false, task.exception?.message ?: "Failed to send reset email.")
                }
            }
    }

    fun login(phoneOrEmail: String, passwordHash: String, loginMethod: String = "email", onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val phoneOrEmailT = phoneOrEmail.trim()
            val passwordHashT = passwordHash.trim()
            if (phoneOrEmailT.isBlank() || passwordHashT.isBlank()) {
                _toastMessage.emit("Please enter valid identifier and Password.")
                _isAuthLoading.value = false
                return@launch
            }
            try {
                kotlinx.coroutines.withTimeout(30000L) {
                    withContext(Dispatchers.IO) {
                        val firebaseIdentifier = if (loginMethod == "email") phoneOrEmailT else "$phoneOrEmailT@phone.velorix.com"
                        auth.signInWithEmailAndPassword(firebaseIdentifier, passwordHashT).await()
                        repository.fetchDataFromServer(force = true)
                    }
                }
                var userItem = repository.getUserSync()
                if (userItem == null) {
                    withContext(Dispatchers.IO) {
                        repository.fetchDataFromServer(force = true)
                    }
                    userItem = repository.getUserSync()
                }
                if (userItem == null) {
                    val firebaseUser = auth.currentUser ?: throw Exception("Authentication session not found. Please try again.")
                    val email = firebaseUser.email ?: if (phoneOrEmailT.contains("@")) phoneOrEmailT else ""
                    val displayName = firebaseUser.displayName ?: if (email.isNotBlank()) email.substringBefore("@") else phoneOrEmailT.substringBefore("@")
                    val newUser = User(
                        id = firebaseUser.uid,
                        username = displayName.ifBlank { "Player" },
                        phoneOrEmail = phoneOrEmailT,
                        fullName = displayName.ifBlank { "Player" },
                        balance = 0.0,
                        avatarIdx = 1,
                        dateOfJoining = System.currentTimeMillis()
                    )
                    withContext(Dispatchers.IO) {
                        repository.updateProfile(newUser)
                    }
                    userItem = newUser
                }
                val username = userItem.username
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putBoolean("has_completed_onboarding", true)
                    .apply()
                _hasCompletedOnboarding.value = true
                _isLoggedIn.value = true
                _toastMessage.emit("Welcome back, ${username}!")
                onComplete()
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _dbErrorDialog.value = "Login Timeout: Please check your internet connection."
            } catch (e: Exception) {
                _dbErrorDialog.value = "Login Failed: ${e.message}"
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun register(username: String, phoneOrEmail: String, passwordHash: String, loginMethod: String = "email", referralCode: String = "", onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val phoneOrEmailT = phoneOrEmail.trim()
            val passwordHashT = passwordHash.trim()
            val usernameT = username.trim()
            if (usernameT.isBlank() || phoneOrEmailT.isBlank() || passwordHashT.isBlank()) {
                _toastMessage.emit("All fields are mandatory.")
                _isAuthLoading.value = false
                return@launch
            }
            if (passwordHashT.length < 6) {
                _toastMessage.emit("Password must be at least 6 characters.")
                _isAuthLoading.value = false
                return@launch
            }
            try {
                kotlinx.coroutines.withTimeout(30000L) {
                    withContext(Dispatchers.IO) {
                        val firebaseIdentifier = if (loginMethod == "email") phoneOrEmailT else "$phoneOrEmailT@phone.velorix.com"
                        auth.createUserWithEmailAndPassword(firebaseIdentifier, passwordHashT).await()
                        repository.saveUserProfile(
                            username = usernameT,
                            phoneOrEmail = phoneOrEmailT,
                            passwordHash = passwordHashT,
                            referralCodeApplied = referralCode.trim().uppercase()
                        )
                    }
                }
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putBoolean("has_completed_onboarding", false)
                    .apply()
                _hasCompletedOnboarding.value = false
                _isLoggedIn.value = true
                _toastMessage.emit("Account created! Complete your gaming profile.")
                onComplete(true)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _dbErrorDialog.value = "Registration Timeout: Please check your internet connection."
                onComplete(false)
            } catch (e: Exception) {
                _dbErrorDialog.value = "Registration Failed: ${e.message}"
                onComplete(false)
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun verifyEmailOtp(email: String, otp: String, usernameForSignup: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _toastMessage.emit("Email Verification not directly supported via OTP in Firebase in this UI flow.")
            onComplete()
        }
    }

    fun submitPhoneLogin(phone: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _toastMessage.emit("Verification code sent to $phone")
            onComplete(true)
        }
    }

    fun verifyPhoneOtp(phone: String, otp: String, usernameForSignup: String? = null, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            try {
                if (repository.user.firstOrNull() == null) {
                    repository.saveUserProfile(usernameForSignup ?: "Player_" + (1000..9999).random(), phone)
                }
                val username = repository.user.firstOrNull()?.username ?: usernameForSignup ?: "Warrior"
                prefs.edit().putBoolean("is_logged_in", true).apply()
                _isLoggedIn.value = true
                _toastMessage.emit("Welcome to the Arena, ${username}!")
                onComplete()
            } catch (e: Exception) {
                _dbErrorDialog.value = "OTP Verification Failed: ${e.message}"
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun loginWithGoogle(context: android.content.Context, onComplete: () -> Unit = {}, onFallback: () -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            android.util.Log.i("FirebaseAuth", "[loginWithGoogle] Starting Google Sign-In flow with CredentialManager...")
            try {
                kotlinx.coroutines.withTimeout(30000L) {
                    val webClientId = "27931798964-h6fiau2df3i6e3049fontnctpsh06763.apps.googleusercontent.com"
                    val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    
                    val request = androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    
                    val credentialManager = try {
                        androidx.credentials.CredentialManager.create(context)
                    } catch (e: Throwable) {
                        android.util.Log.e("FirebaseAuth", "[loginWithGoogle] CredentialManager.create failed: ${e.javaClass.simpleName} - ${e.message}", e)
                        null
                    }

                    if (credentialManager == null) {
                        android.util.Log.w("FirebaseAuth", "[loginWithGoogle] CredentialManager unavailable. Triggering fallback flow.")
                        _isAuthLoading.value = false
                        onFallback()
                        return@withTimeout
                    }
                    
                    android.util.Log.d("FirebaseAuth", "[loginWithGoogle] Requesting credential from CredentialManager...")
                    val result = try {
                        credentialManager.getCredential(request = request, context = context)
                    } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                        android.util.Log.w("FirebaseAuth", "[loginWithGoogle] User cancelled CredentialManager picker.")
                        throw e
                    } catch (e: Throwable) {
                        android.util.Log.e("FirebaseAuth", "[loginWithGoogle] CredentialManager getCredential failed: ${e.javaClass.simpleName} - ${e.message}", e)
                        null
                    }
                    
                    if (result == null) {
                        android.util.Log.w("FirebaseAuth", "[loginWithGoogle] CredentialManager returned null credential. Triggering fallback flow.")
                        _isAuthLoading.value = false
                        onFallback()
                        return@withTimeout
                    }
                    
                    try {
                        val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(result.credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        android.util.Log.i("FirebaseAuth", "[loginWithGoogle] Obtained Google ID Token successfully. Signing in to Firebase Auth...")
                        
                        withContext(Dispatchers.IO) {
                            val credential = GoogleAuthProvider.getCredential(idToken, null)
                            val authResult = auth.signInWithCredential(credential).await()
                            android.util.Log.i("FirebaseAuth", "[loginWithGoogle] Firebase Auth SUCCESS for UID: ${authResult.user?.uid}, Email: ${authResult.user?.email}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseAuth", "[loginWithGoogle] Firebase signInWithCredential FAILED: ${e.javaClass.simpleName} - ${e.message}", e)
                        _isAuthLoading.value = false
                        onFallback()
                        return@withTimeout
                    }
                    
                    val firebaseUser = auth.currentUser
                    if (firebaseUser == null) {
                        android.util.Log.e("FirebaseAuth", "[loginWithGoogle] auth.currentUser is NULL after successful signInWithCredential!")
                        _isAuthLoading.value = false
                        onFallback()
                        return@withTimeout
                    }

                    android.util.Log.i("FirebaseAuth", "[loginWithGoogle] Active Firebase User verified -> UID: ${firebaseUser.uid}, DisplayName: ${firebaseUser.displayName}, Email: ${firebaseUser.email}")

                    withContext(Dispatchers.IO) {
                        android.util.Log.d("FirestoreUser", "[loginWithGoogle] Ensuring user document in Firestore for auth.uid: ${firebaseUser.uid}")
                        repository.ensureFirestoreUserDocument(firebaseUser)
                        repository.fetchDataFromServer(force = true)
                    }
                }
                if (_isAuthLoading.value) { // means fallback was not triggered
                    var userItem = repository.getUserSync()
                    if (userItem == null) {
                        withContext(Dispatchers.IO) {
                            repository.fetchDataFromServer(force = true)
                        }
                        userItem = repository.getUserSync()
                    }
                    if (userItem == null) {
                        val firebaseUser = auth.currentUser ?: throw Exception("Google authentication session not found.")
                        val displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: ("Player_" + (1000..9999).random())
                        val email = firebaseUser.email ?: ""
                        val newUser = User(
                            id = firebaseUser.uid,
                            username = displayName,
                            phoneOrEmail = email,
                            fullName = displayName,
                            avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                            balance = 0.0,
                            avatarIdx = 1,
                            dateOfJoining = System.currentTimeMillis()
                        )
                        withContext(Dispatchers.IO) {
                            repository.updateProfile(newUser)
                        }
                        userItem = newUser
                    }
                    val username = userItem.username
                    android.util.Log.i("FirebaseAuth", "[loginWithGoogle] Google Sign-In pipeline COMPLETE. Mapped User ID: ${userItem.id}, Username: $username")
                    prefs.edit()
                        .putBoolean("is_logged_in", true)
                        .putBoolean("has_completed_onboarding", true)
                        .apply()
                    _hasCompletedOnboarding.value = true
                    _isLoggedIn.value = true
                    _toastMessage.emit("Welcome back, ${username}!")
                    onComplete()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.util.Log.e("FirebaseAuth", "[loginWithGoogle] Timeout (30s) reached during Google Sign-In flow.", e)
                _dbErrorDialog.value = "Google Login Timeout: Please check your internet connection."
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                android.util.Log.w("FirebaseAuth", "[loginWithGoogle] Google Sign-In cancelled by user.")
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("FirebaseAuth", "[loginWithGoogle] General failure during Google Sign-In flow: ${e.message}", e)
                _isAuthLoading.value = false
                onFallback()
            } finally {
                if (_isAuthLoading.value) {
                    _isAuthLoading.value = false
                }
            }
        }
    }

    fun updateAvatar(newAvatarUrl: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateAvatar(newAvatarUrl)
                }
                _toastMessage.emit("Profile picture updated successfully!")
                onComplete(true)
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Failed to update profile picture")
                onComplete(false)
            }
        }
    }

    fun updateProfile(updatedUser: User, onComplete: (Boolean) -> Unit = {}) {
        val rateLimit = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.PROFILE_UPDATE)
        if (rateLimit is UserRateLimiter.RateLimitResult.Denied) {
            viewModelScope.launch { _toastMessage.emit(rateLimit.reason) }
            startActionCooldown("profile_update", rateLimit.waitSeconds)
            onComplete(false)
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateProfile(updatedUser)
                }
                _toastMessage.emit("Profile updated successfully!")
                onComplete(true)
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Failed to update profile")
                onComplete(false)
            }
        }
    }

    fun exportUserData() {
        viewModelScope.launch {
            val userItem = repository.getUserSync()
            if (userItem != null) {
                val updatedUser = userItem.copy(dataExported = true)
                repository.updateProfile(updatedUser)
                _toastMessage.emit("Data Exported & Uploaded to Database for Verification.")
            }
        }
    }
    
    fun requestAccountDeletion(reason: String, details: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.submitAccountDeletionRequest(reason, details)
                }
                logout()
                _toastMessage.emit("Account deletion request submitted. We will process it shortly.")
            } catch (e: Exception) {
                _dbErrorDialog.value = "Failed to submit request: ${e.message}"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    auth.signOut()
                }
            } catch (e: Exception) {
                android.util.Log.e("Auth", "Logout error", e)
            }
            withContext(Dispatchers.IO) {
                repositoryManager.onUserLogout()
            }
            prefs.edit().putBoolean("is_logged_in", false).apply()
            _isLoggedIn.value = false
        }
    }

    fun registerForTournament(id: String, onResult: (Boolean) -> Unit = {}) {
        if (!ongoingRegistrations.add(id)) {
            viewModelScope.launch { _toastMessage.emit("Registration is already processing. Please wait...") }
            onResult(false)
            return
        }
        val rateLimit = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.TOURNAMENT_JOIN, id)
        if (rateLimit is UserRateLimiter.RateLimitResult.Denied) {
            ongoingRegistrations.remove(id)
            viewModelScope.launch { _toastMessage.emit(rateLimit.reason) }
            startActionCooldown("tournament_join_$id", rateLimit.waitSeconds)
            onResult(false)
            return
        }
        viewModelScope.launch {
            try {
                joinMutex.withLock {
                    when (val result = repository.joinTournament(id)) {
                        is JoinResult.Success -> {
                            _toastMessage.emit(result.message)
                            onResult(true)
                        }
                        is JoinResult.Failure -> {
                            _toastMessage.emit(result.message)
                            onResult(false)
                        }
                    }
                }
            } finally {
                ongoingRegistrations.remove(id)
            }
        }
    }

    fun addWalletFunds(amount: Double, utrNumber: String = "", paymentRef: String = "") {
        val depositKey = utrNumber.trim().ifEmpty { "manual_${amount}_${System.currentTimeMillis()}" }
        if (!ongoingDeposits.add(depositKey)) {
            viewModelScope.launch { _toastMessage.emit("Deposit request already being submitted. Please wait...") }
            return
        }
        val rateLimit = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.WALLET_DEPOSIT)
        if (rateLimit is UserRateLimiter.RateLimitResult.Denied) {
            ongoingDeposits.remove(depositKey)
            viewModelScope.launch { _toastMessage.emit(rateLimit.reason) }
            startActionCooldown("wallet_deposit", rateLimit.waitSeconds)
            return
        }
        viewModelScope.launch {
            try {
                depositMutex.withLock {
                    when (val result = repository.submitDepositRequest(amount, utrNumber, paymentRef)) {
                        is PlatformRepository.DepositResult.Success -> {
                            _toastMessage.emit(result.message)
                        }
                        is PlatformRepository.DepositResult.Failure -> {
                            _toastMessage.emit(result.message)
                        }
                    }
                }
            } catch (e: Exception) {
                _dbErrorDialog.value = "Deposit request failed: ${e.message}"
            } finally {
                ongoingDeposits.remove(depositKey)
            }
        }
    }

    fun registerFounderPass(tierId: String, tokensReward: Int, priceInr: Double, paymentRef: String = "", onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val success = repository.registerFounderPass(tierId, tokensReward, priceInr, paymentRef)
                if (success) {
                    _toastMessage.emit("Founder Pass Activated! Tier $tierId confirmed.")
                    onComplete()
                }
            } catch (e: Exception) {
                _dbErrorDialog.value = "Failed to activate Founder Pass: ${e.message}"
            }
        }
    }

    fun withdrawFunds(amount: Double, upiId: String = "") {
        val rateLimit = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.WALLET_WITHDRAWAL)
        if (rateLimit is UserRateLimiter.RateLimitResult.Denied) {
            viewModelScope.launch { _toastMessage.emit(rateLimit.reason) }
            startActionCooldown("wallet_withdrawal", rateLimit.waitSeconds)
            return
        }
        viewModelScope.launch {
            when (val result = repository.withdrawFunds(amount, upiId)) {
                is WithdrawResult.Success -> {
                    _toastMessage.emit(result.message)
                }
                is WithdrawResult.Failure -> {
                    _toastMessage.emit(result.message)
                }
            }
        }
    }

    fun applyReferralCode(code: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val trimmedCode = code.trim().uppercase()
        if (trimmedCode.isBlank()) {
            val msg = "Please enter a valid referral code."
            viewModelScope.launch { _toastMessage.emit(msg) }
            onResult(false, msg)
            return
        }
        viewModelScope.launch {
            val result = repository.applyReferralCode(trimmedCode)
            result.onSuccess { msg ->
                _toastMessage.emit(msg)
                onResult(true, msg)
            }.onFailure { err ->
                val errMsg = err.message ?: "Failed to apply referral code. Please try again."
                _toastMessage.emit(errMsg)
                onResult(false, errMsg)
            }
        }
    }

    fun loginWithGoogleToken(idToken: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential).await()
                    repository.fetchDataFromServer(force = true)
                    
                    var u = repository.getUserSync()
                    if (u == null) {
                        val googleUser = auth.currentUser
                        val displayName = googleUser?.displayName ?: googleUser?.email?.substringBefore("@") ?: ("Player_" + (1000..9999).random())
                        val email = googleUser?.email ?: ""
                        repository.saveUserProfile(displayName, email)
                    }
                }
                
                var userItem = repository.getUserSync()
                if (userItem == null) {
                    withContext(Dispatchers.IO) {
                        repository.fetchDataFromServer(force = true)
                    }
                    userItem = repository.getUserSync()
                }
                if (userItem == null) {
                    val googleUser = auth.currentUser ?: throw Exception("Google authentication session not found.")
                    val displayName = googleUser.displayName ?: googleUser.email?.substringBefore("@") ?: ("Player_" + (1000..9999).random())
                    val email = googleUser.email ?: ""
                    val newUser = User(
                        id = googleUser.uid,
                        username = displayName,
                        phoneOrEmail = email,
                        fullName = displayName,
                        avatarUrl = googleUser.photoUrl?.toString() ?: "",
                        balance = 0.0,
                        avatarIdx = 1,
                        dateOfJoining = System.currentTimeMillis()
                    )
                    withContext(Dispatchers.IO) {
                        repository.updateProfile(newUser)
                    }
                    userItem = newUser
                }
                val username = userItem.username
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putBoolean("has_completed_onboarding", true)
                    .apply()
                _hasCompletedOnboarding.value = true
                _isLoggedIn.value = true
                _toastMessage.emit("Welcome back, ${username}!")
                onComplete()
            } catch (e: Exception) {
                _dbErrorDialog.value = "Google Login Failed: ${e.message}"
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun getParticipants(tournamentId: String): Flow<List<com.example.data.model.TournamentParticipant>> {
        return repository.getParticipants(tournamentId)
    }

    fun getMyParticipant(tournamentId: String, userId: String): Flow<com.example.data.model.TournamentParticipant?> {
        return repository.getMyParticipant(tournamentId, userId)
    }

    fun registerForTournamentWithSlot(
        tournamentId: String,
        slotNumber: Int,
        inGameName: String,
        characterId: String,
        teamName: String = "",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!ongoingRegistrations.add(tournamentId)) {
            viewModelScope.launch { _toastMessage.emit("Registration is already processing. Please wait...") }
            onResult(false, "Registration is already processing. Please wait...")
            return
        }
        val rateLimit = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.TOURNAMENT_JOIN, tournamentId)
        if (rateLimit is UserRateLimiter.RateLimitResult.Denied) {
            ongoingRegistrations.remove(tournamentId)
            viewModelScope.launch { _toastMessage.emit(rateLimit.reason) }
            startActionCooldown("slot_join_$tournamentId", rateLimit.waitSeconds)
            onResult(false, rateLimit.reason)
            return
        }
        viewModelScope.launch {
            try {
                joinMutex.withLock {
                    when (val result = repository.joinTournamentWithSlot(tournamentId, slotNumber, inGameName, characterId, teamName)) {
                        is JoinResult.Success -> {
                            _toastMessage.emit(result.message)
                            _showConfetti.value = true
                            delay(3000)
                            _showConfetti.value = false
                            onResult(true, result.message)
                        }
                        is JoinResult.Failure -> {
                            _toastMessage.emit(result.message)
                            onResult(false, result.message)
                        }
                    }
                }
            } finally {
                ongoingRegistrations.remove(tournamentId)
            }
        }
    }

    fun sendCustomBroadcast(title: String, message: String, type: String = "GENERAL") {
        viewModelScope.launch {
            val notif = com.example.data.model.AppNotification(
                id = "broadcast_${System.currentTimeMillis()}",
                title = title,
                message = message,
                type = type,
                timestamp = System.currentTimeMillis()
            )
            repository.sendAppNotification(notif)
            _toastMessage.emit("Notification sent!")
        }
    }

    fun submitReport(
        category: String,
        title: String,
        description: String,
        contactInfo: String = "",
        incidentTime: String = "",
        relatedId: String = "",
        priority: String = "NORMAL",
        source: String = "MANUAL_FORM",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val rateLimit = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.REPORT_SUBMISSION)
        if (rateLimit is UserRateLimiter.RateLimitResult.Denied) {
            viewModelScope.launch { _toastMessage.emit(rateLimit.reason) }
            startActionCooldown("report_submission", rateLimit.waitSeconds)
            onResult(false, rateLimit.reason)
            return
        }
        viewModelScope.launch {
            val result = repository.submitUserReport(
                category = category,
                title = title,
                description = description,
                contactInfo = contactInfo,
                incidentTime = incidentTime,
                relatedId = relatedId,
                priority = priority,
                source = source
            )
            if (result.isSuccess) {
                val report = result.getOrNull()
                val ticketNo = report?.id?.takeLast(6) ?: "ACK"
                _toastMessage.emit("Report #$ticketNo submitted to Admin Support!")
                onResult(true, "Report submitted successfully! Ticket #$ticketNo")
            } else {
                val msg = result.exceptionOrNull()?.localizedMessage ?: "Failed to submit report"
                _toastMessage.emit(msg)
                onResult(false, msg)
            }
        }
    }

    fun cancelReport(reportId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.cancelUserReport(reportId)
            if (success) {
                _toastMessage.emit("Report cancelled successfully.")
            } else {
                _toastMessage.emit("Failed to cancel report.")
            }
            onResult(success)
        }
    }

    fun deleteReport(reportId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.deleteUserReport(reportId)
            if (success) {
                _toastMessage.emit("Report deleted.")
            } else {
                _toastMessage.emit("Failed to delete report.")
            }
            onResult(success)
        }
    }

    fun submitBannerSatisfaction(bannerId: String, reaction: String, feedbackNote: String = "", onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.submitBannerSatisfaction(bannerId, reaction, feedbackNote)
            if (res.isSuccess) {
                _toastMessage.emit("Thank you! Your feedback has been recorded.")
                onResult(true)
            } else {
                _toastMessage.emit("Failed to record feedback.")
                onResult(false)
            }
        }
    }

    fun saveTournament(tournament: Tournament, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.saveTournament(tournament)
            if (res.isSuccess) {
                _toastMessage.emit("Tournament published live to Cloud!")
                onResult(true)
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "Failed to save tournament"
                _toastMessage.emit(err)
                onResult(false)
            }
        }
    }

    fun deleteTournament(tournamentId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.deleteTournament(tournamentId)
            if (res.isSuccess) {
                _toastMessage.emit("Tournament removed from Cloud")
                onResult(true)
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete tournament"
                _toastMessage.emit(err)
                onResult(false)
            }
        }
    }

    override fun onCleared() {

        super.onCleared()
        // Clean up active listeners to prevent leaks and ghost background queries
        repository.stopRealtimeUserSync()
    }
}
