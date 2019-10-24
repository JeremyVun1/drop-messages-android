package com.example.drop_messages_android

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_loading.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// This is the main entry point which checks whether to route the user to the login/signup pages
// or directly launch the activity with stored credentials
class MainLoaderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
        overridePendingTransition(0, 0)

        initAnimations()

        // check whether we have a saved login
        if (isLoginSaved()) {
            login()

            // when done, go to main activity
        }
        else {
            CoroutineScope(IO).launch {
                delay(2000)

                withContext(Main) {
                    navToUserFront()
                }
            }
        }
    }

    private fun login() {
        // get auth token, open web socket
    }

    private fun isLoginSaved() : Boolean {
        return false
    }

    private fun navToUserFront() {
        val i = Intent(this, UserFrontActivity::class.java)
        startActivity(i)
        finishActivity(0)
    }

    private fun initAnimations() {
        // button animation
        var slideX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, 90f)

        ObjectAnimator.ofPropertyValuesHolder(img_white_cover, slideX).apply {
            interpolator = LinearInterpolator()
            duration = 2000
            repeatMode = ObjectAnimator.RESTART
            repeatCount = ObjectAnimator.INFINITE
        }.start()

        // logo pulse
        var scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f, 1f)
        var scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f, 1f)
        var alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.8f, 1f)
        ObjectAnimator.ofPropertyValuesHolder(img_logo, scaleX, scaleY, alpha).apply {
            interpolator = OvershootInterpolator()
            duration = 1000
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
        }.start()
        /*

        // title animation
        ObjectAnimator.ofPropertyValuesHolder(view.tv_title, scaleX, scaleY, alpha).apply {
            interpolator = OvershootInterpolator()
            duration = 500
        }.start()

        view.hint_action_scroll.translationY = -120f

        scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.7f, 1.1f)
        scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.7f, 1.1f)

        val animator =
            ObjectAnimator.ofPropertyValuesHolder(view.hint_action_scroll, scaleX, scaleY).apply {
                duration = 500
                startDelay = 6000
                repeatMode = ObjectAnimator.REVERSE
                repeatCount = 4
                interpolator = OvershootInterpolator()
            }
        animator.start()

        // text animation
        val text_slideY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, -20f)
        val text_alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.8f, 1f)
        val text_scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.9f, 1.1f)
        val text_scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.9f, 1.1f)
        ObjectAnimator.ofPropertyValuesHolder(
            view.tv_pull_up_hint,
            text_scaleX,
            text_scaleY,
            text_alpha,
            text_slideY
        ).apply {
            interpolator = AccelerateInterpolator()
            duration = 1800
            startDelay = 300
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
        }.start()

         */
    }

    /*
    PostRequest(url, json,
                {
                    val response = gson.fromJson(it.toString(), JsonObject::class.java)

                    if (response.has("token")) {
                        Toast.makeText(applicationContext, "successfully authorised", Toast.LENGTH_SHORT).show()
                        //
                    }

                },
                {

                }
            )
     */
}