package com.example.drop_messages_android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.example.drop_messages_android.fragments.IndexFragment
import com.example.drop_messages_android.fragments.RegisterFragment
import com.example.drop_messages_android.fragments.TestFragment
import com.example.drop_messages_android.network.NetworkSingleton
import com.example.drop_messages_android.network.SignUpModel
import com.example.drop_messages_android.viewpager.VerticalPageAdapter
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity(), RegisterFragment.OnSignUpUserListener {

    private var regFrag : RegisterFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initialiseUI()
        //initTestUI()
    }

    private fun initialiseUI() {
        regFrag = RegisterFragment()

        val verticalPageAdapter = VerticalPageAdapter(
            arrayOf(IndexFragment(), regFrag as Fragment),
            supportFragmentManager
        )
        fragment_container.adapter = verticalPageAdapter
        fragment_container.offscreenPageLimit = 10
    }

    // event listener for sign up fragment
    override fun onSignUpUser(bundle: Bundle, errorListener: (err: SignUpModel) -> Unit) {
        CoroutineScope(IO).launch {
            val url = resources.getString(R.string.sign_up_url)

            val model = SignUpModel(bundle.getString("username") ?: "",
                bundle.getString("password") ?: "",
                bundle.getString("email") ?: "")

            val json = Gson().toJson(model)

            //make the post request
            PostRequest(url, json,
                {
                    val gson = Gson()
                    val response = gson.fromJson(it.toString(), JsonObject::class.java)

                    if (response.has("id")) {
                        Toast.makeText(applicationContext, "sign up successful!", Toast.LENGTH_SHORT).show()
                        // TODO
                        // save user details to DB
                        // redirect to loading screen
                    }
                    else errorListener(gson.fromJson(response, SignUpModel::class.java))
                },
                {
                    Log.e("POST", it.toString())
                    Toast.makeText(applicationContext, it.toString(), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Make a network request
    fun PostRequest(url: String, json: String,
                                    responseListener: (it: JSONObject) -> Unit,
                                    errorListener: (it: VolleyError) -> Unit ) {
        // build the request
        val request = object : JsonObjectRequest(Request.Method.POST, url, null,
            Response.Listener { responseListener(it) },
            Response.ErrorListener { errorListener(it) }
        ) {
            override fun getBody(): ByteArray {
                println("json: ${json}")
                return json.toByteArray()
            }
        }

        println(request)
        println(request.body)
        println(request.method)
        NetworkSingleton.getInstance(this).addToRequestQueue(request)
    }

    // for testing with colorful cards
    private fun initTestUI() {
        val fragA = TestFragment()
        val fragB = TestFragment()
        val fragC = TestFragment()
        val fragD = TestFragment()

        val fragments = arrayOf<Fragment>(
            fragA,
            fragB,
            fragC,
            fragD
        )

        val verticalPageAdapter = VerticalPageAdapter(
            fragments,
            supportFragmentManager
        )
        fragment_container.adapter = verticalPageAdapter
        fragment_container.offscreenPageLimit = 10
    }
}
