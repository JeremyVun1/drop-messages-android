package com.example.drop_messages_android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.R
import com.example.drop_messages_android.api.DropMessage
import com.google.gson.Gson
import java.util.*


class TestFragment(val str: String) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        val rootView = inflater.inflate(R.layout.card_test_large, container, false) as ViewGroup

        val rnd = Random()
        //rootView.tv_output.text = rnd.nextInt(20).toString()

        val gson = Gson()
        val list = gson.fromJson(str, Array<DropMessage>::class.java)
        println("LIST")
        println(list)

        println("ELEMENTS")
        for (l in list) {
            println(l)
        }




        return rootView
    }
}