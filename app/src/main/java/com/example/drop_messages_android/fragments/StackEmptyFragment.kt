package com.example.drop_messages_android.fragments

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.R
import kotlinx.android.synthetic.main.card_stack_empty.*
import kotlinx.android.synthetic.main.card_test_small.view.*
import java.util.*


class StackEmptyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.card_stack_empty, container, false) as ViewGroup
        return rootView
    }

    /**
     * Start animations when it comes into foreground
     */
    override fun onResume() {
        super.onResume()

        initAnimations()
    }

    private fun initAnimations() {
        // button animation
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.5f, 0.65f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.5f, 0.65f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.95f, 1f)
        val pullDownSlideY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, 30f)

        ObjectAnimator.ofPropertyValuesHolder(pull_down_fab, scaleX, scaleY, alpha, pullDownSlideY).apply {
            interpolator = OvershootInterpolator()
            startDelay = 0
            duration = 1000
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
        }.start()


        val pullUpSlideY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, -30f)
        ObjectAnimator.ofPropertyValuesHolder(pull_up_fab, scaleX, scaleY, alpha, pullUpSlideY).apply {
            interpolator = OvershootInterpolator()
            startDelay = 0
            duration = 1000
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
        }.start()
    }
}