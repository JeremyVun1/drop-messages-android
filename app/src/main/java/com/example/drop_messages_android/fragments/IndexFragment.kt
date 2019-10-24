package com.example.drop_messages_android.fragments

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.R
import kotlinx.android.synthetic.main.card_index_fragment.view.*
import kotlinx.android.synthetic.main.card_index_fragment.view.tv_pull_up_hint
import kotlinx.android.synthetic.main.card_index_fragment.view.tv_title as tv_title

class IndexFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.card_index_fragment, container, false) as ViewGroup
        play_entry_animation(root)




        return root
    }

    private fun play_entry_animation(view: View) {
        // button animation
        var scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.3f, 1.1f)
        var scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.3f, 1.1f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f)
        var slideY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, -120f)

        ObjectAnimator.ofPropertyValuesHolder(view.hint_action_scroll, scaleX, scaleY, alpha, slideY).apply {
            interpolator = OvershootInterpolator()
            startDelay = 0
            duration = 1000
        }.start()

        // title animation
        ObjectAnimator.ofPropertyValuesHolder(view.tv_title, scaleX, scaleY, alpha).apply {
            interpolator = OvershootInterpolator()
            duration = 500
        }.start()

        view.hint_action_scroll.translationY = -120f

        scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.7f, 1.1f)
        scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.7f, 1.1f)

        val animator = ObjectAnimator.ofPropertyValuesHolder(view.hint_action_scroll, scaleX, scaleY).apply {
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
        ObjectAnimator.ofPropertyValuesHolder(view.tv_pull_up_hint, text_scaleX, text_scaleY, text_alpha, text_slideY).apply {
            interpolator = AccelerateInterpolator()
            duration = 1800
            startDelay = 300
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
        }.start()
    }
}