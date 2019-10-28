package com.example.drop_messages_android.api

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.drop_messages_android.Util
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar


class LocationHandler(val context: Context) : GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationCallback() {
    private var apiClient: GoogleApiClient = GoogleApiClient.Builder(context)
        .addApi(LocationServices.API)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .build()

    private var flpClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private var request: LocationRequest = LocationRequest.create()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(Util.LOCATION_INTERVAL)
        .setFastestInterval(Util.LOCATION_INTERVAL)

    init {
        apiClient.connect()
    }

    fun disconnect() {
        if (apiClient.isConnected)
            apiClient.disconnect()
    }

    fun connect() {
        if (!apiClient.isConnected) {
            apiClient.connect()
        }
    }

    fun getLastLocation(listener: (loc: Geolocation) -> Unit, errorListener: (ex: Exception) -> Unit) {
        if (hasLocationPermissions()) {
            if (!apiClient.isConnected)
                apiClient.connect()

            flpClient.lastLocation.addOnSuccessListener {
                if (it == null) {
                    Toast.makeText(context, "Could not get location, defaulting to (1,1)", Toast.LENGTH_LONG).show()
                    listener(Geolocation(1.00, 1.00))
                }
                else listener(Geolocation(it.latitude, it.longitude))
            }.addOnFailureListener {
                errorListener(it)
            }
        }
        else errorListener(Exception("App requires location permissions"))
    }

    fun subscribe(listener: (loc: Geolocation) -> Unit) {
        flpClient.requestLocationUpdates(request, this, Looper.getMainLooper())
    }

    private fun hasLocationPermissions() : Boolean {
        return context.checkSelfPermission(permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Google API client callbacks
     */
    override fun onConnected(p0: Bundle?) {
        Log.d("FLP", "google api client connected")
        //flpClient.requestLocationUpdates(request,this, apiClient)
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d("FLP", "google api client disconnected")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.e("FLP", "google api client failed to connect")
    }

    /**
     * FLP subscription callbacks
     */
    override fun onLocationResult(result: LocationResult?) {
        result ?: return
        for (loc in result.locations) {
            println("FLP SUBSCRIPTION EVENT: $loc")
        }
    }

    override fun onLocationAvailability(result: LocationAvailability?) {
        super.onLocationAvailability(result)
    }
}