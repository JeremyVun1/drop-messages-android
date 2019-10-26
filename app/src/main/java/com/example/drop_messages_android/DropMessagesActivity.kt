package com.example.drop_messages_android

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.api.*
import com.example.drop_messages_android.fragments.TestFragmentSmall
import com.example.drop_messages_android.viewpager.VerticalPageAdapter
import com.tinder.scarlet.WebSocket
import kotlinx.android.synthetic.main.activity_drop_messages.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Main activity where user can make api requests through a web socket to create and retrieve data
 */
class DropMessagesActivity : AppCompatActivity() {

    private var socket: DropMessageService? = null
    private val locationManager by lazy {
        LocationManager(applicationContext)
    }
    private var userModel: UserModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drop_messages)

        userModel = intent.getParcelableExtra("user")

        initialiseTestUI()
        //initialiseUI()
    }

    private fun initialiseTestUI() {
        val fragments = mutableListOf<Fragment>(
            TestFragmentSmall(),
            TestFragmentSmall()
        )
        val pageAdapater = VerticalPageAdapter(
            fragments,
            supportFragmentManager
        )

        pager.adapter = pageAdapater
        pager.offscreenPageLimit = 10


        // setup button listeners
        // test adding a fragment
        btn_get_top.setOnClickListener {
            println("adding a fragment")
            pageAdapater.addFragment(TestFragmentSmall())
        }

        // test swapping out the fragments
        btn_create_drop.setOnClickListener {
            println("swapping with 10 fragments")
            pager.adapter = null

            val new_frags = mutableListOf<Fragment>(
                TestFragmentSmall(),
                TestFragmentSmall(),
                TestFragmentSmall(),
                TestFragmentSmall(),
                TestFragmentSmall(),
                TestFragmentSmall(),
                TestFragmentSmall(),
                TestFragmentSmall(),
                TestFragmentSmall(),
                TestFragmentSmall()
            )

            pageAdapater.setFragments(new_frags)

            pager.adapter = pageAdapater
        }

        // test swapping out the fragments
        btn_my_drops.setOnClickListener {
            println("swapping with 4 fragments")
            pager.adapter = null

            val new_frags = mutableListOf<Fragment>(
                TestFragmentSmall(),
                TestFragmentSmall(),
                TestFragmentSmall(),
                TestFragmentSmall()
            )

            pageAdapater.setFragments(new_frags)

            pager.adapter = pageAdapater
        }
    }

    private fun initialiseUI() {
        val fragments = mutableListOf<Fragment>()
        val verticalPageAdapter = VerticalPageAdapter(
            fragments,
            supportFragmentManager
        )

        pager.adapter = verticalPageAdapter
        pager.offscreenPageLimit = 10
    }

    override fun onResume() {
        super.onResume()

        initAnimations()

        CoroutineScope(IO).launch {
            //setupSocketHandlers()
        }
    }

    /**
     * handling of different socket REQUEST & RESPONSES messages
     */
    private suspend fun setupSocketHandlers() {
        if (!Util.hasInternet(applicationContext))
            navToNoInternet()

        socket = SocketManager.getWebSocket()
        if (socket == null) {
            Log.e("ERROR", "Cannot use a null socket")
            println("SOCKET IS NULL ERROR")
            navToMainLoader()
        }

        /**
         * Web socket message handling
         *
         * Socket Open event
         */
        socket!!.observeWebSocketEvent()
            .filter { it is WebSocket.Event.OnConnectionOpened<*> }
            .subscribe {
                socket!!.authenticate(AuthenticateSocket(userModel!!.token as String))
                println("<<[SND]attempt socket auth: ${userModel!!.token}")
            }

        /**
         * Socket Close event
         */
        socket!!.observeWebSocketEvent()
            .filter { it is WebSocket.Event.OnConnectionClosed }
            .subscribe {
                println(">>[REC] Web socket connection was closed!")

                // Scarlet should handle reconnection for us
                // if not, logic goes here
            }



        /**
         * Create Drop Message
         */
        //socket.downvote()

        /**
         * Change Geolocation block
         */

        /**
         * Get Top messages
         */

        /**
         * Get Newest messages
         */

        /**
         * Get random messages
         */

        /**
         * Get messages within radius
         */

        /**
         * Get my messages
         */

        /**
         * Upvote
         */

        /**
         * Downvote
         */

        /**
         * Server response handling
         */
        socket!!.observeSocketResponse()
            .subscribe {
                println(">>[REC]: $it")
                val category = it.category
                val data = it.data

                when {
                    category == "socket" -> {
                        handleSocketStatusResponses(data)
                    }
                    category == "post" -> {
                        handlePostResponses(data)
                    }
                    category == "retrieve" -> {
                        handleRetrieveResponses(data)
                    }
                    category == "error" -> {
                        handleErrorResponses(data)
                    }
                    category == "notification" -> {
                        handleNotificationResponses(data)
                    }
                }
            }
    }

    private fun handleSocketStatusResponses(data: String) {
        println(data)
    }

    private fun handlePostResponses(data: String) {
        println(data)
    }

    private fun handleRetrieveResponses(data: String) {
        println(data)
    }

    private fun handleErrorResponses(data: String) {
        println(data)
    }

    private fun handleNotificationResponses(data: String) {
        println(data)
    }

    /**
     * If we need to get a new token

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
                            val token = response["token"]
                                .toString()
                                .removePrefix("\"")
                                .removeSuffix("\"")

                            val sp = getSharedPreferences("Login", Context.MODE_PRIVATE)
                            sp.edit().putString("token", token).commit()
                            println(sp.getString("token", null))

                            // connect the web socket
                            setupConnection(token)
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
     */


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
            // waitingForPermissions = true
            requestPermissions(permissions.toTypedArray(), 1)
        }
    }

    private suspend fun hasLocationPermissions() : Boolean {
        return hasPermission(permission.ACCESS_COARSE_LOCATION) && hasPermission(permission.ACCESS_FINE_LOCATION)
    }

    private suspend fun hasPermission(permission: String) : Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        println("permissions granted!")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // waitingForPermissions = false
    }

    /**
     * Navigation function
     */
    private suspend fun navToMainLoader() {
        withContext(Main) {
            val i = Intent(applicationContext, MainLoaderActivity::class.java)
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

    /**
     * View Animations
     */
    private fun initAnimations() {
        /*
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

         */
    }
}