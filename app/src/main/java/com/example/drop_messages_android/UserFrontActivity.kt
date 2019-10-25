package com.example.drop_messages_android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.fragments.IndexFragment
import com.example.drop_messages_android.fragments.LoginFragment
import com.example.drop_messages_android.fragments.RegisterFragment
import com.example.drop_messages_android.fragments.TestFragment
import com.example.drop_messages_android.viewpager.VerticalPageAdapter
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_user_front.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.drop_messages_android.api.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext


class UserFrontActivity : AppCompatActivity(), RegisterFragment.RegisterUserListener, LoginFragment.LoginUserListener {

    private var regFrag : RegisterFragment? = null
    private var loginFrag : LoginFragment?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_front)
        overridePendingTransition(0, 0)

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
    }

    private fun navToMainLoader() {
        val i = Intent(applicationContext, MainLoaderActivity::class.java)
        startActivity(i)
        finish()
    }

    /**
     * Event handler when user presses sign in button on login fragment
     */
    override fun onSignIn(bundle: Bundle, errorListener: (err: InvalidLoginResponseModel) -> Unit) {
        CoroutineScope(IO).launch {
            val model = GetTokenModel(bundle.getString("username") ?: "",
                bundle.getString("password") ?: "")

            println("Authenticating as $model")

            // auth the user by getting a JWT token from remote server
            val url = resources.getString(R.string.get_token_url)
            val gson = Gson()
            val json = gson.toJson(model)

            // make the post request with inline listeners
            Postie().sendPostRequest(applicationContext, url, json,
                {
                    val response = gson.fromJson(it.toString(), JsonObject::class.java)

                    if (response.has("token")) {
                        showSnackBar("Successfully authenticated as ${model.username}!")

                        // handle the sign in and routing async
                        CoroutineScope(Default).launch {
                            signInUser(model, response["token"].toString(), errorListener)
                        }
                    }
                    else if (response.has("non_field_errors"))
                        errorListener(gson.fromJson(response, InvalidLoginResponseModel::class.java))
                },
                {
                    Log.e("POST", it.toString())
                    Toast.makeText(applicationContext, it.toString(), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    /**
     * Store un/pw securely for quick login when user opens app again
     * Then route user back to MainLoaderActivity for web socket connection creation etc.
     */
    private suspend fun signInUser(userDetails: GetTokenModel, token: String, errorListener: (err: InvalidLoginResponseModel) -> Unit) {
        if (storeUserDetails(userDetails.username, userDetails.password, token)) {
            withContext(Main) {
                navToMainLoader() // go to the main loader activity
            }
        }
        else {
            errorListener(InvalidLoginResponseModel(arrayOf("Error storing user details for auto login!")))
        }
    }

    /**
     * Event handler when user presses sign up on login fragment
     */
    override fun onSignUpUser(bundle: Bundle, errorListener: (err: SignUpModel) -> Unit) {
        CoroutineScope(IO).launch {
            val gson = Gson()
            val model = SignUpModel(bundle.getString("username") ?: "",
                bundle.getString("password") ?: "",
                bundle.getString("email") ?: "")

            val url = resources.getString(R.string.sign_up_url)

            val json = gson.toJson(model)
            println("registering user with json: $json")

            // make the post request with inline listeners
            Postie().sendPostRequest(applicationContext, url, json,
                {
                    val response = gson.fromJson(it.toString(), JsonObject::class.java)
                    if (response.has("id")) {
                        showSnackBar("Account created as ${response["username"]}!")

                        CoroutineScope(Main).launch {
                            storeUserDetails(model.username, model.password)
                            navToMainLoader() // go to the main loader activity
                        }
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

    /**
     * Store user details in app's private preferences. Don't need a DB as long as we encrypt properly
     * Password encrypted using assymmetric keys managed by android keystore.
     * Username and JWT token stored plaintext (not sensitive)
     * username, password used to get new JWT tokens in subsequent app logins
     * return whether it was successful or not
     **/
    private suspend fun storeUserDetails(username: String, password: String, token: String? = null) : Boolean {
        try {
            // use hash of the username and password as an alias for the keystore secret key
            val alias = username.toLowerCase().hashCode().toString()
            val encryptedPassword = UserStorageManager.encrypt(alias, password, this) ?: return false

            // store user details to shared preferences
            val sp = getSharedPreferences("Login", MODE_PRIVATE)

            sp.edit()
                .putString("username", username)
                .putString("password", encryptedPassword)
                .putString("token", token)
                .commit()
            return true
        } catch (ex: Exception) {
            Log.e("SIGNUP", Log.getStackTraceString(ex))
            return false
        }
    }

    /**
     * show snackbar with message
     */
    private fun showSnackBar(text: String) {
        val snack = Snackbar.make(user_front_container, text, Snackbar.LENGTH_SHORT)
        snack.setAction("OK", { } )

        val actionColor = ContextCompat.getColor(applicationContext, R.color.colorAccent)
        val textColor =ContextCompat.getColor(applicationContext, R.color.colorLightHint)

        snack.setActionTextColor(actionColor).setTextColor(textColor).show()
    }

    /////////////////////
    // TESTING VERTICAL VIEW PAGER CARD STACK
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
