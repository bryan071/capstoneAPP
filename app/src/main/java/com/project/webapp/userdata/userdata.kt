package com.project.webapp.userdata

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserData(
    val firstname: String = "",
    val lastname: String = "",
    val email: String = "",
    val address: String = "",
    val phoneNumber: String = "",
    val profilePicture: String = "",
    val userType: String = "",
    val productsListed: Int = 0,
    val salesCompleted: Int = 0
) : Parcelable
