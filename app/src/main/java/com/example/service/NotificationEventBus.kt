package com.example.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NotificationEventBus {
    data class NotificationEvent(
        val title: String,
        val body: String,
        val timestamp: Long = System.currentTimeMillis(),
        val tournamentId: String? = null,
        val roomId: String? = null,
        val roomPassword: String? = null,
        val actionType: String? = null
    )
    
    private val _events = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
    
    fun postEvent(
        title: String,
        body: String,
        tournamentId: String? = null,
        roomId: String? = null,
        roomPassword: String? = null,
        actionType: String? = null
    ) {
        _events.tryEmit(
            NotificationEvent(
                title = title,
                body = body,
                tournamentId = tournamentId,
                roomId = roomId,
                roomPassword = roomPassword,
                actionType = actionType
            )
        )
    }
}
