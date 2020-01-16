package com.example.finddine.DevMenu

import android.os.Bundle
import com.example.finddine.R

import android.os.Handler
import android.util.Log

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class TestActivity : AppCompatActivity() {
    private val TAG = "TestActivity"

    // UI Elements.
    private lateinit var mUserLatLngTextView: TextView
    private lateinit var mNumOfUpdatesTextView: TextView


    // RTT Service
    private lateinit var wifiRttService: WifiRttService

    // Update lat long on interval
    private val mUpdateLatLngHandler = Handler()
    private var mMillisecondsDelayBeforeLatLngUpdate: Int = 1000

    private var mNumOfUpdates: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        // Initializes UI elements.
        mUserLatLngTextView = findViewById(R.id.user_latlng_value)
        mNumOfUpdatesTextView = findViewById(R.id.num_updates_value)
    }

    private fun updateUserLatLng(curUserLocation: DoubleArray) {
        Log.d(TAG, "curUserLocation: ${Arrays.toString(curUserLocation)}")

        mNumOfUpdates++

        mUserLatLngTextView.setText(Arrays.toString(curUserLocation))
        mNumOfUpdatesTextView.setText(mNumOfUpdates.toString())
    }

    override fun onResume() {
        super.onResume()
        // Initialize wifiRttService
        wifiRttService = WifiRttService(this)
        wifiRttService.subscribeToUpdates(this::updateUserLatLng)
    }

    override fun onPause() {
        super.onPause()
        wifiRttService.stopWifiRttService()
    }

    override fun onStop() {
        super.onStop()
        wifiRttService.stopWifiRttService()
    }
}
