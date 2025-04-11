package com.project.webapp.components

import com.google.firebase.firestore.FirebaseFirestore

fun fetchOwnerName(firestore: FirebaseFirestore, ownerId: String, onResult: (String) -> Unit) {
    firestore.collection("users").document(ownerId)
        .get()
        .addOnSuccessListener { document ->
            val firstName = document.getString("firstname") ?: ""
            val lastName = document.getString("lastname") ?: ""

            val fullName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                "$firstName $lastName".trim()
            } else {
                document.getString("name") ?: document.getString("email") ?: "Unknown"
            }

            onResult(fullName)
        }
        .addOnFailureListener {
            onResult("Unknown")
        }
}