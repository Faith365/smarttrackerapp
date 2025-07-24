package com.smarttracker.app.util

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smarttracker.app.model.LocationRecord
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun rememberTodayTrail(): State<List<LocationRecord>> {
    val records = remember { mutableStateOf<List<LocationRecord>>(emptyList()) }

    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        val firestore = FirebaseFirestore.getInstance()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val docRef = firestore.collection("devices")
            .document(userId)
            .collection("locationHistory")
            .document(today)

        try {
            val snapshot = docRef.get().await()
            val recordList = snapshot.get("records") as? List<Map<String, Any>> ?: emptyList()

            val parsed = recordList.mapNotNull { record ->
                val lat = (record["lat"] as? Double)
                val lng = (record["lng"] as? Double)
                val ts = (record["timestamp"] as? Long)
                if (lat != null && lng != null && ts != null) {
                    LocationRecord(lat, lng, ts)
                } else null
            }

            records.value = parsed
        } catch (e: Exception) {
            Log.e("Firestore", "Error reading trail: ${e.message}", e)
        }
    }

    return records
}
