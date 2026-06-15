package com.example.kotlinrpg

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameState.init(this)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.btnPlay).setOnClickListener {
            if (GameState.isClassSelected) {
                startActivity(Intent(this, GameActivity::class.java))
            } else {
                startActivity(Intent(this, CharacterSelectActivity::class.java))
            }
            finish()
        }

        findViewById<TextView>(R.id.btnNewGame).setOnClickListener {
            startActivity(Intent(this, CharacterSelectActivity::class.java))
            finish()
        }
    }
}
