package com.project.webapp.datas

import com.google.firebase.Timestamp

data class Announcement(
    val title: String = "",
    val message: String = "",
    val audience: String = "",
    val status: String = "",
    val date: Timestamp? = null,
    val dateSent: Timestamp? = null,
    val id: String? = null
)

