package com.example.kotlinrpg

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class GameActivity : AppCompatActivity() {

    private lateinit var pager: ViewPager2
    private val tabs = listOf("MAP", "BATTLE", "HEROES", "CAMP")
    private val tabViews = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        GameState.processIdleTime()

        pager = findViewById(R.id.viewPager)
        pager.adapter = GamePagerAdapter(this)
        pager.offscreenPageLimit = 3

        val navBar = findViewById<LinearLayout>(R.id.navBar)
        tabs.forEachIndexed { i, label ->
            val tv = TextView(this).apply {
                text = label
                textSize = 11f
                typeface = android.graphics.Typeface.MONOSPACE
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener { pager.currentItem = i }
                setPadding(0, 4, 0, 4)
                letterSpacing = 0.08f
            }
            tabViews.add(tv)
            navBar.addView(tv)
        }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabs(position)
            }
        })
        updateTabs(0)
    }

    private fun updateTabs(pos: Int) {
        tabViews.forEachIndexed { i, tv ->
            tv.setTextColor(if (i == pos) 0xFFFFDD00.toInt() else 0xFF445566.toInt())
            tv.setBackgroundColor(if (i == pos) 0xFF14141E.toInt() else 0xFF0A0A12.toInt())
        }
    }

    fun navigateTo(page: Int) { pager.currentItem = page }
    override fun onPause() { super.onPause(); GameState.save() }
    override fun onResume() { super.onResume(); GameState.processIdleTime() }
}
