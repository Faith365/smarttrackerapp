package com.smarttracker.app.util

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.firestore.GeoPoint
import java.util.Date

data class SafeLocationResult(
    val location: GeoPoint?,
    val timestamp: Date?
)

@SuppressLint("UseKtx")
fun saveLastLocation(context: Context, lat: Double, lng: Double, timestamp: Long) {
    val prefs = context.getSharedPreferences("last_location_cache", Context.MODE_PRIVATE)
    prefs.edit()
        .putLong("lat", lat.toRawBits())
        .putLong("lng", lng.toRawBits())
        .putLong("timestamp", timestamp)
        .apply()
}

fun loadLastLocation(context: Context): SafeLocationResult {
    val prefs = context.getSharedPreferences("last_location_cache", Context.MODE_PRIVATE)
    val latBits = prefs.getLong("lat", 0L)
    val lngBits = prefs.getLong("lng", 0L)
    val time = prefs.getLong("timestamp", 0L)

    return if (time > 0L) {
        SafeLocationResult(
            GeoPoint(Double.fromBits(latBits), Double.fromBits(lngBits)),
            Date(time)
        )
    } else {
        SafeLocationResult(null, null)
    }
}
