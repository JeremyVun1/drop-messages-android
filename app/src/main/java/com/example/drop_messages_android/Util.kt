package com.example.drop_messages_android

import android.app.Activity
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.Manifest.permission
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlin.math.pow


object Util {
    /**
     * Constants
     */
    val MAX_MESSAGE_LENGTH by lazy { 240 }
    val LOCATION_INTERVAL by lazy { 1000L }
    val DEFAULT_MAP_ZOOM by lazy { 17f }

    /**
     * Check whether there is internet or not
     */
    fun hasInternet(context: Context): Boolean {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).apply {
            return getNetworkCapabilities(activeNetwork)?.run {
                when {
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } ?: false
        }
    }

    /**
     * Check if user has google play
     */
    fun hasGooglePlayServices(context: Context): Boolean {
        val googleApiInstance = GoogleApiAvailability.getInstance()
        val result = googleApiInstance.isGooglePlayServicesAvailable(context)
        if (result != ConnectionResult.SUCCESS) {
            googleApiInstance.getErrorDialog(context as Activity, result, 9000).show()
            return false
        }
        return true
    }

    /**
     * Check if user has gps
     */
    fun hasGpsProvider(context: Context) : Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Check user has given the app permissions
     */
    var waitingForPermissions = false
    fun getPermissions(activity: Activity) {
        val permissions = mutableListOf<String>()

        // check internet permissions
        if (activity.checkSelfPermission(permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
            permissions.add(permission.INTERNET)

        // check location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                permissions.add(permission.ACCESS_FINE_LOCATION)
            if (activity.checkSelfPermission(permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                permissions.add(permission.ACCESS_COARSE_LOCATION)
        }

        if (permissions.size > 0) {
            waitingForPermissions = true
            activity.requestPermissions(permissions.toTypedArray(), 1)
        }
    }
}

/**
 * Extension functions
 */
fun Float.format(digits: Int) = "%.${digits}f".format(this)
fun Double.format(digits: Int) = "%.${digits}f".format(this)

fun Double.round(decimals: Int): Double {
    if (decimals < 0) return this

    val factor = Math.pow(10.toDouble(), decimals.toDouble()).toLong()
    val value = this * factor
    val tmp = Math.round(value)
    return (tmp / factor).toDouble()
}

/**
 * Enums
 */
enum class VoteState {
    UPVOTE,
    DOWNVOTE,
    NONE;
}

