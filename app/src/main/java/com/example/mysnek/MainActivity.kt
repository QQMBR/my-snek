package com.example.mysnek

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "switch_theme") {
            Log.d(TAG, "Switched theme")

            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Creating MainActivity")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        if (sharedPreferences.getBoolean("switch_theme", false)) {
            setTheme(R.style.DarkTheme)
        }

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroyed")

        super.onDestroy()
    }

    override fun onResume() {
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(listener)

        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "Paused")

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(listener)

        super.onPause()
    }

    override fun onStart() {
        Log.d(TAG, "Start")

        super.onStart()
    }

    override fun onRestart() {
        Log.d(TAG, "Restart")
        super.onRestart()
    }

    companion object {
        const val TAG = "MainActivity"
    }

}