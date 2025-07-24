package com.smarttracker.app.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ResetPasswordScreen(email: String, navController: NavController) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var success by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                loading = false
                if (task.isSuccessful) {
                    success = true
                    Toast.makeText(context, "Reset email sent to $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to send reset email", Toast.LENGTH_LONG).show()
                }
            }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            loading -> CircularProgressIndicator()
            success -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Reset email sent to:")
                Text(email, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Back")
                }
            }
            else -> {
                Text("An error occurred.")
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Back")
                }
            }
        }
    }
}
