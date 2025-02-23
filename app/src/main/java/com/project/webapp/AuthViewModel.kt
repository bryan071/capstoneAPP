package com.project.webapp

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class AuthViewModel : ViewModel() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableLiveData<AuthState>()
    val authState: MutableLiveData<AuthState> = _authState

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        if (auth.currentUser == null) {
            _authState.value = AuthState.UnAuthenticated
        } else {
            _authState.value = AuthState.Authenticated
        }
    }

    fun login (email: String, password: String) {

        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("You need to fill this.")
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value =
                        AuthState.Error(task.exception?.message ?: "Something went wrong.")
                }
            }

    }
    fun signup (email: String, password: String, firstname: String, lastname: String, address: String, contact: String, confirmpass: String) {

        if (email.isEmpty() || password.isEmpty() || firstname.isEmpty() || lastname.isEmpty()
            || address.isEmpty() || contact.isEmpty() || confirmpass.isEmpty()) {
            _authState.value = AuthState.Error("You need to fill this.")
            return
        }

        if (password != confirmpass){
            _authState.value = AuthState.Error("Password does not match.")
            return
        }

        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters long.")
            return
        }

        _authState.value = AuthState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid

                    if (userId != null) {
                        val user = hashMapOf(
                            "firstname" to firstname,
                            "lastname" to lastname,
                            "email" to email,
                            "address" to address,
                            "contact" to contact
                        )

                        // Store user data in Firestore
                        firestore.collection("users")
                            .document(userId)
                            .set(user)
                            .addOnSuccessListener {
                                Log.d("Register", "User data saved successfully")
                                _authState.value = AuthState.Authenticated
                            }
                            .addOnFailureListener { e ->
                                Log.e("Register", "Failed to save user data: ${e.message}")
                                _authState.value = AuthState.Error(e.message ?: "Failed to save user data.")
                            }
                    } else {
                        Log.e("Register", "User ID is null after registration")
                        _authState.value = AuthState.Error("User ID is null.")
                    }
                } else {
                    Log.e("Register", "Registration failed: ${task.exception?.message}")
                    _authState.value = AuthState.Error(task.exception?.message ?: "Something went wrong.")
                }
            }
    }
    fun logout() {

        auth.signOut()
        _authState.value = AuthState.UnAuthenticated
    }

    fun googleSignIn(credential: AuthCredential) {
        _authState.value = AuthState.Loading

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val userId = user.uid
                        val userData = hashMapOf(
                            "firstname" to (user.displayName ?: ""),
                            "email" to (user.email ?: ""),
                            "profilePicture" to (user.photoUrl?.toString() ?: "")
                        )

                        firestore.collection("users").document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d("GoogleSignIn", "User data saved successfully")
                                _authState.value = AuthState.Authenticated
                            }
                            .addOnFailureListener { e ->
                                Log.e("GoogleSignIn", "Failed to save user data: ${e.message}")
                                _authState.value = AuthState.Error(e.message ?: "Failed to save user data.")
                            }
                    } else {
                        _authState.value = AuthState.Error("User data is null after sign-in.")
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Google sign-in failed.")
                }
            }
    }

}
    sealed class AuthState {
        object Authenticated : AuthState()
        object UnAuthenticated : AuthState()
        object Loading : AuthState()
        data class Error(val message: String) : AuthState()
    }
