package com.project.webapp.datas

data class UserActivity(
    val id: String = "",
    val userId: String = "",
    val description: String = "",
    val timestamp: Any = System.currentTimeMillis()
) {
    fun getTimestampAsLong(): Long {
        return when (timestamp) {
            is Long -> timestamp as Long
            is com.google.firebase.Timestamp -> (timestamp as com.google.firebase.Timestamp).toDate().time
            else -> 0L
        }
    }
}