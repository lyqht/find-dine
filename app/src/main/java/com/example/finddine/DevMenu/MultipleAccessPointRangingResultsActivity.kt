package com.example.finddine.DevMenu

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.finddine.R
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResultCallback
import android.net.wifi.rtt.WifiRttManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.lemmingapex.trilateration.TrilaterationFunction
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import java.util.*


data class APLocation(
    val name: String,
    val location: String,
    val macAddress: String,
    val latitude: Double,
    val longitude: Double,
    var numberOfSuccessRequests: Int = 0,
    var numberOfRequests: Int = 0,

    // Used to loop over a list of distances to calculate averages (ensures data structure never
    // get larger than sample size).
    var statisticRangeHistoryEndIndex: Int = 0,
    var statisticRangeHistory: ArrayList<Int> = arrayListOf(),

    // Used to loop over a list of the standard deviation of the measured distance to calculate
    // averages  (ensures data structure never get larger than sample size).
    var statisticRangeSDHistoryEndIndex: Int = 0,
    var statisticRangeSDHistory: ArrayList<Int> = arrayListOf(),

    // Max sample size to calculate average for
    // 1. Distance to device (getDistanceMm) over time
    // 2. Standard deviation of the measured distance to the device (getDistanceStdDevMm) over time
    // NOTE: A RangeRequest result already consists of the average of 7 readings from a burst,
    // so the average in (1) is the average of these averages.
    val sampleSize: Int = 50
) {
    // Adds distance to history. If larger than sample size value, loops back over and replaces the
    // oldest distance record in the list.
    fun addDistanceToHistory(distance: Int) {
        if (statisticRangeHistory.size >= sampleSize) {

            if (statisticRangeHistoryEndIndex >= sampleSize) {
                statisticRangeHistoryEndIndex = 0
            }

            statisticRangeHistory[statisticRangeHistoryEndIndex] = distance
            statisticRangeHistoryEndIndex++

        } else {
            statisticRangeHistory.add(distance)
        }
    }


    // Adds standard deviation of the measured distance to history. If larger than sample size
    // value, loops back over and replaces the oldest distance record in the list.
    fun addStandardDeviationOfDistanceToHistory(distanceSd: Int) {

        if (statisticRangeSDHistory.size >= sampleSize) {

            if (statisticRangeSDHistoryEndIndex >= sampleSize) {
                statisticRangeSDHistoryEndIndex = 0
            }

            statisticRangeSDHistory[statisticRangeSDHistoryEndIndex] = distanceSd
            statisticRangeSDHistoryEndIndex++

        } else {
            statisticRangeSDHistory.add(distanceSd)
        }
    }

    // Calculates average distance based on stored history.
    fun getDistanceMean(): Float {
        var distanceSum = 0f

        for (distance in statisticRangeHistory) {
            distanceSum += distance.toFloat()
        }

        return distanceSum / statisticRangeHistory.size
    }

    fun hasDistanceHistory(): Boolean {
        return statisticRangeHistory.size != 0
    }

    // Calculates standard deviation of the measured distance based on stored history.
    private fun getStandardDeviationOfDistanceMean(): Float {
        var distanceSdSum = 0f

        for (distanceSd in statisticRangeSDHistory) {
            distanceSdSum += distanceSd.toFloat()
        }

        return distanceSdSum / statisticRangeHistory.size
    }

    fun resetData() {
        numberOfSuccessRequests = 0
        numberOfRequests = 0

        statisticRangeHistoryEndIndex = 0
        statisticRangeHistory.clear()

        statisticRangeSDHistoryEndIndex = 0
        statisticRangeSDHistory.clear()
    }

}

class MultipleAccessPointRangingResultsActivity : AppCompatActivity() {
    private val TAG = "APRRActivity"

    val SCAN_RESULT_EXTRA = "com.example.android.wifirttscan.extra.SCAN_RESULT"

    private val SAMPLE_SIZE_DEFAULT = 50
    private val MILLISECONDS_DELAY_BEFORE_NEW_RANGING_REQUEST_DEFAULT = 1000

    // UI Elements.
    private lateinit var ssidTextView: TextView
    private lateinit var mBssidTextView: TextView

    private lateinit var mRangeTextView: TextView
    private lateinit var mRangeMeanTextView: TextView
    private lateinit var mRangeSDTextView: TextView
    private lateinit var mRangeSDMeanTextView: TextView
    private lateinit var mRssiTextView: TextView
    private lateinit var successesInBurstTextView: TextView
    private lateinit var successRatioTextView: TextView
    private lateinit var mNumberOfRequestsTextView: TextView

    // Non UI variables.
    private lateinit var scanResult: ScanResult
    private lateinit var mMAC: String

    // Permissions
    private var mLocationPermissionApproved = false
    private var mWifiRttAvailable = false


    private var scanResults: List<ScanResult> = listOf();

    private var locationOfAPs: List<APLocation> = listOf(
        APLocation(
            name = "AP1",
            location = "Entrance",
            macAddress = "f0:72:ea:22:40:0d",
            latitude = 1.2997752863150822,
            longitude = 103.78912420378691
        ),
        APLocation(
            name = "AP2",
            location = "FGD Room",
            macAddress = "f0:72:ea:25:34:d5",
            latitude = 1.2999517743563445,
            longitude = 103.78912297367003
        ),
        APLocation(
            name = "AP3",
            location = "Open Space(Ping Pong)",
            macAddress = "f0:72:ea:3f:a2:c5",
            latitude = 1.2998518673161072,
            longitude = 103.78911880515608
        ),
        APLocation(
            name = "AP4",
            location = "Open Area",
            macAddress = "f0:72:ea:39:83:a5",
            latitude = 1.2999510917177304,
            longitude = 103.78921274179027
        ),
        APLocation(
            name = "AP5",
            location = "Nest Wifi",
            macAddress = "cc:f4:11:0a:b8:41",
            latitude = 0.0,
            longitude = 0.0
        )
    )
    private var mMillisecondsDelayBeforeNewRangingRequest: Int = 0

    private var sampleSize: Int = 0

    private lateinit var mWifiRttManager: WifiRttManager
    private lateinit var wifiManager: WifiManager
    private var mRttRangingResultCallback: RttRangingResultCallback = RttRangingResultCallback()

    // Triggers additional RangingRequests with delay (mMillisecondsDelayBeforeNewRangingRequest).
    internal val mRangeRequestDelayHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access_point_ranging_results)

        // Initializes UI elements.
        ssidTextView = findViewById(R.id.ssid)
        mBssidTextView = findViewById(R.id.bssid)

        mRangeTextView = findViewById(R.id.range_value)
        mRangeMeanTextView = findViewById(R.id.range_mean_value)
        mRangeSDTextView = findViewById(R.id.range_sd_value)
        mRangeSDMeanTextView = findViewById(R.id.range_sd_mean_value)
        mRssiTextView = findViewById(R.id.rssi_value)
        successesInBurstTextView = findViewById(R.id.successes_in_burst_value)
        successRatioTextView = findViewById(R.id.success_ratio_value)
        mNumberOfRequestsTextView = findViewById(R.id.number_of_requests_value)

        // Check compatibility

        // Device supports wifi rtt
        if (!this.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)) {
            Log.d(TAG, "Device does not support wifi rtt")
            return
        } else {
            Log.d(TAG, "Device supports wifi rtt")
        }

        // Device has location permissions
        mLocationPermissionApproved = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!mLocationPermissionApproved) {
            Log.d(TAG, "Permission not given, starting intent to request permission")

            // On 23+ (M+) devices, fine location permission not granted. Request permission.
            val startIntent = Intent(this, LocationPermissionRequestActivity::class.java)
            startActivity(startIntent)
        } else {
            Log.d(TAG, "Location permissions granted!")
        }

        // Initialize scanning services
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        mWifiRttManager = getSystemService(Context.WIFI_RTT_RANGING_SERVICE) as WifiRttManager

        // Register wifi rtt receiver and set variable
        Log.d(TAG, "registerReceiver")

        val filter = IntentFilter(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED)
        val myReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "onReceive")

                mWifiRttAvailable = mWifiRttManager.isAvailable()
            }
        }

        this.registerReceiver(myReceiver, filter)
        mWifiRttAvailable = mWifiRttManager.isAvailable()


        // Start scanning for available wifi rtt devices
        wifiManager.startScan()

        scanResults = wifiManager.scanResults.filter { it.is80211mcResponder }
        if (scanResults.isEmpty()) {
            // TODO: Try to scan again before continuing
            finish()
        }

        ssidTextView.text = "${scanResults.size} AP"
        mBssidTextView.text = "Granted?"
        2


        // Start ranging request
        resetData()

        Log.d(TAG, "Starting ranging request...")

        if (mWifiRttAvailable) {
            Log.d(TAG, "Location permissions approved and wifi rtt available")

//            logToUi(getString(R.string.retrieving_access_points))


            bumpNumberOfRequests()

            startRangingRequest()

        } else {
            Log.d(TAG, "Device not Wifi Rtt compatible!")
        }
    }

    private fun bumpNumberOfRequests() {
        locationOfAPs.forEach { it.numberOfRequests++ }
    }

    private fun resetData() {
        locationOfAPs.forEach { it.resetData() }
    }


    fun onResetButtonClick(view: View) {
        resetData()
    }

    // This is checked via mLocationPermissionApproved boolean
    @SuppressLint("MissingPermission")
    private fun startRangingRequest() {
        val rangingRequest = RangingRequest.Builder().addAccessPoints(scanResults).build()

        mWifiRttManager.startRanging(
            rangingRequest, application.mainExecutor, mRttRangingResultCallback
        )
    }

    // Class that handles callbacks for all RangingRequests and issues new RangingRequests.
    private inner class RttRangingResultCallback : RangingResultCallback() {

        private fun queueNextRangingRequest() {
            mRangeRequestDelayHandler.postDelayed(
                { startRangingRequest() },
                mMillisecondsDelayBeforeNewRangingRequest.toLong()
            )
        }

        override fun onRangingFailure(code: Int) {
            Log.d(TAG, "onRangingFailure() code: $code")
            queueNextRangingRequest()
        }

        override fun onRangingResults(results: List<RangingResult>) {
            Log.d(TAG, "onRangingResults(): $results")

            results.forEach() { result ->
                val macAddress = result.macAddress!!.toString()
                val apLocation = locationOfAPs.find { AP -> AP.macAddress == macAddress } ?: return

                if (result.status == RangingResult.STATUS_SUCCESS) {

                    apLocation.numberOfSuccessRequests++
//                    Add distance to this AP's history

                    Log.d(TAG, "apLocation:, ${apLocation.name}, distanceMm: ${result.distanceMm}")

                    apLocation.addDistanceToHistory(result.distanceMm)

                    apLocation.addStandardDeviationOfDistanceToHistory(result.distanceStdDevMm)
                }
            }

            Log.d(TAG, "multipleAPResult: $locationOfAPs")

            var positions = arrayListOf<DoubleArray>()
            var distances = arrayListOf<Double>()

            locationOfAPs.forEach { ap ->
                if (ap.hasDistanceHistory()) {
                    positions.add(
                        doubleArrayOf(
                            ap.latitude, ap.longitude
                        )
                    )

                    distances.add(ap.getDistanceMean().toDouble())
                }
            }

            val solver = NonLinearLeastSquaresSolver(
                TrilaterationFunction(positions.toTypedArray(), distances.toDoubleArray()),
                LevenbergMarquardtOptimizer()
            )

            val optimum = solver.solve()


            var centroid = optimum.getPoint().toArray();

            mRangeTextView.text = Arrays.toString(centroid)
            queueNextRangingRequest()
        }

    }
}
