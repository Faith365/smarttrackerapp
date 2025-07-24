@file:OptIn(ExperimentalMaterial3Api::class)

package com.smarttracker.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun LimitedTrackingScreen(
    userId: String,
    firebaseUid: String, // 👈 this is the Firestore document ID
    navController: NavController
)
 {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Emergency Access") })
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Limited Tracking Access Enabled", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("User ID: $userId", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) // 👈 Added here
                Spacer(modifier = Modifier.height(20.dp))
                Text("You have limited access. Settings and profile editing are disabled.")

                Text(
                    "Emergency Mode: Some features are disabled for security.",
                    color = Color.Red
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    // Navigate back or log out if needed
                    navController.navigate("authScreen") {
                        popUpTo("authScreen") { inclusive = true }
                    }
                }) {
                    Text("Log Out")
                }

                Button(onClick = {
                    navController.navigate("trackingScreen/$firebaseUid?emergency=true")
                }) {
                    Text("Go to Tracking")
                }


                Button(
                    onClick = { /* navController.navigate("supportScreen") */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Text("Need Help?", color = Color.White)
                }


            }
        }
    )
}
