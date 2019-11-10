package com.example.drop_messages_android.fragments

interface DropMessageFragmentListener {
    fun onUpvote(id: String?)
    fun onDownvote(id: String?)
    fun onDelete(id: String?)
}