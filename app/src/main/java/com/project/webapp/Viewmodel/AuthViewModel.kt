package com.project.webapp.Viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
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
                result.user?.uid?.let { fetchUserType(it) }
                    ?: run { _authState.value = AuthState.Error("User ID is null.") }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Something went wrong.")
            }
        }
    }

    fun signup(email: String, password: String, firstname: String, lastname: String, address: String, phoneNumber: String, userType: String, confirmpass: String) {
        if (password != confirmpass) {
            _authState.postValue(AuthState.Error("Passwords do not match"))
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val userMap = hashMapOf(
                        "firstname" to firstname,
                        "lastname" to lastname,
                        "email" to email,
                        "address" to address,
                        "phoneNumber" to phoneNumber,
                        "userType" to userType
                    )

                    firestore.collection("users").document(userId).set(userMap)
                        .addOnSuccessListener {
                            _authState.postValue(AuthState.Authenticated(userId, userType))
                        }
                        .addOnFailureListener {
                            _authState.postValue(AuthState.Error("Firestore error: ${it.message}"))
                        }
                } else {
                    _authState.postValue(AuthState.Error(task.exception?.message ?: "Signup failed"))
                }
            }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.UnAuthenticated
    }
}

sealed class AuthState {
    data class Authenticated(val userId: String, val userType: String) : AuthState()
    object UnAuthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}
