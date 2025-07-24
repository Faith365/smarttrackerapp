package com.smarttracker.app.util

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

fun removeDeviceIdsFromUsers(context: Context, onComplete: () -> Unit, onError: (String) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("users")
        .get()
        .addOnSuccessListener { snapshot ->
            val users = snapshot.documents
            if (users.isEmpty()) {
                onComplete()
                return@addOnSuccessListener
            }

            var pending = users.size
            for (userDoc in users) {
                userDoc.reference.update("deviceIds", FieldValue.delete())
                    .addOnSuccessListener {
                        println("✅ Removed deviceIds from ${userDoc.id}")
                        pending--
                        if (pending == 0) onComplete()
                    }
                    .addOnFailureListener {
                        onError("❌ Failed for ${userDoc.id}: ${it.message}")
                    }
            }
        }
        .addOnFailureListener {
            onError("Failed to fetch users: ${it.message}")
        }
}
