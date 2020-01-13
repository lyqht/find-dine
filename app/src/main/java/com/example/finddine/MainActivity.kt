package com.example.finddine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResultCallback
import android.net.wifi.rtt.WifiRttManager
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        val filter = IntentFilter(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED)
        val rttManager = getSystemService(Context.WIFI_RTT_RANGING_SERVICE) as WifiRttManager

        //Make sure that you register the broadcast receiver before checking availability.
        // Otherwise, there could be a period of time when the app thinks that
        // Wi-Fi RTT is available but isn't notified if availability changes.

//        val myReceiver = object: BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                if (rttManager.isAvailable) {
//                    val request: RangingRequest
//
//                    rttManager.startRanging(request, executor, object : RangingResultCallback() {
//                        override fun onRangingResults(results: List<RangingResult>) { … }
//
//                        override fun onRangingFailure(code: Int) { … }
//                    })
//
//                } else {
//                    //
//                }
//            }
//        }
//        context.registerReceiver(myReceiver, filter)
//
//        val mgr = context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE) as WifiRttManager
//        val request: RangingRequest = myRequest
//        mgr.startRanging(request, executor, object : RangingResultCallback() {
//
//            override fun onRangingResults(results: List<RangingResult>) { … }
//
//            override fun onRangingFailure(code: Int) { … }
//        })
    }


}
