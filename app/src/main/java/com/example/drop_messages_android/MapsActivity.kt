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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.api.*
import com.example.drop_messages_android.api.LocationHandler.LocationUpdateListener
import com.example.drop_messages_android.fragments.DropMessageFragmentListener
import com.example.drop_messages_android.fragments.MapDropMessageFragment
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.layout_toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, DropMessageFragmentListener,
    LocationUpdateListener {
    private var locationHandler: LocationHandler? = null
    private var location: Geolocation? = null

    private lateinit var mapFragment: GoogleMap
    private var lastMarkerClicked: String = ""
    private var pickupFragment: MapDropMessageFragment? = null

    private var firebaseDb: FirebaseFirestore? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null

    private var requesting: Boolean = false
    private var forceUpdateMap: Boolean = false

    // store our map markers and messages
    private var messages = mutableListOf<DropMessage>()
    private val markers = mutableMapOf<String, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        setSupportActionBar(toolbar as Toolbar)
        loadToolbarLocationText()

        locationHandler = LocationHandler(applicationContext)
        locationHandler!!.getLastLocation(::onLocationReceived, ::onLocationError)

        firebaseDb = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
        user = firebaseAuth!!.currentUser

        // get google maps
        val mf =
            supportFragmentManager.findFragmentById(R.id.google_map_fragment) as SupportMapFragment
        mf.getMapAsync(this)
    }

    /**
     * lifecycle stuff to maximise battery
     */
    override fun onResume() {
        super.onResume()
        locationHandler?.connect()
    }

    override fun onPause() {
        super.onPause()
        locationHandler?.disconnect()
    }

    /**
     * Callbacks for Fused location provider
     */
    override fun onLocationReceived(newLoc: Geolocation) {
        val currLoc = location
        println(currLoc)

        /**
         * to conserve data, only populate the map on location updates if,
         * 1) our geolocation block changed
         * 2) user pressed refresh button
         */
        if (forceUpdateMap || currLoc == null || (currLoc.lat.format(2) != newLoc.lat.format(2) && currLoc.long.format(2) != newLoc.long.format(2))) {
            location = newLoc

            if (!Util.hasInternet(applicationContext))
                navToNoInternet()

            showSnackBar("Geolocation block changed to: ${newLoc.formattedString()}")
            loadToolbarLocationText()

            buildQuery().get()
                .addOnSuccessListener { documents ->
                    CoroutineScope(Default).launch {
                        messages.clear()

                        for (doc in documents) {
                            val model = DropMessage(
                                id = doc.data["id"] as String,
                                lat = doc.data["lat"] as Double,
                                long = doc.data["long"] as Double,
                                lat_block = doc.data["lat_block"] as Double,
                                long_block = doc.data["long_block"] as Double,
                                message = doc.data["message"] as String,
                                date = doc.data["date"] as Timestamp,
                                seen = (doc.data["seen"] as Long).toInt(),
                                votes = (doc.data["votes"] as Long).toInt(),
                                author = doc.data["author"] as String
                            )
                            messages.add(model)
                        }

                        populateMap(messages)
                    }
                }
                .addOnFailureListener {
                    Log.e("MESSAGE", "Map activity could not retrieve messages")
                }
        } else location = newLoc

        requesting = false
        forceUpdateMap = false
    }

    private fun onLocationError(ex: Exception) {
        Log.e("ERROR", ex.toString())
        Toast.makeText(applicationContext, "Google play services error!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun buildQuery(): Query {
        return firebaseDb!!.collection("messages")
            .whereEqualTo("lat_block", location!!.lat.round(2))
            .whereEqualTo("long_block", location!!.long.round(2))
            .whereGreaterThan("votes", -6)
    }

    private fun populateMap(messages: List<DropMessage>) {
        if (!::mapFragment.isInitialized)
            return

        CoroutineScope(Main).launch {
            mapFragment.clear()
            markers.clear()

            for (m in messages) {
                val marker = mapFragment.addMarker(
                    MarkerOptions()
                        .position(LatLng(m.lat, m.long))
                        .title(m.author)
                )
                marker.tag = m.id
                markers[m.id] = marker
            }

            val focus = LatLng(location!!.lat, location!!.long)
            mapFragment.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, Util.DEFAULT_MAP_ZOOM))
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

    /**
     * Drop message fragment stuff
     */
    private fun createMapDropMessageFragment(model: DropMessage): Fragment {
        val b = Bundle()
        b.putParcelable("model", model)
        b.putBoolean("canDelete", model.author == user!!.displayName)

        val result = MapDropMessageFragment()
        result.arguments = b

        return result
    }


    /**
     * Drop message fragment listeners
     */
    override fun onUpvote(id: String?) {
        if (id != null) {
            firebaseDb!!.collection("messages").document(id)
                .update("votes", FieldValue.increment(1))
        }
    }

    override fun onDownvote(id: String?) {
        if (id != null) {
            firebaseDb!!.collection("messages").document(id)
                .update("votes", FieldValue.increment(-1))
        }
    }

    override fun onDelete(id: String?) {
        if (id != null) {
            val marker = markers[id]
            if (marker != null) {
                marker.remove()
                markers.remove(id)
            }

            firebaseDb!!.collection("messages").document(id).delete()
            messages.removeIf { m -> m.id == id }

            closePickupContainer()
        }
    }

    /**
     * Callback for google maps stuff
     */
    override fun onMapReady(map: GoogleMap) {
        mapFragment = map

        // clicking a marker will request the full msg from the server
        mapFragment.setOnMarkerClickListener { marker ->
            val id = marker.tag as String
            if (lastMarkerClicked == id) {
                marker.hideInfoWindow()
                closePickupContainer()
                lastMarkerClicked = ""
            } else {
                lastMarkerClicked = id
                marker.showInfoWindow()

                // find the message and open it in popup container view
                val model = messages.find { m -> m.id == id }

                if (model != null) {
                    if (pickupFragment == null) {
                        val newFrag = createMapDropMessageFragment(model)
                        pickupFragment = newFrag as MapDropMessageFragment

                        supportFragmentManager.beginTransaction()
                            .add(R.id.map_pickup_container, newFrag, "map_pickup_fragment")
                            .commit()
                    } else pickupFragment!!.loadModel(model)
                    openPickupContainer()
                }
            }
            true
        }
    }

    /**
     * Navigation functions
     */
    private fun navToNoInternet() {
        val i = Intent(applicationContext, NoInternetActivity::class.java)
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

    private fun logout() {
        CoroutineScope(Default).launch {
            // clear user details from shared preferences
            val sp = getSharedPreferences("Login", MODE_PRIVATE)
            sp.edit().clear().commit()
            firebaseAuth!!.signOut()

            // nav to user front
            navToMainLoaderAsync()
        }
    }

    private fun changeLocation() {
        if (!requesting) {
            forceUpdateMap = true
            locationHandler!!.connect()
            locationHandler!!.getLastLocation(::onLocationReceived, ::onLocationError)
        }
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

    /**
     * Toolbar stuff
     */
    private fun loadToolbarLocationText() {
        CoroutineScope(Main).launch {
            val lat = location?.lat
            val long = location?.long

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