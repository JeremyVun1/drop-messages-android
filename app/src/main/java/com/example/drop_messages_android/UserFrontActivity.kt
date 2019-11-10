package com.example.drop_messages_android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.fragments.IndexFragment
import com.example.drop_messages_android.fragments.LoginFragment
import com.example.drop_messages_android.fragments.RegisterFragment
import com.example.drop_messages_android.viewpager.VerticalPageAdapter
import kotlinx.android.synthetic.main.activity_user_front.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.drop_messages_android.api.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_user_front.toolbar
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore


class UserFrontActivity : AppCompatActivity(), RegisterFragment.RegisterUserListener, LoginFragment.LoginUserListener {

    private var regFrag : RegisterFragment? = null
    private var loginFrag : LoginFragment?= null

    private var firebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_front)

        setSupportActionBar(toolbar as Toolbar)

        initialiseUI()
        firebaseAuth = FirebaseAuth.getInstance()
    }

    private fun initialiseUI() {
        regFrag = RegisterFragment()
        loginFrag = LoginFragment()

        val verticalPageAdapter = VerticalPageAdapter(
            mutableListOf(IndexFragment(), regFrag as Fragment, loginFrag as Fragment),
            supportFragmentManager
        )
        fragment_container.adapter = verticalPageAdapter
        fragment_container.offscreenPageLimit = 10
    }


    /**
     * Fragment call backs
     */
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

    private fun navToForgotPassword() {
        val i = Intent(applicationContext, ForgotPasswordActivity::class.java)
        startActivity(i)
    }

    override fun onForgotPassword() {
        navToForgotPassword()
    }

    /**
     * Event handler when user presses sign in button on login fragment
     */
    override fun onSignIn(bundle: Bundle, errorListener: (err: String) -> Unit) {
        val email = bundle.getString("email") ?: ""
        val password = bundle.getString("password") ?: ""

        firebaseAuth!!.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth!!.currentUser

                    showSnackBar("Successfully authenticated as ${user?.displayName}!")
                    Log.d("SIGNIN", "$email signed in")

                    CoroutineScope(Default).launch {
                        val user = firebaseAuth!!.currentUser
                        storeUserDetails(user!!.displayName as String, password)
                        withContext(Main) {
                            navToMainLoader() // go to the main loader activity
                        }
                    }
                }
                else {
                    Log.d("SIGNIN", "$email failed to sign in")
                    errorListener(task.exception.toString())
                }
            }
    }

    /**
     * Event handler when user presses sign up on login fragment
     */
    override fun onSignUpUser(bundle: Bundle, errorListener: (err: SignUpModel) -> Unit) {
        val username = bundle.getString("username") ?: ""
        val email = bundle.getString("email") ?: ""
        val password = bundle.getString("password") ?: ""

        firebaseAuth!!.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showSnackBar("Account created!")

                    CoroutineScope(Default).launch {
                        storeUserDetails(username, password)
                    }
                }
                else {
                    Log.e("SIGNUP", task.exception.toString())
                    errorListener(SignUpModel("", "", "Error ${task.exception.toString()}"))
                }
            }
    }

    private fun verifyEmail() {
        val user = firebaseAuth!!.currentUser
        user!!.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.e("SIGNUP", "Verification email sent to ${user.email}")
                }
                else {
                    Log.e("SIGNUP", "Failed to send verification email to ${user.email}")
                }
            }
    }

    /**
     * 1. Store user details in app's private preferences. Don't need a DB as long as we encrypt properly
     * 2. Set user's display name
     * Password encrypted using assymmetric keys managed by android keystore.
     **/
    private suspend fun storeUserDetails(username: String, password: String) : Boolean {
        try {
            println("trying to store user: $username - $password")
            /**
             * Add to shared preferences
             */
            // use hash of the username and password as an alias for the keystore secret key
            val alias = username.toLowerCase().hashCode().toString()
            val encryptedPassword = UserStorageManager.encrypt(alias, password, this) ?: return false
            println("Encrypted password: $encryptedPassword")

            // store user details to shared preferences
            val sp = getSharedPreferences("Login", MODE_PRIVATE)

            sp.edit()
                .putString("username", username)
                .putString("password", encryptedPassword)
                .commit()
            println("shared preferences committed")

            /**
             * Set user display name
             */
            val user = firebaseAuth!!.currentUser
            println("user: $user")

            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build()
            println("profile update built")

            user?.updateProfile(profileUpdate)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        verifyEmail()

                        println("DISPLAY NAME WAS UPDATED IN PROFILE")
                        Log.d("SIGNUP", "Display name set to $username")
                        CoroutineScope(Main).launch {
                            navToMainLoader()
                        }
                    }
                    else {
                        println("DISPLAY NAME WAS NOT UPDATED IN PROFILE")
                        Log.d("SIGNUP", "Display name could not be set to $username")
                    }
                }

            return true
        } catch (ex: Exception) {
            println("STORE USER DETAILS FAILED!")
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
}
