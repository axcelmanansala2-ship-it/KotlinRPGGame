package com.example.kotlinrpg

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.math.sin

class HeroFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_hero, container, false)
        refresh(root)
        return root
    }

    private fun refresh(root: View) {
        val gs = GameState
        root.findViewById<TextView>(R.id.tvHeroName).text = "${gs.heroClass.displayName}  •  Level ${gs.heroLevel}"
        root.findViewById<TextView>(R.id.tvHeroDesc).text = gs.heroClass.description
        root.findViewById<TextView>(R.id.tvStats).text =
            "HP      ${gs.heroHp} / ${gs.heroMaxHp}\n" +
            "ATK     ${gs.heroAtk}\n" +
            "DEF     ${gs.heroDef}\n" +
            "EXP     ${gs.heroExp} / ${gs.heroExpNext}\n" +
            "Gold    ${gs.gold}\n" +
            "Potions ${gs.potions}\n" +
            "Kills   ${gs.totalKills}\n" +
            "Zone    ${gs.currentZone().name}"
        val expPct = gs.heroExp.toFloat() / gs.heroExpNext
        root.findViewById<View>(R.id.expBar).scaleX = expPct.coerceIn(0f, 1f)
        root.findViewById<View>(R.id.hpBar).scaleX = (gs.heroHp.toFloat() / gs.heroMaxHp).coerceIn(0f, 1f)
    }

    override fun onResume() { super.onResume(); view?.let { refresh(it) } }
}
