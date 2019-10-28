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
import com.google.android.gms.common.api.GoogleApiClient
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tinder.scarlet.WebSocket
import kotlinx.android.synthetic.main.activity_loading.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// This is the main entry point which checks whether to route the user to the login/signup pages
// or directly launch the activity with stored credentials
class MainLoaderActivity : AppCompatActivity() {

    var waitingForPermissions = false
    var userModel : UserModel? = null

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

        setLoadingText("Checking Google Play Availability")
        if (!Util.hasGooglePlayServices(applicationContext)) {
            setLoadingText("Google play services not found!")
            finish()
        }

        CoroutineScope(Default).launch {
            handlePermissionRequests()

            //spin while we wait for user to respond to a potential permission request
            while (waitingForPermissions) { delay(100) }

            handleRouting()
        }
    }


    /**
     * Need to handle 3 states
     * 1) User has no internet
     * 2) User details are not stored
     *      - route to UserFrontActivity
     * 3) User details are stored
     *      - make token request
     *      - attempt to create web socket connection
     *      - route to MainActivity
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


    // event listener for location manager
    private fun onLocationReceived(location: Geolocation) {
        userModel!!.location = location
        println("location gotten: $location")
        CoroutineScope(IO).launch {
            setupConnection(location)
        }
    }
    private fun onLocationError(ex: Exception) {
        CoroutineScope(IO).launch {
            Log.e("ERROR", ex.message)
            setLoadingTextAsync(ex.message as String)
            delay(resources.getInteger(R.integer.STATUS_PAUSE_MS_LONG).toLong())
            finish()
        }
    }

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
                    .init(application).getWebSocket()

                // send authentication token as soon as web socket is opened
                socket!!.observeWebSocketEvent()
                    .filter { it is WebSocket.Event.OnConnectionOpened<*> }
                    .subscribe {
                        socket.authenticate(
                            AuthenticateSocket(
                                userModel!!.token as String,
                                location.lat.toFloat(),
                                location.long.toFloat()
                            )
                        )
                        println("<<[SND]attempt auth: ${userModel!!.token} @${location}")
                    }

                // observe response from the token authentication
                socket.observeAuthResponse()
                    .subscribe {
                        println(">>[REC]: $it")
                        if (it.category == "socket" && it.data == "open") {
                            println("SOCKET AUTHENTICATED")

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
     * Handle permission requests
     */
    private suspend fun handlePermissionRequests() {
        val permissions = mutableListOf<String>()

        // check internet
        if (!hasPermission(permission.INTERNET))
            permissions.add(permission.INTERNET)

        // location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermission(permission.ACCESS_FINE_LOCATION))
                permissions.add(permission.ACCESS_FINE_LOCATION)
            if (!hasPermission(permission.ACCESS_COARSE_LOCATION))
                permissions.add(permission.ACCESS_COARSE_LOCATION)
        }

        if (permissions.size > 0) {
            waitingForPermissions = true
            requestPermissions(permissions.toTypedArray(), 1)
        }
    }

    private suspend fun hasPermission(permission: String) : Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        waitingForPermissions = false
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
     * Navigation function
     */
    private suspend fun navToUserFront() {
        withContext(Main) {
            val i = Intent(applicationContext, UserFrontActivity::class.java)
            startActivity(i)
            finishActivity(0)
        }
    }

    private suspend fun navToNoInternet() {
        withContext(Main) {
            val i = Intent(applicationContext, NoInternetActivity::class.java)
            startActivity(i)
        }
    }

    private fun navToDropMessagesActivity() {
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