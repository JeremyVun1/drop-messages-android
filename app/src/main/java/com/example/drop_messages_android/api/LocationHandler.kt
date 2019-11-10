package com.example.drop_messages_android.api

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.drop_messages_android.Util
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*


class LocationHandler(val context: Context) : GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationCallback() {
    private var apiClient: GoogleApiClient = GoogleApiClient.Builder(context)
        .addApi(LocationServices.API)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .build()

    private var flpClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private var request: LocationRequest = LocationRequest.create().apply {
        interval = Util.INTERVAL
        fastestInterval = Util.FASTEST_INTERVAL
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private var mListener: LocationUpdateListener? = null

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

    fun subscribe(listener: LocationUpdateListener) {
        mListener = listener
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

        println("LOCATION SUBSCRIPTION EVENT")
        val lastLocation = result.lastLocation
        val geoloc = Geolocation(lastLocation.latitude, lastLocation.longitude)
        mListener?.onLocationReceived(geoloc)
    }

    override fun onLocationAvailability(result: LocationAvailability?) {
        super.onLocationAvailability(result)
    }

    interface LocationUpdateListener {
        fun onLocationReceived(newLoc: Geolocation)
    }
}