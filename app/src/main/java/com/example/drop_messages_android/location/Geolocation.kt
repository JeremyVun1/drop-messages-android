package com.example.drop_messages_android.location

data class Geolocation(var lat: Double, var long: Double) {
    fun formattedString(dp: Int = 2): String {
        return "${"%.${dp}f".format(lat)}, ${"%.${dp}f".format(lat)}"
    }

    fun isValid(): Boolean {
        return (lat >= -90 && lat <= 90 && long >= -180 && long <= 180)
    }
}