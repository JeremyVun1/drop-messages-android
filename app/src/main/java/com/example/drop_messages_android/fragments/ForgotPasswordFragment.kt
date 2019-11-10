package com.example.drop_messages_android.fragments


import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.R
import com.example.drop_messages_android.ValidatorHelper
import kotlinx.android.synthetic.main.card_forgot_password.*
import kotlinx.android.synthetic.main.card_forgot_password.view.*

class ForgotPasswordFragment : Fragment() {
    private var whiteBoxAnimator : ObjectAnimator? = null
    private var parent: Context? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.card_forgot_password, container, false) as ViewGroup
        val listener = activity as SendPasswordResetListener

        // send password reset
        rootView.btn_send_password_reset.setOnClickListener {
            val email = tv_input_email.editText?.text.toString()
            var validated = true

            // email validation
            if (email.isBlank()) {
                tv_input_email.error = "Must not be blank"
                tv_input_email.requestFocus()
                validated = false
            }
            if (!ValidatorHelper.isValidEmail(email)) {
                tv_input_email.error = "Must be a valid email"
                tv_input_email.requestFocus()
                validated = false
            }

            //fields are validated
            if (validated) {
                val b = Bundle()
                b.putString("email", email)

                startProgressBar()
                listener.onSendPasswordReset(b, ::successListener, ::errorListener)
            }
        }

        rootView.tv_card_label.text = "PASSWORD RECOVERY)"

        return rootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        parent = context
    }

    private fun startProgressBar() {
        progress_container.visibility= View.VISIBLE
        btn_send_password_reset.visibility = View.GONE

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
        btn_send_password_reset.visibility = View.VISIBLE
        if (whiteBoxAnimator != null)
            whiteBoxAnimator!!.end()
    }

    // error listener for invalid sign in e.g. invalid username/password
    // callback for userfront activity
    fun errorListener(error: String) {
        tv_send_password_reset_output.text = error
        tv_send_password_reset_output.setTextColor(ContextCompat.getColor(parent!!, R.color.colorError))

        // reset UI back
        stopProgressBar()
    }

    fun successListener() {
        tv_send_password_reset_output.text = "Password reset email sent!"
        tv_send_password_reset_output.setTextColor(ContextCompat.getColor(parent!!, R.color.colorSuccess))
    }

    interface SendPasswordResetListener {
        fun onSendPasswordReset(bundle: Bundle, succListener: () -> Unit, errListener: (err: String) -> Unit)
    }
}