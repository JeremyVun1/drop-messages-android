package com.example.drop_messages_android.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.R
import com.example.drop_messages_android.ValidatorHelper
import com.example.drop_messages_android.network.GetTokenModel
import com.example.drop_messages_android.network.SignUpModel
import kotlinx.android.synthetic.main.card_login_fragment.*
import kotlinx.android.synthetic.main.card_login_fragment.view.*

class LoginFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.card_login_fragment, container, false) as ViewGroup

        // Signup button
        rootView.btn_signup.setOnClickListener {
            println("sign up listener called!")
            val listener = activity as LoginUserListener
            listener.navToSignUp()
        }

        // signin button
        rootView.btn_signin.setOnClickListener {
            val username = tv_input_username.editText!!.text.toString()
            val password = tv_input_password.editText!!.text.toString()

            var validated = true

            //validate fields with ugly code
            if (username.isBlank()) {
                tv_input_username.error = "Must not be blank"
                validated = false
            }
            if (password.isBlank()) {
                tv_input_password.error = "Must not be blank"
                validated = false
            }
            if (!ValidatorHelper.isValidPassword(password)) {
                tv_input_password.error = "must be 8 or more chars"
                validated = false
            }

            if (validated) {
                val b = Bundle()
                b.putString("username", username)
                b.putString("password", password)

                val listener = activity as LoginUserListener

                listener.onSignIn(b, ::errorListener)
            }
        }

        return rootView
    }

    // error listener if http request from sign up bounces e.g. username taken
    // callback for main activity
    fun errorListener(response: GetTokenModel) {
        if (response.username.isNotEmpty())
            tv_input_username.editText!!.error = response.username
        if (response.password.isNotEmpty())
            tv_input_username.editText!!.error = response.password
    }

    interface LoginUserListener {
        fun onSignIn(bundle: Bundle, errorListener: (err: GetTokenModel) -> Unit)
        fun navToSignUp()
    }
}