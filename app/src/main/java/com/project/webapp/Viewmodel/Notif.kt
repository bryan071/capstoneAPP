package com.project.webapp.Viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotificationItem(
    val message: String,
    val timestamp: String
)

class NotificationViewModel : ViewModel() {
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications = _notifications.asStateFlow()

    fun addNotification(message: String) {
        _notifications.value = _notifications.value + NotificationItem(
            message = message,
            timestamp = getCurrentTime()
        )
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }
}
