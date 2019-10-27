package com.example.drop_messages_android


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.fragments.TestFragment
import com.example.drop_messages_android.viewpager.VerticalPageAdapter
import kotlinx.android.synthetic.main.activity_test_index_pager.*


class TestActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_index_pager)

        initTestUI()
        //initTestUISmall()
    }

    private fun initTestUI() {
        val fragA = TestFragment("[{'id': 3, 'lat': 1.0, 'long': 1.0, 'date': '27/10/2019', 'message': 'oihoihoh', 'votes': 1, 'seen': 0}, {'id': 4, 'lat': 1.0, 'long': 1.0, 'date': '27/10/2019', 'message': 'hrrrrrrrrrr', 'votes': 1, 'seen': 0}, {'id': 5, 'lat': 1.0, 'long': 1.0, 'date': '27/10/2019', 'message': 'kjhkljhoijoji', 'votes': 1, 'seen': 0}]")

        val fragments = mutableListOf<Fragment>(
            fragA
        )

        val verticalPageAdapter = VerticalPageAdapter(
            fragments,
            supportFragmentManager
        )
        fragment_container.adapter = verticalPageAdapter
        //fragment_container.pageMargin = -80
        fragment_container.offscreenPageLimit = 10
    }

    /*
    private fun initTestUISmall() {
        val fragA = TestFragmentSmall()
        val fragB = TestFragmentSmall()
        val fragC = TestFragmentSmall()
        val fragD = TestFragmentSmall()

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
        fragment_container.adapter = verticalPageAdapter
        //fragment_container.pageMargin = -80
        fragment_container.offscreenPageLimit = 10
    }

     */
}