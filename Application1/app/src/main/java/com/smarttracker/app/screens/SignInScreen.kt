package com.smarttracker.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.smarttracker.app.MainActivity
import com.smarttracker.app.SecureStorage


@Composable
fun SignInScreen(
    signInUser: (String, String, (String, String) -> Unit) -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sign In", style = MaterialTheme.typography.titleLarge)

        // Email input
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage.isNotEmpty(),
            singleLine = true
        )

        // Password input
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = icon,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            isError = errorMessage.isNotEmpty(),
            singleLine = true
        )

        // Error message
        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }

        // Loading spinner
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        // Sign In button
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    errorMessage = ""

                    // Attempt sign-in
                    signInUser(email, password) { message, _ ->
                        isLoading = false
                        if (message == "Sign-in successful!") {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            val uid = currentUser?.uid ?: ""

                            if (uid.isNotEmpty()) {
                                SecureStorage.saveEmail(navController.context, email)
                                SecureStorage.savePassword(navController.context, password)

                                navController.navigate("homeScreen/$email/$uid") // ✅ Exact route match
                            } else {
                                errorMessage = "Failed to get user ID"
                            }
                        } else {
                            errorMessage = message
                        }
                    }
                } else {
                    errorMessage = "Please fill in both fields"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotEmpty() && password.isNotEmpty() && !isLoading
        ) {
            Text("Sign In")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Or use another sign-in method:", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Use Passkey (only show if email is not blank)
            if (email.isNotBlank()) {
                Button(onClick = {
                    (context as? MainActivity)?.startPasskeyLogin(email, navController)
                }) {
                    Text("Use Passkey")
                }
            }

//            Button(onClick = { navController.navigate("webauthn_demo") }) {
//                Text("Try WebAuthn")
//            }




            // Biometric login button
            Button(onClick = {
                (context as? MainActivity)?.startBiometricAuthentication(navController)
            }) {
                Text("Use Biometrics")
            }
        }

        // Navigate to sign-up
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = {
                navController.navigate("authScreen")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Don't have an account? Sign up")
        }

        // Navigate to backup code recovery
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = {
                navController.navigate("backupCodeScreen")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Forgot password? Use backup code")
        }
    }
}
