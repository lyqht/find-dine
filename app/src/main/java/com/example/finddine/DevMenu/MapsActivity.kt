package com.example.finddine.DevMenu

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.finddine.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var marker: Marker

    // RTT Service
    private lateinit var wifiRttService: WifiRttService

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

    }

    private fun updateUserLatLng(curUserLocation: DoubleArray) {
        val latLng = LatLng(curUserLocation[0], curUserLocation[1])
        marker.position = latLng
    }
}
