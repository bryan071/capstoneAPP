package com.project.webapp.components

import com.google.firebase.firestore.FirebaseFirestore

fun fetchOwnerName(firestore: FirebaseFirestore, ownerId: String, onResult: (String) -> Unit) {
    firestore.collection("users").document(ownerId)
        .get()
        .addOnSuccessListener { document ->
            val name = document.getString("name")
            val email = document.getString("email")
            onResult(name ?: email ?: "Unknown") // Prioritize name, fallback to email
        }
        .addOnFailureListener {
            onResult("Unknown")
        }
}