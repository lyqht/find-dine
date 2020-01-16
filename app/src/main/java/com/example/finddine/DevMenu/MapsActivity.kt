package com.example.finddine.DevMenu

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.finddine.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.net.MalformedURLException
import java.net.URL
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var marker: Marker

    // RTT Service
    private var wifiRttService: WifiRttService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onResume() {
        super.onResume()
        // Initialize wifiRttService
        wifiRttService = WifiRttService(this)

        wifiRttService?.subscribeToUpdates(this::updateUserLatLng)
    }

    override fun onPause() {
        super.onPause()
        wifiRttService?.stopWifiRttService()
    }

    override fun onStop() {
        super.onStop()
        wifiRttService?.stopWifiRttService()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Add a marker in Sydney and move the camera
        val sandcrawler = LatLng(1.2998518673161072, 103.78911880515608)
        marker = map.addMarker(MarkerOptions().position(sandcrawler).title("You are here!"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(sandcrawler, 22.0f))

        val tileProvider = object : UrlTileProvider(256, 256) {
           @Synchronized override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {

                /* Define the URL pattern for the tile images */
                val s = String.format(
                    "https://api.mapbox.com/v4/ijasm.974bq1q8/%d/%d/%d@2x.png?access_token=pk.eyJ1IjoiaWphc20iLCJhIjoiY2s1ZXdnMW9hMjhqejNtcG5rNGZyZnZodiJ9.qWOLNLQPCVXETV2sVNq3gw",
                    zoom, x, y
                )

                if (!checkTileExists(x, y, zoom)) {
                    return null
                }

                try {
                    return URL(s)
                } catch (e: MalformedURLException) {
                    throw AssertionError(e)
                }

            }
            private fun checkTileExists(x: Int, y: Int, zoom: Int): Boolean {
                val minZoom = 12
                val maxZoom = 32

                return if (zoom < minZoom || zoom > maxZoom) {
                    false
                } else true

            }
        }
        map.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))
    }

    private fun updateUserLatLng(curUserLocation: DoubleArray) {
        val latLng = LatLng(curUserLocation[0], curUserLocation[1])
        marker.position = latLng
    }
}
