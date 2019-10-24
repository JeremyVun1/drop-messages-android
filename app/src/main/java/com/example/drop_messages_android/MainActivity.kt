package com.example.drop_messages_android

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.fragments.IndexFragment
import com.example.drop_messages_android.fragments.RegisterFragment
import com.example.drop_messages_android.fragments.TestFragment
import com.example.drop_messages_android.viewpager.VerticalPageAdapter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //initialiseUI()
        initTestUI()
    }

    private fun initialiseUI() {

        val indexFragment = IndexFragment()
        val registerFragment = RegisterFragment()
        val registerFragmentB = RegisterFragment()
        val registerFragmentC = RegisterFragment()
        val registerFragmentD = RegisterFragment()

        val fragments = arrayOf(
            indexFragment,
            registerFragment,
            registerFragmentB,
            registerFragmentC,
            registerFragmentD
        )

        val vertPageAdapter = VerticalPageAdapter(
            fragments,
            supportFragmentManager
        )
        fragment_container.adapter = vertPageAdapter
        //fragment_container.elevation = 20f
        //fragment_container.pageMargin = -70
        fragment_container.offscreenPageLimit = 10
    }

    private fun initTestUI() {
        val fragA = TestFragment()
        val fragB = TestFragment()
        val fragC = TestFragment()
        val fragD = TestFragment()

        val fragments = arrayOf<Fragment>(
            fragA,
            fragB,
            fragC,
            fragD
        )

        val verticalPageAdapter = VerticalPageAdapter(
            fragments,
            supportFragmentManager
        )
        // val cardPageAdapter = CardPageAdapter(this, data)
        fragment_container.adapter = verticalPageAdapter
        //fragment_container.elevation = 20f
        //pager_container.setBackgroundColor(Color.RED)
        fragment_container.offscreenPageLimit = 10
    }
}
