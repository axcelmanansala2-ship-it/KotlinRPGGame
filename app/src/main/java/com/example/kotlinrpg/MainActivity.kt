package com.example.kotlinrpg

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    data class Player(
        var maxHp: Int = 100,
        var hp: Int = 100,
        var attack: Int = 15,
        var defense: Int = 5,
        var level: Int = 1,
        var exp: Int = 0,
        var expToNext: Int = 100,
        var potions: Int = 3,
        var stage: Int = 0,
        var isDefending: Boolean = false
    )

    data class Monster(
        val name: String,
        val emoji: String,
        val maxHp: Int,
        var hp: Int,
        val attack: Int,
        val defense: Int,
        val expReward: Int,
        val description: String
    )

    private lateinit var flipper: ViewFlipper
    private lateinit var tvMonsterEmoji: TextView
    private lateinit var tvMonsterName: TextView
    private lateinit var tvMonsterHp: TextView
    private lateinit var progressMonsterHp: ProgressBar
    private lateinit var tvPlayerHp: TextView
    private lateinit var tvPlayerStats: TextView
    private lateinit var progressPlayerHp: ProgressBar
    private lateinit var tvBattleLog: TextView
    private lateinit var scrollBattleLog: ScrollView
    private lateinit var btnAttack: Button
    private lateinit var btnDefend: Button
    private lateinit var btnHeal: Button
    private lateinit var tvStageInfo: TextView
    private lateinit var tvPotions: TextView
    private lateinit var tvStageClear: TextView
    private lateinit var tvStageClearInfo: TextView
    private lateinit var btnNextStage: Button
    private lateinit var tvGameOverMsg: TextView
    private lateinit var btnRetry: Button
    private lateinit var btnPlayAgain: Button

    private var player = Player()
    private var currentMonster: Monster? = null
    private val battleLog = StringBuilder()

    private val stages = listOf(
        Monster("Slime",       "👾", 40,  40,  10, 0,  30,  "A wobbly blob. Your first challenge!"),
        Monster("Goblin",      "👺", 65,  65,  18, 3,  55,  "A sneaky goblin with a rusty dagger!"),
        Monster("Orc Warrior", "🗿", 110, 110, 26, 8,  90,  "A massive orc fueled by brute rage!"),
        Monster("Dark Knight", "🦹", 160, 160, 38, 14, 140, "A fearsome knight cloaked in shadows!"),
        Monster("Dragon",      "🐉", 260, 260, 48, 18, 350, "THE FINAL BOSS — The Ancient Dragon!")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        setupListeners()
    }

    private fun initViews() {
        flipper           = findViewById(R.id.viewFlipper)
        tvMonsterEmoji    = findViewById(R.id.tvMonsterEmoji)
        tvMonsterName     = findViewById(R.id.tvMonsterName)
        tvMonsterHp       = findViewById(R.id.tvMonsterHp)
        progressMonsterHp = findViewById(R.id.progressMonsterHp)
        tvPlayerHp        = findViewById(R.id.tvPlayerHp)
        tvPlayerStats     = findViewById(R.id.tvPlayerStats)
        progressPlayerHp  = findViewById(R.id.progressPlayerHp)
        tvBattleLog       = findViewById(R.id.tvBattleLog)
        scrollBattleLog   = findViewById(R.id.scrollBattleLog)
        btnAttack         = findViewById(R.id.btnAttack)
        btnDefend         = findViewById(R.id.btnDefend)
        btnHeal           = findViewById(R.id.btnHeal)
        tvStageInfo       = findViewById(R.id.tvStageInfo)
        tvPotions         = findViewById(R.id.tvPotions)
        tvStageClear      = findViewById(R.id.tvStageClear)
        tvStageClearInfo  = findViewById(R.id.tvStageClearInfo)
        btnNextStage      = findViewById(R.id.btnNextStage)
        tvGameOverMsg     = findViewById(R.id.tvGameOverMsg)
        btnRetry          = findViewById(R.id.btnRetry)
        btnPlayAgain      = findViewById(R.id.btnPlayAgain)
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.btnNewGame).setOnClickListener { startNewGame() }
        btnAttack.setOnClickListener   { playerAttack() }
        btnDefend.setOnClickListener   { playerDefend() }
        btnHeal.setOnClickListener     { playerHeal()   }
        btnNextStage.setOnClickListener { loadNextStage() }
        btnRetry.setOnClickListener    { resetGame()    }
        btnPlayAgain.setOnClickListener { resetGame()   }
    }

    private fun startNewGame() {
        player = Player()
        battleLog.clear()
        loadNextStage()
    }

    private fun resetGame() {
        player = Player()
        battleLog.clear()
        flipper.displayedChild = 0
    }

    private fun loadNextStage() {
        if (player.stage >= stages.size) {
            flipper.displayedChild = 4
            return
        }
        val template = stages[player.stage]
        currentMonster = template.copy(hp = template.maxHp)
        player.isDefending = false
        battleLog.clear()
        addLog("=== STAGE ${player.stage + 1} / ${stages.size} ===")
        addLog("${template.emoji}  ${template.name} appears!")
        addLog("\"${template.description}\"")
        addLog("-".repeat(30))
        updateBattleUI()
        setActionsEnabled(true)
        flipper.displayedChild = 1
    }

    private fun playerAttack() {
        val monster = currentMonster ?: return
        setActionsEnabled(false)
        val isCrit  = Random.nextFloat() < 0.15f
        val rawDmg  = max(1, player.attack - monster.defense + Random.nextInt(-3, 4))
        val damage  = if (isCrit) (rawDmg * 1.8).toInt() else rawDmg
        monster.hp  = max(0, monster.hp - damage)
        if (isCrit) addLog("*** CRITICAL HIT! You deal $damage dmg! ***")
        else        addLog("You attack ${monster.name} for $damage dmg!")
        player.isDefending = false
        updateBattleUI()
        if (monster.hp <= 0) { onMonsterDefeated(); return }
        monsterTurn()
    }

    private fun playerDefend() {
        player.isDefending = true
        addLog("You raise your guard! (next hit -60%)")
        monsterTurn()
    }

    private fun playerHeal() {
        if (player.potions <= 0) { addLog("No potions left!"); return }
        val heal   = min((player.maxHp * 0.30).toInt(), player.maxHp - player.hp)
        player.hp += heal
        player.potions--
        addLog("You drink a potion! Recover $heal HP. (${player.potions} left)")
        player.isDefending = false
        monsterTurn()
    }

    private fun monsterTurn() {
        val monster  = currentMonster ?: return
        val rawDmg   = max(1, monster.attack - player.defense + Random.nextInt(-3, 4))
        val damage   = if (player.isDefending) max(1, (rawDmg * 0.4).toInt()) else rawDmg
        player.hp    = max(0, player.hp - damage)
        val extra    = if (player.isDefending) " [BLOCKED]" else ""
        addLog("${monster.name} hits you for $damage dmg!$extra")
        player.isDefending = false
        addLog("-".repeat(30))
        updateBattleUI()
        if (player.hp <= 0) onPlayerDefeated() else setActionsEnabled(true)
    }

    private fun onMonsterDefeated() {
        val monster  = currentMonster!!
        player.exp  += monster.expReward
        addLog("${monster.name} is defeated!")
        addLog("You gained ${monster.expReward} EXP!")
        while (player.exp >= player.expToNext) levelUp()
        player.stage++
        updateBattleUI()
        if (player.stage >= stages.size) {
            tvStageClear.text    = "VICTORY!!"
            tvStageClearInfo.text = "You have conquered ALL enemies!\nLv.${player.level}  HP:${player.hp}/${player.maxHp}\nATK:${player.attack}  DEF:${player.defense}"
            btnNextStage.text    = "See Final Screen"
        } else {
            val next = stages[player.stage]
            tvStageClear.text    = "STAGE ${player.stage} CLEAR!"
            tvStageClearInfo.text = "Lv.${player.level}  HP:${player.hp}/${player.maxHp}\nATK:${player.attack}  DEF:${player.defense}\nEXP:${player.exp}/${player.expToNext}\n\nNext: ${next.emoji} ${next.name}"
            btnNextStage.text    = "Next Battle"
        }
        flipper.displayedChild = 2
    }

    private fun levelUp() {
        player.level++
        player.exp      -= player.expToNext
        player.expToNext = (player.expToNext * 1.5).toInt()
        player.maxHp += 20
        player.hp     = min(player.hp + 20, player.maxHp)
        player.attack += 5
        player.defense += 2
        addLog("*** LEVEL UP! Now Lv.${player.level} | HP+20 ATK+5 DEF+2 ***")
    }

    private fun onPlayerDefeated() {
        tvGameOverMsg.text = "You were defeated by ${currentMonster?.name}...\n\nReached Stage ${player.stage + 1}\nLevel ${player.level} Hero"
        flipper.displayedChild = 3
    }

    private fun addLog(msg: String) {
        battleLog.append(msg).append("\n")
        tvBattleLog.text = battleLog.toString()
        scrollBattleLog.post { scrollBattleLog.fullScroll(View.FOCUS_DOWN) }
    }

    private fun setActionsEnabled(enabled: Boolean) {
        btnAttack.isEnabled = enabled
        btnDefend.isEnabled = enabled
        btnHeal.isEnabled   = enabled && player.potions > 0
    }

    private fun updateBattleUI() {
        val monster = currentMonster ?: return
        tvStageInfo.text       = "Stage ${player.stage + 1} / ${stages.size}"
        tvMonsterEmoji.text    = monster.emoji
        tvMonsterName.text     = monster.name
        tvMonsterHp.text       = "HP: ${monster.hp} / ${monster.maxHp}"
        progressMonsterHp.max  = monster.maxHp
        progressMonsterHp.progress = monster.hp
        tvPlayerHp.text        = "HP: ${player.hp} / ${player.maxHp}"
        progressPlayerHp.max   = player.maxHp
        progressPlayerHp.progress = player.hp
        tvPlayerStats.text     = "Lv.${player.level}   ATK:${player.attack}   DEF:${player.defense}   EXP:${player.exp}/${player.expToNext}"
        tvPotions.text         = "Potions: ${player.potions}"
        btnHeal.isEnabled      = player.potions > 0
    }
}
