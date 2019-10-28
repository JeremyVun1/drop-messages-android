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
import androidx.appcompat.widget.Toolbar
import com.example.drop_messages_android.api.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tinder.scarlet.WebSocket
import kotlinx.android.synthetic.main.activity_drop_messages.*
import kotlinx.android.synthetic.main.activity_loading.*
import kotlinx.android.synthetic.main.activity_no_internet.*
import kotlinx.android.synthetic.main.activity_no_internet.toolbar
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

        setSupportActionBar(toolbar as Toolbar)

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
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.97f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.97f, 1f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.92f, 1f)
        val slideZ = PropertyValuesHolder.ofFloat(View.TRANSLATION_Z, 0f, 1.5f)
        ObjectAnimator.ofPropertyValuesHolder(btn_retry, scaleX, scaleY, slideZ, alpha).apply {
            interpolator = OvershootInterpolator()
            duration = 1200
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
        }.start()
    }
}