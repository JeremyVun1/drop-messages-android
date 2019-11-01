package com.example.drop_messages_android

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.example.drop_messages_android.api.*
import com.example.drop_messages_android.fragments.CreateDropDialogFragment
import com.example.drop_messages_android.fragments.DropMessageFragment
import com.example.drop_messages_android.fragments.StackEmptyFragment
import com.example.drop_messages_android.viewpager.VerticalPageAdapter
import com.example.drop_messages_android.fragments.CreateDropDialogFragment.CreateDropListener
import com.example.drop_messages_android.fragments.DropMessageFragment.DropMessageFragmentListener
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_drop_messages.*
import kotlinx.android.synthetic.main.layout_toolbar.*
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

    private var locationHandler: LocationHandler? = null
    private val gson by lazy { Gson() }

    private var socket: DropMessageService? = null
    private var socketSubscriber: Disposable? = null
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

        locationHandler = LocationHandler(applicationContext)

        userModel = intent.getParcelableExtra("user")
        pager.offscreenPageLimit = 10

        setSupportActionBar(toolbar as Toolbar)
        loadToolbarLocationText()

        setupButtonHandlers()
        setupPageLoading()
        setupPageViewer()
    }

    /**
     * Make sure we aren't doing location updating while app is not in foreground
     * Also make sure our web socket is dead
     */
    override fun onResume() {
        super.onResume()
        locationHandler!!.connect()

        SocketManager.openSocket()
        setupSocketListeners()
    }

    override fun onPause() {
        super.onPause()
        locationHandler!!.disconnect()

        SocketManager.closeSocket()
        socketSubscriber?.dispose()
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
            navToMap()
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
    private fun setupSocketListeners() {
        if (!Util.hasInternet(applicationContext))
            navToNoInternet()

        socket = SocketManager.getWebSocket()
        if (socket == null) {
            Log.e("ERROR", "Cannot use a null socket")
            navToMainLoader()
        }
        else {
            // Socket response routing
            socketSubscriber = socket!!.observeSocketResponse()
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
                        DropResponse.GEOLOC -> handleGeolocResponse(data)
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
                val geolocStr = "(${response.echo.lat.format(2)}, ${response.echo.long.format(2)})"
                showSnackBarWithJump("Message dropped at $geolocStr", response)
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

    private fun handleGeolocResponse(data: String) {
        val response = gson.fromJson(data, GeolocationResponse::class.java)
        println("RESPONSE $response")

        if (response.result) {
            val currLoc = userModel!!.location

            if (currLoc!!.lat.round(2) != response.lat.toDouble().round(2)) {
                // our new geolocation block is different, so update
                userModel!!.location = Geolocation(response.lat.toDouble(), response.long.toDouble())
                SocketManager.setNewUserLocation(userModel!!)
                loadToolbarLocationText()
            }
        }

        requesting = false
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
                showSnackBar(text)
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
        else pageAdapter!!.setFragments(fragmentList)

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
     * Callbacks for Fused location provider
     */
    private fun onLocationReceived(location: Geolocation) {
        socket!!.changeGeolocation(
            ChangeGeolocation(
                DropRequest.CHANGE_LOC.value,
                location.lat.toFloat(),
                location.long.toFloat()
            )
        )

        requesting = true
    }

    private fun onLocationError(ex: Exception) {
        Log.e("ERROR", ex.toString())
        Toast.makeText(applicationContext, "Google play services error!", Toast.LENGTH_SHORT).show()
        finish()
    }

    /**
     * Navigation function
     */
    private fun navToMap() {
        val i = Intent(applicationContext, MapsActivity::class.java)
        i.putExtra("user", userModel)
        startActivity(i)
    }

    private suspend fun navToMainLoaderAsync() {
        withContext(Main) {
            navToMainLoader()
        }
    }

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

    private fun showSnackBarWithJump(text: String, response: PostDataResponse) {

        val snack = Snackbar.make(root_container, text, Snackbar.LENGTH_LONG)
        snack.setAction("View it!") {
            val fragment = createDropMessageFragment(response.echo)
            val list = mutableListOf(fragment)
            fragments = list
            pageNumber = 1
            lastRequest = null
            loadFragmentsIntoPageViewer(list)
        }

        val actionColor = ContextCompat.getColor(applicationContext, R.color.colorAccent)
        val textColor = ContextCompat.getColor(applicationContext, R.color.colorLightHint)

        snack.setActionTextColor(actionColor).setTextColor(textColor).show()
    }

    /**
     * Toolbar functions
     */
    private fun loadToolbarLocationText() {
        CoroutineScope(Main).launch {
            val lat = userModel?.location?.lat
            val long = userModel?.location?.long

            val toolbarLocText = "(${lat?.format(2)}, ${long?.format(2)})"
            tv_toolbar_geolocation.text = toolbarLocText
            tv_toolbar_geolocation.visibility = View.VISIBLE
        }
    }

    private fun logout() {
        CoroutineScope(Default).launch {
            // clear user details from shared preferences
            val sp = getSharedPreferences("Login", MODE_PRIVATE)
            sp.edit().clear().commit()

            // nav to user front
            navToMainLoaderAsync()
        }
    }

    private fun changeLocation() {
        locationHandler!!.connect()
        locationHandler!!.getLastLocation(::onLocationReceived, ::onLocationError)
    }

    /**
     * Toolbar stuff
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.opt_logout -> logout()
            R.id.opt_refresh -> changeLocation()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.global_menu, menu)
        return true
    }

    /**
     * Animations
     */
    private fun setLoadingAnimation() {
        println("animate something")
    }
}