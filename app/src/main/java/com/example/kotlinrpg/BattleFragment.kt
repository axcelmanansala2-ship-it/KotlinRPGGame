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
    private lateinit var tvWave: TextView
    private lateinit var btnPotion: TextView
    private val log = StringBuilder()
    private var waveIndex = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_battle, container, false)
        battleView = root.findViewById(R.id.battleView)
        tvLog      = root.findViewById(R.id.tvLog)
        svLog      = root.findViewById(R.id.svLog)
        tvStatus   = root.findViewById(R.id.tvStatus)
        tvWave     = root.findViewById(R.id.tvWave)
        btnPotion  = root.findViewById(R.id.btnPotion)

        setupBattleCallbacks()
        btnPotion.setOnClickListener { usePotion() }

        battleView.post {
            battleView.loadParty()
            startWave()
        }
        updateStatus()
        return root
    }

    private fun setupBattleCallbacks() {
        battleView.onBattleLog = { msg ->
            activity?.runOnUiThread { appendLog(msg) }
        }
        battleView.onStatsChanged = {
            activity?.runOnUiThread { updateStatus() }
        }
        battleView.onWaveCleared = { _, _ ->
            activity?.runOnUiThread {
                handler.postDelayed({ nextWave() }, 600)
            }
        }
        battleView.onAllDefeated = {
            activity?.runOnUiThread {
                appendLog("☠ Party defeated! Recovering...")
                handler.postDelayed({
                    battleView.loadParty()
                    waveIndex = 0
                    startWave()
                    updateStatus()
                }, 2500)
            }
        }
    }

    private fun startWave() {
        val zone = GameState.currentZone()
        val enemies = zone.enemies
        if (enemies.isEmpty()) return
        val waveEnemies = buildList {
            val e1 = enemies[waveIndex % enemies.size]
            add(e1)
            if (enemies.size > 1) add(enemies[(waveIndex + 1) % enemies.size])
            if (enemies.size > 2) add(enemies[(waveIndex + 2) % enemies.size])
        }
        battleView.loadEnemyWave(waveEnemies)
        appendLog("--- Wave ${waveIndex + 1}: ${zone.name} ---")
        waveEnemies.forEach { appendLog("  ▸ ${it.name} (HP:${it.maxHp})") }
        updateWaveInfo()
    }

    private fun nextWave() {
        waveIndex++
        battleView.restoreHeroHp()
        startWave()
        updateStatus()
    }

    private fun usePotion() {
        if (GameState.potions <= 0) { appendLog("No potions!"); return }
        GameState.potions--
        battleView.loadParty()
        appendLog("🧪 Potions used! Party healed! (${GameState.potions} left)")
        GameState.save()
        updateStatus()
    }

    private fun appendLog(msg: String) {
        log.append(msg).append("\n")
        if (log.length > 2000) log.delete(0, 600)
        tvLog.text = log.toString()
        svLog.post { svLog.fullScroll(View.FOCUS_DOWN) }
    }

    private fun updateStatus() {
        val zone = GameState.currentZone()
        tvStatus.text = "Zone: ${zone.name}  •  Kills: ${GameState.totalKills}  •  Gold: ${GameState.gold}  •  Lv.${GameState.playerLevel}"
        btnPotion.text = "POTION (${GameState.potions})"
        btnPotion.alpha = if (GameState.potions > 0) 1f else 0.4f
    }

    private fun updateWaveInfo() {
        tvWave.text = "Wave ${waveIndex + 1}  •  ${GameState.currentZone().name}"
    }

    fun onResumeFromPager() { /* battle auto-runs via handler in BattleView */ }
    fun onPauseFromPager() { /* nothing, idle continues */ }

    override fun onResume() { super.onResume(); updateStatus() }
    override fun onPause() { super.onPause(); GameState.save() }
    override fun onDestroyView() { super.onDestroyView(); handler.removeCallbacksAndMessages(null) }
}
