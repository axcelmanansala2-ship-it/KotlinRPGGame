package com.example.kotlinrpg

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class GamePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 4
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> MapFragment()
        1 -> BattleFragment()
        2 -> HeroFragment()
        3 -> CampFragment()
        else -> MapFragment()
    }
}
