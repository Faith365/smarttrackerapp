package com.smarttracker.app.screens

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun SignInUpScreen(
    signUpUser: (
        email: String,
        password: String,
        fullName: String,
        phone: String,
        role: String,
        callback: (userId: String, backupCode: String) -> Unit
    ) -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val coroutineScope = rememberCoroutineScope()

    // State variables
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val fullName = remember { mutableStateOf("") }
    val phoneNumber = remember { mutableStateOf("") }
    val role = remember { mutableStateOf("") }
    val acceptedPrivacyPolicy = remember { mutableStateOf(false) }

    val showBackupDialog = remember { mutableStateOf(false) }
    val backupCode = remember { mutableStateOf("") }
    val userId = remember { mutableStateOf("") }
//    val startPasskeyRegistration = remember { mutableStateOf(false) }

    // ✅ Only start passkey registration after successful sign up
//    LaunchedEffect(startPasskeyRegistration.value, email.value, userId.value) {
//        val activity = context as? ComponentActivity
//        val emailSnapshot = email.value
//        val userIdSnapshot = userId.value
//
//        if (startPasskeyRegistration.value && activity != null && emailSnapshot.isNotBlank() && userIdSnapshot.isNotBlank()) {
//            Log.d("PASSKEY", "Starting passkey for $emailSnapshot, UID: $userIdSnapshot")
//
//            PasskeyHelper.registerPasskey(
//                activity = activity,
//                email = emailSnapshot,
//                onSuccess = { passkeyId ->
//                    Log.d("PASSKEY", "Passkey registered: $passkeyId")
//                    startPasskeyRegistration.value = false
//
//                    // Navigate to Home
//                    navController.navigate("homeScreen/$emailSnapshot/$userIdSnapshot")
//
//                    // Save to Firestore
//                    Firebase.firestore.collection("users")
//                        .document(userIdSnapshot)
//                        .set(mapOf("passkeyId" to passkeyId), SetOptions.merge())
//                        .addOnSuccessListener {
//                            Log.d("PASSKEY", "Passkey ID saved successfully")
//                        }
//                        .addOnFailureListener { e ->
//                            Log.e("PASSKEY", "Failed to save passkey ID", e)
//                        }
//                },
//                onError = { error ->
//                    Log.e("PASSKEY", "Passkey registration failed: $error")
//                    Toast.makeText(context, "Passkey failed: $error", Toast.LENGTH_LONG).show()
//                    startPasskeyRegistration.value = false
//                }
//            )
//        } else if (startPasskeyRegistration.value) {
//            Toast.makeText(context, "Passkey not available — try again later", Toast.LENGTH_SHORT).show()
//            startPasskeyRegistration.value = false
//        }
//    }



    Column(modifier = Modifier.padding(16.dp)) {
        TextField(value = email.value, onValueChange = { email.value = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = password.value, onValueChange = { password.value = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = fullName.value, onValueChange = { fullName.value = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = phoneNumber.value, onValueChange = { phoneNumber.value = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = role.value, onValueChange = { role.value = it }, label = { Text("Role") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Privacy policy checkbox
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Checkbox(checked = acceptedPrivacyPolicy.value, onCheckedChange = { acceptedPrivacyPolicy.value = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text("I accept the ", style = MaterialTheme.typography.bodySmall)
            Text(
                "Privacy Policy",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
                modifier = Modifier.clickable { navController.navigate("privacyPolicyScreen") }
            )
        }

        // ✅ Sign Up button
        Button(
            onClick = {
                if (
                    email.value.isNotBlank() &&
                    password.value.isNotBlank() &&
                    fullName.value.isNotBlank() &&
                    phoneNumber.value.isNotBlank() &&
                    role.value.isNotBlank()
                ) {
                    if (acceptedPrivacyPolicy.value) {
                        signUpUser(
                            email.value,
                            password.value,
                            fullName.value,
                            phoneNumber.value,
                            role.value
                        ) { id, code ->
                            userId.value = id
                            backupCode.value = code
                            showBackupDialog.value = true
//                            startPasskeyRegistration.value = true
                        }
                    } else {
                        Toast.makeText(context, "You must accept the Privacy Policy", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "All fields must be filled", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ✅ Navigate to Sign In
        Button(
            onClick = { navController.navigate("signInScreen") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign In")
        }
    }

    // ✅ Show backup code dialog
    if (showBackupDialog.value) {
        ShowBackupCodeDialog(
            backupCode = backupCode.value,
            onDismiss = {
                showBackupDialog.value = false
                // ✅ Navigate to the dedicated passkey screen after backup shown
                navController.navigate("createPasskeyScreen/${email.value}/${userId.value}")
            }
        )
    }

}
