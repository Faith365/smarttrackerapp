package com.smarttracker.app.screens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.smarttracker.app.databinding.ActivityHeatmapBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HeatmapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityHeatmapBinding
    private lateinit var map: GoogleMap
    private val db = FirebaseFirestore.getInstance()
    private val latLngs = mutableListOf<LatLng>()
    private val firebaseUid by lazy {
        intent.getStringExtra("firebaseUid") ?: "YOUR_FIREBASE_UID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeatmapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.heatmapMapView.onCreate(savedInstanceState)
        binding.heatmapMapView.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        fetchLocationData()
    }

    private fun fetchLocationData() {
        CoroutineScope(Dispatchers.Main).launch {
            db.collection("devices")
                .document(firebaseUid)
                .collection("locationHistory")
                .get()
                .addOnSuccessListener { snapshot ->
                    val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000

                    for (doc in snapshot.documents) {
                        val records = (doc.get("records") as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: continue
                        for (record in records) {
                            val lat = record["lat"] as? Double
                            val lng = record["lng"] as? Double
                            val timestamp = (record["timestamp"] as? Timestamp)?.toDate()?.time ?: continue

                            if (lat != null && lng != null && timestamp >= sevenDaysAgo) {
                                latLngs.add(LatLng(lat, lng))
                            }
                        }
                    }

                    if (latLngs.isNotEmpty()) {
                        renderHeatmapAndAutoZoom()
                    }
                }
        }
    }

    private fun renderHeatmapAndAutoZoom() {
        // 🟥 Show the heatmap overlay
        val provider = HeatmapTileProvider.Builder()
            .data(latLngs)
            .radius(40)
            .build()
        map.addTileOverlay(TileOverlayOptions().tileProvider(provider))

        // 🟩 Group locations by 4 decimal places (~11m accuracy) to count visit frequency
        val grouped = latLngs.groupBy {
            "%.4f,%.4f".format(it.latitude, it.longitude)
        }.mapValues { it.value.size }

        // 🟦 Find the most visited location
        val mostVisited = grouped.maxByOrNull { it.value }?.key?.split(",")?.map { it.toDouble() }

        val focusLocation = if (mostVisited != null && mostVisited.size == 2) {
            LatLng(mostVisited[0], mostVisited[1])
        } else {
            latLngs.first()
        }

        // ✅ Auto zoom after map finishes rendering
        map.setOnMapLoadedCallback {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(focusLocation, 17f))

            // 🏷️ Optional: show marker with visit count
            grouped.forEach { (key, count) ->
                val (lat, lng) = key.split(",").map { it.toDouble() }
                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(lat, lng))
                        .title("Visited $count time${if (count > 1) "s" else ""}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
            }
        }
    }

    // Required lifecycle methods for MapView
    override fun onResume() { super.onResume(); binding.heatmapMapView.onResume() }
    override fun onPause() { super.onPause(); binding.heatmapMapView.onPause() }
    override fun onDestroy() { super.onDestroy(); binding.heatmapMapView.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); binding.heatmapMapView.onLowMemory() }
}
