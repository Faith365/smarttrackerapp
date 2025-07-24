package com.smarttracker.app.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

fun cleanInvalidLocationHistoryDocuments(
    firebaseUid: String,
    context: Context,
    onComplete: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val historyRef = db.collection("devices").document(firebaseUid).collection("locationHistory")

    historyRef.get().addOnSuccessListener { snapshot ->
        val invalidDocs = snapshot.documents.filter { doc ->
            // Keep only date-based document IDs (yyyy-MM-dd format)
            !doc.id.matches(Regex("""\d{4}-\d{2}-\d{2}"""))
        }

        if (invalidDocs.isEmpty()) {
            Toast.makeText(context, "No invalid documents to delete.", Toast.LENGTH_SHORT).show()
            onComplete()
            return@addOnSuccessListener
        }

        var pending = invalidDocs.size
        for (doc in invalidDocs) {
            doc.reference.delete().addOnSuccessListener {
                Log.d("FirestoreCleanup", "✅ Deleted ${doc.id}")
                pending--
                if (pending == 0) onComplete()
            }.addOnFailureListener {
                onError("❌ Failed to delete ${doc.id}: ${it.message}")
            }
        }
    }.addOnFailureListener {
        onError("Failed to fetch locationHistory: ${it.message}")
    }
}
