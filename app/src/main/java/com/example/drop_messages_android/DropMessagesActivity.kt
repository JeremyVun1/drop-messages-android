package com.example.drop_messages_android

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.example.drop_messages_android.api.*
import com.example.drop_messages_android.fragments.StackEmptyFragment
import com.example.drop_messages_android.fragments.TestFragmentSmall
import com.example.drop_messages_android.viewpager.VerticalPageAdapter
import com.example.drop_messages_android.viewpager.VerticalViewPager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tinder.scarlet.WebSocket
import kotlinx.android.synthetic.main.activity_drop_messages.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Main activity where user can make api requests through a web socket to create and retrieve data
 */
class DropMessagesActivity : AppCompatActivity() {

    private val locationManager by lazy { LocationManager(applicationContext) }
    private val gson by lazy { Gson() }

    private var socket: DropMessageService? = null
    private var userModel: UserModel? = null

    // page viewer references
    private var pageView: VerticalViewPager? = null
    private var pageAdapter: VerticalPageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drop_messages)

        userModel = intent.getParcelableExtra("user")

        CoroutineScope(Default).launch {
            setupSocketHandlers()
            setupButtonHandlers()
        }

        //initialiseTestUI()
        setupPageViewer()
    }

    /**
     * Error handling to recreate the socket manually if we have to do so for whatever reason
     */
    private fun setupButtonHandlers() {
        btn_get_top.setOnClickListener {
            println("getting top drops")
        }

        btn_get_latest.setOnClickListener {
            println("getting latest drops")
        }

        btn_create_drop.setOnClickListener {
            println("creating a drop")
        }

        btn_get_random.setOnClickListener {
            println("getting random drops")
        }

        btn_map.setOnClickListener {
            println("go to map")
        }

        btn_my_drops.setOnClickListener {
            println("get my drops")
        }
    }

    private fun loadFragmentsIntoPageViewer(fragments: MutableList<Fragment>) {
        pager.adapter = null

        fragments.add(StackEmptyFragment())
        pageAdapter!!.setFragments(fragments)

        pager.reset()
        pager.adapter = pageAdapter
    }

    private fun setupPageViewer() {
        val fragments = mutableListOf<Fragment>()
        val verticalPageAdapter = VerticalPageAdapter(
            fragments,
            supportFragmentManager
        )

        pager.adapter = verticalPageAdapter
        pager.offscreenPageLimit = 10
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
        else {
            //Socket closed
            socket!!.observeWebSocketEvent()
                .filter { it is WebSocket.Event.OnConnectionClosed }
                .subscribe {
                    println(">>[REC] Web socket connection was closed!")

                    // Scarlet should handle reconnection for us
                    // if not, logic goes here
                }


            // Socket response routing
            socket!!.observeSocketResponse()
                .subscribe {
                    println(">>[REC]: $it")

                    val category = it.category
                    val data = it.data

                    when (category) {
                        "socket" -> handleSocketStatusResponses(data)
                        "post" -> handlePostResponses(data)
                        "vote" -> handleVoteResponses(data)
                        "retrieve" -> handleRetrieveResponses(data)
                        "error" -> handleErrorResponses(data)
                        "notification" -> handleNotificationResponses(data)
                        "token" -> handleTokenResponses(data)
                    }
                }
        }
    }

    private fun handleSocketStatusResponses(data: String) {
        Log.d("DEBUG", data)
    }

    private fun handlePostResponses(data: String) {
        val response = gson.fromJson(data, PostDataResponse::class.java)
        println(response)
    }

    private fun handleRetrieveResponses(data: String) {
        val response = gson.fromJson(data, Array<DropMessage>::class.java)
        println(response)
    }

    private fun handleVoteResponses(data: String) {
        val response = gson.fromJson(data, PostDataResponse::class.java)
        println(response)
    }

    private fun handleErrorResponses(data: String) {
        Log.e("ERROR", data)
    }

    private fun handleNotificationResponses(data: String) {
        println(data)
    }

    private fun handleTokenResponses(data: String) {
        Log.d("DEBUG", data)
        // token message means we need to recreate a connection with a new token
        CoroutineScope(IO).launch {
            fetchJsonWebToken()
        }
    }

    private suspend fun fetchJsonWebToken() {
        val url = resources.getString(R.string.get_token_url)
        val gson = Gson()
        if (userModel == null) {
            Log.e("ERROR", "Must be logged in to refresh token")
            navToMainLoader()
        }
        val json = gson.toJson(GetTokenModel(userModel!!.username as String, userModel!!.password as String))

        Postie().sendPostRequest(applicationContext, url, json,
            {
                val response = gson.fromJson(it.toString(), JsonObject::class.java)
                if (response.has("token")) {
                    println("JWT response: ${response["token"]}")

                    //strip any " chars pended on by the api server
                    val token = response["token"]
                        .toString()
                        .removePrefix("\"")
                        .removeSuffix("\"")

                    userModel!!.token = token

                    // save new token to shared preferences file
                    CoroutineScope(IO).launch {
                        val sp = getSharedPreferences("Login", MODE_PRIVATE)
                        sp.edit().putString("token", token).commit()
                        val locationManager = LocationManager(applicationContext)
                        locationManager.updateLocation(::onLocationReceived, ::onLocationError)
                    }
                }
                else {
                    CoroutineScope(Default).launch {
                        Log.e("ERROR", "Server failed to provide a token")
                        Toast.makeText(applicationContext, "Server connection lost!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            {
                Log.e("POST", it.toString())

            }
        )
    }

    // event listener for location manager
    private fun onLocationReceived(location: Geolocation) {
        userModel!!.location = location
        CoroutineScope(IO).launch {
            setupConnection(location)
        }
    }
    private fun onLocationError(ex: Exception) {
        Log.e("ERROR", ex.toString())
        Toast.makeText(applicationContext, "Google play services error!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private suspend fun setupConnection(location: Geolocation) {
        if (userModel!!.token == null)
            navToMainLoader()
        else {
            val socket = SocketManager.init(application).createSocket()

            // send authentication token as soon as web socket is opened
            socket.observeWebSocketEvent()
                .filter { it is WebSocket.Event.OnConnectionOpened<*> }
                .subscribe {
                    socket.authenticate(
                        AuthenticateSocket(
                            userModel!!.token as String,
                            location.lat.toFloat(),
                            location.long.toFloat()
                        )
                    )
                    println("<<[SND]Authenticate: ${userModel!!.token} @${location}")
                }

            // observe response from the token authentication
            socket.observeSocketResponse()
                .subscribe {
                    println(">>[REC]: $it")
                    if (it.category == "socket" && it.data == "open") {
                        println(">>[REC]Authenticated")
                    }
                    // failed token authentication for whatever reason
                    else {
                        Log.e("ERROR", "SOCKET AUTH FAILED")
                        Toast.makeText(applicationContext, "Could not connect to server!", Toast.LENGTH_SHORT).show()
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
     * TESTING
     */
    private fun initialiseTestUI() {

        val fragments = mutableListOf<Fragment>(
            TestFragmentSmall(),
            TestFragmentSmall(),
            StackEmptyFragment()
        )
        pageAdapter = VerticalPageAdapter(
            fragments,
            supportFragmentManager
        )

        pager.adapter = pageAdapter
        pager.offscreenPageLimit = 10

        // check if we are on the last page. scroll up on last page will attempt to retrieve more messages from the server
        pager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            private var scrolling: Boolean = false
            private var lastPageScrolled: Boolean = false

            override fun onPageScrollStateChanged(state: Int) {
                scrolling = state == 1
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (scrolling && position == pageAdapter!!.count - 1 && !lastPageScrolled) {
                    lastPageScrolled = true
                    println("last page scrolled")

                    // get more pages here
                }
            }

            override fun onPageSelected(position: Int) {
                if (position == pageAdapter!!.count - 1) {
                    println("on last page")
                } else {
                    lastPageScrolled = false
                }
            }
        })


        // setup button listeners
        // test adding a fragment
        btn_get_top.setOnClickListener {
            println("adding a fragment")
            pageAdapter!!.addFragment(TestFragmentSmall())
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
                StackEmptyFragment()
            )

            pageAdapter!!.setFragments(new_frags)

            pager.reset()
            pager.adapter = pageAdapter
        }

        // test swapping out the fragments
        btn_my_drops.setOnClickListener {
            println("swapping with 4 fragments")
            pager.adapter = null

            val new_frags = mutableListOf<Fragment>(
                TestFragmentSmall(),
                TestFragmentSmall(),
                TestFragmentSmall(),
                TestFragmentSmall(),
                StackEmptyFragment()
            )

            pageAdapter!!.setFragments(new_frags)

            pager.reset()
            pager.adapter = pageAdapter
        }
    }
}