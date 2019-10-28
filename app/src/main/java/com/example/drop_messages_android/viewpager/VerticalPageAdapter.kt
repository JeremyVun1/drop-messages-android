package com.example.drop_messages_android.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
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
        val newIndex = fragments.indexOf(obj)
        if (newIndex == -1)
            return POSITION_NONE
        else return newIndex
    }

    fun addFragment(fragment: Fragment) {
        fragments.add(fragments.lastIndex-1, fragment)
        notifyDataSetChanged()
    }

    fun removeFragment(id: Int) : Int? {
        for (i in fragments.indices) {
            val f = fragments[i]
            if (f is DropMessageFragment && f.msgId == id) {
                fragments.remove(f)
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