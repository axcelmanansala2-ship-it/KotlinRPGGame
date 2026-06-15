package com.example.kotlinrpg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class MapFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_map, container, false)
        val mapView  = root.findViewById<MapView>(R.id.mapView)
        val tvInfo   = root.findViewById<TextView>(R.id.tvZoneInfo)
        val btnEnter = root.findViewById<TextView>(R.id.btnExplore)

        mapView.onZoneSelected = { idx ->
            updateInfo(tvInfo, btnEnter, idx)
        }
        btnEnter.setOnClickListener {
            (activity as? GameActivity)?.navigateTo(1)
        }
        updateInfo(tvInfo, btnEnter, GameState.currentZoneIndex)
        return root
    }

    private fun updateInfo(tvInfo: TextView, btn: TextView, idx: Int) {
        val zone = GameState.ZONES[idx]
        val unlocked = GameState.isZoneUnlocked(idx)
        if (unlocked) {
            val idleGold = (idx + 1) * 4
            tvInfo.text = "${zone.name}\n${zone.description}\n${zone.enemies.size} enemy types  •  +${idleGold} gold/10s idle"
            btn.visibility = View.VISIBLE
            btn.text = "[ ENTER ${zone.name.uppercase()} ]"
        } else {
            tvInfo.text = "${zone.name} — LOCKED (Requires Player Level ${zone.unlockLevel})"
            btn.visibility = View.GONE
        }
    }

    override fun onResume() { super.onResume(); view?.let { updateInfo(it.findViewById(R.id.tvZoneInfo), it.findViewById(R.id.btnExplore), GameState.currentZoneIndex) } }
}
