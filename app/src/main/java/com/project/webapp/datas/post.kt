package com.project.webapp.datas

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Post(
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImage: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0,     // Using Long type as in your UI code
    val likes: Int = 0,
    val comments: Int = 0
) : Parcelable

