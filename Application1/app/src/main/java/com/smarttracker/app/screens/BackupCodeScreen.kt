package com.smarttracker.app.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun BackupCodeScreen(navController: NavController) {
    val context = LocalContext.current

    // State variables for user input and UI behavior
    var email by remember { mutableStateOf("") }
    var backupCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Screen title
        Text("Login Using Backup Code", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // Email input field
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Backup code input field
        TextField(
            value = backupCode,
            onValueChange = { backupCode = it },
            label = { Text("Backup Code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Error message display
        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Login button
        Button(
            onClick = {
                if (email.isNotEmpty() && backupCode.isNotEmpty()) {
                    isLoading = true
                    errorMessage = ""

                    val db = FirebaseFirestore.getInstance()

                    // Firestore query to find user with matching email and backup code
                    db.collection("users")
                        .whereEqualTo("email", email)
                        .whereEqualTo("backupCode", backupCode)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            isLoading = false
                            if (!querySnapshot.isEmpty) {
                                val userDoc = querySnapshot.documents.firstOrNull()
                                val userId = userDoc?.id ?: ""

                                Log.d("BackupCodeLogin", "User found: $userDoc")

                                // ✅ Navigate to HomeScreen WITH userId
                                navController.navigate("LimitedTrackingScreen/$userId")

                                Toast.makeText(
                                    context,
                                    "Logged in with backup code. Please reset your password from settings.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                errorMessage = "Invalid email or backup code"
                            }
                        }
                        .addOnFailureListener { exception ->
                            isLoading = false
                            errorMessage = "Error: ${exception.message}"
                        }
                } else {
                    errorMessage = "Please fill in both fields"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotEmpty() && backupCode.isNotEmpty() && !isLoading
        ) {
            Text("Login with Backup Code")
        }

        // Loading spinner if Firebase is working
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
