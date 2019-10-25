package com.example.drop_messages_android.location

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

object LocationHandler {

}