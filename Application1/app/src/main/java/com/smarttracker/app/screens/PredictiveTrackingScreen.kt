package com.smarttracker.app.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.heatmaps.HeatmapTileProvider
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// Data class to hold location with time
data class TimedLocation(val latLng: LatLng, val timestamp: Long)

@OptIn(MapsComposeExperimentalApi::class)
@SuppressLint("SimpleDateFormat")
@Composable
fun PredictiveTrackingScreen(navController: NavController, firebaseUid: String) {
    val db = FirebaseFirestore.getInstance()
    val heatmapProvider = remember { mutableStateOf<HeatmapTileProvider?>(null) }
    val heatmapOverlay = remember { mutableStateOf<TileOverlay?>(null) }
    val lastTwoLocations = remember { mutableStateListOf<TimedLocation>() }
    val allPoints = remember { mutableStateListOf<TimedLocation>() }
    val predictedLocation = remember { mutableStateOf<LatLng?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    val context = LocalContext.current

    // Fetch Firestore location history and process
    LaunchedEffect(firebaseUid) {
        try {
            val snapshot = db.collection("devices")
                .document(firebaseUid)
                .collection("locationHistory")
                .get().await()

            val rawPoints = mutableListOf<TimedLocation>()

            for (doc in snapshot.documents) {
                val records = (doc.get("records") as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: continue

                for (record in records) {
                    val lat = record["lat"] as? Double
                    val lng = record["lng"] as? Double
                    val timestamp = (record["timestamp"] as? Timestamp)?.toDate()?.time
                    if (lat != null && lng != null && timestamp != null) {
                        rawPoints.add(TimedLocation(LatLng(lat, lng), timestamp))
                    }
                }
            }

            val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
            val recent = rawPoints.filter { it.timestamp >= sevenDaysAgo }.sortedBy { it.timestamp }
            allPoints.clear(); allPoints.addAll(recent)

            if (recent.size >= 3) {
                val last3 = recent.takeLast(3)
                lastTwoLocations.clear(); lastTwoLocations.addAll(last3.takeLast(2))
                predictedLocation.value = simulatePredictionTimeWeighted(last3)

                predictedLocation.value?.let { pred ->
                    savePredictionToFirestore(db, firebaseUid, pred, System.currentTimeMillis())
                }
            }

            if (recent.isNotEmpty()) {
                heatmapProvider.value = HeatmapTileProvider.Builder()
                    .data(recent.map { it.latLng })
                    .radius(40)
                    .build()
            }
        } catch (e: Exception) {
            Log.e("Prediction", "Error fetching locations: ${e.message}")
        } finally {
            isLoading.value = false
        }
    }

    // Setup camera position
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            lastTwoLocations.lastOrNull()?.latLng ?: LatLng(-1.2921, 36.8219), 17f
        )
    }

    // Auto zoom to trail
    LaunchedEffect(allPoints.size) {
        if (allPoints.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            allPoints.forEach { boundsBuilder.include(it.latLng) }
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
        }
    }

    // === UI Layout ===
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Predicted Movement (AI Simulated)",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            if (isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                lastTwoLocations.forEachIndexed { i, location ->
                    Marker(
                        state = MarkerState(location.latLng),
                        title = "Location ${i + 1}",
                        snippet = "Time: ${timeFormatter.format(Date(location.timestamp))}"
                    )
                }

                Polyline(
                    points = allPoints.map { it.latLng },
                    color = Color.Blue,
                    width = 4f
                )

                predictedLocation.value?.let { pred ->
                    Marker(
                        state = MarkerState(pred),
                        title = "Predicted Next",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                    Circle(
                        center = pred,
                        radius = 20.0,
                        strokeColor = Color.Red,
                        fillColor = Color.Red.copy(alpha = 0.1f),
                        strokeWidth = 2f
                    )
                    lastTwoLocations.lastOrNull()?.let { last ->
                        Polyline(
                            points = listOf(last.latLng, pred),
                            color = Color.Green,
                            width = 5f,
                            pattern = listOf(Dot(), Gap(10f))
                        )
                    }
                }

                heatmapProvider.value?.let { provider ->
                    MapEffect(Unit) { googleMap ->
                        if (heatmapOverlay.value == null && heatmapProvider.value != null) {
                            heatmapOverlay.value = googleMap.addTileOverlay(
                                TileOverlayOptions().tileProvider(heatmapProvider.value!!)
                            )
                        }
                    }

                }
            }
        }

        // Bottom heatmap button
        Button(
            onClick = {
                val intent = Intent(context, HeatmapActivity::class.java)
                intent.putExtra("firebaseUid", firebaseUid) // ✅ use the parameter
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("View Full Heatmap", style = MaterialTheme.typography.titleMedium)
        }

    }
}

fun simulatePredictionTimeWeighted(points: List<TimedLocation>, predictSecondsAhead: Int = 10): LatLng? {
    if (points.size < 2) return null
    val p1 = points[points.size - 2]
    val p2 = points[points.size - 1]
    val timeDiffSec = (p2.timestamp - p1.timestamp) / 1000.0
    if (timeDiffSec == 0.0) return null

    val deltaLatPerSec = (p2.latLng.latitude - p1.latLng.latitude) / timeDiffSec
    val deltaLngPerSec = (p2.latLng.longitude - p1.latLng.longitude) / timeDiffSec

    val predictedLat = p2.latLng.latitude + (deltaLatPerSec * predictSecondsAhead)
    val predictedLng = p2.latLng.longitude + (deltaLngPerSec * predictSecondsAhead)
    return LatLng(predictedLat, predictedLng)
}

suspend fun savePredictionToFirestore(
    db: FirebaseFirestore,
    firebaseUid: String,
    prediction: LatLng,
    timestamp: Long
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val documentId = dateFormat.format(Date(timestamp))

    val predictionData = mapOf(
        "lat" to prediction.latitude,
        "lng" to prediction.longitude,
        "timestamp" to timestamp,
        "source" to "simulated"
    )

    try {
        db.collection("devices")
            .document(firebaseUid)
            .collection("predictions")
            .document(documentId)
            .set(predictionData)
            .await()
        Log.d("Prediction", "Saved prediction successfully")
    } catch (e: Exception) {
        Log.e("Prediction", "Failed to save prediction: ${e.message}")
    }
}
