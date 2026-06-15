package com.example.kotlinrpg

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment

class BattleFragment : Fragment() {

    private lateinit var battleView: BattleView
    private lateinit var tvLog: TextView
    private lateinit var svLog: ScrollView
    private lateinit var tvStatus: TextView
    private lateinit var tvIdleInfo: TextView
    private lateinit var btnPotion: TextView
    private val log = StringBuilder()
    private var currentEnemyIdx = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_battle, container, false)
        battleView = root.findViewById(R.id.battleView)
        tvLog = root.findViewById(R.id.tvLog)
        svLog = root.findViewById(R.id.svLog)
        tvStatus = root.findViewById(R.id.tvStatus)
        tvIdleInfo = root.findViewById(R.id.tvIdleInfo)
        btnPotion = root.findViewById(R.id.btnPotion)

        setupBattleView()
        btnPotion.setOnClickListener { usePotion() }
        startNewEnemy()
        updateStatus()
        return root
    }

    private fun setupBattleView() {
        battleView.onBattleLog = { msg ->
            activity?.runOnUiThread {
                log.append(msg).append("\n")
                tvLog.text = log.toString()
                svLog.post { svLog.fullScroll(View.FOCUS_DOWN) }
            }
        }
        battleView.onStatsChanged = { activity?.runOnUiThread { updateStatus() } }
        battleView.onEnemyDefeated = { exp, gold ->
            activity?.runOnUiThread {
                appendLog("▶ +$exp EXP  +$gold Gold")
                GameState.save()
                updateStatus()
                handler.postDelayed({ loadNextEnemy() }, 1200)
            }
        }
        battleView.onPlayerDefeated = {
            activity?.runOnUiThread {
                appendLog("✦ Defeated! Recovering...")
                GameState.resetHpPartial()
                GameState.save()
                updateStatus()
                handler.postDelayed({ startNewEnemy() }, 2500)
            }
        }
        battleView.onLevelUp = {
            activity?.runOnUiThread {
                appendLog("★ ★ LEVEL UP! Now Lv.${GameState.heroLevel} ★ ★")
                appendLog("  HP: ${GameState.heroMaxHp}  ATK: ${GameState.heroAtk}  DEF: ${GameState.heroDef}")
            }
        }
    }

    private fun startNewEnemy() {
        val zone = GameState.currentZone()
        currentEnemyIdx = 0
        val enemy = zone.enemies[currentEnemyIdx]
        appendLog("=== ${zone.name} ===")
        appendLog("A wild ${enemy.name} appears!")
        battleView.startBattle(enemy)
        updateStatus()
    }

    private fun loadNextEnemy() {
        val zone = GameState.currentZone()
        currentEnemyIdx = (currentEnemyIdx + 1) % zone.enemies.size
        val enemy = zone.enemies[currentEnemyIdx]
        appendLog("--- ${enemy.name} appears! ---")
        battleView.startBattle(enemy)
        updateStatus()
    }

    private fun usePotion() {
        if (GameState.potions <= 0) { appendLog("No potions left!"); return }
        val heal = (GameState.heroMaxHp * 0.35).toInt()
        val actual = minOf(heal, GameState.heroMaxHp - GameState.heroHp)
        if (actual <= 0) { appendLog("HP is already full!"); return }
        GameState.heroHp += actual
        GameState.potions--
        appendLog("🧪 Potion! +$actual HP  (${GameState.potions} left)")
        GameState.save()
        updateStatus()
    }

    private fun appendLog(msg: String) {
        log.append(msg).append("\n")
        if (log.length > 2000) log.delete(0, 500)
        tvLog.text = log.toString()
        svLog.post { svLog.fullScroll(View.FOCUS_DOWN) }
    }

    private fun updateStatus() {
        val gs = GameState
        tvStatus.text = "Lv.${gs.heroLevel} ${gs.heroClass.displayName}   HP:${gs.heroHp}/${gs.heroMaxHp}   ATK:${gs.heroAtk}   DEF:${gs.heroDef}   Gold:${gs.gold}"
        btnPotion.text = "POTION (${gs.potions})"
        btnPotion.alpha = if (gs.potions > 0) 1f else 0.4f
        val zone = gs.currentZone()
        tvIdleInfo.text = "Zone: ${zone.name}  •  Idle: +${(gs.currentZoneIndex + 1) * 3} gold/10s  •  Kills: ${gs.totalKills}"
    }

    fun onResumeFromPager() { battleView.resumeBattle() }
    fun onPauseFromPager() { battleView.stopBattle() }

    override fun onResume() { super.onResume(); updateStatus() }
    override fun onPause() { super.onPause(); battleView.stopBattle(); GameState.save() }
    override fun onDestroyView() { super.onDestroyView(); handler.removeCallbacksAndMessages(null) }
}
