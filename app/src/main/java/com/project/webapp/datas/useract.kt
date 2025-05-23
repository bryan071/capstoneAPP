package com.project.webapp.datas

import com.google.firebase.Timestamp

data class UserActivity(
    val id: String = "",
    val userId: String = "",
    val description: String = "",
    val timestamp: Timestamp = Timestamp.now()  // Store as proper Timestamp
)
