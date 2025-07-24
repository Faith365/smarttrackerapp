package com.smarttracker.app.screens

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.smarttracker.app.util.NetworkUtils
import com.smarttracker.app.util.getLocationStatusMessage
import com.smarttracker.app.util.getNetworkColor
import com.smarttracker.app.util.loadLastLocation
import com.smarttracker.app.util.requestDndPermission
import com.smarttracker.app.util.saveLastLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

var ringtone: Ringtone? = null
var firestoreListener: ListenerRegistration? = null

fun isOnline(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Suppress("DEPRECATION")
suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val result = geocoder.getFromLocation(lat, lng, 1)
            val address = result?.firstOrNull()

            if (address != null) {
                listOfNotNull(
                    address.featureName,
                    address.subLocality,
                    address.locality,
                    address.adminArea,
                    address.countryName
                ).distinct().joinToString(", ")
            } else {
                "Unknown Location"
            }
        } catch (_: Exception) {
            "Address not found"
        }
    }
}

// Make phone call
@SuppressLint("MissingPermission")
fun makeCall(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, "tel:$phoneNumber".toUri())
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

// Send SMS
@SuppressLint("MissingPermission")
fun sendText(context: Context, phoneNumber: String, message: String = "Ping from Tracker App") {
    val intent = Intent(Intent.ACTION_VIEW, "sms:$phoneNumber".toUri()).apply {
        putExtra("sms_body", message)
    }
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

// Get security status string based on online status and last seen
fun getSecurityStatus(isOnline: Boolean, lastSeen: Date?): String {
    if (isOnline && lastSeen != null) {
        val timeDiff = System.currentTimeMillis() - lastSeen.time
        return if (timeDiff < 5 * 60 * 1000) {
            "Trusted ✅"
        } else {
            "Unreachable 🟡"
        }
    }

    if (!isOnline && lastSeen != null) {
        val timeDiff = System.currentTimeMillis() - lastSeen.time
        return if (timeDiff < 30 * 60 * 1000) {
            "Unreachable 🟡"
        } else {
            "Lost 🔴"
        }
    }

    return "Unknown ⚠️"
}



@SuppressLint("ServiceCast", "ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DeviceDetailsScreen(
    deviceId: String,
    isDelegate: Boolean,
    userEmail: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid

    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var lastSeenAt by remember { mutableStateOf<Date?>(null) }
    var address by remember { mutableStateOf("Fetching address...") }
    var batteryStatus by remember { mutableStateOf("Battery: --%") }
    var error by remember { mutableStateOf<String?>(null) }
    var isRinging by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        requestDndPermission(context)
    }

    LaunchedEffect(deviceId) {
        try {
            firestoreListener?.remove()

            val firestore = FirebaseFirestore.getInstance()
            val deviceDoc = firestore.collection("devices").document(deviceId).get().await()
            val ownerId = deviceDoc.getString("ownerId")

            if (!ownerId.isNullOrEmpty()) {
                val userSnapshot = firestore.collection("users")
                    .whereEqualTo("email", ownerId)
                    .get()
                    .await()

                if (!userSnapshot.isEmpty) {
                    val userDoc = userSnapshot.documents[0]
                    phoneNumber = userDoc.getString("phoneNumber")
                }

                Log.d("DeviceDetails", "Phone number fetched: $phoneNumber")
            }

            firestoreListener = firestore.collection("devices")
                .document(deviceId)
                .addSnapshotListener { snapshot, _ ->
                    val shouldRing = snapshot?.getBoolean("ringCommand") == true

                    if (shouldRing && !isRinging) {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                            notificationManager.isNotificationPolicyAccessGranted
                        ) {
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_RING,
                                audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),
                                0
                            )
                            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        }

                        val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                        if (ringtone == null) {
                            ringtone = RingtoneManager.getRingtone(context, notification)
                        }
                        ringtone?.play()
                        isRinging = true
                    } else if (!shouldRing && isRinging) {
                        ringtone?.stop()
                        ringtone = null
                        isRinging = false
                    }
                }

            if (NetworkUtils.isOnline(context)) {
                val fetchedLocation = deviceDoc.getGeoPoint("lastKnownLocation")
                if (fetchedLocation != null && fetchedLocation.latitude != 0.0 && fetchedLocation.longitude != 0.0) {
                    currentLocation = fetchedLocation
                    val now = System.currentTimeMillis()
                    lastSeenAt = Date(now)
                    saveLastLocation(context, fetchedLocation.latitude, fetchedLocation.longitude, now)
                    address = reverseGeocode(context, fetchedLocation.latitude, fetchedLocation.longitude)
                } else {
                    val cached = loadLastLocation(context)
                    currentLocation = cached.location
                    lastSeenAt = cached.timestamp
                    address = reverseGeocode(context, cached.location!!.latitude, cached.location.longitude)
                }
            } else {
                val cached = loadLastLocation(context)
                currentLocation = cached.location
                lastSeenAt = cached.timestamp
                address = reverseGeocode(context, cached.location!!.latitude, cached.location.longitude)
            }

        } catch (e: Exception) {
            error = e.message
        }
    }

    val onlineStatus = isOnline(context)
    val hasValidLocation = currentLocation?.latitude != 0.0 && currentLocation?.longitude != 0.0
    val statusMessage = getLocationStatusMessage(hasValidLocation, onlineStatus)
    val badgeColor = getNetworkColor(onlineStatus)
    val securityStatus = getSecurityStatus(onlineStatus, lastSeenAt)

    if (isDelegate) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "🔒 You are viewing this device as a Trusted Contact.",
                color = Color(0xFF1976D2),
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .background(badgeColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        BatteryStatusReceiver { newStatus ->
            batteryStatus = newStatus
        }

        Text(if (onlineStatus) "🟢 ONLINE" else "🔵 OFFLINE", color = badgeColor, fontWeight = FontWeight.Bold)
        Text(statusMessage, color = Color.DarkGray)
        Spacer(Modifier.height(12.dp))

        DeviceAvatar()
        Spacer(Modifier.height(12.dp))

        Text("Device ID: $deviceId", style = MaterialTheme.typography.bodyLarge)
        Text("📞 Phone: ${phoneNumber ?: "Loading..."}")
        Text("🔐 Security Status: $securityStatus")
        Text("🔋 $batteryStatus")
        Text("🕒 Last Seen: ${lastSeenAt?.let { formatDate(it.time) } ?: "Unknown"}")
        Text("📍 Location: ${currentLocation?.latitude}, ${currentLocation?.longitude}")
        Text("🗺️ Address: $address")

        Spacer(Modifier.height(16.dp))

        currentLocation?.let {
            CurrentLocationMap(it, lastSeenAt, onlineStatus)
        } ?: Text("⚠️ No location data available", color = Color.Red)

        Spacer(Modifier.height(16.dp))

        Text("📞 Ping Device", fontWeight = FontWeight.Bold)
        if (!isDelegate) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { phoneNumber?.let { makeCall(context, it) } },
                    enabled = phoneNumber != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Call")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { phoneNumber?.let { sendText(context, it) } },
                    enabled = phoneNumber != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Text")
                }
            }
        } else {
            Text("🔒 Ping restricted for trusted contacts.", color = Color.Gray)
        }

        Spacer(Modifier.height(16.dp))

        Text("🔔 Ring Control", fontWeight = FontWeight.Bold)
        if (!isDelegate) {
            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        FirebaseFirestore.getInstance().collection("devices")
                            .document(deviceId).update("ringCommand", true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ring Device")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        FirebaseFirestore.getInstance().collection("devices")
                            .document(deviceId).update("ringCommand", false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop Ringing")
                }
            }
        } else {
            Text("🔒 Ringing control is for owners only.", color = Color.Gray)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "View Location History",
            modifier = Modifier
                .clickable { navController.navigate("trailList/${firebaseUid}") }
                .padding(4.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!isDelegate) {
            Button(
                onClick = {
                    navController.navigate("manageTrustedContacts/$deviceId")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949AB))
            ) {
                Text("👥 Manage Trusted Contacts", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }



        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }

    }
}



@Composable
fun CurrentLocationMap(location: GeoPoint, timestamp: Date?, isOnline: Boolean) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), 15f)
    }

    val markerTitle = if (isOnline) "Current Location" else "Last Known Location"
    val markerSnippet = "Last seen: ${timestamp?.let { formatDate(it.time) } ?: "Unknown"}"
    val markerColor = if (isOnline) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_BLUE

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(LatLng(location.latitude, location.longitude)),
            title = markerTitle,
            snippet = markerSnippet,
            icon = BitmapDescriptorFactory.defaultMarker(markerColor)
        )
    }
}

@Composable
fun BatteryStatusReceiver(onStatusUpdate: (String) -> Unit) {
    val context = LocalContext.current
    val batteryStatusReceiver = rememberUpdatedState(onStatusUpdate)

    DisposableEffect(Unit) {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                val batteryText = "Battery: $level%" + if (isCharging) " (Charging)" else ""
                batteryStatusReceiver.value(batteryText)
            }
        }

        context.registerReceiver(receiver, intentFilter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}

@Composable
fun DeviceAvatar() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(Color.LightGray, shape = RoundedCornerShape(40.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("📱", fontSize = 32.sp)
    }
}
