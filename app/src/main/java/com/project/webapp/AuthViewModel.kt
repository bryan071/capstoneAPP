package com.project.webapp

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
                _authState.value = AuthState.Authenticated(userId, userType)
            }
            .addOnFailureListener {
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

    fun signup(
        email: String, password: String, firstname: String, lastname: String,
        address: String, contact: String, confirmpass: String, userType: String
    ) {
        val errorMsg = validateSignupData(email, password, firstname, lastname, address, contact, confirmpass)
        if (errorMsg != null) {
            _authState.value = AuthState.Error(errorMsg)
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid ?: throw Exception("User ID is null.")

                val user = hashMapOf(
                    "firstname" to firstname,
                    "lastname" to lastname,
                    "email" to email,
                    "address" to address,
                    "contact" to contact,
                    "userType" to userType
                )

                firestore.collection("users").document(userId).set(user).await()
                _authState.value = AuthState.Authenticated(userId, userType)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Something went wrong.")
            }
        }
    }

    private fun validateSignupData(
        email: String, password: String, firstname: String,
        lastname: String, address: String, contact: String, confirmpass: String
    ): String? {
        return when {
            email.isEmpty() || password.isEmpty() || firstname.isEmpty() ||
                    lastname.isEmpty() || address.isEmpty() || contact.isEmpty() || confirmpass.isEmpty() ->
                "You need to fill this."
            password != confirmpass -> "Passwords do not match."
            password.length < 6 -> "Password must be at least 6 characters long."
            else -> null
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
