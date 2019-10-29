package com.example.drop_messages_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.api.*
import com.example.drop_messages_android.fragments.DropMessageFragment
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_drop_messages.*
import kotlinx.android.synthetic.main.activity_drop_messages.toolbar
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.layout_toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    DropMessageFragment.DropMessageFragmentListener {

    private val gson by lazy { Gson() }

    private lateinit var mapFragment: GoogleMap
    private var locationHandler: LocationHandler? = null
    private var pickupFragment: DropMessageFragment? = null
    private var lastMarkerClicked: Int = -1

    private var sockSubscriber: Disposable? = null
    private val socket by lazy { SocketManager.getWebSocket() }
    private var requesting: Boolean = false

    private val userModel by lazy { intent.getParcelableExtra<UserModel>("user") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        setSupportActionBar(toolbar as Toolbar)
        loadToolbarLocationText()

        locationHandler = LocationHandler(applicationContext)

        // get google maps
        val mf = supportFragmentManager.findFragmentById(R.id.google_map_fragment) as SupportMapFragment
        mf.getMapAsync(this)
    }

    /**
     * lifecycle stuff to maximise battery
     */
    override fun onResume() {
        super.onResume()
        locationHandler?.connect()
        SocketManager.openSocket()
        setupSocketListeners()
    }

    override fun onPause() {
        super.onPause()
        locationHandler?.disconnect()
        SocketManager.closeSocket()
        sockSubscriber?.dispose()
    }

    /**
     * Set up socket listeners for map activity events
     */
    private fun setupSocketListeners() {
        if (!Util.hasInternet(applicationContext))
            navToNoInternet()

        if (socket == null) {
            Log.e("ERROR", "Cannot use a null socket")
            navToMainLoader()
        }
        else {
            sockSubscriber = socket!!.observeSocketResponse()
                .subscribe {
                    val category = it.category
                    val data = it.data

                    when (DropResponse.getEnum(category)) {
                        DropResponse.SOCKET -> handleSocketStatusResponses(data)
                        DropResponse.ERROR -> handleErrorResponses(data)
                        DropResponse.NOTIFICATION -> handleNotificationResponses(data)
                        DropResponse.TOKEN -> handleTokenResponses(data)
                        DropResponse.GEOLOC -> handleGeolocResponse(data)

                        DropResponse.SINGLE -> handleGetSingleResponse(data)
                        DropResponse.STUBS -> handleGetStubsResponse(data)
                        DropResponse.UNKNOWN -> Log.e("ERROR", "Unknown server response category")
                    }
                }
        }
    }

    /**
     * Handlers for socket responses from server
     */
    private fun handleGeolocResponse(data: String) {
        val response = gson.fromJson(data, GeolocationResponse::class.java)
        println("GEOLOC RESPONSE $response")

        if (response.result) {
            val currLoc = userModel!!.location

            //if (currLoc!!.lat.round(2) != response.lat.toDouble().round(2)) {
                //our geolocation block has changed
                userModel!!.location = Geolocation(response.lat.toDouble(), response.long.toDouble())
                loadToolbarLocationText()

                // request message stubs for new geolocation block
                requestMessageStubs()
            //}
        }

        requesting = false
    }

    // TODO
    private fun handleGetSingleResponse(data: String) {
        val response = gson.fromJson(data, DropMessage::class.java)

        // add new fragment if none exists, else replace the one we already have
        // No need to recreate a fragment and replace it if one already exists
        CoroutineScope(Main).launch {
            if (pickupFragment == null) {
                val newFrag = createDropMessageFragment(response)
                pickupFragment = newFrag as DropMessageFragment

                supportFragmentManager.beginTransaction()
                    .add(R.id.map_pickup_container, newFrag, "map_pickup_fragment")
                    .commit()
            } else pickupFragment!!.loadModel(response)

            requesting = false
        }

        openPickupContainer()
    }

    private fun openPickupContainer() {
        CoroutineScope(Main).launch {
            if (map_pickup_container.visibility == View.GONE) {
                map_pickup_container.visibility = View.VISIBLE
            }
        }
    }

    private fun closePickupContainer() {
        CoroutineScope(Main).launch {
            if (map_pickup_container.visibility == View.VISIBLE) {
                map_pickup_container.visibility = View.GONE
            }
        }
    }

    private fun handleGetStubsResponse(data: String) {
        if (!::mapFragment.isInitialized)
            return

        val response = gson.fromJson(data, Array<DropMessageStub>::class.java)

        val location = userModel.location
        if (location == null) {
            val errString = "Need to be on a geolocation block to use the map activity"
            Log.e("ERROR", errString)
            Toast.makeText(applicationContext, errString, Toast.LENGTH_SHORT).show()
            navToMainLoader()
        }

        CoroutineScope(Main).launch {
            mapFragment.clear()
            for (stub in response) {
                val marker = mapFragment.addMarker(
                    MarkerOptions()
                        .position(LatLng(stub.lat.toDouble(), stub.long.toDouble()))
                        .title(stub.author)
                )
                marker.tag = stub.id
            }

            val focus = LatLng(location!!.lat, location.long)
            mapFragment.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 20f))
        }

        requesting = false
    }

    private fun handleSocketStatusResponses(data: String) {
        Log.d("DEBUG", data)
    }

    private fun handleErrorResponses(data: String) {
        Log.e("ERROR", data)
    }

    private fun handleTokenResponses(data: String) {
        Log.d("DEBUG", data)

        // We need to get a new, fresh JWT token
        CoroutineScope(Dispatchers.IO).launch {
            fetchJsonWebToken()
        }
    }

    private fun handleNotificationResponses(data: String) {
        if (!::mapFragment.isInitialized)
            return

        val response = gson.fromJson(data, DropMessage::class.java)
        Log.d("DEBUG", data)

        // create a marker titled with author name, and tag it with msg id
        val loc = LatLng(response.lat.toDouble(), response.long.toDouble())
        mapFragment.addMarker(
            MarkerOptions()
                .position(loc)
                .title(response.author)
        ).tag = response.id

        showSnackbarWithJump("You picked up a drop!", loc)
    }

    private fun requestMessageStubs() {
        println("requesting stubs called requesting: $requesting")
        if (!requesting) {
            println("requesting stubs")
            socket!!.requestStubs(RequestStubs(DropRequest.GET_STUBS.value))
            println("request stubs message sent")
            requesting = true
        }
    }

    /**
     * Drop message fragment stuff
     */
    private fun createDropMessageFragment(model: DropMessage): Fragment {
        val b = Bundle()
        b.putParcelable("model", model)
        b.putBoolean("canDelete", model.author == userModel!!.username)

        val result = DropMessageFragment()
        result.arguments = b

        return result
    }

    /**
     * Drop message fragment listeners
     */
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
            supportFragmentManager.beginTransaction().remove(pickupFragment as Fragment)
            pickupFragment = null
        }
    }

    /**
     * Callback for google maps stuff
     */
    override fun onMapReady(map: GoogleMap) {
        println("map is ready!")
        mapFragment = map

        // clicking a marker will request the full msg from the server
        mapFragment.setOnMarkerClickListener {marker ->
            val id = (marker.tag as String).toInt()
            if (lastMarkerClicked == id) {
                println("closing info window")
                marker.hideInfoWindow()
                closePickupContainer()
            } else {
                lastMarkerClicked = id
                marker.showInfoWindow()
                socket!!.requestSingle(RequestSingle(data=marker.tag as String))
            }
            true
        }

        requestMessageStubs()
    }

    /**
     * Snackbar to jump to a notification
     */
    private fun showSnackbarWithJump(text: String, loc: LatLng) {
        if (!::mapFragment.isInitialized)
            return

        val snack = Snackbar.make(root_container, text, Snackbar.LENGTH_LONG)

        snack.setAction("View it!") {
            mapFragment.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 20f))
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
                    CoroutineScope(Dispatchers.IO).launch {
                        val sp = getSharedPreferences("Login", MODE_PRIVATE)
                        sp.edit().putString("token", token).commit()

                        // get our geolocation to recreate a connection
                        locationHandler!!.connect()
                        locationHandler!!.getLastLocation(::onLocationReceived, ::onLocationError)
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
        println("location received")
        socket!!.changeGeolocation(
            ChangeGeolocation(
                DropRequest.CHANGE_LOC.value,
                location.lat.toFloat(),
                location.long.toFloat()
            )
        )
        println("change geolocation sent")

        requesting = false
    }

    private fun onLocationError(ex: Exception) {
        Log.e("ERROR", ex.toString())
        Toast.makeText(applicationContext, "Google play services error!", Toast.LENGTH_SHORT).show()
        finish()
    }

    /**
     * Navigation functions
     */
    private fun navToNoInternet() {
        val i = Intent(applicationContext, NoInternetActivity::class.java)
        startActivity(i)
    }

    private suspend fun navToMainLoaderAsync() {
        withContext(Dispatchers.Main) {
            navToMainLoader()
        }
    }
    private fun navToMainLoader() {
        val i = Intent(applicationContext, MainLoaderActivity::class.java)
        startActivity(i)
        finish()
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
        println("change location called")
        locationHandler!!.connect()
        locationHandler!!.getLastLocation(::onLocationReceived, ::onLocationError)
    }

    /**
     * Toolbar stuff
     */
    private fun loadToolbarLocationText() {
        CoroutineScope(Dispatchers.Main).launch {
            val toolbarLocText = "(${userModel?.location?.lat}, ${userModel?.location?.long})"
            tv_toolbar_geolocation.text = toolbarLocText
            tv_toolbar_geolocation.visibility = View.VISIBLE
        }
    }

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
}