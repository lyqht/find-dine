package com.example.finddine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResultCallback
import android.net.wifi.rtt.WifiRttManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.ArrayList

class AccessPointRangingResultsActivity : AppCompatActivity() {
    private val TAG = "APRRActivity"

    val SCAN_RESULT_EXTRA = "com.example.android.wifirttscan.extra.SCAN_RESULT"

    private val SAMPLE_SIZE_DEFAULT = 50
    private val MILLISECONDS_DELAY_BEFORE_NEW_RANGING_REQUEST_DEFAULT = 1000

    // UI Elements.
    private var mSsidTextView: TextView? = null
    private var mBssidTextView: TextView? = null

    private var mRangeTextView: TextView? = null
    private var mRangeMeanTextView: TextView? = null
    private var mRangeSDTextView: TextView? = null
    private var mRangeSDMeanTextView: TextView? = null
    private var mRssiTextView: TextView? = null
    private var mSuccessesInBurstTextView: TextView? = null
    private var mSuccessRatioTextView: TextView? = null
    private var mNumberOfRequestsTextView: TextView? = null

    private var mSampleSizeEditText: EditText? = null
    private var mMillisecondsDelayBeforeNewRangingRequestEditText: EditText? = null

    // Non UI variables.
    private var mScanResult: ScanResult? = null
    private var mMAC: String? = null

    private var mNumberOfRangeRequests: Int = 0
    private var mNumberOfSuccessfulRangeRequests: Int = 0

    private var mMillisecondsDelayBeforeNewRangingRequest: Int = 0

    // Max sample size to calculate average for
    // 1. Distance to device (getDistanceMm) over time
    // 2. Standard deviation of the measured distance to the device (getDistanceStdDevMm) over time
    // Note: A RangeRequest result already consists of the average of 7 readings from a burst,
    // so the average in (1) is the average of these averages.
    private var mSampleSize: Int = 0

    // Used to loop over a list of distances to calculate averages (ensures data structure never
    // get larger than sample size).
    private var mStatisticRangeHistoryEndIndex: Int = 0
    private var mStatisticRangeHistory: ArrayList<Int> = arrayListOf()

    // Used to loop over a list of the standard deviation of the measured distance to calculate
    // averages  (ensures data structure never get larger than sample size).
    private var mStatisticRangeSDHistoryEndIndex: Int = 0
    private var mStatisticRangeSDHistory: ArrayList<Int> = arrayListOf()

    private var mWifiRttManager: WifiRttManager? = null
    private var mRttRangingResultCallback: RttRangingResultCallback = RttRangingResultCallback()

    // Triggers additional RangingRequests with delay (mMillisecondsDelayBeforeNewRangingRequest).
    internal val mRangeRequestDelayHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access_point_ranging_results)

        // Initializes UI elements.
        mSsidTextView = findViewById(R.id.ssid)
        mBssidTextView = findViewById(R.id.bssid)

        mRangeTextView = findViewById(R.id.range_value)
        mRangeMeanTextView = findViewById(R.id.range_mean_value)
        mRangeSDTextView = findViewById(R.id.range_sd_value)
        mRangeSDMeanTextView = findViewById(R.id.range_sd_mean_value)
        mRssiTextView = findViewById(R.id.rssi_value)
        mSuccessesInBurstTextView = findViewById(R.id.successes_in_burst_value)
        mSuccessRatioTextView = findViewById(R.id.success_ratio_value)
        mNumberOfRequestsTextView = findViewById(R.id.number_of_requests_value)

        mSampleSizeEditText = findViewById(R.id.stats_window_size_edit_value)
        mSampleSizeEditText?.setText(SAMPLE_SIZE_DEFAULT.toString() + "")

        mMillisecondsDelayBeforeNewRangingRequestEditText =
            findViewById(R.id.ranging_period_edit_value)
        mMillisecondsDelayBeforeNewRangingRequestEditText?.setText(
            MILLISECONDS_DELAY_BEFORE_NEW_RANGING_REQUEST_DEFAULT.toString() + ""
        )

        // Retrieve ScanResult from Intent.
        val intent = intent
        mScanResult = intent.getParcelableExtra(SCAN_RESULT_EXTRA)

        if (mScanResult == null) {
            finish()
        }

        mMAC = mScanResult?.BSSID

        mSsidTextView?.text = mScanResult?.SSID
        mBssidTextView?.text = mScanResult?.BSSID

        mWifiRttManager = getSystemService(Context.WIFI_RTT_RANGING_SERVICE) as WifiRttManager
        mRttRangingResultCallback = RttRangingResultCallback()

        // Used to store range (distance) and rangeSd (standard deviation of the measured distance)
        // history to calculate averages.
        mStatisticRangeHistory = ArrayList()
        mStatisticRangeSDHistory = ArrayList()

        resetData()

        startRangingRequest()
    }

    private fun resetData() {
        mSampleSize = Integer.parseInt(mSampleSizeEditText?.getText().toString())

        mMillisecondsDelayBeforeNewRangingRequest = Integer.parseInt(
            mMillisecondsDelayBeforeNewRangingRequestEditText?.getText().toString()
        )

        mNumberOfSuccessfulRangeRequests = 0
        mNumberOfRangeRequests = 0

        mStatisticRangeHistoryEndIndex = 0
        mStatisticRangeHistory?.clear()

        mStatisticRangeSDHistoryEndIndex = 0
        mStatisticRangeSDHistory?.clear()
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

        mNumberOfRangeRequests++

        if (mScanResult == null) return

        val scanResult = mScanResult as ScanResult
        val rangingRequest = RangingRequest.Builder().addAccessPoint(scanResult).build()

        mWifiRttManager?.startRanging(
            rangingRequest, application.mainExecutor, mRttRangingResultCallback
        )
    }

    // Calculates average distance based on stored history.
    private fun getDistanceMean(): Float {
        var distanceSum = 0f

        for (distance in mStatisticRangeHistory) {
            distanceSum += distance.toFloat()
        }

        return distanceSum / mStatisticRangeHistory.size
    }

    // Adds distance to history. If larger than sample size value, loops back over and replaces the
    // oldest distance record in the list.
    private fun addDistanceToHistory(distance: Int) {

        if (mStatisticRangeHistory.size >= mSampleSize) {

            if (mStatisticRangeHistoryEndIndex >= mSampleSize) {
                mStatisticRangeHistoryEndIndex = 0
            }

            mStatisticRangeHistory[mStatisticRangeHistoryEndIndex] = distance
            mStatisticRangeHistoryEndIndex++

        } else {
            mStatisticRangeHistory.add(distance)
        }
    }

    // Calculates standard deviation of the measured distance based on stored history.
    private fun getStandardDeviationOfDistanceMean(): Float {
        var distanceSdSum = 0f

        for (distanceSd in mStatisticRangeSDHistory) {
            distanceSdSum += distanceSd.toFloat()
        }

        return distanceSdSum / mStatisticRangeHistory.size
    }

    // Adds standard deviation of the measured distance to history. If larger than sample size
    // value, loops back over and replaces the oldest distance record in the list.
    private fun addStandardDeviationOfDistanceToHistory(distanceSd: Int) {

        if (mStatisticRangeSDHistory.size >= mSampleSize) {

            if (mStatisticRangeSDHistoryEndIndex >= mSampleSize) {
                mStatisticRangeSDHistoryEndIndex = 0
            }

            mStatisticRangeSDHistory[mStatisticRangeSDHistoryEndIndex] = distanceSd
            mStatisticRangeSDHistoryEndIndex++

        } else {
            mStatisticRangeSDHistory.add(distanceSd)
        }
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

        override fun onRangingResults(list: List<RangingResult>) {
            Log.d(TAG, "onRangingResults(): $list")

            // Because we are only requesting RangingResult for one access point (not multiple
            // access points), this will only ever be one. (Use loops when requesting RangingResults
            // for multiple access points.)
            if (list.size == 1) {

                val rangingResult = list[0]

                if (mMAC == rangingResult.macAddress!!.toString()) {

                    if (rangingResult.status == RangingResult.STATUS_SUCCESS) {

                        mNumberOfSuccessfulRangeRequests++

                        mRangeTextView?.setText(((rangingResult.getDistanceMm() / 1000f)).toString() + "")
                        addDistanceToHistory(rangingResult.distanceMm)
                        mRangeMeanTextView?.setText(((getDistanceMean() / 1000f)).toString() + "")

                        mRangeSDTextView?.setText(
                            ((rangingResult.getDistanceStdDevMm() / 1000f)).toString() + ""
                        )
                        addStandardDeviationOfDistanceToHistory(
                            rangingResult.distanceStdDevMm
                        )
                        mRangeSDMeanTextView?.setText(
                            ((getStandardDeviationOfDistanceMean() / 1000f)).toString() + ""
                        )

                        mRssiTextView?.setText((rangingResult.getRssi()).toString() + "")
                        mSuccessesInBurstTextView?.setText(
                            (rangingResult.getNumSuccessfulMeasurements()).toString()
                                    + "/"
                                    + rangingResult.numAttemptedMeasurements
                        )

                        val successRatio =
                            (((mNumberOfSuccessfulRangeRequests.toFloat() / mNumberOfRangeRequests.toFloat())) * 100)
                        mSuccessRatioTextView?.setText((successRatio).toString() + "%")

                        mNumberOfRequestsTextView?.setText((mNumberOfRangeRequests).toString() + "")

                    } else if ((rangingResult.status == RangingResult.STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC)) {
                        Log.d(TAG, "RangingResult failed (AP doesn't support IEEE80211 MC.")

                    } else {
                        Log.d(TAG, "RangingResult failed.")
                    }

                } else {
                    Toast.makeText(
                        applicationContext,
                        R.string
                            .mac_mismatch_message_activity_access_point_ranging_results,
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }

            queueNextRangingRequest()
        }
    }
}