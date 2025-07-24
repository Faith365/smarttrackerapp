package com.smarttracker.app.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.smarttracker.app.util.PasskeyHelper

@Composable
fun CreatePasskeyScreen(
    email: String,
    userId: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    Column(modifier = Modifier.padding(24.dp)) {
        Text("Set Up Passkey", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text("You're signing in as:")
        Text(email, style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (activity != null) {
                    PasskeyHelper.registerPasskey(
                        activity = activity,
                        email = email,
                        onSuccess = { passkeyId ->
                            Firebase.firestore.collection("users")
                                .document(userId)
                                .set(mapOf("passkeyId" to passkeyId), SetOptions.merge())
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Passkey registered!", Toast.LENGTH_SHORT).show()
                                    navController.navigate("homeScreen/$email/$userId") {
                                        popUpTo("createPasskeyScreen/$email/$userId") { inclusive = true }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Firestore save failed", Toast.LENGTH_LONG).show()
                                    Log.e("PASSKEY", "Firestore error: ", e)
                                }
                        },
                        onError = { error ->
                            Toast.makeText(context, "Failed: $error", Toast.LENGTH_LONG).show()
                            Log.e("PASSKEY", "Passkey creation failed: $error")
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Passkey")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Skip for now",
            modifier = Modifier
                .clickable {
                    navController.navigate("homeScreen/$email/$userId") {
                        popUpTo("createPasskeyScreen/$email/$userId") { inclusive = true }
                    }
                }
                .padding(8.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
