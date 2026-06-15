package com.example.kotlinrpg

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class BattleActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var tvHeroHp: TextView
    private lateinit var tvEnemyHp: TextView
    private lateinit var tvLog: TextView
    private lateinit var svLog: ScrollView
    private lateinit var btnAttack: Button
    private lateinit var btnDefend: Button
    private lateinit var btnHeal: Button
    private val log = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)

        gameView  = findViewById(R.id.gameView)
        tvHeroHp  = findViewById(R.id.tvHeroHp)
        tvEnemyHp = findViewById(R.id.tvEnemyHp)
        tvLog     = findViewById(R.id.tvLog)
        svLog     = findViewById(R.id.svLog)
        btnAttack = findViewById(R.id.btnAttack)
        btnDefend = findViewById(R.id.btnDefend)
        btnHeal   = findViewById(R.id.btnHeal)

        gameView.onLog = { msg ->
            runOnUiThread {
                log.append(msg).append("\n")
                tvLog.text = log.toString()
                svLog.post { svLog.fullScroll(View.FOCUS_DOWN) }
            }
        }

        gameView.onStatsChanged = { heroHp, enemyHp, potions ->
            runOnUiThread {
                tvHeroHp.text  = "HERO  HP: $heroHp / ${gameView.playerMaxHp}   Lv.${gameView.playerLevel}"
                val en = gameView.currentEnemy
                tvEnemyHp.text = "${en?.name ?: "Enemy"}  HP: $enemyHp / ${gameView.enemyMaxHp}"
                btnHeal.text   = "HEAL (x$potions)"
            }
        }

        gameView.onStageClear = { _, _ ->
            runOnUiThread {
                setButtons(false)
                val next = gameView.currentStage + 1
                if (next >= gameView.STAGES.size) showVictory()
                else showStageClear(next)
            }
        }

        gameView.onGameOver = {
            runOnUiThread { setButtons(false); showGameOver() }
        }

        btnAttack.setOnClickListener {
            if (gameView.phase == GameView.Phase.PLAYER_TURN) {
                setButtons(false)
                gameView.playerAttack()
                gameView.postDelayed({ enableIfPlayerTurn() }, 1200)
            }
        }
        btnDefend.setOnClickListener {
            if (gameView.phase == GameView.Phase.PLAYER_TURN) {
                setButtons(false)
                gameView.playerDefend()
                gameView.postDelayed({ enableIfPlayerTurn() }, 1200)
            }
        }
        btnHeal.setOnClickListener {
            if (gameView.phase == GameView.Phase.PLAYER_TURN) {
                setButtons(false)
                gameView.playerHeal()
                gameView.postDelayed({ enableIfPlayerTurn() }, 1200)
            }
        }

        log.append("=== STAGE 1 ===\n")
        log.append("A wild Slime appears!\n")
        tvLog.text = log.toString()
        gameView.startGame()
    }

    private fun enableIfPlayerTurn() {
        if (gameView.phase == GameView.Phase.PLAYER_TURN) setButtons(true)
        else gameView.postDelayed({ enableIfPlayerTurn() }, 200)
    }

    private fun setButtons(on: Boolean) {
        btnAttack.isEnabled = on
        btnDefend.isEnabled = on
        btnHeal.isEnabled   = on && gameView.playerPotions > 0
    }

    private fun showStageClear(next: Int) {
        val en = gameView.STAGES[next]
        AlertDialog.Builder(this)
            .setTitle("⭐ STAGE ${gameView.currentStage + 1} CLEAR!")
            .setMessage(
                "Enemy defeated!\n\n" +
                "Lv.${gameView.playerLevel}  " +
                "HP:${gameView.playerHp}/${gameView.playerMaxHp}\n" +
                "ATK:${gameView.playerAtk}  DEF:${gameView.playerDef}\n\n" +
                "Next: ${en.name}"
            )
            .setPositiveButton("Next Battle") { _, _ ->
                log.clear()
                log.append("=== STAGE ${next + 1} ===\n${en.name} appears!\n")
                tvLog.text = log.toString()
                gameView.loadStage(next)
                gameView.postDelayed({ enableIfPlayerTurn() }, 300)
            }
            .setCancelable(false).show()
    }

    private fun showVictory() {
        AlertDialog.Builder(this)
            .setTitle("🏆 VICTORY!")
            .setMessage("You defeated ALL enemies!\nYou are a true hero of legend!\n\nFinal Level: ${gameView.playerLevel}")
            .setPositiveButton("Play Again") { _, _ -> finish() }
            .setCancelable(false).show()
    }

    private fun showGameOver() {
        AlertDialog.Builder(this)
            .setTitle("💀 GAME OVER")
            .setMessage("Defeated by ${gameView.currentEnemy?.name}...\n\nLevel ${gameView.playerLevel} Hero\nStage ${gameView.currentStage + 1} / ${gameView.STAGES.size}")
            .setPositiveButton("Retry") { _, _ -> finish() }
            .setCancelable(false).show()
    }
}
