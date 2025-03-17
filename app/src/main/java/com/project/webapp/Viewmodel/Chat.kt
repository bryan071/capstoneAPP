package com.project.webapp.Viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.project.webapp.components.ChatRepository
import com.project.webapp.datas.ChatMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val chatRepo = ChatRepository()
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> get() = _messages

    private val _unreadMessagesCount = MutableStateFlow(0)
    val unreadMessagesCount: StateFlow<Int> get() = _unreadMessagesCount

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val _chatRoomId = MutableStateFlow<String?>(null)

    fun setChatRoomId(chatRoomId: String) {
        _chatRoomId.value = chatRoomId
        loadMessages(chatRoomId)
    }

    private fun loadMessages(chatRoomId: String) {
        viewModelScope.launch {
            chatRepo.getMessages(chatRoomId).collect { messagesList ->
                _messages.value = messagesList
                _unreadMessagesCount.value = messagesList.count { it.senderId != currentUserId }
            }
        }
    }

    fun sendMessage(text: String, isAdmin: Boolean = false) {
        val chatRoomId = _chatRoomId.value ?: return
        val senderId = if (isAdmin) "ADMIN" else currentUserId  // Admin ID handling
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            message = text,
            timestamp = System.currentTimeMillis()
        )
        chatRepo.sendMessage(chatRoomId, msg)
    }

    fun markMessagesAsRead() {
        _unreadMessagesCount.value = 0
    }
}



