package com.project.webapp.Viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.project.webapp.datas.UserActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ActivityViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _activities = MutableStateFlow<List<UserActivity>>(emptyList())
    val activities: StateFlow<List<UserActivity>> = _activities
    private var listenerRegistration: ListenerRegistration? = null

    fun fetchActivities(userType: String, userId: String) {
        listenerRegistration?.remove()
        _activities.value = emptyList() // Clear previous data

        val collection = "activities" // Change to the correct collection name
        Log.d("ActivityViewModel", "Querying with userId: $userId, collection: $collection")

        listenerRegistration = db.collection(collection)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ActivityViewModel", "Error fetching activities", error)
                    _activities.value = emptyList()
                    return@addSnapshotListener
                }

                snapshot?.documents?.forEach {
                    Log.d("ActivityViewModel", "Document data: ${it.data}")
                }

                Log.d("ActivityViewModel", "Snapshot size: ${snapshot?.documents?.size}")

                val fetchedActivities = snapshot?.documents?.mapNotNull {
                    it.toObject(UserActivity::class.java)
                } ?: emptyList()

                _activities.value = fetchedActivities
                Log.d("ActivityViewModel", "Fetched ${fetchedActivities.size} activities")
            }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}