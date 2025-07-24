package com.smarttracker.app.location  // Make sure this matches your app's package name

import android.Manifest
import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    Log.d("LocationService", "Location: ${location.latitude}, ${location.longitude}")
                    uploadLocationToFirestore(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun uploadLocationToFirestore(location: Location) {
        val deviceId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()
        val geoPoint = GeoPoint(location.latitude, location.longitude)

        // 👣 Update current location separately
        val lastLocationData = mapOf(
            "timestamp" to Timestamp.now(),
            "location" to geoPoint
        )

        firestore.collection("devices")
            .document(deviceId)
            .update("currentLocation", lastLocationData)
            .addOnFailureListener {
                firestore.collection("devices")
                    .document(deviceId)
                    .set(mapOf("currentLocation" to lastLocationData))
            }

        // 📆 Today's document name
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val historyRef = firestore.collection("devices")
            .document(deviceId)
            .collection("locationHistory")
            .document(today)

        val newEntry = mapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "timestamp" to Timestamp.now()
        )

        // 🧠 Use a transaction to append to the records array
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(historyRef)
            val existing = snapshot.get("records") as? List<Map<String, Any>> ?: emptyList()
            val updated = existing + newEntry

            transaction.set(historyRef, mapOf("records" to updated))
        }.addOnSuccessListener {
            Log.d("LocationService", "✅ Location saved under $today")
        }.addOnFailureListener {
            Log.e("LocationService", "❌ Failed to save location: ${it.message}", it)
        }
    }





    private fun createNotification(): Notification {
        val channelId = "location_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracking location")
            .setContentText("Your location is being tracked in background.")
            .setSmallIcon(R.drawable.ic_menu_mylocation)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
