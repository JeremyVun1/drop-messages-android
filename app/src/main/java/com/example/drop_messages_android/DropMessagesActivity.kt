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
import androidx.viewpager.widget.ViewPager
import com.example.drop_messages_android.api.*
import com.example.drop_messages_android.api.LocationHandler.LocationUpdateListener
import com.example.drop_messages_android.fragments.CreateDropDialogFragment
import com.example.drop_messages_android.fragments.DropMessageFragment
import com.example.drop_messages_android.fragments.StackEmptyFragment
import com.example.drop_messages_android.viewpager.VerticalPageAdapter
import com.example.drop_messages_android.fragments.CreateDropDialogFragment.CreateDropListener
import com.example.drop_messages_android.fragments.DropMessageFragmentListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_drop_messages.*
import kotlinx.android.synthetic.main.layout_toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


/**
 * Main activity where user can make api requests through a web socket to create and retrieve data
 */
class DropMessagesActivity : AppCompatActivity(), CreateDropListener, DropMessageFragmentListener, LocationUpdateListener {

    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseDb: FirebaseFirestore? = null
    private var user: FirebaseUser? = null

    private var locationHandler: LocationHandler? = null
    private var location: Geolocation? = null

    private val gson by lazy { Gson() }

    private var lastRequest: DropRequest? = null
    private var lastVisible: DocumentSnapshot? = null
    private var requesting: Boolean = false
    private var lastPageScrolled: Boolean = false

    private var seenDrops = mutableMapOf<String, Boolean>()

    // page viewer references
    private var pageAdapter: VerticalPageAdapter? = null
    private var fragments: MutableList<Fragment>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drop_messages)

        firebaseAuth = FirebaseAuth.getInstance()
        user = firebaseAuth!!.currentUser
        if (user == null) navToMainLoader()

        firebaseDb = FirebaseFirestore.getInstance()

        locationHandler = LocationHandler(applicationContext)
        location = intent.getParcelableExtra("location")
        locationHandler!!.getLastLocation(::onLocationReceived, ::onLocationError)

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
    }

    override fun onPause() {
        super.onPause()
        locationHandler!!.disconnect()
    }

    /**
     * setup button click listeners
     */
    private fun setupButtonHandlers() {
        btn_get_top.setOnClickListener { requestTop() }
        btn_get_latest.setOnClickListener { requestLatest() }
        btn_get_random.setOnClickListener { requestRandom() }
        btn_my_drops.setOnClickListener { requestMine() }
        btn_map.setOnClickListener { navToMap() }
        btn_create_drop.setOnClickListener { createDropDialog() }
    }

    /**
     * Document queries
     */
    private fun requestTop() {
        if (!Util.hasInternet(applicationContext))
            navToNoInternet()

        if (requesting) return

        val query = firebaseDb!!.collection("messages")
            .whereEqualTo("lat_block", location!!.lat.round(2))
            .whereEqualTo("long_block", location!!.long.round(2))
            .whereGreaterThan("votes", -6)
            .orderBy("votes", Query.Direction.DESCENDING)

        if (lastRequest == DropRequest.GET_TOP && lastVisible != null)
            query.startAfter(lastVisible!!)
        else lastRequest = DropRequest.GET_TOP

        println("getting at ${location!!.lat.round(2)} - ${location!!.long.round(2)}")

        query.limit(Util.PAGE_SIZE.toLong())
            .get()
            .addOnSuccessListener {
                if (!it.isEmpty)
                    lastVisible = it.documents[it.size() - 1]

                loadDocumentsIntoPageViewer(it)
                requesting = false
            }
            .addOnFailureListener {
                Toast.makeText(applicationContext, it.toString(), Toast.LENGTH_SHORT).show()
                Log.e("MESSAGE", it.toString())
                requesting = false
            }
        requesting = true
    }

    private fun requestLatest() {
        if (!Util.hasInternet(applicationContext))
            navToNoInternet()

        if (requesting) return

        val query = firebaseDb!!.collection("messages")
            .whereEqualTo("lat_block", location!!.lat.round(2))
            .whereEqualTo("long_block", location!!.long.round(2))
            .whereGreaterThan("votes", -6)
            .orderBy("votes", Query.Direction.DESCENDING)
            .orderBy("date", Query.Direction.DESCENDING)

        if (lastRequest == DropRequest.GET_TOP && lastVisible != null)
            query.startAfter(lastVisible!!)
        else lastRequest = DropRequest.GET_TOP

        query.limit(Util.PAGE_SIZE.toLong())
            .get()
            .addOnSuccessListener {
                if (!it.isEmpty)
                    lastVisible = it.documents[it.size() - 1]
                loadDocumentsIntoPageViewer(it)
                requesting = false
            }
            .addOnFailureListener {
                Toast.makeText(applicationContext, it.toString(), Toast.LENGTH_SHORT).show()
                Log.e("MESSAGE", it.toString())
                requesting = false
            }
        requesting = true
    }

    private fun requestRandom() {
        if (!Util.hasInternet(applicationContext))
            navToNoInternet()

        if (requesting) return

        val query = firebaseDb!!.collection("messages")
            .whereEqualTo("lat_block", location!!.lat.round(2))
            .whereEqualTo("long_block", location!!.long.round(2))
            .whereGreaterThan("votes", -6)

        if (lastRequest == DropRequest.GET_RANDOM && lastVisible != null)
            query.startAfter(lastVisible!!)
        else lastRequest = DropRequest.GET_RANDOM

        query.limit(Util.PAGE_SIZE.toLong())
            .get()
            .addOnSuccessListener {
                if (!it.isEmpty)
                    lastVisible = it.documents[it.size() - 1]
                loadDocumentsIntoPageViewer(it)
                requesting = false
            }
            .addOnFailureListener {
                Toast.makeText(applicationContext, it.toString(), Toast.LENGTH_SHORT).show()
                Log.e("MESSAGE", it.toString())
                requesting = false
            }
        requesting = true
    }

    private fun requestMine() {
        if (!Util.hasInternet(applicationContext))
            navToNoInternet()

        if (requesting) return

        val query = firebaseDb!!.collection("messages")
            .whereEqualTo("lat_block", location!!.lat.round(2))
            .whereEqualTo("long_block", location!!.long.round(2))
            .whereEqualTo("author", user!!.displayName)
            .whereGreaterThan("votes", -6)
            .orderBy("votes", Query.Direction.ASCENDING)
            .orderBy("date", Query.Direction.DESCENDING)

        if (lastRequest == DropRequest.GET_MINE && lastVisible != null)
            query.startAfter(lastVisible!!)
        else lastRequest = DropRequest.GET_MINE

        query.limit(Util.PAGE_SIZE.toLong())
            .get()
            .addOnSuccessListener {
                if (!it.isEmpty)
                    lastVisible = it.documents[it.size()-1]
                loadDocumentsIntoPageViewer(it)
                requesting = false
            }
            .addOnFailureListener {
                Toast.makeText(applicationContext, it.toString(), Toast.LENGTH_SHORT).show()
                Log.e("MESSAGE", it.toString())
                requesting = false
            }
        requesting = true
    }

    /**
     * Dialog popup for creating a new drop message
     */
    private fun createDropDialog() {
        val dialog = CreateDropDialogFragment()
        val b = Bundle()
        b.putString("author", user?.displayName)
        dialog.arguments = b

        dialog.show(supportFragmentManager, "Create Drop Message")
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
                    else {
                        when(lastRequest) {
                            DropRequest.GET_NEW -> requestLatest()
                            DropRequest.GET_TOP -> requestTop()
                            DropRequest.GET_MINE -> requestMine()
                            DropRequest.GET_RANDOM -> requestRandom()
                        }
                    }
                }
            }

            override fun onPageSelected(position: Int) {
                if (position != pageAdapter!!.count - 1)
                    lastPageScrolled = false
            }
        })
    }

    private fun removeFragmentFromPageViewer(msgId: String) {
        CoroutineScope(Main).launch {
            pageAdapter!!.removeFragment(msgId)
            showSnackBar("Drop Message deleted!")
        }
    }

    /**
     * Handle creation and display of message fragments to the fragment page viewer
     */
    private fun createDropMessageFragment(model: DropMessage): Fragment {
        val b = Bundle()
        b.putParcelable("model", model)
        b.putBoolean("canDelete", model.author == user!!.displayName)

        val result = DropMessageFragment()
        result.arguments = b

        return result
    }

    private fun loadDocumentsIntoPageViewer(docs: QuerySnapshot) {
        if (docs.isEmpty) {
            showSnackBar("No more drop messages")
            return
        }

        val fragments = mutableListOf<Fragment>()
        println("documents gotten: ${docs.size()}")
        updateSeen(docs)

        for (doc in docs) {
            val msgId = doc.data["id"] as String
            seenDrops[msgId] = true

            val model = DropMessage(
                id=msgId,
                lat=doc.data["lat"] as Double,
                long=doc.data["long"] as Double,
                lat_block=doc.data["lat_block"] as Double,
                long_block=doc.data["long_block"] as Double,
                message=doc.data["message"] as String,
                date=doc.data["date"] as Timestamp,
                seen=(doc.data["seen"] as Long).toInt(),
                votes=(doc.data["votes"] as Long).toInt(),
                author=doc.data["author"] as String
            )
            fragments.add(createDropMessageFragment(model))
        }

        loadFragmentsIntoPageViewer(fragments)
    }

    private fun updateSeen(docs: QuerySnapshot) {
        val toUpdate = mutableListOf<QueryDocumentSnapshot>()
        for (doc in docs) {
            if (!seenDrops.containsKey(doc.data["id"] as String))
                toUpdate.add(doc)
        }

        firebaseDb!!.runBatch { batch ->
            for (item in toUpdate) {
                batch.update(item.reference, "seen", FieldValue.increment(1))
            }
        }
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

        val reference = firebaseDb!!.collection("messages").document()

        val message = DropMessage(
            id = reference.id,
            author=user!!.displayName as String,
            message = msg,
            date = Timestamp(Date()),
            lat = location!!.lat,
            long = location!!.long,
            lat_block = location!!.lat.round(2),
            long_block = location!!.long.round(2),
            votes = 1,
            seen = 0
        )

        reference.set(message)
            .addOnSuccessListener {
                showSnackBarWithJump("Message dropped at (${message.lat_block.format(2)}, ${message.long_block.format(2)})", message)
            }
            .addOnFailureListener {
                showSnackBar("Failed to create message")
                Log.e("MESSAGE", "Failed to create message")
            }
    }

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
            firebaseDb!!.collection("messages").document(id).delete()
                .addOnSuccessListener {
                    removeFragmentFromPageViewer(id)
                    showSnackBar("Message deleted!")
                }
        }
    }

    /**
     * Callbacks for Fused location provider
     */
    override fun onLocationReceived(newLoc: Geolocation) {
        val currLoc = location

        // check if our geolocation block changed
        if (currLoc == null || (currLoc.lat.format(2) != newLoc.lat.format(2) && currLoc.long.format(2) != newLoc.long.format(2))) {
            showSnackBar("Geolocation block changed to: ${newLoc.formattedString()}")
            loadToolbarLocationText()
        }

        location = newLoc
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
        startActivity(i)
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

    private fun showSnackBarWithJump(text: String, message: DropMessage) {
        val snack = Snackbar.make(root_container, text, Snackbar.LENGTH_LONG)

        snack.setAction("View it!") {
            val fragment = createDropMessageFragment(message)
            val list = mutableListOf(fragment)
            fragments = list
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
            val lat = location?.lat
            val long = location?.long

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
            firebaseAuth!!.signOut()

            // nav to user front
            withContext(Main) {
                navToMainLoader()
            }
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
}