package com.example.drop_messages_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.api.*
import com.example.drop_messages_android.fragments.DropMessageFragmentListener
import com.example.drop_messages_android.fragments.MapDropMessageFragment
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.tinder.scarlet.WebSocket
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

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, DropMessageFragmentListener {

    private val gson by lazy { Gson() }

    private lateinit var mapFragment: GoogleMap
    private var locationHandler: LocationHandler? = null
    private var pickupFragment: MapDropMessageFragment? = null
    private var lastMarkerClicked: Int = -1

    private lateinit var responseObserver: Disposable
    private lateinit var initialStubsObserver: Disposable
    private val socket by lazy { SocketManager.getWebSocket() }
    private var requesting: Boolean = false

    private val userModel by lazy { intent.getParcelableExtra<UserModel>("user") }

    // store our map markers
    private val markers: SparseArray<Marker> by lazy {
        SparseArray<Marker>()
    }

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
        if (::responseObserver.isInitialized)
            responseObserver.dispose()
        if (::initialStubsObserver.isInitialized)
            initialStubsObserver.dispose()
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
            initialStubsObserver = socket!!.observeWebSocketEvent()
                .filter { it is WebSocket.Event.OnConnectionOpened<*> }
                .subscribe {
                    requesting = false
                    requestMessageStubs()
                }

            responseObserver = socket!!.observeSocketResponse()
                .subscribe {
                    val category = it.category
                    val data = it.data

                    when (DropResponse.getEnum(category)) {
                        DropResponse.SOCKET -> handleSocketStatusResponses(data)
                        DropResponse.ERROR -> handleErrorResponses(data)
                        DropResponse.NOTIFICATION -> handleNotificationResponses(data)
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

        if (response.result) {
            val currLoc = userModel!!.location

            if (currLoc!!.lat.round(2) != response.lat.toDouble().round(2)) {
                //our geolocation block has changed
                userModel!!.location = Geolocation(response.lat.toDouble(), response.long.toDouble())
                loadToolbarLocationText()
            }

            // request message stubs for new geolocation block
            requestMessageStubs()
        } else requesting = false
    }

    private fun handleGetSingleResponse(data: String) {
        val response = gson.fromJson(data, DropMessage::class.java)

        // add new fragment if none exists, else replace the one we already have
        // No need to recreate a fragment and replace it if one already exists
        // make sure we are in the UI thread
        CoroutineScope(Main).launch {
            if (pickupFragment == null) {
                val newFrag = createMapDropMessageFragment(response)
                pickupFragment = newFrag as MapDropMessageFragment

                supportFragmentManager.beginTransaction()
                    .add(R.id.map_pickup_container, newFrag, "map_pickup_fragment")
                    .commit()
            } else pickupFragment!!.loadModel(response)

            requesting = false
            openPickupContainer()
        }
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

        val location = userModel!!.location
        if (location == null) {
            val errString = "Need to be on a geolocation block to use the map activity"
            Log.e("ERROR", errString)
            Toast.makeText(applicationContext, errString, Toast.LENGTH_SHORT).show()
            navToMainLoader()
        }

        CoroutineScope(Main).launch {
            mapFragment.clear()
            markers.clear()

            for (stub in response) {
                val marker = mapFragment.addMarker(
                    MarkerOptions()
                        .position(LatLng(stub.lat.toDouble(), stub.long.toDouble()))
                        .title(stub.author)
                )
                marker.tag = stub.id
                markers.put(stub.id.toInt(), marker)
            }

            val focus = LatLng(location!!.lat, location.long)
            mapFragment.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, Util.DEFAULT_MAP_ZOOM))
        }

        requesting = false
    }

    private fun handleSocketStatusResponses(data: String) {
        Log.d("DEBUG", data)
    }

    private fun handleErrorResponses(data: String) {
        Log.e("ERROR", data)
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
        if (!requesting) {
            socket!!.requestStubs(RequestStubs(DropRequest.GET_STUBS.value))
            requesting = true
        }
    }

    /**
     * Drop message fragment stuff
     */
    private fun createMapDropMessageFragment(model: DropMessage): Fragment {
        val b = Bundle()
        b.putParcelable("model", model)
        b.putBoolean("canDelete", model.author == userModel!!.username)

        val result = MapDropMessageFragment()
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

            val marker = markers[id]
            if (marker != null) {
                marker.remove()
                markers.remove(id)
            }

            closePickupContainer()
        }
    }

    /**
     * Callback for google maps stuff
     */
    override fun onMapReady(map: GoogleMap) {
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
    }

    /**
     * Snackbar to jump to a notification
     */
    private fun showSnackbarWithJump(text: String, loc: LatLng) {
        if (!::mapFragment.isInitialized)
            return

        val snack = Snackbar.make(root_container, text, Snackbar.LENGTH_LONG)

        snack.setAction("View it!") {
            mapFragment.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, Util.DEFAULT_MAP_ZOOM))
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
        locationHandler!!.connect()
        locationHandler!!.getLastLocation(::onLocationReceived, ::onLocationError)
    }

    /**
     * Toolbar stuff
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