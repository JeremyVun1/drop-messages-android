package com.example.drop_messages_android

import android.Manifest.permission
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.drop_messages_android.api.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tinder.scarlet.WebSocket
import kotlinx.android.synthetic.main.activity_loading.*
import kotlinx.android.synthetic.main.activity_no_internet.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// This is the main entry point which checks whether to route the user to the login/signup pages
// or directly launch the activity with stored credentials
class NoInternetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_internet)
        overridePendingTransition(0, 0)

        initAnimations()

        btn_retry.setOnClickListener {
            finish()
        }
    }

    /**
     * View Animations
     */
    private fun initAnimations() {
        // button pulse
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.95f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.95f, 1f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.9f, 1f)
        ObjectAnimator.ofPropertyValuesHolder(btn_retry, scaleX, scaleY, alpha).apply {
            interpolator = OvershootInterpolator()
            duration = 1000
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
        }.start()
    }
}