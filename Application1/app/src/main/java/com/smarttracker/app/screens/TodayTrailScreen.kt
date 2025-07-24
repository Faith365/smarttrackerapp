package com.smarttracker.app.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.smarttracker.app.util.formatTimestamp

@Composable
fun TodayTrailScreen(
    navController: NavController,
    deviceId: String, // ✅ This is the tracked device's UID, not the current phone's
    date: String // Expected format: yyyyMMdd
) {
    val firestore = FirebaseFirestore.getInstance()

    // 🔁 UI States
    val trailPoints = remember { mutableStateListOf<LatLng>() }
    val rawDescriptions = remember { mutableStateListOf<String>() }
    val timestamps = remember { mutableStateListOf<Long>() }
    var loading by remember { mutableStateOf(true) }

    // 📡 Fetch trail data once on screen load
    LaunchedEffect(deviceId, date) {
        firestore.collection("devices")
            .document(deviceId)
            .collection("locationHistory")
            .document(date)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val rawRecords = document.get("records") as? List<Map<String, Any>>
                    rawRecords?.forEach { record ->
                        val lat = record["lat"] as? Double ?: return@forEach
                        val lng = record["lng"] as? Double ?: return@forEach
                        val timestamp = record["timestamp"] as? Timestamp
                        val formatted = formatTimestamp(timestamp)

                        val latLng = LatLng(lat, lng)
                        trailPoints.add(latLng)
                        rawDescriptions.add("Lat: $lat, Lng: $lng @ $formatted")
                        timestamps.add(timestamp?.toDate()?.time ?: 0L)
                    }

                    // ⏪ Reverse to show most recent first (optional)
                    trailPoints.reverse()
                    rawDescriptions.reverse()
                    timestamps.reverse()
                }
                loading = false
            }
            .addOnFailureListener {
                loading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Trail on $date", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        when {
            loading -> CircularProgressIndicator()

            trailPoints.isEmpty() -> Text("No trail data found for this day.")

            else -> {
                // 🎯 Center camera on the first point
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(
                        trailPoints.first(), 16f
                    )
                }

                // 🗺️ Map View
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    cameraPositionState = cameraPositionState
                ) {
                    // 📍 Polyline for trail
                    Polyline(
                        points = trailPoints,
                        color = Color.Blue,
                        width = 6f
                    )

                    // 🟢 Start Marker
                    Marker(
                        state = MarkerState(trailPoints.first()),
                        title = "Start",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )

                    // 🔴 End Marker
                    Marker(
                        state = MarkerState(trailPoints.last()),
                        title = "End",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )

                    // 🟡 Markers every 15 minutes
                    val interval = 15 * 60 * 1000L // 15 mins in ms
                    var lastTime = timestamps.firstOrNull() ?: 0L

                    for (i in 1 until trailPoints.lastIndex) {
                        val currentTime = timestamps[i]
                        if (currentTime - lastTime >= interval) {
                            Marker(
                                state = MarkerState(trailPoints[i]),
                                title = "Checkpoint",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                            )
                            lastTime = currentTime
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 📋 Trail Description List
                rawDescriptions.forEach { point ->
                    Text(point, modifier = Modifier.padding(4.dp))
                }

                Spacer(Modifier.height(16.dp))

                // 🔙 Back Button
                Button(onClick = { navController.popBackStack() }) {
                    Text("Back")
                }
            }
        }
    }
}
