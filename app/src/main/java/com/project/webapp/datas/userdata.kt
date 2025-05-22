package com.project.webapp.datas

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserData(
    val uid: String = "",
    val firstname: String = "",
    val lastname: String = "",
    val email: String = "",
    val address: String = "",
    val phoneNumber: String = "",
    val profilePicture: String = "",
    val userType: String = "",
    val certificateUrl: String = "",
    val status: String = "",
    val dateJoined: Timestamp? = null
) : Parcelable

