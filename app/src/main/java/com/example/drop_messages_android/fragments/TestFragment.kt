package com.example.drop_messages_android.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.R
import kotlinx.android.synthetic.main.card_test.*
import kotlinx.android.synthetic.main.card_test.view.*
import java.util.*


class TestFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        val rootView = inflater.inflate(R.layout.card_test_large, container, false) as ViewGroup

        val rnd = Random()

        rootView.tv_output.text = rnd.nextInt(20).toString()

        return rootView
    }
}