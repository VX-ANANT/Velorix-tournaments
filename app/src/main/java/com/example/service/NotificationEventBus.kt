package com.example.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NotificationEventBus {
    data class NotificationEvent(val title: String, val body: String, val timestamp: Long = System.currentTimeMillis())
    
    private val _events = MutableSharedFlow<NotificationEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
    
    fun postEvent(title: String, body: String) {
        _events.tryEmit(NotificationEvent(title, body))
    }
}
