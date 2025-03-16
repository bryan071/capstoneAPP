    package com.project.webapp

    import androidx.lifecycle.MutableLiveData
    import androidx.lifecycle.ViewModel
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.auth.FirebaseUser
    import com.google.firebase.firestore.SetOptions

    class AuthViewModel : ViewModel() {

        private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
        private val auth: FirebaseAuth = FirebaseAuth.getInstance()

        val currentUser: FirebaseUser?
            get() = auth.currentUser

        private val _authState = MutableLiveData<AuthState>()
        val authState: MutableLiveData<AuthState> = _authState

        init {
            checkAuthStatus()
        }

        fun checkAuthStatus() {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _authState.value = AuthState.UnAuthenticated
            } else {
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
        }


        fun login(email: String, password: String) {
            if (email.isEmpty() || password.isEmpty()) {
                _authState.value = AuthState.Error("You need to fill this.")
                return
            }

            _authState.value = AuthState.Loading

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            firestore.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener { document ->
                                    val userType = document.getString("userType") ?: "unknown"
                                    _authState.value = AuthState.Authenticated(userId, userType)
                                }
                                .addOnFailureListener {
                                    _authState.value = AuthState.Error("Failed to fetch user type.")
                                }
                        } else {
                            _authState.value = AuthState.Error("User ID is null.")
                        }
                    } else {
                        _authState.value = AuthState.Error(task.exception?.message ?: "Something went wrong.")
                    }
                }
        }


        fun signup(
            email: String, password: String, firstname: String, lastname: String,
            address: String, contact: String, confirmpass: String, userType: String
        ) {
            if (email.isEmpty() || password.isEmpty() || firstname.isEmpty() || lastname.isEmpty()
                || address.isEmpty() || contact.isEmpty() || confirmpass.isEmpty()
            ) {
                _authState.value = AuthState.Error("You need to fill this.")
                return
            }

            if (password != confirmpass) {
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
                                "contact" to contact,
                                "userType" to userType
                            )

                            firestore.collection("users")
                                .document(userId)
                                .set(user)
                                .addOnSuccessListener {
                                    _authState.value = AuthState.Authenticated(userId, userType)
                                }
                                .addOnFailureListener { e ->
                                    _authState.value = AuthState.Error(e.message ?: "Failed to save user data.")
                                }
                        } else {
                            _authState.value = AuthState.Error("User ID is null.")
                        }
                    } else {
                        _authState.value = AuthState.Error(task.exception?.message ?: "Something went wrong.")
                    }
                }
        }

        fun logout() {
            auth.signOut()
            _authState.value = AuthState.UnAuthenticated
        }

        fun getUserType(userId: String, onResult: (String) -> Unit) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val userType = document.getString("userType") ?: ""
                    onResult(userType)
                }
                .addOnFailureListener {
                    onResult("")
                }
        }
    }

    sealed class AuthState {
        data class Authenticated(val userId: String, val userType: String) : AuthState()
        object UnAuthenticated : AuthState()
        object Loading : AuthState()
        data class Error(val message: String) : AuthState()
    }


