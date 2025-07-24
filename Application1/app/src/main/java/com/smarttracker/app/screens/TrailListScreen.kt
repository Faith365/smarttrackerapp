package com.smarttracker.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smarttracker.app.util.formatDateLabel

@Composable
fun TrailListScreen(navController: NavController, firebaseUid: String) {
    val firestore = FirebaseFirestore.getInstance()
    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
    val trailDates = remember { mutableStateListOf<String>() }

    LaunchedEffect(firebaseUid) {
        if (firebaseUid != null) {
            firestore.collection("devices")
                .document(firebaseUid)
                .collection("locationHistory")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val dates = snapshot.documents.mapNotNull { it.id }.sortedDescending()
                    trailDates.clear()
                    trailDates.addAll(dates)
                }
        }
    }

    // ✅ Scrollable full-screen container
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                "Available Trails",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(8.dp)
            )

            Text("Firebase UID: ${firebaseUid ?: "Not Logged In"}")
            Spacer(Modifier.height(16.dp))
        }

        if (trailDates.isEmpty()) {
            item {
                Text("No trail data available", color = MaterialTheme.colorScheme.error)
            }
        } else {
            items(trailDates) { date ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            navController.navigate("trail/${firebaseUid}/$date")
                        }
                ) {
                    Text(
                        text = formatDateLabel(date),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

}
