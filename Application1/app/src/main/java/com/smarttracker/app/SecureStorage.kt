package com.smarttracker.app

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SecureStorage {

    private const val PREFS_NAME = "smarttracker_secure_prefs"
    private const val PREF_NAME = "user_preferences"
    private const val KEY_EMAIL = "email"
    private const val KEY_PASSWORD = "password"
    private const val KEY_BACKUP_CODE = "backup_code"

    fun save(context: Context, key: String, value: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit() { putString(key, value) }
    }

    fun get(context: Context, key: String): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(key, null)
    }

    fun clear(context: Context, key: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit() { remove(key) }
    }



    // Save email
    fun saveEmail(context: Context, email: String) {
        val sharedPref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_EMAIL, email)
            apply()
        }
    }

    // Retrieve email
    fun getEmail(context: Context): String? {
        val sharedPref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_EMAIL, null)
    }

    // Save password
    fun savePassword(context: Context, password: String) {
        val sharedPref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_PASSWORD, password)
            apply()
        }
    }

    // Retrieve password
    fun getPassword(context: Context): String? {
        val sharedPref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_PASSWORD, null)
    }


    fun saveBackupCode(context: Context, backupCode: String) {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_BACKUP_CODE, backupCode)
            apply()
        }
    }

    fun getBackupCode(context: Context): String? {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_BACKUP_CODE, null)
    }

    fun clear(context: Context) {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }
    }


}
