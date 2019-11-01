package com.example.drop_messages_android

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_loading.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Main entry point that routes user either to sign in/register or to the main activity
 * based on whether they were previously logged in or not. Handles web socket creation, authentication etc.
 */
class MainLoaderActivity : AppCompatActivity() {

    private var waitingForPermissions = false
    private var userModel : UserModel? = null
    private var authResponseObserver: Disposable? = null

    private var locationHandler: LocationHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        locationHandler = LocationHandler(applicationContext)
    }

    /**
     * Do stuff in onResume so that we can also "restart" the activity on back button
     * Also prevents user from being sneaky and bypassing permissions
     */
    override fun onResume() {
        super.onResume()

        initAnimations()

        /**
         * Perform checks
         * 1) google services is available
         * 2) user has gps
         * 3) location and internet permissions
         */

        //Check google play services
        setLoadingText("Checking Google Play Availability")
        if (!Util.hasGooglePlayServices(applicationContext)) {
            CoroutineScope(Main).launch {
                setLoadingTextAsync("Google play services not found!")
                delay(resources.getInteger(R.integer.STATUS_PAUSE_MS_LONG).toLong())
                finish()
            }
        }
        // check gps
        if (!Util.hasGpsProvider(applicationContext)) {
            CoroutineScope(Main).launch {
                setLoadingTextAsync("No GPS provider found")
                delay(resources.getInteger(R.integer.STATUS_PAUSE_MS_LONG).toLong())
                finish()
            }
        }

        Util.getPermissions(this)

        CoroutineScope(Default).launch {
            //spin while we wait for user to respond to a potential permission request
            while (Util.waitingForPermissions) { delay(100) }

            handleRouting()
        }
    }

    /**
     * permission request callback
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        var gotPermissions = true

        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                gotPermissions = false
                println("permission denied")
                finish()
            }
        }

        if (gotPermissions) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            Util.waitingForPermissions = false
        }
    }

    /**
     * Need to handle 3 states
     * 1) User has no internet connection
     * 2) User details are not stored
     *      - route to UserFrontActivity where user logs in / registers
     * 3) User details are stored
     *      - get JWT token from server
     *      - create web socket and authenticate with JWT
     *      - route to Drop Message Activity
     */
    private suspend fun handleRouting() {
        withContext(IO) {
            setLoadingTextAsync("Finding User Details")
            userModel = UserStorageManager.getUserDetails(applicationContext)

            when {
                // no internet connection error
                !Util.hasInternet(applicationContext) -> navToNoInternet()

                // no user details, route to user front activity
                userModel == null -> {
                    println("ROUTING TO USER FRONT")
                    setLoadingTextAsync("Welcome first time user!")
                    delay(resources.getInteger(R.integer.STATUS_PAUSE_MS_LONG).toLong())
                    navToUserFront()
                }

                // get JWT from server and use it to authenticate a web socket connection
                else -> fetchJsonWebToken()
            }
        }
    }

    private suspend fun fetchJsonWebToken() {
        setLoadingTextAsync("Fetching Authentication Token")

        val url = resources.getString(R.string.get_token_url)
        val gson = Gson()
        val json = gson.toJson(GetTokenModel(userModel!!.username as String, userModel!!.password as String))

        Postie().sendPostRequest(applicationContext, url, json,
            {
                val response = gson.fromJson(it.toString(), JsonObject::class.java)
                if (response.has("token")) {
                    setLoadingText("Token received!")
                    println("JWT response: ${response["token"]}")

                    CoroutineScope(Main).launch {
                        //strip any " chars pended on by the api server
                        val token = response["token"]
                            .toString()
                            .removePrefix("\"")
                            .removeSuffix("\"")

                        val sp = getSharedPreferences("Login", Context.MODE_PRIVATE)
                        sp.edit().putString("token", token).commit()

                        userModel!!.token = token

                        setLoadingText("Fetching current location")
                        locationHandler!!.connect()
                        locationHandler!!.getLastLocation(::onLocationReceived, ::onLocationError)
                    }
                }
                else {
                    CoroutineScope(Default).launch {
                        setLoadingTextAsync("Failed to get Token")
                        delay(resources.getInteger(R.integer.STATUS_PAUSE_MS_LONG).toLong())
                        navToUserFront()
                    }
                }
            },
            {
                Log.e("POST", it.toString())
                Toast.makeText(applicationContext, it.toString(), Toast.LENGTH_SHORT).show()

                CoroutineScope(Default).launch {
                    setLoadingTextAsync("Server connection failed")

                    delay(resources.getInteger(R.integer.STATUS_PAUSE_MS_LONG).toLong())
                    navToUserFront()
                }
            }
        )
    }


    /**
     * Event listeners for google location services
     */
    private fun onLocationReceived(location: Geolocation) {
        userModel!!.location = location
        println("location received: $location")
        CoroutineScope(IO).launch {
            setupConnection(location)
        }
    }
    private fun onLocationError(ex: Exception) {
        CoroutineScope(IO).launch {
            Log.e("ERROR", ex.message ?: "error")
            setLoadingTextAsync(ex.message ?: "error")
            delay(resources.getInteger(R.integer.STATUS_PAUSE_MS_LONG).toLong())
            finish()
        }
    }

    /**
     * Set up the web socket and authenticate it with server
     */
    private suspend fun setupConnection(location: Geolocation) {
        withContext(Main) {
            if (userModel!!.token == null) {
                setLoadingTextAsync("Token not found")
                delay(resources.getInteger(R.integer.STATUS_PAUSE_MS_SHORT).toLong())
                navToUserFront()
            } else {
                setLoadingTextAsync("Connecting to server")

                // initialise our socket singleton
                val socket = SocketManager
                    .init(application, location, userModel!!)
                    .getWebSocket()

                // observe response from the token authentication
                authResponseObserver = socket!!.observeAuthResponse()
                    .subscribe {
                        println(">>[REC]: $it")
                        if (it.category == "socket" && it.data == "open") {
                            SocketManager.authenticated = true

                            CoroutineScope(Default).launch {
                                setLoadingTextAsync("Connection established")
                                delay(resources.getInteger(R.integer.STATUS_PAUSE_MS_SHORT).toLong())

                                navToDropMessagesActivity()
                            }
                        } else if (it.category == "socket" && it.data == "closed") {
                            CoroutineScope(Default).launch {
                                setLoadingTextAsync("Authentication failed")
                                SocketManager.closeSocket()

                                delay(resources.getInteger(R.integer.STATUS_PAUSE_MS_SHORT).toLong())

                                navToUserFront()
                            }
                        }
                    }
            }
        }
    }

    /**
     * utility functions to change the output of our loading text
     */
    private suspend fun setLoadingTextAsync(text: String) {
        withContext(Main) {
            setLoadingText(text)
        }
    }
    @SuppressLint("SetTextI18n")
    private fun setLoadingText(text: String) {
        tv_loading.text = "$text . . ."
    }

    /**
     * Navigation functions
     */
    private suspend fun navToUserFront() {
        withContext(Main) {
            authResponseObserver?.dispose()
            val i = Intent(applicationContext, UserFrontActivity::class.java)
            startActivity(i)
            finish()
        }
    }

    private suspend fun navToNoInternet() {
        withContext(Main) {
            val i = Intent(applicationContext, NoInternetActivity::class.java)
            startActivity(i)
        }
    }

    private fun navToDropMessagesActivity() {
        authResponseObserver?.dispose()
        val i = Intent(applicationContext, DropMessagesActivity::class.java)
        i.putExtra("user", userModel)
        startActivity(i)

        finish() // back button should not come back to the main loader
    }

    /**
     * View Animations
     */
    private fun initAnimations() {
        // dots animation on loading text
        val slideX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, 120f)

        ObjectAnimator.ofPropertyValuesHolder(img_white_cover, slideX).apply {
            interpolator = LinearInterpolator()
            duration = 2500
            repeatMode = ObjectAnimator.RESTART
            repeatCount = ObjectAnimator.INFINITE
        }.start()

        // logo pulse
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f, 1f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.8f, 1f)
        ObjectAnimator.ofPropertyValuesHolder(img_logo, scaleX, scaleY, alpha).apply {
            interpolator = OvershootInterpolator()
            duration = 1000
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
        }.start()
    }
}