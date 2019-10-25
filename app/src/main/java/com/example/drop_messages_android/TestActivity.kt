package com.example.drop_messages_android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_test.*


class TestActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        val flp = LocationServices.getFusedLocationProviderClient(this)
        btn_get_location.setOnClickListener {

            flp.lastLocation.addOnSuccessListener {
                println("flp success")
                tv_lat.text = it.latitude.toString()
                tv_long.text = it.longitude.toString()
            }
        }
    }










}