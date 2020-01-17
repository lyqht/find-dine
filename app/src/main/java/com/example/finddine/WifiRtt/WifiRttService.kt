package com.example.finddine.DevMenu

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
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResultCallback
import android.net.wifi.rtt.WifiRttManager
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.finddine.WifiRtt.SvyConvert.LatLonCoordinate
import com.example.finddine.WifiRtt.SvyConvert.SVY21Coordinate
import com.lemmingapex.trilateration.TrilaterationFunction
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import java.lang.Error
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.*


data class AccessPoint(
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
    val sampleSize: Int = 10
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
    fun getDistanceMean(): Double {
        var distanceSum = 0.0

        for (distance in statisticRangeHistory) {
            distanceSum += distance.toDouble()
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

class WifiRttService(val context: AppCompatActivity) {
    private val TAG = "APRRActivity"

    val SCAN_RESULT_EXTRA = "com.example.android.wifirttscan.extra.SCAN_RESULT"

    val EARTH_RADIUS_MM: Double = 6371008771.4

    private lateinit var mMAC: String

    // Permissions
    private var mLocationPermissionApproved = false
    private var mWifiRttAvailable = false


    private var scanResults: List<ScanResult> = listOf();
    companion object {

    var locationOfAPs: List<AccessPoint> = listOf(
        AccessPoint(
            name = "AP1",
            location = "Entrance",
            macAddress = "f0:72:ea:22:40:0d",
            latitude = 1.2997752863150822,
            longitude = 103.78912420378691
        ),
        AccessPoint(
            name = "AP2",
            location = "FGD Room",
            macAddress = "f0:72:ea:25:34:d5",
            latitude = 1.2999517743563445,
            longitude = 103.78912297367003
        ),
        AccessPoint(
            name = "AP3",
            location = "Open Space(Ping Pong)",
            macAddress = "f0:72:ea:3f:a2:c5",
            latitude = 1.2998518673161072,
            longitude = 103.78911880515608
        ),
        AccessPoint(
            name = "AP4",
            location = "Open Area",
            macAddress = "f0:72:ea:39:83:a5",
            latitude = 1.2999510917177304,
            longitude = 103.78921274179027
        ),
        AccessPoint(
            name = "AP5",
            location = "Nest Wifi",
            macAddress = "cc:f4:11:0a:b8:41",
            latitude = 1.2999823163346869,
            longitude = 103.78925833851099
        ))

    }
    private var mMillisecondsDelayBeforeNewRangingRequest: Int = 500


    private lateinit var mWifiRttManager: WifiRttManager
    private lateinit var wifiManager: WifiManager
    private var mRttRangingResultCallback: RttRangingResultCallback = RttRangingResultCallback()
    private var mTimer: TimerTask? = null
    private var stopTimer: Boolean = false
    private lateinit var myReceiver: BroadcastReceiver

    private lateinit var mOnUpdateHandler: (DoubleArray) -> Unit

    fun subscribeToUpdates(onUpdate: (DoubleArray) -> Unit) {
        mOnUpdateHandler = onUpdate
    }

    fun stopWifiRttService() {
        Log.d(TAG, "Stop wifi rtt service")

        if (mLocationPermissionApproved) {
            this.stopTimer = true
            try {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(myReceiver)
            } catch (e: Error) {
                Log.d(TAG, "Could not destroy receiver", e)
            }
        }
    }


    init {
        // Check compatibility

        // Device supports wifi rtt
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)) {
            Log.d(TAG, "Device does not support wifi rtt")
            context.finish()
        } else {
            Log.d(TAG, "Device supports wifi rtt")
        }

        // Device has location permissions
        mLocationPermissionApproved = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!mLocationPermissionApproved) {
            Log.d(TAG, "Permission not given, starting intent to request permission")

            // On 23+ (M+) devices, fine location permission not granted. Request permission.
            val startIntent = Intent(context, LocationPermissionRequestActivity::class.java)
            context.startActivity(startIntent)
        } else {
            Log.d(TAG, "Location permissions granted!")
            startService()
        }

    }

    fun startService() {
        // Initialize scanning services
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mWifiRttManager =
            context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE) as WifiRttManager

        // Register wifi rtt receiver and set variable
        Log.d(TAG, "registerReceiver")

        val filter = IntentFilter(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED)
        myReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "onReceive")

                mWifiRttAvailable = mWifiRttManager.isAvailable()
            }
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(myReceiver, filter)
        mWifiRttAvailable = mWifiRttManager.isAvailable()


        // Start scanning for available wifi rtt devices
        wifiManager.startScan()

        scanResults = wifiManager.scanResults.filter { it.is80211mcResponder }
        if (scanResults.isEmpty()) {
            // TODO: Try to scan again before continuing
            context.finish()
        }

        // Start ranging request
        resetData()

        Log.d(TAG, "Starting ranging request...")

        if (mWifiRttAvailable) {
            Log.d(TAG, "Location permissions approved and wifi rtt available")

//            logToUi(getString(R.string.retrieving_access_points))


            bumpNumberOfRequests()

            runTimerWithFunc(this::startRangingRequest)

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

    // This is checked via mLocationPermissionApproved boolean
    @SuppressLint("MissingPermission")
    private fun startRangingRequest() {
        Log.d(TAG, "startRangingRequest")

        val rangingRequest = RangingRequest.Builder().addAccessPoints(scanResults).build()

        mWifiRttManager.startRanging(
            rangingRequest, context.application.mainExecutor, mRttRangingResultCallback
        )

        if (!stopTimer) {
            runTimerWithFunc(this::startRangingRequest)
        }
    }

    private fun runTimerWithFunc(timerFunc: () -> Unit) {
        mTimer = Timer("startRangingRequest", false)
            .schedule(mMillisecondsDelayBeforeNewRangingRequest.toLong()) {
                timerFunc()
            }
    }

    // Class that handles callbacks for all RangingRequests and issues new RangingRequests.
    private inner class RttRangingResultCallback : RangingResultCallback() {


        override fun onRangingFailure(code: Int) {
            Log.d(TAG, "onRangingFailure() code: $code")
        }

        override fun onRangingResults(results: List<RangingResult>) {
            Log.d(TAG, "onRangingResults(): $results")

            results.forEach() { result ->
                val macAddress = result.macAddress!!.toString()
                val accessPoint = locationOfAPs.find { AP -> AP.macAddress == macAddress } ?: return

                if (result.status == RangingResult.STATUS_SUCCESS && result.distanceMm >= 0) {

                    accessPoint.numberOfSuccessRequests++
//                    Add distance to this AP's history

                    Log.d(
                        TAG,
                        "accessPoint:, ${accessPoint.name}, distanceMm: ${result.distanceMm}"
                    )

                    accessPoint.addDistanceToHistory(result.distanceMm)

                    accessPoint.addStandardDeviationOfDistanceToHistory(result.distanceStdDevMm)
                }
            }

            Log.d(TAG, "multipleAPResult: $locationOfAPs")

            var positions = arrayListOf<DoubleArray>()
            var distances = arrayListOf<Double>()

            locationOfAPs.forEach { ap ->
                if (ap.hasDistanceHistory()) {
                    Log.d(TAG, ">>> lat: ${ap.latitude}, lng: ${ap.longitude}")
                    Log.d(TAG, ">>> converted: ${Arrays.toString(latlngToCartersian(ap.latitude, ap.longitude))}")

                    positions.add(
                        latlngToCartersian(ap.latitude, ap.longitude)
                    )

                    distances.add(ap.getDistanceMean())
                }
            }

            if (positions.size < 2) {
                return
            }



            val solver = NonLinearLeastSquaresSolver(
                TrilaterationFunction(positions.toTypedArray(), distances.toDoubleArray()),
                LevenbergMarquardtOptimizer()
            )

            val optimum = solver.solve()


            var centroid = optimum.getPoint().toArray();

            // TODO: change to continuously return current centroid
//            val centroidLatLng = cartesianToLatlng(centroid[0], centroid[1], centroid[2])
            val centroidLatLng = cartesianToLatlng(centroid[0], centroid[1], null)

            Log.d(TAG, "centroidLatLng: ${Arrays.toString(centroidLatLng)}")
            mOnUpdateHandler(centroidLatLng)
        }

        private fun latlngToCartersian(lat: Double, lng: Double): DoubleArray {
//            x = R * cos(lat) * cos(lon)
//
//            y = R * cos(lat) * sin(lon)
//
//            z = R *sin(lat)

            val latLngCoordinate = LatLonCoordinate(lat, lng)

            return doubleArrayOf(latLngCoordinate.asSVY21().northing, latLngCoordinate.asSVY21().easting)
//
//            val latRads = (90 - lat) * Math.PI / 180
//            val lngRads = (180 - lng) * Math.PI / 180
//            val xPos = EARTH_RADIUS_MM * sin(latRads) * cos(lngRads)
//            val yPos = EARTH_RADIUS_MM * cos(latRads)
//            val zPos = EARTH_RADIUS_MM * sin(latRads) * sin(lngRads)
//
//            return doubleArrayOf(xPos, yPos, zPos)
        }

        private fun cartesianToLatlng(xPos: Double, yPos: Double, zPos: Double?): DoubleArray {
//            lat = asin(z / R)
//            lon = atan2(y, x)


            val svy21Coordinate = SVY21Coordinate(xPos, yPos)

            return doubleArrayOf(svy21Coordinate.asLatLon().latitude, svy21Coordinate.asLatLon().longitude)

//            val latRads = acos(yPos / EARTH_RADIUS_MM)
//            val lngRads = atan2(zPos, xPos)
//
//            val lat = (Math.PI / 2 - latRads) * (180 / Math.PI)
//            val lng = (Math.PI - lngRads) * (180 / Math.PI)
//
//            return doubleArrayOf(lat, lng)
        }
    }
}
