package com.project.webapp.datas

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false // ✅ Add this field to track read status
)
