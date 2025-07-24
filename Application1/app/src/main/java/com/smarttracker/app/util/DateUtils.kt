// file: dateutils.kt
package com.smarttracker.app.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

fun formatDateLabel(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        if (date != null) {
            outputFormat.format(date)
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

fun formatTimestamp(timestamp: Any?): String {
    return try {
        val date = when (timestamp) {
            is Timestamp -> timestamp.toDate()
            is String -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(timestamp)
            else -> null
        }
        if (date != null) {
            SimpleDateFormat("hh:mm a, MMM d", Locale.getDefault()).format(date)
        } else {
            "Unknown Time"
        }
    } catch (e: Exception) {
        "Invalid Time"
    }
}
