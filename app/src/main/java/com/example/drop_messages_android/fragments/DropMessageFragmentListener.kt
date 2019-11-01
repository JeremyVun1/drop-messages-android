package com.example.drop_messages_android.fragments

interface DropMessageFragmentListener {
    fun onUpvote(id: Int?)
    fun onDownvote(id: Int?)
    fun onDelete(id: Int?)
}