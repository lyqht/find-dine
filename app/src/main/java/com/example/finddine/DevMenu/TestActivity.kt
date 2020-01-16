package com.example.finddine.DevMenu

import android.os.Bundle
import com.example.finddine.R

import android.os.Handler
import android.util.Log

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import kotlin.concurrent.fixedRateTimer

class TestActivity : AppCompatActivity() {
    private val TAG = "TestActivity"

    // UI Elements.
    private lateinit var mUserLatLngTextView: TextView
    private lateinit var mNumOfUpdatesTextView: TextView


    // RTT Service
    private lateinit var wifiRttService: WifiRttService

    // Update lat long on interval
    private val mUpdateLatLngHandler = Handler()
    private var mMillisecondsDelayBeforeLatLngUpdate: Int = 0

    private var mNumOfUpdates: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        // Initializes UI elements.
        mUserLatLngTextView = findViewById(R.id.user_latlng_value)
        mNumOfUpdatesTextView = findViewById(R.id.num_updates_value)


        // Initialize wifiRttService
        wifiRttService = WifiRttService(this)

        intervalUpdateLatLng()
    }

    private fun updateUserLatLng() {
        val curUserLocation = wifiRttService.getUserLocation()
        Log.d(TAG, "curUserLocation: ${Arrays.toString(curUserLocation)}")

        mNumOfUpdates++

        mUserLatLngTextView.setText(Arrays.toString(curUserLocation))
        mNumOfUpdatesTextView.setText(mNumOfUpdates.toString())
        intervalUpdateLatLng()
    }

    private fun intervalUpdateLatLng() {
        mUpdateLatLngHandler.postDelayed(
            { updateUserLatLng() },
            mMillisecondsDelayBeforeLatLngUpdate.toLong()
        )
    }
}
