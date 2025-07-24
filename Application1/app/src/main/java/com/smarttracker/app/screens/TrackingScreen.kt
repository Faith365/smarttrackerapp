package com.smarttracker.app.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    firebaseUid: String,
    navController: NavController
) {
    val firestore = FirebaseFirestore.getInstance()
    val cameraPositionState = rememberCameraPositionState()

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var historyByDate by remember {
        mutableStateOf<Map<String, List<Pair<LatLng, String>>>>(emptyMap())
    }
    var totalDistanceKm by remember { mutableDoubleStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }

    // Load data when screen starts
    LaunchedEffect(firebaseUid) {
        val deviceRef = firestore.collection("devices").document(firebaseUid)
        val historyRef = deviceRef.collection("locationHistory")

        // 1. Try to get the current location from Firestore
        val deviceSnapshot = deviceRef.get().await()
        val lastKnown = deviceSnapshot.getGeoPoint("lastKnownLocation")
        val isValidLocation = lastKnown != null && !(lastKnown.latitude == 0.0 && lastKnown.longitude == 0.0)

        if (isValidLocation) {
            currentLocation = LatLng(lastKnown.latitude, lastKnown.longitude)
        }

        // 2. Fetch last 3 days' trails
        val today = LocalDate.now()
        val last3Dates = (0..2).map { today.minusDays(it.toLong()).toString() }

        val dateMap = mutableMapOf<String, List<Pair<LatLng, String>>>()

        for (date in last3Dates) {
            val doc = historyRef.document(date).get().await()

            val records = doc.get("records") as? List<Map<String, Any>> ?: continue

            val points = records.mapNotNull { entry ->
                val lat = entry["lat"] as? Double
                val lng = entry["lng"] as? Double
                val ts = entry["timestamp"] as? Timestamp
                if (lat != null && lng != null && ts != null) {
                    LatLng(lat, lng) to ts.toDate().toString()
                } else null
            }

            if (points.isNotEmpty()) {
                dateMap[date] = points
            }
        }

        historyByDate = dateMap

        // 3. Use the last point from trails as fallback for currentLocation
        if (currentLocation == null && dateMap.isNotEmpty()) {
            currentLocation = dateMap.values.flatten().lastOrNull()?.first
        }

        // 4. Zoom to current location
        currentLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 17f)
        }

        // 5. Flatten points and calculate total distance
        val allPoints = dateMap.values.flatten().map { it.first }
        totalDistanceKm = calculateTotalDistance(allPoints)

        isLoading = false
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Trail View (Last 3 Days)",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )

                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        cameraPositionState = cameraPositionState
                    ) {
                        // Assign unique colors per day
                        val colors = listOf(Color.Red, Color(0xFF8A2BE2), Color.Green)
                        val sortedDates = historyByDate.keys.sortedDescending()

                        sortedDates.forEachIndexed { index, date ->
                            val trail = historyByDate[date] ?: return@forEachIndexed

                            // Draw trail line
                            if (trail.size >= 2) {
                                Polyline(
                                    points = trail.map { it.first },
                                    color = colors[index % colors.size],
                                    width = 5f
                                )
                            }

                            // Add markers
                            trail.forEach { (latLng, time) ->
                                Marker(
                                    state = MarkerState(latLng),
                                    title = "Trail on $date",
                                    snippet = time,
                                    icon = BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_VIOLET
                                    )
                                )
                            }
                        }

                        // 🔵 Current device location
                        currentLocation?.let {
                            Marker(
                                state = MarkerState(it),
                                title = "Current Location",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                            )
                        }
                    }

                    Text(
                        text = "Total distance covered: %.2f km".format(totalDistanceKm),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp)
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    }
}

// Calculates total distance using Haversine formula
fun calculateTotalDistance(points: List<LatLng>): Double {
    var total = 0.0
    for (i in 0 until points.size - 1) {
        val start = points[i]
        val end = points[i + 1]

        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLng = Math.toRadians(end.longitude - start.longitude)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                sin(dLng / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        total += 6371.0 * c  // Earth's radius in km
    }
    return total
}
