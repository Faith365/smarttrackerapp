package com.smarttracker.app.screens

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 📧 Utility function to validate email format
fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTrustedContactsScreen(
    deviceId: String,
    navController: NavHostController
) {
    val firestore = FirebaseFirestore.getInstance()
    val snackbarHostState = remember { SnackbarHostState() } // Snackbar manager
    val context = LocalContext.current

    var delegates by remember { mutableStateOf<List<String>>(emptyList()) } // Current delegates
    var newEmail by remember { mutableStateOf("") } // Email input
    var showDialog by remember { mutableStateOf<Pair<Boolean, String?>>(false to null) } // For confirmation dialog

    val scope = rememberCoroutineScope()

    // 🔁 Load current delegates from Firestore when screen is first composed
    LaunchedEffect(deviceId) {
        val snapshot = firestore.collection("devices").document(deviceId).get().await()
        val list = snapshot.get("delegates") as? List<String> ?: emptyList()
        delegates = list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Trusted Contacts", fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 🔹 Email Input Section
            Text("Add Trusted Contact", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("Enter email address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    // ✅ Email Validation
                    if (!isValidEmail(newEmail)) {
                        scope.launch { snackbarHostState.showSnackbar("Invalid email address.") }
                        return@Button
                    }

                    if (delegates.contains(newEmail)) {
                        scope.launch { snackbarHostState.showSnackbar("This email is already a delegate.") }
                        return@Button
                    }

                    // ✅ Update Firestore
                    val updatedList = delegates + newEmail
                    firestore.collection("devices").document(deviceId)
                        .update("delegates", updatedList)
                        .addOnSuccessListener {
                            delegates = updatedList
                            newEmail = ""
                            scope.launch {
                                snackbarHostState.showSnackbar("Delegate added successfully.")
                            }
                        }
                        .addOnFailureListener {
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to add delegate.")
                            }
                        }
                },
                enabled = newEmail.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add")
            }

            Spacer(Modifier.height(24.dp))

            // 🔹 List of Current Delegates
            Text("Current Delegates", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            delegates.forEach { email ->
                // Animate appearance of delegate cards
                AnimatedVisibility(visible = true) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = email,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { showDialog = true to email },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Remove", color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }

        // 🧾 Confirmation dialog when removing delegate
        if (showDialog.first && showDialog.second != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false to null },
                title = { Text("Remove Delegate") },
                text = { Text("Are you sure you want to remove ${showDialog.second}?") },
                confirmButton = {
                    TextButton(onClick = {
                        val toRemove = showDialog.second!!
                        val updatedList = delegates.filterNot { it == toRemove }

                        firestore.collection("devices").document(deviceId)
                            .update("delegates", updatedList)
                            .addOnSuccessListener {
                                delegates = updatedList
                                scope.launch {
                                    snackbarHostState.showSnackbar("Delegate removed successfully.")
                                }
                            }
                            .addOnFailureListener {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to remove delegate.")
                                }
                            }

                        showDialog = false to null
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false to null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
