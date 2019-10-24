package com.example.drop_messages_android.viewpager

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.card_test.view.*
import kotlin.math.max
import kotlin.math.min

class VerticalViewPager(context: Context, attrs: AttributeSet?): ViewPager(context, attrs) {

    init {
        setPageTransformer(true,
            VerticalPage()
        )
        overScrollMode = View.OVER_SCROLL_NEVER
    }

    // swap x and y coordinates of touch event
    private fun swapXY(ev: MotionEvent) : MotionEvent {
        val newX = (ev.y / height) * width
        val newY = (ev.x / width) * height

        ev.setLocation(newX, newY)
        return ev
    }


    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val intercepted: Boolean = super.onInterceptTouchEvent(swapXY(ev))
        swapXY(ev)
        return intercepted
    }

    override fun onTouchEvent(ev: MotionEvent) : Boolean {
        return super.onTouchEvent(swapXY(ev))
    }

    private class VerticalPage : PageTransformer {

        val pageState = HashMap<View, PageState>()
        val STACK_MARGIN = 0.01f
        val MAX_MARGIN = 0.05f

        var start_mod = 0.04f
        var start_delta = STACK_MARGIN
        var start_elevation = 12f
        var curr_card_elevation = 10f
        val elevation_step = 2f
        var starting = true

        private fun pushTopStack(page: View, dir: Int) {
            for ((k, v) in pageState) {
                if (v.state == PageViewState.TOP) {
                    v.margin += STACK_MARGIN * dir

                    var newY = 0f
                    if (dir == 1) {
                        newY = -0.90f + min(v.margin, MAX_MARGIN)
                        k.elevation -= elevation_step
                    }
                    else {
                        newY = -0.90f + min(v.margin, MAX_MARGIN)
                        k.elevation += elevation_step
                    }

                    k.translationY = newY * page.height
                }
            }
        }

        private fun pushBotStack(page: View, dir: Int) {
            for ((k, v) in pageState) {
                if (v.state == PageViewState.BOTTOM) {
                    v.margin += (STACK_MARGIN - start_delta) * dir

                    var newY = 0f
                    if (dir == 1) {
                        newY = min(v.margin, MAX_MARGIN)
                        if (starting)
                            k.elevation += elevation_step + 0.5f
                        k.elevation -= elevation_step
                    }
                    else {
                        newY = max(v.margin, 0f)
                        k.elevation -= elevation_step
                    }

                    k.translationY = (-MAX_MARGIN + newY) * page.height
                    //println("${page.tv_output.text} e: ${page.elevation}")
                }
            }
        }


        override fun transformPage(page: View, position: Float) {
            if (!pageState.containsKey(page)) {
                pageState[page] = PageState(PageViewState.CURRENT, 0f)
                page.elevation = curr_card_elevation
            } else {
                start_delta = 0f
                start_mod = 0.05f
                starting = false
            }

            // Card is now at the top
            if (position <= -0.8) {
                val ps = pageState[page]
                page.alpha = 1f

                // card was previously transitioning
                if (ps!!.state == PageViewState.TOP_TRANSITION) {
                    ps.state = PageViewState.TOP
                    pushTopStack(page, 1)
                }

                //cancel X movement
                page.translationX = page.width * -position
            }

            // Card is now between top and current
            else if (position < 0) {
                val ps = pageState[page]

                // card was previously at the top
                if (ps!!.state == PageViewState.TOP) {
                    //take card off the top stack
                    ps.state = PageViewState.TOP_TRANSITION
                    ps.margin = 0f
                    page.elevation = 15f
                    pushTopStack(page, -1)
                }

                // card was previously at current
                else if (ps.state == PageViewState.CURRENT) {
                    ps.state = PageViewState.TOP_TRANSITION
                    ps.margin = 0f
                    page.elevation = 30f
                }

                page.alpha = (1+(position/3)) + 0.1f

                page.translationX = page.width * -position
                page.translationY = (position * page.height)
            }

            // card is in current position
            else if (position == 0f) {
                val ps = pageState[page]
                page.alpha = 1f
                page.rotation = 0f

                //came from top transition
                if (ps!!.state == PageViewState.TOP_TRANSITION) {
                    ps.state = PageViewState.CURRENT
                    ps.margin = 0f
                    page.elevation = curr_card_elevation
                }

                // came from bottom stack
                else if (ps.state == PageViewState.BOTTOM) {
                    // push bottom stack cards up
                    ps.state = PageViewState.CURRENT
                    ps.margin = 0f
                    page.elevation = curr_card_elevation
                    pushBotStack(page, -1)
                }
                page.translationX = 0f
                page.translationY = -MAX_MARGIN * page.height
            }

            // card is at the bottom
            else {
                val ps = pageState[page]

                // card was previously the current card
                if (ps!!.state == PageViewState.CURRENT) {
                    ps.state = PageViewState.BOTTOM
                    ps.margin = MAX_MARGIN - start_mod
                    page.elevation = 8f
                    start_mod -= 0.01f

                    pushBotStack(page, 1)
                }
                // cancel X movements
                page.translationX = page.width * -position
            }
        }
    }
}