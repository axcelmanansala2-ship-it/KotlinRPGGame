package com.example.kotlinrpg

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class GameActivity : AppCompatActivity() {

    lateinit var pager: ViewPager2
    private val tabs = listOf("MAP", "BATTLE", "HERO", "CAMP")
    private val tabViews = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        GameState.processIdleTime()

        pager = findViewById(R.id.viewPager)
        pager.adapter = GamePagerAdapter(this)
        pager.offscreenPageLimit = 3
        pager.isUserInputEnabled = true

        val navContainer = findViewById<android.widget.LinearLayout>(R.id.navBar)
        tabs.forEachIndexed { i, label ->
            val tv = TextView(this).apply {
                text = label
                textSize = 12f
                typeface = android.graphics.Typeface.MONOSPACE
                gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener { pager.currentItem = i }
                setPadding(0, 4, 0, 4)
            }
            tabViews.add(tv)
            navContainer.addView(tv)
        }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabHighlight(position)
                if (position == 1) {
                    val frag = supportFragmentManager.findFragmentByTag("f1") as? BattleFragment
                    frag?.onResumeFromPager()
                } else {
                    val frag = supportFragmentManager.findFragmentByTag("f1") as? BattleFragment
                    frag?.onPauseFromPager()
                }
            }
        })
        updateTabHighlight(0)
    }

    private fun updateTabHighlight(pos: Int) {
        tabViews.forEachIndexed { i, tv ->
            if (i == pos) {
                tv.setTextColor(0xFFFFDD00.toInt())
                tv.setBackgroundColor(0xFF1A1A2A.toInt())
            } else {
                tv.setTextColor(0xFF666688.toInt())
                tv.setBackgroundColor(0xFF0D0D18.toInt())
            }
        }
    }

    fun navigateTo(page: Int) { pager.currentItem = page }

    override fun onPause() { super.onPause(); GameState.save() }
    override fun onResume() { super.onResume(); GameState.processIdleTime() }
}
