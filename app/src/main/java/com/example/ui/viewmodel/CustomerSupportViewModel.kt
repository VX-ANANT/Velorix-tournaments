package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiSupportService
import com.example.data.api.OwnerAlertManager
import com.example.ui.screens.ChatMessage
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class StoredChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val imageUriString: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val messages: List<StoredChatMessage> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

class CustomerSupportViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("velorix_gemini_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    private val rtdb by lazy {
        FirebaseDatabase.getInstance("https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app")
    }

    // RLS User ID binding
    private var activeUserId: String = "guest"

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String>("")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Rate Limiting States
    private val _cooldownSeconds = MutableStateFlow(0)
    val cooldownSeconds: StateFlow<Int> = _cooldownSeconds.asStateFlow()

    // Auto-clear on exit / self-backing (Enabled by default to guarantee zero data leakage between user screens)
    private val _autoClearOnExit = MutableStateFlow(true)
    val autoClearOnExit: StateFlow<Boolean> = _autoClearOnExit.asStateFlow()

    val customApiKey = MutableStateFlow(prefs.getString("custom_api_key", "") ?: "")
    val selectedModel = MutableStateFlow(prefs.getString("selected_model", "gemini-3.7-flash") ?: "gemini-3.7-flash")
    val useThinking = MutableStateFlow(prefs.getBoolean("use_thinking", false))

    // Rate Limiter tracking
    private val requestTimestamps = mutableListOf<Long>()
    private var lastRequestTime = 0L

    init {
        // Purge legacy non-isolated shared storage key so older shared queries never leak
        if (prefs.contains("saved_chat_sessions")) {
            prefs.edit().remove("saved_chat_sessions").remove("last_active_session_id").apply()
        }
        loadSessionsForUser("guest")
    }

    /**
     * RLS: Binds the active authenticated user ID to isolate chat and search history.
     * Guarantees that User A can NEVER view or access User B's search/chat history.
     */
    fun bindUser(userId: String?) {
        val targetUid = if (userId.isNullOrBlank()) "guest" else userId.trim()
        if (targetUid != activeUserId) {
            activeUserId = targetUid
            _autoClearOnExit.value = prefs.getBoolean("auto_clear_on_exit_$activeUserId", true)
            loadSessionsForUser(activeUserId)
        }
    }

    private fun getUserSessionsKey(uid: String) = "saved_chat_sessions_$uid"
    private fun getUserLastActiveKey(uid: String) = "last_active_session_id_$uid"

    private fun loadSessionsForUser(uid: String) {
        try {
            val sessionsJson = prefs.getString(getUserSessionsKey(uid), null)
            if (!sessionsJson.isNullOrBlank()) {
                val loadedList = json.decodeFromString<List<ChatSession>>(sessionsJson)
                if (loadedList.isNotEmpty()) {
                    _sessions.value = loadedList
                    val lastActiveId = prefs.getString(getUserLastActiveKey(uid), loadedList.first().id) ?: loadedList.first().id
                    val activeSession = loadedList.find { it.id == lastActiveId } ?: loadedList.first()
                    _currentSessionId.value = activeSession.id
                    _chatMessages.value = activeSession.messages.map { stored ->
                        ChatMessage(
                            id = stored.id,
                            text = stored.text,
                            isUser = stored.isUser,
                            imageUri = stored.imageUriString?.let { Uri.parse(it) },
                            timestamp = stored.timestamp
                        )
                    }
                    return
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomerSupportViewModel", "Failed to load sessions for user $uid", e)
        }

        // Default: Start a fresh new session
        startNewChat(saveImmediately = false)
    }

    fun setAutoClearOnExit(enabled: Boolean) {
        _autoClearOnExit.value = enabled
        prefs.edit().putBoolean("auto_clear_on_exit_$activeUserId", enabled).apply()
    }

    /**
     * Triggered on self-backing / leaving the Gemini search screen.
     * Clears active in-memory chat and starts a pristine fresh session if autoClear is enabled.
     */
    fun onScreenExit() {
        if (_autoClearOnExit.value) {
            _chatMessages.value = emptyList()
            _errorMessage.value = null
            startNewChat(saveImmediately = true)
        }
    }

    /**
     * Completely wipes all Gemini search & chat history for the current user ID.
     */
    fun clearAllHistoryForUser() {
        _chatMessages.value = emptyList()
        _sessions.value = emptyList()
        _errorMessage.value = null
        val uid = activeUserId
        prefs.edit()
            .remove(getUserSessionsKey(uid))
            .remove(getUserLastActiveKey(uid))
            .apply()
        
        if (uid != "guest") {
            try {
                rtdb.getReference("gemini_history").child(uid).removeValue()
                rtdb.getReference("ai_support_chats").child(uid).removeValue()
            } catch (e: Exception) {
                android.util.Log.e("CustomerSupportViewModel", "Failed clearing RTDB history for $uid", e)
            }
        }
        startNewChat(saveImmediately = false)
    }

    fun startNewChat(saveImmediately: Boolean = true) {
        val newSessionId = UUID.randomUUID().toString()
        val newSession = ChatSession(
            id = newSessionId,
            title = "New Chat",
            messages = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        _currentSessionId.value = newSessionId
        _chatMessages.value = emptyList()
        _sessions.value = listOf(newSession) + _sessions.value.filter { it.messages.isNotEmpty() }
        prefs.edit().putString(getUserLastActiveKey(activeUserId), newSessionId).apply()
        if (saveImmediately) {
            persistSessions()
        }
    }

    fun switchSession(sessionId: String) {
        val target = _sessions.value.find { it.id == sessionId } ?: return
        _currentSessionId.value = sessionId
        _chatMessages.value = target.messages.map { stored ->
            ChatMessage(
                id = stored.id,
                text = stored.text,
                isUser = stored.isUser,
                imageUri = stored.imageUriString?.let { Uri.parse(it) },
                timestamp = stored.timestamp
            )
        }
        prefs.edit().putString(getUserLastActiveKey(activeUserId), sessionId).apply()
    }

    fun deleteSession(sessionId: String) {
        val remaining = _sessions.value.filter { it.id != sessionId }
        _sessions.value = remaining
        if (_currentSessionId.value == sessionId) {
            if (remaining.isNotEmpty()) {
                switchSession(remaining.first().id)
            } else {
                startNewChat(saveImmediately = false)
            }
        }
        val uid = activeUserId
        if (uid != "guest") {
            try {
                rtdb.getReference("gemini_history").child(uid).child(sessionId).removeValue()
                rtdb.getReference("ai_support_chats").child(uid).child(sessionId).removeValue()
            } catch (e: Exception) {
                android.util.Log.e("CustomerSupportViewModel", "Failed deleting RTDB session $sessionId", e)
            }
        }
        persistSessions()
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
        val currentId = _currentSessionId.value
        val updated = _sessions.value.map { session ->
            if (session.id == currentId) session.copy(messages = emptyList(), title = "New Chat") else session
        }
        _sessions.value = updated
        persistSessions()
    }

    private fun persistSessions() {
        val uid = activeUserId
        try {
            val sessionsToSave = _sessions.value.filter { it.messages.isNotEmpty() }
            val raw = json.encodeToString(sessionsToSave)
            prefs.edit().putString(getUserSessionsKey(uid), raw).apply()

            // Also synchronize partitioned sessions to Firebase Realtime Database
            if (uid != "guest") {
                val rtdbPayload = sessionsToSave.associate { session ->
                    session.id to mapOf(
                        "id" to session.id,
                        "title" to session.title,
                        "updatedAt" to session.updatedAt,
                        "messages" to session.messages.associate { msg ->
                            msg.id to mapOf(
                                "id" to msg.id,
                                "text" to msg.text,
                                "isUser" to msg.isUser,
                                "timestamp" to msg.timestamp
                            )
                        }
                    )
                }
                rtdb.getReference("gemini_history").child(uid).setValue(rtdbPayload)
                rtdb.getReference("ai_support_chats").child(uid).setValue(rtdbPayload)
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomerSupportViewModel", "Failed persisting sessions for user $uid", e)
        }
    }

    fun saveApiKey(key: String) {
        val trimmed = key.trim()
        customApiKey.value = trimmed
        prefs.edit().putString("custom_api_key", trimmed).apply()
    }

    fun setModel(model: String) {
        selectedModel.value = model
        prefs.edit().putString("selected_model", model).apply()
        if (model == "gemini-3.1-pro-preview") {
            setThinking(true)
        } else {
            setThinking(false)
        }
    }

    fun setThinking(enabled: Boolean) {
        useThinking.value = enabled
        prefs.edit().putBoolean("use_thinking", enabled).apply()
    }

    fun sendMessage(userText: String, attachedImageUri: Uri? = null, userContextSummary: String? = null) {
        val trimmed = userText.trim()
        if ((trimmed.isEmpty() && attachedImageUri == null) || _isSending.value) return

        // 1. Rate Limiting Check
        val now = System.currentTimeMillis()
        
        // Cooldown between single requests (3 seconds)
        val timeSinceLast = now - lastRequestTime
        if (timeSinceLast < 3000L && lastRequestTime > 0) {
            val waitSec = ((3000L - timeSinceLast) / 1000L + 1).toInt()
            _errorMessage.value = "⏳ Rate limit: Please wait $waitSec seconds before sending another message."
            return
        }

        // Sliding window rate limit: Max 8 queries in 60 seconds per user
        requestTimestamps.removeAll { now - it > 60000L }
        if (requestTimestamps.size >= 8) {
            val oldest = requestTimestamps.first()
            val remainingSec = ((60000L - (now - oldest)) / 1000L + 1).toInt()
            _errorMessage.value = "⏳ Rate limit: Maximum 8 requests/minute reached. Please wait $remainingSec seconds."
            return
        }

        requestTimestamps.add(now)
        lastRequestTime = now

        // Trigger cooldown countdown timer in UI
        startCooldownCountdown(3)

        val userMessage = ChatMessage(
            text = if (trimmed.isEmpty()) "Attached screenshot for AI verification." else trimmed,
            isUser = true,
            imageUri = attachedImageUri
        )
        val updatedList = _chatMessages.value + userMessage
        _chatMessages.value = updatedList
        _isSending.value = true
        _errorMessage.value = null

        // Derive title if it's the first user message
        val currentTitle = if (_chatMessages.value.size <= 1) {
            if (trimmed.isNotBlank()) trimmed.take(28) + if (trimmed.length > 28) "..." else ""
            else "Screenshot Verification"
        } else {
            _sessions.value.find { it.id == _currentSessionId.value }?.title ?: "Chat"
        }

        updateCurrentSessionInList(updatedList, currentTitle)

        val historyPairs = updatedList.map { Pair(it.text, it.isUser) }

        viewModelScope.launch {
            try {
                val imageBase64 = attachedImageUri?.let { uri ->
                    withContext(Dispatchers.IO) { getBase64FromUri(uri) }
                }

                val promptToSend = if (trimmed.isEmpty()) {
                    "Please analyze this attached screenshot image and verify its details for my Velorix account."
                } else {
                    trimmed
                }

                // Owner auto alert for reports
                OwnerAlertManager.checkAndTriggerAutoAlert(
                    userMessage = promptToSend,
                    userContextSummary = userContextSummary
                )

                // Enforce Row-Level Security Context
                val rlsContext = (userContextSummary ?: "Active User: $activeUserId") + " [RLS Session Scope: $activeUserId]"

                val aiResponseText = GeminiSupportService.getResponse(
                    userMessage = promptToSend,
                    imageUriBase64 = imageBase64,
                    previousMessages = historyPairs,
                    customApiKey = customApiKey.value,
                    modelName = selectedModel.value,
                    useThinking = useThinking.value,
                    userContextSummary = rlsContext
                )

                val aiMessage = ChatMessage(text = aiResponseText, isUser = false)
                val finalList = _chatMessages.value + aiMessage
                _chatMessages.value = finalList
                updateCurrentSessionInList(finalList, currentTitle)
                persistSessions()
            } catch (e: Throwable) {
                _errorMessage.value = "Connection issue: ${e.localizedMessage ?: "Unknown error"}"
                val fallbackMsg = ChatMessage(
                    text = "I encountered a connection issue. Please verify your Gemini API key in Key Settings or try again shortly!",
                    isUser = false
                )
                val finalList = _chatMessages.value + fallbackMsg
                _chatMessages.value = finalList
                updateCurrentSessionInList(finalList, currentTitle)
                persistSessions()
            } finally {
                _isSending.value = false
            }
        }
    }

    private fun startCooldownCountdown(seconds: Int) {
        viewModelScope.launch {
            for (i in seconds downTo 1) {
                _cooldownSeconds.value = i
                kotlinx.coroutines.delay(1000L)
            }
            _cooldownSeconds.value = 0
        }
    }

    private fun updateCurrentSessionInList(messages: List<ChatMessage>, title: String) {
        val currentId = _currentSessionId.value
        val storedMessages = messages.map { msg ->
            StoredChatMessage(
                id = msg.id,
                text = msg.text,
                isUser = msg.isUser,
                imageUriString = msg.imageUri?.toString(),
                timestamp = msg.timestamp
            )
        }
        val currentSession = ChatSession(
            id = currentId,
            title = title,
            messages = storedMessages,
            updatedAt = System.currentTimeMillis()
        )
        val filtered = _sessions.value.filter { it.id != currentId }
        _sessions.value = listOf(currentSession) + filtered
    }

    private fun getBase64FromUri(uri: Uri): String? {
        return try {
            val resolver = getApplication<Application>().contentResolver
            val inputStream = resolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()

            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                val baos = java.io.ByteArrayOutputStream()
                val scaledBitmap = scaleDown(bitmap, 1024f)
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
                android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
            } else {
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            }
        } catch (e: Throwable) {
            android.util.Log.e("CustomerSupportViewModel", "Failed to encode image to Base64", e)
            null
        }
    }

    private fun scaleDown(realImage: android.graphics.Bitmap, maxImageSize: Float): android.graphics.Bitmap {
        val ratio = Math.min(
            maxImageSize / realImage.width,
            maxImageSize / realImage.height
        )
        if (ratio >= 1.0f) return realImage
        val width = Math.round(ratio * realImage.width)
        val height = Math.round(ratio * realImage.height)
        return android.graphics.Bitmap.createScaledBitmap(realImage, width, height, true)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

