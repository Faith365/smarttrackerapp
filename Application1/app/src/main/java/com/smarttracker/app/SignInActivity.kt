package com.smarttracker.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.smarttracker.app.screens.SignInScreen

class SignInActivity : ComponentActivity() {

    // Declare FirebaseAuth instance
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Set up the Composable UI content
        setContent {
            // Initialize navigation controller for handling screen transitions
            val navController = rememberNavController()

            // Pass the signInUser function and navController to the SignInScreen Composable
            SignInScreen(
                signInUser = { email, password, navCtrl ->
                    signInUser(email, password, navCtrl as NavHostController)
                },
                navController = navController
            )
        }
    }

    // Sign-in logic: Authenticates the user using email and password
    private fun signInUser(email: String, password: String, navController: NavHostController) {
        // Attempt to sign in with email and password using Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign-in successful, display a toast message and navigate to home screen
                    val user = auth.currentUser
                    Toast.makeText(applicationContext, "Sign-in successful!", Toast.LENGTH_SHORT).show()
                    println("Sign-in successful: User ID = ${user?.uid}")

                    // Navigate to HomeScreen after successful sign-in
                    navController.navigate("homeScreen")  // Navigate to the home screen
                } else {
                    // Handle sign-in failure and show an error message
                    Toast.makeText(applicationContext, "Sign-in failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    println("Sign-in failed: ${task.exception?.message}")
                }
            }
    }
}

// Composable function for the Sign-In screen
@Composable
fun SignInScreen(
    signInUser: (String, String, NavHostController) -> Unit, // Function that takes email, password, and NavHostController
    navController: NavHostController // Navigation controller to handle screen transitions
) {
    // State variables to hold email, password, and error message
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf("") }

    // UI structure for the SignIn screen
    Column {
        // Email input field
        TextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Email") }
        )

        // Password input field
        TextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password") }
        )

        // Show error message if present
        if (errorMessage.value.isNotEmpty()) {
            Text(text = errorMessage.value, color = Color.Red)
        }

        // Sign-in button
        Button(onClick = {
            // Check if email and password are not empty
            if (email.value.isEmpty() || password.value.isEmpty()) {
                errorMessage.value = "Email and password are required" // Show error if fields are empty
            } else {
                errorMessage.value = "" // Clear error message if fields are valid
                // Call signInUser function with email, password, and navController
                signInUser(email.value, password.value, navController)
            }
        }) {
            Text("Sign In") // Button text
        }
    }
}
