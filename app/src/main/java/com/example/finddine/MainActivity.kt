package com.example.finddine

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.rtt.RangingRequest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), MyAdapter.ScanResultClickListener {

    private val TAG = "MainActivity"

    private val SCAN_RESULT_EXTRA = "com.example.android.wifirttscan.extra.SCAN_RESULT"

    private var mLocationPermissionApproved = false

    var mAccessPointsSupporting80211mc: MutableList<ScanResult> = mutableListOf()

    private var mWifiManager: WifiManager? = null
    private var mWifiScanReceiver: WifiScanReceiver? = null

    private var mOutputTextView: TextView? = null
    private var mRecyclerView: RecyclerView? = null

    private var mAdapter: MyAdapter? = null

    companion object {
        const val REQUEST_CODE_ACCESS_COARSE_LOCATION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mOutputTextView = findViewById(R.id.access_point_summary_text_view);
        mRecyclerView = findViewById(R.id.recycler_view);

        // Improve performance if you know that changes in content do not change the layout size
        // of the RecyclerView
        mRecyclerView?.setHasFixedSize(true)

        // use a linear layout manager
        val layoutManager = LinearLayoutManager(this)
        mRecyclerView?.setLayoutManager(layoutManager)

        mAdapter = MyAdapter(mAccessPointsSupporting80211mc, this);
        mRecyclerView?.setAdapter(mAdapter);

        mWifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        mWifiScanReceiver = WifiScanReceiver()

    }

    override fun onResume() {

        Log.d(TAG, "onResume()")
        super.onResume()

        mLocationPermissionApproved = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        registerReceiver(
            mWifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")
        super.onPause()
        unregisterReceiver(mWifiScanReceiver)
    }

    private fun logToUi(message: String) {
        if (!message.isEmpty()) {
            Log.d(TAG, message)
            mOutputTextView?.setText(message)
        }
    }

    override fun onScanResultItemClick(scanResult: ScanResult?) {
        Log.d(TAG, "onScanResultItemClick(): ssid: " + scanResult?.SSID)

        val intent = Intent(this, AccessPointRangingResultsActivity::class.java)
        intent.putExtra(SCAN_RESULT_EXTRA, scanResult)
        startActivity(intent)
    }

    fun onClickFindDistancesToAccessPoints(view: View) {
        if (mLocationPermissionApproved) {
            logToUi(getString(R.string.retrieving_access_points))
            mWifiManager?.startScan()

        } else {
            // On 23+ (M+) devices, fine location permission not granted. Request permission.
            val startIntent = Intent(this, LocationPermissionRequestActivity::class.java)
            startActivity(startIntent)
        }
    }

    private inner class WifiScanReceiver : BroadcastReceiver() {

        private fun find80211mcSupportedAccessPoints(
            originalList: MutableList<ScanResult>
        ): MutableList<ScanResult> {
            val newList = mutableListOf<ScanResult>()

            for (scanResult in originalList) {

                if (scanResult.is80211mcResponder) {
                    newList.add(scanResult)
                }

                if (newList.size >= RangingRequest.getMaxPeers()) {
                    break
                }
            }
            return newList
        }

        // This is checked via mLocationPermissionApproved boolean
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {

            val scanResults = mWifiManager?.getScanResults()

            if (scanResults != null) {

                if (mLocationPermissionApproved) {
                    mAccessPointsSupporting80211mc = find80211mcSupportedAccessPoints(scanResults!!)

                    mAdapter?.swapData(mAccessPointsSupporting80211mc)

                    logToUi(
                        (scanResults!!.size).toString()
                                + " APs discovered, "
                                + mAccessPointsSupporting80211mc.size
                                + " RTT capable."
                    )

                } else {
                    // TODO (jewalker): Add Snackbar regarding permissions
                    Log.d(TAG, "Permissions not allowed.")
                }
            }
        }
    }
}
