package com.example.finddine.DevMenu


/*
 * Copyright (C) 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.Manifest
import android.Manifest.permission
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.example.finddine.R

/**
 * This is a simple splash screen (activity) for giving more details on why the user should approve
 * fine location permissions. If they choose to move forward, the permission screen is brought up.
 * Either way (approve or disapprove), this will exit to the MainActivity after they are finished
 * with their final decision.
 */
class LocationPermissionRequestActivity : AppCompatActivity(),
    ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If permissions granted, we start the main activity (shut this activity down).
        if (ActivityCompat.checkSelfPermission(
                this,
                permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            finish()
        }

        setContentView(R.layout.activity_location_permission_request)
    }

    fun onClickApprovePermissionRequest(view: View) {
        Log.d(TAG, "onClickApprovePermissionRequest()")

        // On 23+ (M+) devices, fine location permission not granted. Request permission.
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_FINE_LOCATION
        )
    }

    fun onClickDenyPermissionRequest(view: View) {
        Log.d(TAG, "onClickDenyPermissionRequest()")
        finish()
    }

    /*
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {

        val permissionResult = ("Request code: "
                + requestCode
                + ", Permissions: "
                + permissions
                + ", Results: "
                + grantResults)
        Log.d(TAG, "onRequestPermissionsResult(): $permissionResult")

        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            // Close activity regardless of user's decision (decision picked up in main activity).
            finish()
        }
    }

    companion object {

        private val TAG = "LocationPermission"

        /* Id to identify Location permission request. */
        private val PERMISSION_REQUEST_FINE_LOCATION = 1
    }
}
