package com.smarttracker.app.util

import androidx.compose.ui.graphics.Color

//fun isOnline(context: Context): Boolean {
//    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//    val network = cm.activeNetwork ?: return false
//    val capabilities = cm.getNetworkCapabilities(network) ?: return false
//    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//}

// Badge color based on online/offline
fun getNetworkColor(isOnline: Boolean): Color {
    return if (isOnline) Color(0xFF4CAF50) else Color(0xFF2196F3) // green or blue
}

// Status text based on location state
fun getLocationStatusMessage(hasValidLocation: Boolean, isOnline: Boolean): String {
    return if (!hasValidLocation) {
        "⚠️ Location is off or unavailable. Showing last recorded location."
    } else if (!isOnline) {
        "📡 You are offline. Location may not be updated."
    } else {
        "✅ Live location data."
    }
}
