package com.smarttracker.app.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.smarttracker.app.SecureStorage
import com.smarttracker.app.location.LocationService
import com.smarttracker.app.model.Device
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    firebaseUid: String,
    userEmail: String, // 🔄 Changed from userId to userEmail
    navController: NavHostController
) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }


    // Start the location service when screen is launched
    LaunchedEffect(Unit) {
        val intent = Intent(context, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    // Detect if device was added and show snackbar
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val deviceAdded = savedStateHandle?.get<Boolean>("device_added") == true
    LaunchedEffect(deviceAdded) {
        if (deviceAdded) {
            snackbarHostState.showSnackbar("Device added successfully!")
            savedStateHandle.remove<Boolean>("device_added")
        }
    }

    var isLoading by remember { mutableStateOf(true) }
    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    // ✅ Fetch user devices by email, not UID
    LocationPermissionHandler {
        LaunchedEffect(userEmail) {
            val locationManager = context.getSystemService(LocationManager::class.java)
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val status = if (isGpsEnabled) "online" else "offline"

            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val permissionGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                val location = if (permissionGranted) {
                    fusedLocationClient.lastLocation.await()
                } else {
                    null
                }

                val geoPoint = location?.let { GeoPoint(it.latitude, it.longitude) } ?: GeoPoint(0.0, 0.0)

                val deviceData = mapOf(
                    "status" to status,
                    "lastKnownLocation" to geoPoint
                )

                val ownedSnapshot = firestore.collection("devices")
                    .whereEqualTo("ownerId", userEmail)
                    .get()
                    .await()

                ownedSnapshot.documents.forEach { doc ->
                    doc.reference.update(deviceData)
                }

                val delegateSnapshot = firestore.collection("devices")
                    .whereArrayContains("delegates", userEmail)
                    .get()
                    .await()

                val allDevices = (ownedSnapshot.documents + delegateSnapshot.documents).distinctBy { it.id }

                val deviceList = allDevices.map { document ->
                    val deviceId = document.id
                    val deviceName = document.getString("deviceName") ?: "Unknown"
                    val statusUpdated = document.getString("status") ?: "Unknown"
                    val lastKnownLocation = document.getGeoPoint("lastKnownLocation") ?: GeoPoint(0.0, 0.0)
                    val ownerId = document.getString("ownerId") ?: "unknown"
                    Device(deviceId, deviceName, statusUpdated, lastKnownLocation, ownerId)
                }

                devices = deviceList
                isLoading = false
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
    }



    // UI Layout
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("settingsScreen/${firebaseUid}")

                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("addDeviceScreen") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Device")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Welcome to Home Screen", style = MaterialTheme.typography.titleLarge)

            if (isLoading) {
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator()
            } else if (error != null) {
                Text("Error loading devices: $error", color = MaterialTheme.colorScheme.error)
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your Devices:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                devices.forEach { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                navController.navigate("deviceDetails/${device.id}?isDelegate=${device.ownerId != userEmail}&userEmail=$userEmail")
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Device: ${device.deviceName}")
                            Text("Status: ${device.status}")
                            Text("Location: ${device.lastKnownLocation.latitude}, ${device.lastKnownLocation.longitude}")
                            Text(
                                if (device.ownerId == userEmail) "👑 You own this device"
                                else "👥 You’re a trusted contact",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                SecureStorage.clear(context)
                navController.popBackStack()
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Log Out")
            }



            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                navController.navigate("addDeviceScreen/$userEmail") // still passing email
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Device")
            }
        }
    }
}

//@Preview
//@Composable
//fun PreviewHomeScreen() {
//    HomeScreen(userEmail = "test@example.com", navController = rememberNavController())
//}
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionHandler(onPermissionGranted: @Composable () -> Unit) {
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    when {
        permissionState.status.isGranted -> {
            onPermissionGranted()
        }
        else -> {
            // Automatically trigger permission request once
            LaunchedEffect(Unit) {
                permissionState.launchPermissionRequest()
            }
            // Optional: Show some fallback UI while permission is being requested
            Text("Please grant location permission to continue.")
        }
    }
}
