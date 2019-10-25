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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
    }

    /**
     * Do stuff in onResume so that we can also "restart" the activity on back button
     */
    override fun onResume() {
        super.onResume()

        initAnimations()

        /**
         * start Coroutine on UI thread to handle user routing
         * Hand off work to coroutines on IO/Default scopes as we go
         */
        CoroutineScope(Main).launch {
            handlePermissionRequests()
            handleRouting()
        }
    }


    /**
     * Need to handle 3 states
     * 1) User details are not stored
     *      - route to UserFrontActivity
     * 2) User details are stored but they don't have a JWT token
     *      - make token request
     *      - attempt to create web socket connection
     *      - route to MainActivity
     * 3) User details are stored, and they have a JWT token to make web socket connection
     *      - attempt to create web socket connection
     *      - route to MainActivity
     */
    private suspend fun handleRouting() {
        val userDetails = getUserDetails()

        when {
            !Util.hasInternet(applicationContext) -> {
                navToNoInternet()
            }
            userDetails == null -> {
                navToUserFront()
            }
            userDetails.token == null -> {
                println("ROUTE getting tokens")
                print("user details: $userDetails")
                getToken(userDetails)
            }
            else -> {
                println("ROUTE making web socket connection")
                connect(userDetails.token)
            }
        }
    }

    private suspend fun getToken(model: SignInModel) {
        setLoadingTextAsync("Fetching Authentication Token")

        withContext(IO) {
            val url = resources.getString(R.string.get_token_url)
            val gson = Gson()
            val json = gson.toJson(GetTokenModel(model.username as String, model.password as String))

            Postie().sendPostRequest(applicationContext, url, json,
                {
                    val response = gson.fromJson(it.toString(), JsonObject::class.java)
                    if (response.has("token")) {
                        setLoadingText("Token received!")
                        println("JWT response: ${response["token"]}")

                        CoroutineScope(Main).launch {
                            val token = response["token"].toString()
                            val sp = getSharedPreferences("Login", Context.MODE_PRIVATE)
                            sp.edit().putString("token", token).apply()

                            // connect the web socket
                            connect(token)
                        }
                    }
                    else {
                        CoroutineScope(Default).launch {
                            setLoadingTextAsync("Failed to get Token")
                            delay(2000)
                            navToUserFront()
                        }
                    }
                },
                {
                    Log.e("POST", it.toString())
                    Toast.makeText(applicationContext, it.toString(), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private suspend fun connect(token: String) {
        setLoadingTextAsync("Connecting to server")
        withContext(IO) {
            // do connection stuff here
            val socket = DropMessageServiceFactory.createSocket(application)

            socket.observeWebSocketEvent()
                .filter { it is WebSocket.Event.OnConnectionOpened<*> }
                .subscribe {
                    println("connection opened")
                    socket.authenticate(AuthenticateSocket(token))
                    println("attempt socket auth: $token")
                }

            socket.observeSocketResponse()
                .subscribe {
                    println("RESPONSE: ${it.category} - ${it.data}")
                }
        }
    }

    private suspend fun getUserDetails() : SignInModel? {
        println("GET USER DETAILS")
        setLoadingTextAsync("Finding User Details")
        var result : SignInModel? = null

        withContext(IO) {
            val sp = getSharedPreferences("Login", Context.MODE_PRIVATE)
            val username = sp.getString("username", null)
            val encryptedPassword = sp.getString("password", null)
            val token = sp.getString("token", null)

            if (username != null && encryptedPassword != null) {
                val alias = username.toLowerCase().hashCode().toString()
                val password = UserStorageManager.decrypt(alias, encryptedPassword, applicationContext)
                result = SignInModel(username, password, token)
            }
        }

        return result
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

        requestPermissions(permissions.toTypedArray(), 1)
    }

    private suspend fun hasPermission(permission: String) : Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
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