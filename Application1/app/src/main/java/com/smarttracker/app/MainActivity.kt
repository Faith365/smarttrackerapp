package com.smarttracker.app

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smarttracker.app.navigation.AppNavigation
import com.smarttracker.app.util.PasskeyHelper

class MainActivity : FragmentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        //Set Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        PasskeyHelper.init(applicationContext)



        // Set the UI content
        setContent {
            val navController = rememberNavController()


            AppNavigation(
                signUpUser = { email, password, fullName, phoneNumber, role, onSuccess ->
                    signUpUser(email, password, fullName, phoneNumber, role, onSuccess)
                },
                signInUser = { email, password, onResults ->
                    signInUser(email, password) { message, userId ->
                        onResults(message, userId) // ✅ Now both values are passed correctly
                    }

                },
                navController = navController
            )
        }
    }

    // Start biometric login
    fun startBiometricAuthentication(navController: NavController) {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)

                val email = SecureStorage.getEmail(applicationContext)
                val password = SecureStorage.getPassword(applicationContext)


                Log.d("Biometric", "Retrieved email: $email, password: ${password?.length}")

                if (email != null && password != null) {
                    // Re-sign in to get a fresh Firebase user session
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            val userId = authResult.user?.uid ?: return@addOnSuccessListener

                            // If your HomeScreen expects email:
                            navController.navigate("homeScreen/$email/$userId")


                            // But if your HomeScreen expects userId (recommended):
                            // navController.navigate("homeScreen/$userId")
                        }
                        .addOnFailureListener {
                            Toast.makeText(applicationContext, "Biometric login failed: ${it.message}", Toast.LENGTH_SHORT).show()
                            showRecoveryOptions(navController)
                        }
                } else {
                    showRecoveryOptions(navController)
                }
            }

            override fun onAuthenticationFailed() {
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Sign in with Biometrics")
            .setSubtitle("Use fingerprint or face to sign in")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }



    // Handles user sign-up
    fun signUpUser(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        role: String,
        onSuccess: (userId: String, backupCode: String) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val backupCode = if (phoneNumber.length >= 5) phoneNumber.substring(0, 5) else "00000"

        auth.fetchSignInMethodsForEmail(email)
            .addOnSuccessListener { result ->
                val signInMethods = result.signInMethods
                if (!signInMethods.isNullOrEmpty()) {
                    // 👤 Email already registered → Log them in
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            val userId = authResult.user?.uid ?: return@addOnSuccessListener
                            SecureStorage.saveEmail(this, email)
                            SecureStorage.savePassword(this, password)
                            SecureStorage.saveBackupCode(this, backupCode)
                            onSuccess(userId, backupCode)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // 🆕 Email is new → Register
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            val userId = authResult.user?.uid ?: return@addOnSuccessListener
                            val userMap = mapOf(
                                "email" to email,
                                "fullName" to fullName,
                                "phoneNumber" to phoneNumber,
                                "role" to role,
                                "backupCode" to backupCode
                            )
                            firestore.collection("users").document(userId).set(userMap)
                                .addOnSuccessListener {
                                    SecureStorage.saveEmail(this, email)
                                    SecureStorage.savePassword(this, password)
                                    SecureStorage.saveBackupCode(this, backupCode)
                                    onSuccess(userId, backupCode)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Registration failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error checking existing user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun startPasskeyLogin(email: String, navController: NavController) {
        val password = SecureStorage.getPassword(applicationContext)

        if (password != null) {
            signInUser(email, password) { message, userId ->
                if (userId.isNotEmpty()) {
                    navController.navigate("homeScreen/$email/$userId")
                } else {
                    Toast.makeText(this, "Login failed: $message", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "No saved password for $email", Toast.LENGTH_SHORT).show()
        }
    }





    // Biometric fails, offer recovery options
    private fun showRecoveryOptions(navController: NavController) {
        AlertDialog.Builder(this)
            .setTitle("Account Recovery")
            .setMessage("How would you like to recover your account?")
            .setPositiveButton("Use Backup Code") { _, _ ->
                navController.navigate("backupCodeScreen")
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()
    }

    // Sign in and save credentials
    // Sign-in user function
    private fun signInUser(email: String, password: String, onResults: (String, String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Save credentials
                    SecureStorage.saveEmail(this, email)
                    SecureStorage.savePassword(this, password)

                    // Return both a success message and the user ID
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    onResults("Sign-in successful!", userId)  // Two parameters
                } else {
                    // If sign-in fails, pass an error message and empty userId
                    task.exception?.let {
                        onResults("Sign-in failed: ${it.message ?: "Unknown error"}", "")
                    }
                }
            }

    }




}



