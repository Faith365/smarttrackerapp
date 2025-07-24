package com.smarttracker.app.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

fun requestDndPermission(context: Context) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (!notificationManager.isNotificationPolicyAccessGranted) {
        Toast.makeText(
            context,
            "Please allow DND access to override silent mode.",
            Toast.LENGTH_LONG
        ).show()

        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
