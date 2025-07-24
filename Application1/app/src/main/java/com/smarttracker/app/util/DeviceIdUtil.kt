package com.smarttracker.app.util

import android.os.Build

object DeviceIdUtil {
    fun getDeviceIdentifier(): String {
        return Build.MODEL + "-" + Build.MANUFACTURER // You could make this more unique using a hashed Android ID
    }
}