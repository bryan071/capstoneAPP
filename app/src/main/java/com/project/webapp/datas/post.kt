package com.project.webapp.datas

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val userId: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null // Change this from Long to Timestamp
)
