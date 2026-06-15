package com.example.kotlinrpg

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CharacterSelectActivity : AppCompatActivity() {

    private var selected = HeroClass.KNIGHT
    private lateinit var cards: List<TextView>
    private lateinit var tvDesc: TextView
    private lateinit var btnStart: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_character_select)

        tvDesc = findViewById(R.id.tvClassDesc)
        btnStart = findViewById(R.id.btnStartAdventure)
        cards = listOf(
            findViewById(R.id.cardKnight),
            findViewById(R.id.cardMage),
            findViewById(R.id.cardArcher)
        )

        HeroClass.entries.forEachIndexed { i, cls ->
            cards[i].setOnClickListener {
                selected = cls
                updateSelection()
            }
        }

        btnStart.setOnClickListener {
            GameState.setClass(selected)
            startActivity(Intent(this, GameActivity::class.java))
            finish()
        }

        updateSelection()
    }

    private fun updateSelection() {
        HeroClass.entries.forEachIndexed { i, cls ->
            val isSelected = cls == selected
            cards[i].setBackgroundColor(if (isSelected) 0xFF1A1A3A.toInt() else 0xFF0D0D18.toInt())
            cards[i].setTextColor(if (isSelected) 0xFFFFDD00.toInt() else 0xFF888899.toInt())
        }
        tvDesc.text = "${selected.displayName}\n\n${selected.description}\n\n" +
            "HP:  ${selected.baseHp}\n" +
            "ATK: ${selected.baseAtk}\n" +
            "DEF: ${selected.baseDef}\n" +
            "CRIT: ${(selected.critChance * 100).toInt()}%\n\n" +
            "Attack: ${selected.attackLabel}\n" +
            "Skill:  ${selected.skillLabel}"
    }
}
