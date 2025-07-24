package com.smarttracker.app.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, firebaseUid: String) {
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    val db = FirebaseFirestore.getInstance()

    var userRole by remember { mutableStateOf("") }
    var devTapCount by remember { mutableIntStateOf(0) }
    var developerModeUnlocked by remember { mutableStateOf(false) }

    var passkeyEnabled by remember { mutableStateOf(false) }


    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Hold user profile data
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }

    // Track the loading state
    var isLoading by remember { mutableStateOf(true) }

    var isDarkMode by remember { mutableStateOf(false) }
    var showDeactivateDialog by remember { mutableStateOf(false) }

    // Function to load user profile info from Firestore
    fun loadUserProfile(uid: String?) {
        uid?.let {
            db.collection("users").document(it).get()
                .addOnSuccessListener { doc ->
                    fullName = doc.getString("fullName") ?: ""
                    email = doc.getString("email") ?: currentUser?.email ?: ""
                    photoUrl = currentUser?.photoUrl?.toString()
                    isLoading = false  // Set loading to false when data is loaded
                    userRole = doc.getString("role") ?: "User"

                    passkeyEnabled = doc.getBoolean("passkeyEnabled") == true

                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
                    isLoading = false  // Set loading to false even on failure to stop loading state
                }
        }


    }


    // Reload the user data when the user logs in or the screen is launched
    // In your LaunchedEffect, update the null safety handling of currentUser:
    LaunchedEffect(currentUser?.uid) {
        currentUser?.let { user ->
            if (isOnline(context)) {
                user.reload().addOnCompleteListener {
                    if (it.isSuccessful) {
                        loadUserProfile(user.uid)
                    } else {
                        Toast.makeText(context, "Failed to reload user data", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // If offline, just try loading the profile directly from Firestore (may be cached)
                loadUserProfile(user.uid)
                Toast.makeText(context, "Offline: using cached user data", Toast.LENGTH_SHORT).show()
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("homeScreen") {
                            popUpTo("settingsScreen") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Show a loading spinner while data is being fetched
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                // ==== Profile Section ====
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Display user's profile photo
                    if (photoUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(photoUrl),
                            contentDescription = "Profile photo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        // If no photo, display first letter of full name
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fullName.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Display user's full name and email
                    Column {
                        Text(fullName, style = MaterialTheme.typography.titleMedium)
                        Text(email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Account Settings", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()

                // ==== Predict Movement (AI) ====
                ListItem(
                    headlineContent = { Text("Predict Movement (AI)") },
                    leadingContent = { Icon(Icons.Default.SettingsBrightness, contentDescription = null) },
                    modifier = Modifier.clickable {
                        navController.navigate("predictiveTrackingScreen/${firebaseUid}")
                    }
                )
                HorizontalDivider()

                // ==== Passkey Setup ====
                ListItem(
                    headlineContent = { Text("Create Passkey") },
                    supportingContent = {
                        Text(
                            if (passkeyEnabled)
                                "Passkey already set up"
                            else
                                "Enable secure, passwordless login"
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.LockReset,
                            contentDescription = "Passkey Icon"
                        )
                    },
                    modifier = Modifier
                        .clickable {
                            if (!passkeyEnabled) {
                                navController.navigate("createPasskeyScreen/${Uri.encode(email)}/$firebaseUid")

                            } else {
                                Toast.makeText(context, "Passkey already enabled", Toast.LENGTH_SHORT).show()
                            }
                        }
                )
                HorizontalDivider()




                // ==== Reset Password ====
                ListItem(
                    headlineContent = { Text("Reset Password") },
                    supportingContent = { Text("Change your account password") },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.LockReset, contentDescription = "Reset Icon")
                    },
                    modifier = Modifier
                        .clickable {
                            if (email.isNotEmpty()) {
                                navController.navigate("resetPasswordScreen/${Uri.encode(email)}")
                            } else {
                                Toast.makeText(context, "Email not available", Toast.LENGTH_SHORT).show()
                            }
                        }
                )

                HorizontalDivider()

                // ==== Dark Mode ====
                ListItem(
                    headlineContent = { Text("Dark Theme") },
                    supportingContent = {
                        Text(if (isDarkMode) "Dark mode is enabled" else "Dark mode is disabled")
                    },
                    leadingContent = {
                        Icon(Icons.Default.SettingsBrightness, contentDescription = null)
                    },
                    modifier = Modifier
                        .clickable {
                            isDarkMode = !isDarkMode
                            Toast.makeText(
                                context,
                                if (isDarkMode) "Dark mode on (UI only)" else "Dark mode off",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                )
                HorizontalDivider()

                // ==== Privacy Policy Link ====
                ListItem(
                    headlineContent = { Text("Privacy Policy") },
                    supportingContent = { Text("Read our privacy practices") },
                    leadingContent = {
                        Icon(Icons.Default.SettingsBrightness, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        navController.navigate("privacyPolicyScreen")
                    }
                )
                HorizontalDivider()


                // ==== Deactivate Account ====
                ListItem(
                    headlineContent = { Text("Deactivate Account") },
                    supportingContent = { Text("Temporarily disable your account") },
                    leadingContent = {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                    },
                    modifier = Modifier
                        .clickable { showDeactivateDialog = true }
                )
                HorizontalDivider()

                // ==== 🔧 Developer Tools Button ====
                if (userRole == "Admin") {
                    ListItem(
                        headlineContent = { Text("Developer Tools") },
                        supportingContent = {
                            if (!developerModeUnlocked) {
                                Text("Tap 5 times to unlock")
                            } else {
                                Text("Developer mode is unlocked")
                            }
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.SettingsBrightness,
                                contentDescription = "Dev Tools"
                            )
                        },
                        modifier = Modifier.clickable {
                            if (!developerModeUnlocked) {
                                devTapCount++
                                if (devTapCount >= 5) {
                                    developerModeUnlocked = true
                                    Toast.makeText(context, "Developer mode unlocked!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Tap ${5 - devTapCount} more times to unlock", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                navController.navigate("syncDevicesScreen")
                            }
                        }
                    )
                }


            }
        }

        // ==== Deactivation Confirmation Dialog ====
        if (showDeactivateDialog) {
            AlertDialog(
                onDismissRequest = { showDeactivateDialog = false },
                title = { Text("Confirm Deactivation") },
                text = { Text("Are you sure you want to deactivate your account?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeactivateDialog = false
                        Toast.makeText(context, "Deactivated (demo only)", Toast.LENGTH_SHORT).show()
                        navController.navigate("homeScreen") {
                            popUpTo("settingsScreen") { inclusive = true }
                        }
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeactivateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
