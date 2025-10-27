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
    private var isCurrentChatAdmin: Boolean = false
    private var messagesListener: ListenerRegistration? = null

    // Function to create or get admin chat room
    fun createOrGetAdminChatRoom(onChatRoomReady: (String) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) return

        // Create a consistent chat room ID for user-admin chat
        val adminChatRoomId = "admin_chat_$currentUserId"

        // Check if chat room already exists in adminChats collection
        firestore.collection("adminChats")
            .document(adminChatRoomId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    // Create new admin chat room in adminChats collection
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

                    firestore.collection("adminChats")
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

    // Modified setChatRoomId function to handle both collections
    fun setChatRoomId(chatRoomId: String, isAdminChat: Boolean = false) {
        // Clean up previous listener
        messagesListener?.remove()

        currentChatRoomId = chatRoomId
        isCurrentChatAdmin = isAdminChat

        // Determine which collection to use
        val collectionName = if (isAdminChat) "adminChats" else "chats"

        Log.d("ChatViewModel", "Setting up listener for $collectionName/$chatRoomId")

        // Listen to messages in the chat room
        messagesListener = firestore.collection(collectionName)
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
                Log.d("ChatViewModel", "Loaded ${messagesList.size} messages from $collectionName")
            }
    }

    // NEW: Send message to admin chat (adminChats collection)
    fun sendAdminMessage(messageText: String) {
        val currentUserId = auth.currentUser?.uid
        val chatRoomId = currentChatRoomId

        if (currentUserId == null || chatRoomId == null) {
            Log.e("ChatViewModel", "Cannot send admin message: userId=$currentUserId, chatRoomId=$chatRoomId")
            return
        }

        val senderName = auth.currentUser?.displayName ?: "User"
        val message = hashMapOf(
            "message" to messageText,
            "senderId" to currentUserId,
            "senderName" to senderName,
            "timestamp" to Timestamp.now(),
            "chatRoomId" to chatRoomId
        )

        Log.d("ChatViewModel", "Sending admin message to adminChats/$chatRoomId")

        // Add message to adminChats collection
        firestore.collection("adminChats")
            .document(chatRoomId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener { documentRef ->
                Log.d("ChatViewModel", "Admin message sent successfully")

                // Update chat room's last message info
                val updateData = hashMapOf<String, Any>(
                    "lastMessage" to messageText,
                    "lastMessageTime" to Timestamp.now(),
                    "lastSenderId" to currentUserId
                )

                // Update unread count for ADMIN
                updateData["unreadCount.ADMIN"] = FieldValue.increment(1)

                firestore.collection("adminChats")
                    .document(chatRoomId)
                    .update(updateData)
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error sending admin message", e)
            }
    }

    // Function to mark messages as read
    fun markMessagesAsRead(isAdminChat: Boolean = false) {
        val currentUserId = auth.currentUser?.uid
        val chatRoomId = currentChatRoomId

        if (currentUserId == null || chatRoomId == null) return

        val collectionName = if (isAdminChat) "adminChats" else "chats"

        firestore.collection(collectionName)
            .document(chatRoomId)
            .update("unreadCount.$currentUserId", 0)
            .addOnSuccessListener {
                Log.d("ChatViewModel", "Marked messages as read in $collectionName")
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error marking messages as read", e)
            }
    }

    // Function to get unread count for admin chats
    fun getAdminChatUnreadCount(onUnreadCount: (Int) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) return

        val adminChatRoomId = "admin_chat_$currentUserId"

        firestore.collection("adminChats")
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

    fun createOrGetTransactionChatRoom(
        user1Id: String,
        user2Id: String,
        notificationId: String,
        onChatRoomReady: (String) -> Unit
    ) {
        val sortedIds = listOf(user1Id, user2Id).sorted()
        val transactionChatRoomId = "transaction_chat_${sortedIds[0]}_${sortedIds[1]}_$notificationId"

        // Check if chat room already exists in chats collection
        firestore.collection("chats")
            .document(transactionChatRoomId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    // Create new transaction chat room
                    val chatRoomData = hashMapOf(
                        "id" to transactionChatRoomId,
                        "participants" to listOf(user1Id, user2Id),
                        "type" to "transaction_chat",
                        "notificationId" to notificationId,
                        "createdAt" to Timestamp.now(),
                        "createdBy" to FirebaseAuth.getInstance().currentUser?.uid,
                        "lastMessage" to "",
                        "lastMessageTime" to Timestamp.now(),
                        "unreadCount" to mapOf(
                            user1Id to 0,
                            user2Id to 0
                        )
                    )

                    firestore.collection("chats")
                        .document(transactionChatRoomId)
                        .set(chatRoomData)
                        .addOnSuccessListener {
                            onChatRoomReady(transactionChatRoomId)
                        }
                } else {
                    onChatRoomReady(transactionChatRoomId)
                }
            }
    }

    // Get chat room title for display
    fun getChatRoomTitle(chatRoomId: String, onTitleReady: (String) -> Unit) {
        val isAdminChat = chatRoomId.startsWith("admin_chat_")
        val collectionName = if (isAdminChat) "adminChats" else "chats"

        firestore.collection(collectionName)
            .document(chatRoomId)
            .get()
            .addOnSuccessListener { document ->
                val type = document.getString("type") ?: ""
                val participants = document.get("participants") as? List<String> ?: emptyList()

                if (type == "admin_support" || isAdminChat) {
                    onTitleReady("Admin Support")
                    return@addOnSuccessListener
                }

                val currentUserId = auth.currentUser?.uid
                val otherParticipant = participants.find { it != currentUserId } as? String ?: "User"

                // Fetch other participant's name
                firestore.collection("users").document(otherParticipant)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        val firstName = userDoc.getString("firstname") ?: ""
                        val lastName = userDoc.getString("lastname") ?: ""
                        val name = "$firstName $lastName".trim()
                        onTitleReady(if (name.isNotEmpty()) name else "User")
                    }
                    .addOnFailureListener {
                        onTitleReady("User")
                    }
            }
            .addOnFailureListener {
                onTitleReady("Chat")
            }
    }

    // Get the other participant's ID from chat room (for transaction chats only)
    fun getOtherParticipantId(chatRoomId: String, callback: (String) -> Unit) {
        Log.d("ChatViewModel", "Getting other participant for chatRoom: $chatRoomId")

        // Transaction chats are in "chats" collection
        firestore.collection("chats")
            .document(chatRoomId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.e("ChatViewModel", "Chat room document does not exist!")
                    callback("")
                    return@addOnSuccessListener
                }

                val participants = document.get("participants") as? List<String> ?: emptyList()
                Log.d("ChatViewModel", "Participants found: $participants")

                val currentUserId = auth.currentUser?.uid
                Log.d("ChatViewModel", "Current user ID: $currentUserId")

                val otherParticipant = participants.firstOrNull { it != currentUserId }
                Log.d("ChatViewModel", "Other participant: $otherParticipant")

                callback(otherParticipant ?: "")
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error getting other participant", e)
                callback("")
            }
    }

    // Send message to transaction chat (chats collection)
    fun sendMessageToParticipant(messageText: String, receiverId: String) {
        val currentUserId = auth.currentUser?.uid
        val chatRoomId = currentChatRoomId

        if (currentUserId == null || chatRoomId == null) {
            Log.e("ChatViewModel", "Cannot send message: userId=$currentUserId, chatRoomId=$chatRoomId")
            return
        }

        val senderName = auth.currentUser?.displayName ?: "User"
        val message = hashMapOf(
            "message" to messageText,
            "senderId" to currentUserId,
            "senderName" to senderName,
            "timestamp" to Timestamp.now(),
            "chatRoomId" to chatRoomId
        )

        Log.d("ChatViewModel", "Sending transaction message to chats/$chatRoomId")

        // Add message to chats collection
        firestore.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener { documentRef ->
                Log.d("ChatViewModel", "Transaction message sent successfully")

                // Update chat room
                val updateData = hashMapOf<String, Any>(
                    "lastMessage" to messageText,
                    "lastMessageTime" to Timestamp.now(),
                    "lastSenderId" to currentUserId
                )
                updateData["unreadCount.$receiverId"] = FieldValue.increment(1)

                firestore.collection("chats")
                    .document(chatRoomId)
                    .update(updateData)
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error sending transaction message", e)
            }
    }
}