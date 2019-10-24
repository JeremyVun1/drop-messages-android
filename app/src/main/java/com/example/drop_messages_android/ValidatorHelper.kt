package com.example.drop_messages_android

import android.util.Patterns

object ValidatorHelper {
    fun isValidEmail(str: String) : Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(str).matches()
    }

    fun isValidPassword(str: String) : Boolean {
        return str.length >= 8
    }
}