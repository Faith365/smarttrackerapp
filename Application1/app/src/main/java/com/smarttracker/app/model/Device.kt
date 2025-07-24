package com.smarttracker.app.model

import com.google.firebase.firestore.GeoPoint

data class Device(
    val id: String,
    val deviceName: String,
    val status: String,
    val lastKnownLocation: GeoPoint,
    val ownerId: String // 👈 Add this line
)

