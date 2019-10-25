package com.example.drop_messages_android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_test.*


class TestActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        handlePermissions()

        val flp = LocationServices.getFusedLocationProviderClient(this)

        btn_get_location.setOnClickListener {
            println("clicked")
            flp.lastLocation.addOnSuccessListener {
                println("flp success")
                tv_lat.text = it.latitude.toString()
                tv_long.text = it.longitude.toString()
            }
        }
    }

    fun handlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }
    }
}