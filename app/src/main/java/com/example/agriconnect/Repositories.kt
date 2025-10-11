package com.example.agriconnect

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

// FarmRepository
class FarmRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    fun createFarm(farmName: String, address: String?, description: String?, onComplete: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onComplete(false)
            return
        }

        val farm = mapOf(
            "name" to farmName,
            "ownerId" to user.uid,
            "address" to address,
            "description" to description,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("farms")
            .add(farm)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
