package com.example.drop_messages_android

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.fragments.ForgotPasswordFragment
import com.example.drop_messages_android.fragments.ForgotPasswordFragment.SendPasswordResetListener
import com.example.drop_messages_android.viewpager.VerticalPageAdapter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_user_front.*

/**
 * Main activity where user can make api requests through a web socket to create and retrieve data
 */
class ForgotPasswordActivity : AppCompatActivity(), SendPasswordResetListener {
    private var firebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_front)

        firebaseAuth = FirebaseAuth.getInstance()

        setSupportActionBar(toolbar as Toolbar)

        initialiseUI()
    }

    private fun initialiseUI() {
        val frag = ForgotPasswordFragment()

        val verticalPageAdapter = VerticalPageAdapter(
            mutableListOf(frag as Fragment),
            supportFragmentManager
        )

        fragment_container.adapter = verticalPageAdapter
    }

    override fun onSendPasswordReset(bundle: Bundle, succListener: () -> Unit, errListener: (err: String) -> Unit) {
        val email = bundle.getString("email") ?: ""
        firebaseAuth!!.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(applicationContext, "Passwor reset email sent!", Toast.LENGTH_SHORT).show()
                    succListener()
                    finish()
                }
                else {
                    errListener(task.exception.toString())
                }
            }
    }
}