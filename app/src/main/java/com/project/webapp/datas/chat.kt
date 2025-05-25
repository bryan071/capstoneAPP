package com.project.webapp.datas

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val message: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val chatRoomId: String = ""
)