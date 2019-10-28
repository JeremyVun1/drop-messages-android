package com.example.drop_messages_android

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability


object Util {
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

    val MAX_MESSAGE_LENGTH = 240
    val LOCATION_INTERVAL = 1000L
}

/**
 * Extension functions
 */
fun Float.format(digits: Int) = "%.${digits}f".format(this)

/**
 * Enums
 */
enum class VoteState {
    UPVOTE,
    DOWNVOTE,
    NONE;
}

