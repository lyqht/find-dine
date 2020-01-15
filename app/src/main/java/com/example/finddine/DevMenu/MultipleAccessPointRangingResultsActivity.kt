package com.example.finddine.DevMenu

import android.Manifest
import android.content.Context
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
import java.util.ArrayList
import com.lemmingapex.trilateration.TrilaterationFunction
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer




data class APLocation(
    val name: String,
    val location: String,
    val macAddress:String,
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

    private var scanResults: List<ScanResult> = listOf();

    private var locationOfAPs: List<APLocation> = listOf(
        APLocation(name = "AP1", location = "Entrance", macAddress = "f0:72:ea:22:40:0d", latitude = 1.2997752863150822, longitude = 103.78912420378691),
        APLocation(name = "AP2", location = "FGD Room", macAddress = "f0:72:ea:25:34:d5", latitude = 1.2999517743563445, longitude = 103.78912297367003),
        APLocation(name = "AP3", location = "Open Space(Ping Pong)", macAddress = "f0:72:ea:3f:a2:c5", latitude = 1.2998518673161072, longitude = 103.78911880515608),
        APLocation(name = "AP4", location = "Open Area", macAddress = "f0:72:ea:39:83:a5", latitude = 1.2999510917177304, longitude = 103.78921274179027),
        APLocation(name = "AP5", location = "Nest Wifi", macAddress = "cc:f4:11:0a:b8:41", latitude = 0.0, longitude = 0.0)
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

        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        scanResults = wifiManager.scanResults.filter{ it.is80211mcResponder }

        if (scanResults.isEmpty()) {
            finish()
        }

        ssidTextView.text = "${scanResults.size} AP"
        mBssidTextView.text = "Granted?"
        2
        mWifiRttManager = getSystemService(Context.WIFI_RTT_RANGING_SERVICE) as WifiRttManager


        resetData()
        startRangingRequest()
    }



    private fun startRangingRequest() {
        // Permission for fine location should already be granted via MainActivity (you can't get
        // to this class unless you already have permission. If they get to this class, then disable
        // fine location permission, we kick them back to main activity.
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            finish()
        }

        bumpNumberOfRequests()

        val rangingRequest = RangingRequest.Builder().addAccessPoints(scanResults).build()

        mWifiRttManager.startRanging(
            rangingRequest, application.mainExecutor, mRttRangingResultCallback
        )
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

            // Because we are only requesting RangingResult for one access point (not multiple
            // access points), this will only ever be one. (Use loops when requesting RangingResults
            // for multiple access points.)


            results.forEach() { result ->
                val macAddress = result.macAddress!!.toString()
                val apLocation = locationOfAPs.find{ AP -> AP.macAddress == macAddress } ?: return

                if (result.status == RangingResult.STATUS_SUCCESS) {

                    apLocation.numberOfSuccessRequests++
//                    Add distance to this AP's history

                    apLocation.addDistanceToHistory(result.distanceMm)

                    apLocation.addStandardDeviationOfDistanceToHistory(result.distanceStdDevMm)
                }
            }

            Log.d(TAG, "multipleAPResult: $locationOfAPs")

            val positions = locationOfAPs.map { ap -> doubleArrayOf(
                ap.longitude, ap.latitude
            )}.toTypedArray()
            val distances = locationOfAPs.map{ ap -> ap.getDistanceMean().toDouble() }.toDoubleArray()
            val solver = NonLinearLeastSquaresSolver(
                TrilaterationFunction(positions, distances),
                LevenbergMarquardtOptimizer()
            )

            val optimum = solver.solve()


            var centroid = optimum.getPoint().toArray();

            mRangeTextView.text = centroid.toString()
            queueNextRangingRequest()
        }

}
}
