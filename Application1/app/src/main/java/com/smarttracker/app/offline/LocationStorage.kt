package com.smarttracker.app.offline

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Helper data class for serializing LatLng safely using Gson
data class LatLngDTO(val lat: Double, val lng: Double)

object LocationStorage {
    private const val PREF_NAME = "offline_location_storage"
    private const val KEY_HISTORY = "location_history"
    private val gson = Gson()

    /**
     * Save a new LatLng point into shared preferences.
     * Internally converts LatLng -> LatLngDTO (serializable).
     */
    fun saveLocation(context: Context, location: LatLng) {
        val prefs = getPrefs(context)

        // Load previously stored DTO points (safe serializable format)
        val list = loadLocationsDTO(context).toMutableList()

        // Add the new location to the list
        list.add(LatLngDTO(location.latitude, location.longitude))

        // Save the updated list back as JSON
        prefs.edit {
            putString(KEY_HISTORY, gson.toJson(list))
        }
    }

    /**
     * Loads and returns the **last 3 recorded** locations as LatLng objects.
     */
    fun loadLocations(context: Context): List<LatLng> {
        val all = loadLocationsDTO(context)

        // Return only the last 3 points if available
        val recent = all.takeLast(3)

        // Convert DTO -> LatLng to use with Google Maps
        return recent.map { LatLng(it.lat, it.lng) }
    }

    /**
     * Internal function to load all saved LatLngDTOs.
     */
    private fun loadLocationsDTO(context: Context): List<LatLngDTO> {
        val prefs = getPrefs(context)

        // Load the JSON string from preferences
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()

        // Define the type for Gson to deserialize a list of LatLngDTO
        val type = object : TypeToken<List<LatLngDTO>>() {}.type

        // Convert JSON -> List<LatLngDTO>
        return gson.fromJson(json, type)
    }

    /**
     * Clears all stored location data from preferences.
     */
    fun clearAllLocations(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit {
            remove(KEY_HISTORY)
        }
    }

    /**
     * Returns the SharedPreferences instance.
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}
