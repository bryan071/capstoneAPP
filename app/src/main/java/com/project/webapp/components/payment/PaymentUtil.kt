package com.project.webapp.components.payment

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.datas.CartItem


// Helper function to update product inventory after purchase
fun updateProductInventory(productId: String, quantitySold: Int) {
    val firestore = FirebaseFirestore.getInstance()

    // Get current product data
    firestore.collection("products").document(productId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val currentQuantity = document.getLong("quantity")?.toInt() ?: 0
                val newQuantity = (currentQuantity - quantitySold).coerceAtLeast(0)

                // Update the quantity
                firestore.collection("products").document(productId)
                    .update("quantity", newQuantity)
                    .addOnSuccessListener {
                        Log.d("Inventory", "Product quantity updated for $productId")

                        // If quantity is now 0, mark as sold out or remove from active listings
                        if (newQuantity == 0) {
                            firestore.collection("products").document(productId)
                                .update("status", "sold_out")
                                .addOnSuccessListener {
                                    Log.d("Inventory", "Product marked as sold out: $productId")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Inventory", "Error updating product quantity", e)
                    }
            }
        }
}

