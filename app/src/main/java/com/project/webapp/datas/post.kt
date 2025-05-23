package com.project.webapp.datas

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Post(
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImage: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val likes: Int = 0,
    val comments: Int = 0
) : Parcelable

