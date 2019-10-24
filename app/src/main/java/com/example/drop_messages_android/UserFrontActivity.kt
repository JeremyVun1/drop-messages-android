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
import com.example.drop_messages_android.fragments.LoginFragment
import com.example.drop_messages_android.fragments.RegisterFragment
import com.example.drop_messages_android.fragments.TestFragment
import com.example.drop_messages_android.network.GetTokenModel
import com.example.drop_messages_android.network.NetworkSingleton
import com.example.drop_messages_android.network.SignUpModel
import com.example.drop_messages_android.viewpager.VerticalPageAdapter
import com.google.android.gms.auth.api.credentials.CredentialRequest
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.IdentityProviders
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_user_front.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.json.JSONObject

class UserFrontActivity : AppCompatActivity(), RegisterFragment.RegisterUserListener, LoginFragment.LoginUserListener {

    private var regFrag : RegisterFragment? = null
    private var loginFrag : LoginFragment?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_front)
        overridePendingTransition(0, 0)

        val credentialsClient = Credentials.getClient(this)
        val credentialRequest = CredentialRequest.Builder()
            .setPasswordLoginSupported(true)
            .setAccountTypes(IdentityProviders.GOOGLE, IdentityProviders.TWITTER)
            .build()



        initialiseUI()
        //initTestUI()
    }

    private fun initialiseUI() {
        regFrag = RegisterFragment()
        loginFrag = LoginFragment()

        val verticalPageAdapter = VerticalPageAdapter(
            arrayOf(IndexFragment(), regFrag as Fragment, loginFrag as Fragment),
            supportFragmentManager
        )
        fragment_container.adapter = verticalPageAdapter
        fragment_container.offscreenPageLimit = 10
    }

    // login the user (get auth token and make a websocket connection)
    private fun login(model: GetTokenModel) {
        // do stuff
    }


    /////////////////////
    // Fragment callbacks
    /////////////////////
    override fun navToSignIn() {
        fragment_container.currentItem = 2
    }

    override fun navToSignUp() {
        fragment_container.currentItem = 1
        println("Nav to sign up ${fragment_container.currentItem}")
    }

    private fun login() {

    }

    override fun onSignIn(bundle: Bundle, errorListener: (err: GetTokenModel) -> Unit) {
        CoroutineScope(IO).launch {
            val model = GetTokenModel(bundle.getString("username") ?: "",
                bundle.getString("password") ?: "")

            credential = Credential.Builder()

            println("Logging in as ${model}")
            val url = resources.getString(R.string.get_token_url)

            val gson = Gson()
            val json = gson.toJson(model)


        }
    }

    // event listener for sign up fragment
    override fun onSignUpUser(bundle: Bundle, errorListener: (err: SignUpModel) -> Unit) {
        CoroutineScope(IO).launch {
            val model = SignUpModel(bundle.getString("username") ?: "",
                bundle.getString("password") ?: "",
                bundle.getString("email") ?: "")

            val url = resources.getString(R.string.sign_up_url)

            val gson = Gson()
            val json = gson.toJson(model)

            //make the post request
            PostRequest(url, json,
                {
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

    /////////////////////
    // TESTING
    /////////////////////
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
