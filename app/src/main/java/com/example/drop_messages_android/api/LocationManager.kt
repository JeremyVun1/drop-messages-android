package com.example.drop_messages_android.api

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar


class LocationManager(val context: Context) {
    private var flpClient: FusedLocationProviderClient? = null
    private var currLoc: Location? = null

    init {
        flpClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun getLastKnownLocation() : Geolocation {
        if (!hasLocationPermissions() || currLoc == null) {
            // Alert the user that they need to enable location permissions
            // or else it will default them to Geolocation(1,1)
            Toast.makeText(context, "Defaulting to lat,long (1,1). Enable location permissions to use your current location", Toast.LENGTH_LONG).show()
            return Geolocation(1.0, 1.0)
        }
        else return Geolocation(currLoc!!.latitude, currLoc!!.longitude)
    }

    fun updateLocation(listener: (loc: Geolocation) -> Unit, errorListener: (ex: Exception) -> Unit) {
        println("UPDATING LOCATION")
        flpClient!!.lastLocation.addOnSuccessListener {
            println("WE GOT THE LOCATION!")
            currLoc = it
            listener(getLastKnownLocation())
        }.addOnFailureListener {
            println("LOCATION FAILURE")
            errorListener(it)
        }.addOnCanceledListener {
            errorListener(Exception("cancelled"))
        }
    }

    private fun hasLocationPermissions() : Boolean {
        return context.checkSelfPermission(permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}