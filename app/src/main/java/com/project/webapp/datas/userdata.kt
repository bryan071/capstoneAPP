package com.project.webapp.datas

import android.os.Parcelable
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
    val productsListed: Int = 0,
    val salesCompleted: Int = 0,
    val createdAt: Long = System.currentTimeMillis() // Timestamp for user creation
) : Parcelable

