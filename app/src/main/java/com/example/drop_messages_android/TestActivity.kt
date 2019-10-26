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
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.fragments.TestFragment
import com.example.drop_messages_android.viewpager.VerticalPageAdapter
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_test.*
import kotlinx.android.synthetic.main.activity_test_pager.*


class TestActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_pager)

        initTestUI()
    }

    private fun initTestUI() {
        val fragA = TestFragment()
        val fragB = TestFragment()
        val fragC = TestFragment()
        val fragD = TestFragment()

        val fragments = arrayOf<Fragment>(
            fragA,
            fragB,
            fragC,
            fragD
        )

        val verticalPageAdapter = VerticalPageAdapter(
            fragments,
            supportFragmentManager
        )
        fragment_container.adapter = verticalPageAdapter
        fragment_container.offscreenPageLimit = 10
    }
}