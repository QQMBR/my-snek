package com.example.mysnek

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Created")
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        setContentView(R.layout.activity_main)
    }

    override fun onDestroy() {
        Log.d(TAG, "Destroyed")

        super.onDestroy()
    }

    override fun onPause() {
        Log.d(TAG, "Paused")

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