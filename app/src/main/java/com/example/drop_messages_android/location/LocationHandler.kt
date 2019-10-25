package com.example.drop_messages_android.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat

/**
 * Handles getting the user's lat, long location
 * decision logic between COARSE, FINE and permission handling
 *
 * 1. If user denies all permissions, we default to returning lat long (1, 1)
 *      - also manually request permission from them.
 * 2. If user only allows coarse, we use that
 * 3. If user only allows fine, we use that
 * 4. how much accuracay do i need?
 *
 * USE FLP
 */

/**
 * GPS high battery use
 * used when requested highest accuracy
 *
 * WIFI
 * pretty fine, can see where you are in a building
 *
 * CELL
 * good battery usage
 * coverage everywhere
 * city and neighbourhood location
 *
 * when device is stationary, switch to accelerometer to save battery
 * location batching
 *
 * set interval(50_000) every minute
 * setmaxwaittime(300_000) enables batching for 5 minutes
 */

/**
 * to see location immediately, flush the FLP
 * status = FusedLocationPRoviderAPI.flushLocations(mClient)
 */

/**
 * If we are in the background, STOP!!!
 */

/**
 * USE GEOFENCING TO DETERMINE GEOLOC BLOCKS
 * Use geofencing to detect if user has entered, stayed in, or exited an area of interest
 */

object LocationHandler {
    fun getCurrentLocation() {

    }
    /*
    private fun runtimePermissions() : Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(getActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
            }
        }
    }
     */
}