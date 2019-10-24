package com.example.drop_messages_android.viewpager


// View states
enum class PageViewState {
    TOP, TOP_TRANSITION, CURRENT, BOTTOM
}

// Helper data struct to keep track of fragment state for the vertical view pager
class PageState(var state: PageViewState, var margin: Float)

