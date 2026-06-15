package com.example.kotlinrpg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class CampFragment : Fragment() {
    private var selectedHeroIdx = 0
    private lateinit var tvGold: TextView
    private lateinit var tvInfo: TextView
    private lateinit var heroTabsLayout: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_camp, container, false)
        tvGold = root.findViewById(R.id.tvGold)
        tvInfo = root.findViewById(R.id.tvInfo)
        heroTabsLayout = root.findViewById(R.id.heroTabs)

        root.findViewById<TextView>(R.id.btnBuyPotion).setOnClickListener { buyPotion() }
        root.findViewById<TextView>(R.id.btnUpgradeAtk).setOnClickListener { upgradeAtk() }
        root.findViewById<TextView>(R.id.btnUpgradeDef).setOnClickListener { upgradeDef() }
        root.findViewById<TextView>(R.id.btnUpgradeHp).setOnClickListener  { upgradeHp() }
        root.findViewById<TextView>(R.id.btnRest).setOnClickListener       { rest() }
        root.findViewById<TextView>(R.id.btnLevelUp).setOnClickListener    { levelUpHero() }

        root.findViewById<TextView>(R.id.btnUnlockAssassin).setOnClickListener { unlockHero(HeroClass.ASSASSIN, 300L) }
        root.findViewById<TextView>(R.id.btnUnlockPriest).setOnClickListener   { unlockHero(HeroClass.PRIEST,   280L) }

        setupHeroTabs()
        updateDisplay()
        return root
    }

    private fun setupHeroTabs() {
        heroTabsLayout.removeAllViews()
        GameState.party.forEachIndexed { i, hero ->
            val btn = TextView(requireContext()).apply {
                text = hero.heroClass.displayName.take(4)
                textSize = 11f
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(12, 8, 12, 8)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = android.view.Gravity.CENTER
                setOnClickListener { selectedHeroIdx = i; setupHeroTabs(); updateDisplay() }
            }
            btn.setBackgroundColor(if (i == selectedHeroIdx) 0xFF1A1A3A.toInt() else 0xFF0D0D18.toInt())
            btn.setTextColor(if (i == selectedHeroIdx) hero.heroClass.spriteColor else 0xFF555566.toInt())
            heroTabsLayout.addView(btn)
        }
    }

    private fun currentHero() = GameState.party.getOrNull(selectedHeroIdx)

    private fun buyPotion() {
        if (GameState.gold < 30) { show("Need 30 gold!"); return }
        GameState.gold -= 30; GameState.potions++; GameState.save()
        show("Potion bought! (${GameState.potions} total)"); updateDisplay()
    }

    private fun upgradeAtk() {
        val cost = GameState.heroUpgradeCost(selectedHeroIdx, "atk")
        val h = currentHero() ?: return
        if (GameState.gold < cost) { show("Need $cost gold!"); return }
        GameState.gold -= cost; h.bonusAtk += 2; GameState.save()
        show("${h.heroClass.displayName} ATK +2! Now ${h.atk}"); updateDisplay()
    }

    private fun upgradeDef() {
        val cost = GameState.heroUpgradeCost(selectedHeroIdx, "def")
        val h = currentHero() ?: return
        if (GameState.gold < cost) { show("Need $cost gold!"); return }
        GameState.gold -= cost; h.bonusDef += 1; GameState.save()
        show("${h.heroClass.displayName} DEF +1! Now ${h.def}"); updateDisplay()
    }

    private fun upgradeHp() {
        val cost = GameState.heroUpgradeCost(selectedHeroIdx, "hp")
        val h = currentHero() ?: return
        if (GameState.gold < cost) { show("Need $cost gold!"); return }
        GameState.gold -= cost; h.bonusHp += 20; GameState.save()
        show("${h.heroClass.displayName} MaxHP +20! Now ${h.maxHp}"); updateDisplay()
    }

    private fun rest() {
        if (GameState.gold < 25) { show("Need 25 gold!"); return }
        GameState.gold -= 25; GameState.save()
        show("Party rested & healed fully!"); updateDisplay()
    }

    private fun levelUpHero() {
        val cost = GameState.heroUpgradeCost(selectedHeroIdx, "lvl")
        val h = currentHero() ?: return
        if (GameState.gold < cost) { show("Need $cost gold for level up!"); return }
        GameState.gold -= cost; h.level++; GameState.save()
        show("${h.heroClass.displayName} → Level ${h.level}!  All stats increased!"); updateDisplay()
    }

    private fun unlockHero(cls: HeroClass, cost: Long) {
        if (cls in GameState.unlockedClasses && GameState.party.any { it.heroClass == cls }) {
            show("${cls.displayName} already in party!"); return
        }
        if (GameState.gold < cost) { show("Need $cost gold!"); return }
        if (GameState.party.size >= 3) { show("Party is full! (max 3 heroes)"); return }
        GameState.gold -= cost
        GameState.unlockedClasses.add(cls)
        GameState.party.add(GameState.PartyHero(cls))
        GameState.save()
        setupHeroTabs()
        show("${cls.displayName} joined the party!"); updateDisplay()
    }

    private fun show(msg: String) { tvInfo.text = msg }

    private fun updateDisplay() {
        val h = currentHero()
        tvGold.text = "Gold: ${GameState.gold}"
        val atkCost = if (h != null) GameState.heroUpgradeCost(selectedHeroIdx, "atk") else 0
        val defCost = if (h != null) GameState.heroUpgradeCost(selectedHeroIdx, "def") else 0
        val hpCost  = if (h != null) GameState.heroUpgradeCost(selectedHeroIdx, "hp")  else 0
        val lvlCost = if (h != null) GameState.heroUpgradeCost(selectedHeroIdx, "lvl") else 0
        tvInfo.text = buildString {
            appendLine("[ CAMP ]  Select hero tab to upgrade")
            appendLine()
            if (h != null) {
                appendLine("Selected: ${h.heroClass.displayName}  Lv.${h.level}")
                appendLine("  HP ${h.maxHp}  ATK ${h.atk}  DEF ${h.def}  CRIT ${(h.critChance*100).toInt()}%")
                appendLine()
                appendLine("▸ ATK +2    ${atkCost}g")
                appendLine("▸ DEF +1    ${defCost}g")
                appendLine("▸ HP +20    ${hpCost}g")
                appendLine("▸ LEVEL UP  ${lvlCost}g")
            }
            appendLine()
            appendLine("▸ POTION    30g  (${GameState.potions} owned)")
            appendLine("▸ REST      25g  (full party heal)")
            appendLine()
            if (HeroClass.ASSASSIN !in GameState.unlockedClasses) appendLine("▸ Unlock Assassin  300g  (Glass Cannon)")
            if (HeroClass.PRIEST   !in GameState.unlockedClasses) appendLine("▸ Unlock Priest    280g  (Healer Support)")
        }
    }

    override fun onResume() { super.onResume(); if (::tvGold.isInitialized) { setupHeroTabs(); updateDisplay() } }
}
