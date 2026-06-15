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
            startActivity(Intent(this, GameActivity::class.java)); finish()
        }
        findViewById<TextView>(R.id.btnNewGame).setOnClickListener {
            getSharedPreferences("rpg_save_v3", MODE_PRIVATE).edit().clear().apply()
            GameState.init(this)
            startActivity(Intent(this, GameActivity::class.java)); finish()
        }
    }
}
