package com.smarttracker.app.screens



import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CleanupScreen(navController: NavController) {
    var isCleaning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🧹 Firestore Cleanup Utility",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                isCleaning = true
                cleanupInvalidHistoryDocs { result ->
                    statusMessage = result
                    isCleaning = false
                }
            },
            enabled = !isCleaning
        ) {
            Text("Delete Invalid History Docs")
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isCleaning) {
            CircularProgressIndicator()
        } else if (statusMessage.isNotEmpty()) {
            Text("✅ $statusMessage", color = MaterialTheme.colorScheme.primary)
        }
    }
}

fun cleanupInvalidHistoryDocs(onDone: (String) -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid == null) {
        onDone("❌ User not logged in.")
        return
    }

    val db = FirebaseFirestore.getInstance()
    val collection = db.collection("devices")
        .document(uid)
        .collection("locationHistory")

    collection.get().addOnSuccessListener { snapshot ->
        val docsToDelete = snapshot.documents.filterNot { doc ->
            doc.id.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) // Keep only date-formatted IDs
        }

        if (docsToDelete.isEmpty()) {
            onDone("No invalid documents found.")
            return@addOnSuccessListener
        }

        val batch = db.batch()
        for (doc in docsToDelete) {
            batch.delete(doc.reference)
            Log.d("Cleanup", "🗑 Deleted ${doc.id}")
        }

        batch.commit().addOnSuccessListener {
            onDone("Deleted ${docsToDelete.size} invalid documents.")
        }.addOnFailureListener {
            onDone("❌ Failed to delete documents: ${it.message}")
        }
    }.addOnFailureListener {
        onDone("❌ Failed to fetch documents: ${it.message}")
    }
}
