package com.smarttracker.app.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.smarttracker.app.util.cleanInvalidLocationHistoryDocuments
import com.smarttracker.app.util.removeDeviceIdsFromUsers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDevicesToUsersScreen() {
    val context = LocalContext.current
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sync Devices to Users") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("This will sync all existing devices to their owners by updating the `deviceIds` list in each user document.")

            Button(
                onClick = {
                    isSyncing = true
                    syncDevicesToUsers(
                        onComplete = {
                            isSyncing = false
                            syncStatus = "Device sync complete."
                            Toast.makeText(context, "Sync complete!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            isSyncing = false
                            syncStatus = "Error: $error"
                            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                enabled = !isSyncing
            ) {
                Text(if (isSyncing) "Syncing..." else "Run Sync")
            }

            Button(onClick = {
                removeDeviceIdsFromUsers(
                    context,
                    onComplete = {
                        Toast.makeText(context, "Removed deviceIds from all users.", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                )
            }) {
                Text("Remove deviceIds from Users")
            }

            Button(onClick = {
                cleanInvalidLocationHistoryDocuments(
                    firebaseUid = "4endHs8ipNfWU3DSy6j...", // Replace with real device ID
                    context = context,
                    onComplete = {
                        Toast.makeText(context, "Cleanup complete!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            }) {
                Text("Clean Invalid History Docs")
            }



            // 🔧 Fix old devices with missing ownerUid
            Button(onClick = {
                updateMissingOwnerUidsInDevices(context)
                syncStatus = "Started fixing missing ownerUid..."
            }) {
                Text("Fix Missing ownerUid")
            }

            syncStatus?.let {
                Text("Status: $it")
            }
        }
    }
}




// 🔁 One-time sync function
fun syncDevicesToUsers(onComplete: () -> Unit, onError: (String) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("devices")
        .get()
        .addOnSuccessListener { deviceSnapshot ->
            val userDeviceMap = mutableMapOf<String, MutableList<String>>()

            for (doc in deviceSnapshot) {
                val ownerUid = doc.getString("ownerUid") ?: continue
                val deviceId = doc.id

                userDeviceMap.getOrPut(ownerUid) { mutableListOf() }.add(deviceId)
            }

            var updatesRemaining = userDeviceMap.size
            if (updatesRemaining == 0) onComplete()

            for ((uid, deviceList) in userDeviceMap) {
                firestore.collection("users")
                    .document(uid)
                    .update("deviceIds", deviceList)
                    .addOnSuccessListener {
                        updatesRemaining--
                        if (updatesRemaining == 0) onComplete()
                    }
                    .addOnFailureListener { e ->
                        onError("Failed for $uid: ${e.message}")
                    }
            }
        }
        .addOnFailureListener { e ->
            onError("Failed to load devices: ${e.message}")
        }
}

fun updateMissingOwnerUidsInDevices(context: Context) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("devices")
        .get()
        .addOnSuccessListener { deviceSnapshot ->
            for (deviceDoc in deviceSnapshot) {
                val ownerUid = deviceDoc.getString("ownerUid")
                val ownerEmail = deviceDoc.getString("ownerId")

                if (ownerUid == null && ownerEmail != null) {
                    // Try to find the corresponding user by email
                    firestore.collection("users")
                        .whereEqualTo("email", ownerEmail)
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            if (!userSnapshot.isEmpty) {
                                val userDoc = userSnapshot.documents[0]
                                val correctUid = userDoc.id // Assuming doc ID is the Firebase UID

                                deviceDoc.reference.update("ownerUid", correctUid)
                                    .addOnSuccessListener {
                                        println("✅ Updated ownerUid for device ${deviceDoc.id}")
                                    }
                                    .addOnFailureListener {
                                        println("❌ Failed to update ownerUid for device ${deviceDoc.id}")
                                    }
                            } else {
                                println("⚠️ No matching user found for email: $ownerEmail")
                            }
                        }
                        .addOnFailureListener {
                            println("❌ Failed to fetch user for email: $ownerEmail")
                        }
                }
            }
        }
        .addOnFailureListener {
            println("❌ Failed to fetch devices: ${it.message}")
        }



}

