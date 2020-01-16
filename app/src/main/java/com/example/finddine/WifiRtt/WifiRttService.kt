//package com.example.finddine.WifiRtt
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.net.wifi.ScanResult
//import android.net.wifi.WifiManager
//import android.net.wifi.rtt.RangingRequest
//import android.util.Log
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import java.util.ArrayList
//
//class WifiRttService(private val mWifiManager: WifiManager, private val context: AppCompatActivity) {
//    private val TAG = "WifiRttService"
//
//    private var mLocationPermissionApproved = false
//    private var mAccessPointsSupporting80211mc: MutableList<ScanResult> = mutableListOf()
//    private var mNumberOfRangeRequests: Int = 0
//
//    init {
//    }
//
//    public fun getCurrentLocation(): DoubleArray {
//
//
//        return doubleArrayOf(2.0, 3.0)
//    }
//
//    private fun startRangingRequest() {
//        // Permission for fine location should already be granted via MainActivity (you can't get
//        // to this class unless you already have permission. If they get to this class, then disable
//        // fine location permission, we kick them back to main activity.
//        if (ActivityCompat.checkSelfPermission(
//                context,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            Log.d(TAG, "Permissions not allowed.")
//            context.finish()
//        }
//
//        mNumberOfRangeRequests++
//
//        if (mScanResult == null) return
//
//        val scanResult = mScanResult as ScanResult
//        val rangingRequest = RangingRequest.Builder().addAccessPoint(scanResult).build()
//
//        mWifiRttManager.startRanging(
//            rangingRequest, application.mainExecutor, mRttRangingResultCallback
//        )
//    }
//
//    private inner class WifiScanReceiver : BroadcastReceiver() {
//
//        private fun find80211mcSupportedAccessPoints(
//            originalList: MutableList<ScanResult>
//        ): MutableList<ScanResult> {
//            val newList = mutableListOf<ScanResult>()
//
//            for (scanResult in originalList) {
//
//                if (scanResult.is80211mcResponder) {
//                    newList.add(scanResult)
//                }
//
//                if (newList.size >= RangingRequest.getMaxPeers()) {
//                    break
//                }
//            }
//            return newList
//        }
//
//        // This is checked via mLocationPermissionApproved boolean
//        @SuppressLint("MissingPermission")
//        override fun onReceive(context: Context, intent: Intent) {
//
//            val scanResults = mWifiManager.scanResults
//
//            if (scanResults != null) {
//
//                if (mLocationPermissionApproved) {
//                    mAccessPointsSupporting80211mc = find80211mcSupportedAccessPoints(scanResults!!)
//
//                } else {
//                    // TODO (jewalker): Add Snackbar regarding permissions
//                    Log.d(TAG, "Permissions not allowed.")
//                }
//            }
//        }
//    }
//}
//
//// AP Locations
//data class APLocation(
//    val name: String,
//    val location: String,
//    val macAddress:String,
//    val latitude: Double,
//    val longitude: Double,
//    var numberOfSuccessRequests: Int = 0,
//    var numberOfRequests: Int = 0,
//
//    // Used to loop over a list of distances to calculate averages (ensures data structure never
//    // get larger than sample size).
//    var statisticRangeHistoryEndIndex: Int = 0,
//    var statisticRangeHistory: ArrayList<Int> = arrayListOf(),
//
//    // Used to loop over a list of the standard deviation of the measured distance to calculate
//    // averages  (ensures data structure never get larger than sample size).
//    var statisticRangeSDHistoryEndIndex: Int = 0,
//    var statisticRangeSDHistory: ArrayList<Int> = arrayListOf(),
//
//    // Max sample size to calculate average for
//    // 1. Distance to device (getDistanceMm) over time
//    // 2. Standard deviation of the measured distance to the device (getDistanceStdDevMm) over time
//    // NOTE: A RangeRequest result already consists of the average of 7 readings from a burst,
//    // so the average in (1) is the average of these averages.
//    val sampleSize: Int = 50
//) {
//    // Adds distance to history. If larger than sample size value, loops back over and replaces the
//    // oldest distance record in the list.
//    fun addDistanceToHistory(distance: Int) {
//        if (statisticRangeHistory.size >= sampleSize) {
//
//            if (statisticRangeHistoryEndIndex >= sampleSize) {
//                statisticRangeHistoryEndIndex = 0
//            }
//
//            statisticRangeHistory[statisticRangeHistoryEndIndex] = distance
//            statisticRangeHistoryEndIndex++
//
//        } else {
//            statisticRangeHistory.add(distance)
//        }
//    }
//
//
//    // Adds standard deviation of the measured distance to history. If larger than sample size
//    // value, loops back over and replaces the oldest distance record in the list.
//    fun addStandardDeviationOfDistanceToHistory(distanceSd: Int) {
//
//        if (statisticRangeSDHistory.size >= sampleSize) {
//
//            if (statisticRangeSDHistoryEndIndex >= sampleSize) {
//                statisticRangeSDHistoryEndIndex = 0
//            }
//
//            statisticRangeSDHistory[statisticRangeSDHistoryEndIndex] = distanceSd
//            statisticRangeSDHistoryEndIndex++
//
//        } else {
//            statisticRangeSDHistory.add(distanceSd)
//        }
//    }
//
//    // Calculates average distance based on stored history.
//    fun getDistanceMean(): Float {
//        var distanceSum = 0f
//
//        for (distance in statisticRangeHistory) {
//            distanceSum += distance.toFloat()
//        }
//
//        return distanceSum / statisticRangeHistory.size
//    }
//
//    // Calculates standard deviation of the measured distance based on stored history.
//    private fun getStandardDeviationOfDistanceMean(): Float {
//        var distanceSdSum = 0f
//
//        for (distanceSd in statisticRangeSDHistory) {
//            distanceSdSum += distanceSd.toFloat()
//        }
//
//        return distanceSdSum / statisticRangeHistory.size
//    }
//
//    fun resetData() {
//        numberOfSuccessRequests = 0
//        numberOfRequests = 0
//
//        statisticRangeHistoryEndIndex = 0
//        statisticRangeHistory.clear()
//
//        statisticRangeSDHistoryEndIndex = 0
//        statisticRangeSDHistory.clear()
//    }
//
//}