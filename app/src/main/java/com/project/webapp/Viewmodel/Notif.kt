package com.project.webapp.Viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

data class NotificationItem(
    val id: String = "",
    val type: String? = null,
    val name: String? = null,
    val price: Double? = null,
    val quantity: Int? = null,
    val quantityUnit: String? = null,
    val imageUrl: String? = null,
    val message: String? = null,
    val timestamp: com.google.firebase.Timestamp? = null,
    val userId: String? = null,
    val buyerId: String? = null,
    val sellerId: String? = null,
    val orderStatus: String? = null,
    val paymentStatus: String? = null,
    val orderId: String? = null,
    val transactionId: String? = null,
    val organizationName: String? = null,
    val category: String? = null,
    val location: String? = null,
    val paymentMethod: String? = null,
    val deliveryAddress: String? = null
)

class NotificationViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _isLoading.value = false
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<NotificationItem>()?.copy(id = doc.id)
                    } ?: emptyList()
                    _notifications.value = list
                    _isLoading.value = false
                }
        } ?: run { _isLoading.value = false }
    }
}