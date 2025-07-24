@file:Suppress("DEPRECATION")

package com.smarttracker.app.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

class NetworkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (isOnline(context)) {
            Log.d("NetworkReceiver", "📶 Device is online. Starting sync...")

            val firebaseUID = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val deviceId = "YOUR_DEVICE_ID_HERE" // ✅ Replace this dynamically or hardcode for now

            val syncIntent = Intent(context, LocationService::class.java)
            syncIntent.putExtra("sync_now", true)
            syncIntent.putExtra("firebase_uid", firebaseUID)
            syncIntent.putExtra("device_id", deviceId)
            context.startService(syncIntent)
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }
}
