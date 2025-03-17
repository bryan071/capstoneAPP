package com.project.webapp.components

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.webapp.datas.ChatMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val chatRef = db.collection("chats")

    fun sendMessage(chatId: String, message: ChatMessage) {
        chatRef.document(chatId).collection("messages")
            .add(message)
            .addOnFailureListener { e -> Log.e("Chat", "Error sending message", e) }
    }

    fun getMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = chatRef.document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val messages = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
                trySend(messages).isSuccess
            }
        awaitClose { listener.remove() }
    }

    fun getActiveChats(): Flow<List<String>> = callbackFlow {
        val listener = chatRef.addSnapshotListener { snapshot, _ ->
            val chatRooms = snapshot?.documents?.map { it.id } ?: emptyList()
            trySend(chatRooms).isSuccess
        }
        awaitClose { listener.remove() }
    }
}

