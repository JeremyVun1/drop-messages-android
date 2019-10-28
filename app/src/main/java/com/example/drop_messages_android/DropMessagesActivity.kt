package com.example.drop_messages_android

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.example.drop_messages_android.api.*
import com.example.drop_messages_android.fragments.CreateDropDialogFragment
import com.example.drop_messages_android.fragments.DropMessageFragment
import com.example.drop_messages_android.fragments.StackEmptyFragment
import com.example.drop_messages_android.viewpager.VerticalPageAdapter
import com.example.drop_messages_android.viewpager.VerticalViewPager
import com.example.drop_messages_android.fragments.CreateDropDialogFragment.CreateDropListener
import com.example.drop_messages_android.fragments.DropMessageFragment.DropMessageFragmentListener
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tinder.scarlet.WebSocket
import kotlinx.android.synthetic.main.activity_drop_messages.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Main activity where user can make api requests through a web socket to create and retrieve data
 */
class DropMessagesActivity : AppCompatActivity(), CreateDropListener, DropMessageFragmentListener {

    private val locationManager by lazy { LocationManager(applicationContext) }
    private val gson by lazy { Gson() }

    private var socket: DropMessageService? = null
    private var userModel: UserModel? = null

    private var lastRequest: DropRequest? = null
    private var pageNumber: Int = 1
    private var requesting: Boolean = false
    private var lastPageScrolled: Boolean = false

    // page viewer references
    private var pageAdapter: VerticalPageAdapter? = null
    private var fragments: MutableList<Fragment>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drop_messages)

        userModel = intent.getParcelableExtra("user")
        pager.offscreenPageLimit = 10

        CoroutineScope(Default).launch {
            setupSocketHandlers()
        }
        setupButtonHandlers()
        setupPageLoading()

        //initialiseTestUI()
        setupPageViewer()
    }

    /**
     * setup button click listeners
     */
    private fun setupButtonHandlers() {
        btn_get_top.setOnClickListener { requestDrops(DropRequest.GET_TOP) }
        btn_get_latest.setOnClickListener { requestDrops(DropRequest.GET_NEW) }
        btn_get_random.setOnClickListener { requestDrops(DropRequest.GET_RANDOM) }
        btn_my_drops.setOnClickListener { requestDrops(DropRequest.GET_MINE) }

        btn_create_drop.setOnClickListener {
            val dialog = CreateDropDialogFragment()
            val b = Bundle()
            b.putString("author", userModel!!.username)
            dialog.arguments = b

            dialog.show(supportFragmentManager, "Create Drop Message")
        }

        btn_map.setOnClickListener {
            println("go to map")
        }
    }

    private fun requestDrops(requestType: DropRequest) {
        if (!Util.hasInternet(applicationContext))
            navToNoInternet()

        if (lastRequest != requestType && !requesting) {
            pageNumber = 1
            requesting = true
            lastRequest = requestType
            socket!!.requestDrops(RequestDrops(requestType.value, pageNumber))

            setLoadingAnimation()
        }
    }

    /**
     * Request the next 'page' from the server when the user scrolls up on the last fragment in the page viewer
     */
    private fun setupPageLoading() {
        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            private var scrolling: Boolean = false

            override fun onPageScrollStateChanged(state: Int) {
                scrolling = state == 1
            }

            // check if the user tried to scroll the last page fragment
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (scrolling && position == pageAdapter!!.count - 1 && !lastPageScrolled) {
                    lastPageScrolled = true

                    // get more pages
                    if (lastRequest == null)
                        lastRequest = DropRequest.GET_MINE
                    socket!!.requestDrops(RequestDrops(lastRequest!!.value, pageNumber))
                }
            }

            override fun onPageSelected(position: Int) {
                if (position != pageAdapter!!.count - 1)
                    lastPageScrolled = false
            }
        })
    }

    /**
     * handling different socket REQUEST & RESPONSES messages
     */
    private fun setupSocketHandlers() {
        if (!Util.hasInternet(applicationContext))
            navToNoInternet()

        socket = SocketManager.getWebSocket()
        if (socket == null) {
            Log.e("ERROR", "Cannot use a null socket")
            navToMainLoader()
        }
        else {
            // Socket response routing
            socket!!.observeSocketResponse()
                .subscribe {
                    val category = it.category
                    val data = it.data

                    when (DropResponse.getEnum(category)) {
                        DropResponse.SOCKET -> handleSocketStatusResponses(data)
                        DropResponse.POST -> handlePostResponses(data)
                        DropResponse.VOTE -> handleVoteResponses(data)
                        DropResponse.RETRIEVE -> handleRetrieveResponses(data)
                        DropResponse.ERROR -> handleErrorResponses(data)
                        DropResponse.NOTIFICATION -> handleNotificationResponses(data)
                        DropResponse.TOKEN -> handleTokenResponses(data)
                        DropResponse.UNKNOWN -> Log.e("ERROR", "Unknown server response category")
                    }
                }
        }
    }

    private fun handlePostResponses(data: String) {
        val response = gson.fromJson(data, PostDataResponse::class.java)
        println("HANDLING POST RESPONSE:")
        println(response)

        if (response.result) {
            try {
                println("RESPONSE SUCCESS")
                println(response.meta)
                val fragment = createDropMessageFragment(response.echo)

                val geolocStr = "(${response.echo.lat.format(2)}, ${response.echo.long.format(2)})"
                addFragmentToPageViewer(fragment, "Message dropped at $geolocStr")
            } catch (ex: Exception) {
                Log.e("ERROR", Log.getStackTraceString(ex))
            }
        }
        else showSnackBar("Drop failed: ${response.meta}!")
    }

    private fun handleRetrieveResponses(data: String) {
        CoroutineScope(Main).launch {
            val response = gson.fromJson(data, Array<DropMessage>::class.java)

            if (response.isNotEmpty()) {
                val list = arrayListOf<Fragment>()
                for (message in response)
                    list.add(createDropMessageFragment(message))

                fragments = list
                loadFragmentsIntoPageViewer(list)

                pageNumber += 1
            }
            else {
                pageNumber = 1
                lastRequest = null
                lastPageScrolled = false
                showSnackBar("No more drop messages found!")
            }

            requesting = false
        }
    }

    private fun handleNotificationResponses(data: String) {
        val response = gson.fromJson(data, DropMessage::class.java)

        val fragment = createDropMessageFragment(response)

        addFragmentToPageViewer(fragment, "You picked up a new message!")
    }

    private fun addFragmentToPageViewer(fragment: Fragment, text: String) {
        CoroutineScope(Main).launch {
            if (fragments == null) {
                val fragmentList = mutableListOf(fragment)
                fragments = fragmentList
                loadFragmentsIntoPageViewer(fragmentList)
            }
            else {
                pageAdapter!!.addFragment(fragment)
                println("SIZE: ${fragments!!.size}")
                showSnackBarWithJump(text, fragments!!.size-1)
            }
        }
    }

    private fun removeFragmentFromPageViewer(msgId: Int) {
        CoroutineScope(Main).launch {
            pageAdapter!!.removeFragment(msgId)
            showSnackBar("Drop Message deleted!")
        }
    }

    private fun handleSocketStatusResponses(data: String) {
        Log.d("DEBUG", data)
    }

    private fun handleVoteResponses(data: String) {
        Log.d("VOTE", data)
    }

    private fun handleErrorResponses(data: String) {
        Log.e("ERROR", data)
    }

    private fun handleTokenResponses(data: String) {
        Log.d("DEBUG", data)

        // We need to get a new, fresh JWT token
        CoroutineScope(IO).launch {
            fetchJsonWebToken()
        }
    }

    /**
     * Handle creation and display of message fragments to the fragment page viewer
     */
    private fun createDropMessageFragment(model: DropMessage): Fragment {
        val b = Bundle()
        b.putParcelable("model", model)
        b.putBoolean("canDelete", model.author == userModel!!.username)

        val result = DropMessageFragment()
        result.arguments = b

        return result
    }

    private fun loadFragmentsIntoPageViewer(fragmentList: MutableList<Fragment>) {
        pager.adapter = null

        fragmentList.add(StackEmptyFragment())

        if (pageAdapter == null)
            pageAdapter = VerticalPageAdapter(fragmentList, supportFragmentManager)

        pageAdapter!!.setFragments(fragmentList)

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
     * Fragment callbacks
     */
    // listener for create drop message dialog fragment
    override fun onCreateDrop(msg: String) {
        println("creating msg: $msg")
        socket!!.createDrop(CreateDrop(DropRequest.CREATE_DROP.value, msg))
    }

    override fun onUpvote(id: Int?) {
        if (id != null) {
            println("upvoting msg: $id")
            socket!!.upvote(Upvote(DropRequest.UPVOTE.value, id.toString()))
        }
    }

    override fun onDownvote(id: Int?) {
        if (id != null) {
            println("downvoting msg: $id")
            socket!!.downvote(Downvote(DropRequest.DOWNVOTE.value, id.toString()))
        }
    }

    override fun onDelete(id: Int?) {
        if (id != null) {
            socket!!.delete(Delete(DropRequest.DELETE.value, id.toString()))
            removeFragmentFromPageViewer(id)
        }
    }

    /**
     * For refreshing JWT when it expires
     */
    private suspend fun fetchJsonWebToken() {
        val url = resources.getString(R.string.get_token_url)
        val gson = Gson()
        if (userModel == null) {
            Log.e("ERROR", "Must be logged in to refresh token")
            navToMainLoader()
        }
        val json = gson.toJson(
            GetTokenModel(
                userModel!!.username as String,
                userModel!!.password as String
            )
        )

        // ask mr postie to request a new token
        Postie().sendPostRequest(applicationContext, url, json,
            {
                val response = gson.fromJson(it.toString(), JsonObject::class.java)
                if (response.has("token")) {
                    println("JWT response: ${response["token"]}")

                    //strip any " chars pended on by the api server
                    val token = response["token"].toString()
                        .removePrefix("\"")
                        .removeSuffix("\"")

                    // save the new token in memory and in shared preferences
                    userModel!!.token = token
                    CoroutineScope(IO).launch {
                        val sp = getSharedPreferences("Login", MODE_PRIVATE)
                        sp.edit().putString("token", token).commit()

                        // get our geolocation to recreate a connection
                        val locationManager = LocationManager(applicationContext)
                        locationManager.updateLocation(::onLocationReceived, ::onLocationError)
                    }
                } else {
                    CoroutineScope(Default).launch {
                        Log.e("ERROR", "Server failed to provide a token")
                        Toast.makeText(
                            applicationContext,
                            "Server connection lost!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            {
                Log.e("POST", it.toString())

            }
        )
    }

    /**
     * Callbacks for Fused location provider
     */
    private fun onLocationReceived(location: Geolocation) {
        userModel!!.location = location
        CoroutineScope(IO).launch {
            println("SET UP THE SOCKET AGAIN")
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
            println("DROP MESSAGE ACTIVITY CREATING SOCKET")
            val socket = SocketManager.init(application).getWebSocket()

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
                    println("<<[SND]Authenticate: ${userModel!!.token} @${location}")
                }

            withContext(Main) {
                setupSocketHandlers()
            }
        }
    }

    /**
     * Handle permission requests
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * Navigation function
     */
    private fun navToMainLoader() {
        val i = Intent(applicationContext, MainLoaderActivity::class.java)
        startActivity(i)
        finish()
    }

    private fun navToNoInternet() {
        val i = Intent(applicationContext, NoInternetActivity::class.java)
        startActivity(i)
    }

    /**
     * show snackbar with message
     */
    private fun showSnackBar(text: String) {
        val snack = Snackbar.make(root_container, text, Snackbar.LENGTH_LONG)
        snack.setAction("OK") { }

        val actionColor = ContextCompat.getColor(applicationContext, R.color.colorAccent)
        val textColor = ContextCompat.getColor(applicationContext, R.color.colorLightHint)

        snack.setActionTextColor(actionColor).setTextColor(textColor).show()
    }

    private fun showSnackBarWithJump(text: String, position: Int) {
        println("jumping to $position")
        val snack = Snackbar.make(root_container, text, Snackbar.LENGTH_LONG)
        snack.setAction("View it!") {
            pager.currentItem = position
        }

        val actionColor = ContextCompat.getColor(applicationContext, R.color.colorAccent)
        val textColor = ContextCompat.getColor(applicationContext, R.color.colorLightHint)

        snack.setActionTextColor(actionColor).setTextColor(textColor).show()
    }

    /**
     * Animations
     */
    private fun setLoadingAnimation() {
        println("animate something")
    }
}