package com.smarttracker.app.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(navController: NavController, userId: String) {
    val context = LocalContext.current

    // Used to access device location
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Firestore database instance
    val firestore = FirebaseFirestore.getInstance()

    // UI state
    var deviceName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Checking...") } // Automatically detected
    var isLoading by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var permissionRequested by remember { mutableStateOf(false) }

    // Handle runtime permission request
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            // Check if location services (GPS or Network) are actually enabled
            if (isLocationEnabled(context)) {
                status = "Online"
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    currentLocation = location
                }
            } else {
                status = "Offline"
                Toast.makeText(context, "Please enable location/GPS", Toast.LENGTH_SHORT).show()
            }
        } else {
            status = "Permission Denied"
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Run once when the screen appears
    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            if (isLocationEnabled(context)) {
                status = "Online"
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    currentLocation = location
                }
            } else {
                status = "Offline"
                Toast.makeText(context, "Please enable location services", Toast.LENGTH_SHORT).show()
            }
        } else if (!permissionRequested) {
            permissionRequested = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // UI layout
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Add New Device") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Device name input
            OutlinedTextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text("Device Name") },
                modifier = Modifier.fillMaxWidth()
            )

            // Show current auto-detected status
            Text("Status: $status")

            // Show current location (or loading)
            Text("Current Location: ${currentLocation?.latitude ?: "Loading..."}, ${currentLocation?.longitude ?: "..."}")

            // Save button
            Button(
                onClick = {
                    // Check required values before saving
                    if (deviceName.isNotBlank() && currentLocation != null) {
                        isLoading = true

                        // Data to store in Firestore
                        val device = hashMapOf(
                            "deviceName" to deviceName,
                            "status" to status,
                            "lastKnownLocation" to GeoPoint(currentLocation!!.latitude, currentLocation!!.longitude),
                            "ownerId" to userId,
                            "lastUpdated" to Timestamp.now() // ← ✅ NEW LINE
                        )


                        // Save to Firestore
                        firestore.collection("devices")
                            .add(device)
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Device added!", Toast.LENGTH_SHORT).show()
                                navController.previousBackStackEntry?.savedStateHandle?.set("device_added", true)
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(context, "Please enter device name and enable location", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Device")
                }
            }
        }
    }
}

// 🔍 Helper function to check if GPS or Network location services are on
fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}
