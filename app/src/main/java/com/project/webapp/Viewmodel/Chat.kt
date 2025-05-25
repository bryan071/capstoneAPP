package com.project.webapp.Viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.project.webapp.components.ChatRepository
import com.project.webapp.datas.ChatMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID


class ChatViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var currentChatRoomId: String? = null
    private var messagesListener: ListenerRegistration? = null

    // Function to create or get admin chat room
    fun createOrGetAdminChatRoom(onChatRoomReady: (String) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) return

        // Create a consistent chat room ID for user-admin chat
        val adminChatRoomId = "admin_chat_$currentUserId"

        // Check if chat room already exists
        firestore.collection("chats")
            .document(adminChatRoomId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    // Create new admin chat room
                    val chatRoomData = hashMapOf(
                        "id" to adminChatRoomId,
                        "participants" to listOf(currentUserId, "ADMIN"),
                        "type" to "admin_support",
                        "createdAt" to Timestamp.now(),
                        "createdBy" to currentUserId,
                        "lastMessage" to "",
                        "lastMessageTime" to Timestamp.now(),
                        "unreadCount" to mapOf(
                            currentUserId to 0,
                            "ADMIN" to 0
                        )
                    )

                    firestore.collection("chats")
                        .document(adminChatRoomId)
                        .set(chatRoomData)
                        .addOnSuccessListener {
                            onChatRoomReady(adminChatRoomId)
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChatViewModel", "Error creating admin chat room", e)
                        }
                } else {
                    // Chat room already exists
                    onChatRoomReady(adminChatRoomId)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error checking admin chat room", e)
            }
    }

    // Modified setChatRoomId function
    fun setChatRoomId(chatRoomId: String) {
        // Clean up previous listener
        messagesListener?.remove()

        currentChatRoomId = chatRoomId

        // Listen to messages in the chat room
        messagesListener = firestore.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ChatViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val messagesList = mutableListOf<ChatMessage>()
                snapshot?.documents?.forEach { doc ->
                    try {
                        val message = ChatMessage(
                            id = doc.id,
                            message = doc.getString("message") ?: "",
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                            chatRoomId = chatRoomId
                        )
                        messagesList.add(message)
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Error parsing message", e)
                    }
                }
                _messages.value = messagesList
            }
    }

    // Modified sendMessage function to handle admin messages
    fun sendMessage(messageText: String, isAdmin: Boolean = false) {
        val currentUserId = auth.currentUser?.uid
        val chatRoomId = currentChatRoomId

        if (currentUserId == null || chatRoomId == null) return

        val senderId = if (isAdmin) "ADMIN" else currentUserId
        val senderName = if (isAdmin) "Admin" else (auth.currentUser?.displayName ?: "User")

        val message = hashMapOf(
            "message" to messageText,
            "senderId" to senderId,
            "senderName" to senderName,
            "timestamp" to Timestamp.now(),
            "chatRoomId" to chatRoomId
        )

        // Add message to chat room's messages subcollection
        firestore.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener { documentRef ->
                // Update chat room's last message info
                val updateData = hashMapOf<String, Any>(
                    "lastMessage" to messageText,
                    "lastMessageTime" to Timestamp.now(),
                    "lastSenderId" to senderId
                )

                // Update unread count for the other participant
                val otherParticipant = if (senderId == "ADMIN") currentUserId else "ADMIN"
                updateData["unreadCount.$otherParticipant"] = FieldValue.increment(1)

                firestore.collection("chats")
                    .document(chatRoomId)
                    .update(updateData)
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error sending message", e)
            }
    }

    // Function to mark messages as read
    fun markMessagesAsRead() {
        val currentUserId = auth.currentUser?.uid
        val chatRoomId = currentChatRoomId

        if (currentUserId == null || chatRoomId == null) return

        firestore.collection("chats")
            .document(chatRoomId)
            .update("unreadCount.$currentUserId", 0)
    }

    // Function to get unread count for admin chats
    fun getAdminChatUnreadCount(onUnreadCount: (Int) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) return

        val adminChatRoomId = "admin_chat_$currentUserId"

        firestore.collection("chats")
            .document(adminChatRoomId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Error listening to unread count", error)
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val unreadCount = document.get("unreadCount.$currentUserId") as? Long ?: 0
                    onUnreadCount(unreadCount.toInt())
                } else {
                    onUnreadCount(0)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
    }
}


