package com.smarttracker.app.screens

import android.app.Activity
import android.content.Context
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun WebAuthnFallbackScreen(navController: NavController) {
    val context = LocalContext.current
    val userId = remember { mutableStateOf("") }

    // Result launcher for FIDO2 intent
    val fallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Authentication successful", Toast.LENGTH_SHORT).show()
            // Navigate after successful passkey login
            navController.navigate("homeScreen/${userId.value}/dummyUid") // Replace dummyUid with real UID if needed
        } else {
            Toast.makeText(context, "Authentication failed or cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Enter your Email or User ID", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = userId.value,
            onValueChange = { userId.value = it },
            label = { Text("Email / User ID") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            val input = userId.value.trim()
            if (input.isBlank()) {
                Toast.makeText(context, "Please enter your email or user ID", Toast.LENGTH_SHORT).show()
                return@Button
            }

            // Step 1: Fetch credential ID from Firestore
            fetchCredentialIdFromFirestore(input) { credentialId ->
                if (credentialId == null) {
                    Toast.makeText(context, "No passkey registered for this user", Toast.LENGTH_SHORT).show()
                } else {
                    // Step 2: Launch FIDO2 authentication
                    authenticateWithPasskey(context, credentialId) { senderRequest ->
                        fallbackLauncher.launch(senderRequest)
                    }
                }
            }
        }) {
            Text("Use Passkey")
        }
    }
}

// 🔍 Fetch stored passkey credentialId from Firestore
fun fetchCredentialIdFromFirestore(userId: String, onResult: (String?) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(userId).get()
        .addOnSuccessListener { doc -> onResult(doc.getString("credentialId")) }
        .addOnFailureListener { onResult(null) }
}

// 🔐 Build and trigger FIDO2 sign-in intent
fun authenticateWithPasskey(
    context: Context,
    base64CredentialId: String,
    onRequestReady: (IntentSenderRequest) -> Unit
) {
    val fidoClient = Fido.getFido2ApiClient(context)

    // Match decode mode with registration: URL_SAFE + NO_WRAP
    val decodedId = Base64.decode(base64CredentialId, Base64.URL_SAFE or Base64.NO_WRAP)

    val options = PublicKeyCredentialRequestOptions.Builder()
        .setChallenge("secure-challenge".toByteArray()) // TODO: Replace with real server-generated challenge
        .setRpId("example.com") // Match this with the RP ID used during registration
        .setAllowList(
            listOf(
                PublicKeyCredentialDescriptor("public-key", decodedId, null)
            )
        )
        .build()

    fidoClient.getSignPendingIntent(options)
        .addOnSuccessListener { result ->
            val senderRequest = IntentSenderRequest.Builder(result.intentSender).build()
            onRequestReady(senderRequest)
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to launch passkey prompt", Toast.LENGTH_SHORT).show()
        }
}
