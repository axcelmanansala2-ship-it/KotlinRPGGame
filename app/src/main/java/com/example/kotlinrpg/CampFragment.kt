package com.example.kotlinrpg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class CampFragment : Fragment() {

    private lateinit var tvGold: TextView
    private lateinit var tvInfo: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_camp, container, false)
        tvGold = root.findViewById(R.id.tvGold)
        tvInfo = root.findViewById(R.id.tvInfo)

        root.findViewById<TextView>(R.id.btnBuyPotion).setOnClickListener { buyPotion() }
        root.findViewById<TextView>(R.id.btnUpgradeAtk).setOnClickListener { upgradeAtk() }
        root.findViewById<TextView>(R.id.btnUpgradeDef).setOnClickListener { upgradeDef() }
        root.findViewById<TextView>(R.id.btnUpgradeHp).setOnClickListener { upgradeHp() }
        root.findViewById<TextView>(R.id.btnRest).setOnClickListener { rest() }

        updateDisplay()
        return root
    }

    private fun buyPotion() {
        val cost = 30L
        if (GameState.gold < cost) { showInfo("Need $cost gold!"); return }
        GameState.gold -= cost; GameState.potions++; GameState.save()
        showInfo("Bought a potion! (${GameState.potions} total)")
        updateDisplay()
    }

    private fun upgradeAtk() {
        val cost = (GameState.heroAtk * 8L)
        if (GameState.gold < cost) { showInfo("Need $cost gold!"); return }
        GameState.gold -= cost; GameState.heroAtk += 2; GameState.save()
        showInfo("ATK upgraded to ${GameState.heroAtk}!")
        updateDisplay()
    }

    private fun upgradeDef() {
        val cost = (GameState.heroDef * 10L + 20L)
        if (GameState.gold < cost) { showInfo("Need $cost gold!"); return }
        GameState.gold -= cost; GameState.heroDef += 1; GameState.save()
        showInfo("DEF upgraded to ${GameState.heroDef}!")
        updateDisplay()
    }

    private fun upgradeHp() {
        val cost = ((GameState.heroMaxHp / 10) * 5L)
        if (GameState.gold < cost) { showInfo("Need $cost gold!"); return }
        GameState.gold -= cost; GameState.heroMaxHp += 20; GameState.heroHp += 20; GameState.save()
        showInfo("Max HP upgraded to ${GameState.heroMaxHp}!")
        updateDisplay()
    }

    private fun rest() {
        val cost = 20L
        if (GameState.gold < cost) { showInfo("Need $cost gold to rest!"); return }
        GameState.gold -= cost
        GameState.heroHp = GameState.heroMaxHp
        GameState.save()
        showInfo("Fully rested! HP restored.")
        updateDisplay()
    }

    private fun showInfo(msg: String) { tvInfo.text = msg }

    private fun updateDisplay() {
        val gs = GameState
        tvGold.text = "Gold: ${gs.gold}"
        val atkCost = gs.heroAtk * 8
        val defCost = gs.heroDef * 10 + 20
        val hpCost = (gs.heroMaxHp / 10) * 5
        val lines = buildString {
            appendLine("[ CAMP ]  Rest & upgrade your hero")
            appendLine()
            appendLine("▸ BUY POTION       30 gold  (${gs.potions} owned)")
            appendLine("▸ UPGRADE ATK      $atkCost gold  (ATK +2, now ${gs.heroAtk})")
            appendLine("▸ UPGRADE DEF      $defCost gold  (DEF +1, now ${gs.heroDef})")
            appendLine("▸ UPGRADE HP       $hpCost gold  (MaxHP +20, now ${gs.heroMaxHp})")
            appendLine("▸ REST             20 gold  (Full HP restore)")
        }
        tvInfo.text = lines
    }

    override fun onResume() { super.onResume(); updateDisplay() }
}
