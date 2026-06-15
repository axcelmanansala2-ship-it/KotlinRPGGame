package com.example.kotlinrpg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class HeroFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_hero, container, false)
        refresh(root)
        return root
    }

    private fun refresh(root: View) {
        val container = root.findViewById<LinearLayout>(R.id.heroCardsContainer)
        container.removeAllViews()
        GameState.party.forEachIndexed { i, hero ->
            val card = buildHeroCard(hero, i)
            container.addView(card)
        }
        root.findViewById<TextView>(R.id.tvPlayerLevel).text =
            "Player Lv.${GameState.playerLevel}  •  Gold: ${GameState.gold}  •  Kills: ${GameState.totalKills}"
    }

    private fun buildHeroCard(hero: GameState.PartyHero, idx: Int): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            setBackgroundColor(0xFF0D0D20.toInt())
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, 8)
            layoutParams = lp
        }
        val title = TextView(ctx).apply {
            text = "${hero.heroClass.displayName}  (Lv.${hero.level})"
            setTextColor(hero.heroClass.spriteColor)
            textSize = 15f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0, 0, 0, 4)
        }
        val stats = TextView(ctx).apply {
            text = "HP: ${hero.maxHp}   ATK: ${hero.atk}   DEF: ${hero.def}   CRIT: ${(hero.critChance*100).toInt()}%\n${hero.heroClass.description}"
            setTextColor(0xFF888899.toInt())
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
            setLineSpacing(0f, 1.5f)
        }
        // HP bar
        val hpFrame = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 8)
            setBackgroundColor(0xFF111122.toInt())
        }
        card.addView(title); card.addView(stats); card.addView(hpFrame)
        return card
    }

    override fun onResume() { super.onResume(); view?.let { refresh(it) } }
}
