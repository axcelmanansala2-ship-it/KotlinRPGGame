package com.example.kotlinrpg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var tvZoneInfo: TextView
    private lateinit var btnExplore: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = root.findViewById(R.id.mapView)
        tvZoneInfo = root.findViewById(R.id.tvZoneInfo)
        btnExplore = root.findViewById(R.id.btnExplore)

        mapView.onZoneSelected = { zoneIdx ->
            updateZoneInfo(zoneIdx)
        }

        btnExplore.setOnClickListener {
            (activity as? GameActivity)?.navigateTo(1)
        }

        updateZoneInfo(GameState.currentZoneIndex)
        return root
    }

    private fun updateZoneInfo(idx: Int) {
        val zone = GameState.ZONES[idx]
        val unlocked = GameState.isZoneUnlocked(idx)
        if (unlocked) {
            tvZoneInfo.text = "${zone.name}\n${zone.enemies.size} enemy types  •  ${zone.description}"
            btnExplore.visibility = View.VISIBLE
            btnExplore.text = "[ ENTER ${zone.name.uppercase()} ]"
        } else {
            tvZoneInfo.text = "${zone.name} — LOCKED\nRequires Level ${zone.unlockLevel}  (You are Lv.${GameState.heroLevel})"
            btnExplore.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        updateZoneInfo(GameState.currentZoneIndex)
    }
}
