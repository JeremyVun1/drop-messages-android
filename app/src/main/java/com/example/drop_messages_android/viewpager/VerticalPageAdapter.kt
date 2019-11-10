package com.example.drop_messages_android.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.example.drop_messages_android.fragments.DropMessageFragment


class VerticalPageAdapter(private var fragments: MutableList<Fragment>, fm: FragmentManager)
    : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun getCount(): Int {
        return fragments.size
    }

    // for view pager to repopulate with valid fragment views
    // after notifyDataSetChanged() is called
    override fun getItemPosition(obj: Any): Int {
        //return PagerAdapter.POSITION_NONE

        val newIndex = fragments.indexOf(obj)
        if (newIndex == -1)
            return POSITION_NONE
        else return newIndex

    }

    fun addFragment(fragment: Fragment) {
        if (fragments.size == 0)
            fragments.add(0, fragment)
        else fragments.add(fragments.size-1, fragment)
        notifyDataSetChanged()
    }

    fun removeFragment(id: String) : Int? {
        for (i in fragments.indices) {
            val f = fragments[i]
            if (f is DropMessageFragment && f.msgId == id) {
                fragments.removeAt(i)
                f.fragmentManager?.beginTransaction()!!.remove(f).commit()
                notifyDataSetChanged()
                return i
            }
        }
        return null
    }

    fun setFragments(newFragments: MutableList<Fragment>) {
        fragments = newFragments
        notifyDataSetChanged()
    }

}