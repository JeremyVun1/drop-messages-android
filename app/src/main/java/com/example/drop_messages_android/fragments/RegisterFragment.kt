package com.example.drop_messages_android.fragments

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.R
import com.example.drop_messages_android.ValidatorHelper
import com.example.drop_messages_android.api.SignUpModel
import kotlinx.android.synthetic.main.activity_loading.img_white_cover
import kotlinx.android.synthetic.main.card_register_fragment.*
import kotlinx.android.synthetic.main.card_register_fragment.btn_signin
import kotlinx.android.synthetic.main.card_register_fragment.progress_container
import kotlinx.android.synthetic.main.card_register_fragment.tv_input_password
import kotlinx.android.synthetic.main.card_register_fragment.tv_input_username
import kotlinx.android.synthetic.main.card_register_fragment.view.*

class RegisterFragment : Fragment() {

    var whiteBoxAnimator : ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.card_register_fragment, container, false) as ViewGroup

        // Signin button
        rootView.btn_signin.setOnClickListener {
            val listener = activity as RegisterUserListener
            listener.navToSignIn()
        }


        // Signup button
        rootView.btn_signup.setOnClickListener {
            val username = tv_input_username.editText!!.text.toString()
            val password = tv_input_password.editText!!.text.toString()
            val email = tv_input_email.editText!!.text.toString()

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
            if (email.isBlank()) {
                tv_input_email.error = "Must not be blank"
                validated = false
            }
            if (!ValidatorHelper.isValidEmail(email)) {
                tv_input_email.error = "Invalid Email"
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
                b.putString("email", email)

                startProgressBar()

                val listener = activity as RegisterUserListener
                listener.onSignUpUser(b, ::errorListener)
            }
        }

        return rootView
    }

    private fun startProgressBar() {
        //parent_container
        progress_container.visibility= View.VISIBLE
        btn_signin.visibility = View.GONE

        //loading animation for Loading text dots
        var slideX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, 90f)
        whiteBoxAnimator = ObjectAnimator.ofPropertyValuesHolder(img_white_cover, slideX).apply {
            interpolator = LinearInterpolator()
            duration = 2000
            repeatMode = ObjectAnimator.RESTART
            repeatCount = ObjectAnimator.INFINITE
        }
        whiteBoxAnimator!!.start()
    }

    private fun stopProgressBar() {
        progress_container.visibility= View.GONE
        btn_signin.visibility = View.VISIBLE
        if (whiteBoxAnimator != null)
            whiteBoxAnimator!!.end()
    }

    // error listener if http request from sign up bounces e.g. username taken
    // callback for user front activity
    fun errorListener(response: SignUpModel) {
        if (response.username.isNotEmpty())
            tv_input_username.editText!!.error = response.username
        if (response.password.isNotEmpty())
            tv_input_username.editText!!.error = response.password
        if (response.email.isNotEmpty())
            tv_input_username.editText!!.error = response.email


        // Reset UI
        stopProgressBar()
        btn_signin.visibility = View.VISIBLE
    }

    interface RegisterUserListener {
        fun onSignUpUser(bundle: Bundle, errorListener: (err: SignUpModel) -> Unit)
        fun navToSignIn()
    }
}