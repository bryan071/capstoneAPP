package com.project.webapp.Viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> get() = _authState


    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _authState.value = AuthState.UnAuthenticated
        } else {
            fetchUserType(userId)
        }
    }

    private fun fetchUserType(userId: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val userType = document.getString("userType") ?: "unknown"
                Log.d("AuthViewModel", "Fetched userType: $userType") // Debug log
                _authState.value = AuthState.Authenticated(userId, userType)
            }
            .addOnFailureListener {
                Log.e("AuthViewModel", "Failed to fetch user type: ${it.message}") // Debug log
                _authState.value = AuthState.Error("Failed to fetch user type.")
            }
    }


    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("You need to fill this.")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.uid?.let { uid ->
                    // Check user status before proceeding
                    checkUserStatus(uid) { status ->
                        if (status == "Disabled") {
                            _authState.value = AuthState.Error("Your account has been disabled. Please contact support.")
                        } else {
                            fetchUserType(uid)
                        }
                    }
                } ?: run { _authState.value = AuthState.Error("User ID is null.") }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Something went wrong.")
            }
        }
    }

    // Helper function to check user status from Firestore
    private suspend fun checkUserStatus(uid: String, callback: (String?) -> Unit) {
        try {
            val document = firestore.collection("users").document(uid).get().await()
            val status = document.getString("status")
            callback(status)
        } catch (e: Exception) {
            // If we can't fetch status, assume user is active and proceed
            callback("Active")
        }
    }

    fun signup(
        email: String,
        password: String,
        firstname: String,
        lastname: String,
        address: String,
        phoneNumber: String,
        userType: String,
        confirmpass: String,
        certificateUri: Uri?,
        context: Context
    ) {
        if (password != confirmpass) {
            _authState.postValue(AuthState.Error("Passwords do not match"))
            return
        }

        if (certificateUri == null) {
            _authState.postValue(AuthState.Error("Please upload a certificate or ID"))
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val storageRef = Firebase.storage.reference.child("certificates/$userId.jpg")

                    // Upload certificate
                    storageRef.putFile(certificateUri)
                        .continueWithTask { uploadTask ->
                            if (!uploadTask.isSuccessful) throw uploadTask.exception!!
                            storageRef.downloadUrl
                        }
                        .addOnSuccessListener { downloadUrl ->
                            val userMap = hashMapOf(
                                "firstname" to firstname,
                                "lastname" to lastname,
                                "email" to email,
                                "address" to address,
                                "phoneNumber" to phoneNumber,
                                "userType" to userType,
                                "certificateUrl" to downloadUrl.toString(),
                                "status" to "Pending",
                                "dateJoined" to Timestamp.now()
                            )


                            firestore.collection("users").document(userId).set(userMap)
                                .addOnSuccessListener {
                                    _authState.postValue(AuthState.Authenticated(userId, userType))
                                }
                                .addOnFailureListener {
                                    _authState.postValue(AuthState.Error("Firestore error: ${it.message}"))
                                }
                        }
                        .addOnFailureListener {
                            _authState.postValue(AuthState.Error("Certificate upload failed: ${it.message}"))
                        }

                } else {
                    _authState.postValue(
                        AuthState.Error(
                            task.exception?.message ?: "Signup failed"
                        )
                    )
                }
            }
    }


    fun logout() {
        try {
            FirebaseAuth.getInstance().signOut()
            _authState.value = AuthState.UnAuthenticated
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Logout error", e)
            _authState.value = AuthState.Error("Logout failed: ${e.message}")
        }
    }
}
sealed class AuthState {
    data class Authenticated(val userId: String, val userType: String) : AuthState()
    object UnAuthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}
