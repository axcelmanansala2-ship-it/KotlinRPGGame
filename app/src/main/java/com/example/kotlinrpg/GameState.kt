package com.example.kotlinrpg

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.min

object GameState {
    private lateinit var prefs: SharedPreferences

    // ─── Party System ───────────────────────────────────────────────
    data class PartyHero(
        val heroClass: HeroClass,
        var level: Int = 1,
        var bonusAtk: Int = 0,
        var bonusDef: Int = 0,
        var bonusHp: Int = 0
    ) {
        val maxHp: Int get() = heroClass.baseHp + (level - 1) * 12 + bonusHp
        val atk: Int    get() = heroClass.baseAtk + (level - 1) * 3 + bonusAtk
        val def: Int    get() = heroClass.baseDef + (level - 1) + bonusDef
        val critChance: Float get() = heroClass.critChance
        val atkSpeed: Int get() = heroClass.atkSpeed
    }

    var party: MutableList<PartyHero> = mutableListOf(
        PartyHero(HeroClass.KNIGHT),
        PartyHero(HeroClass.MAGE),
        PartyHero(HeroClass.ARCHER)
    )
    var unlockedClasses: MutableSet<HeroClass> = mutableSetOf(
        HeroClass.KNIGHT, HeroClass.MAGE, HeroClass.ARCHER
    )

    // ─── Player Stats ────────────────────────────────────────────────
    var playerLevel: Int = 1
    var gold: Long = 100L
    var potions: Int = 5
    var currentZoneIndex: Int = 0
    var totalKills: Int = 0
    var lastSaveTime: Long = 0L

    // ─── World Zones ─────────────────────────────────────────────────
    data class EnemyDef(
        val name: String, val maxHp: Int, val attack: Int,
        val defense: Int, val expReward: Int, val spriteId: Int
    ) {
        val goldReward: Int get() = expReward / 3 + 5
    }

    data class Zone(
        val name: String, val envColor: Int, val description: String,
        val enemies: List<EnemyDef>, val unlockLevel: Int = 1
    )

    val ZONES: List<Zone> = listOf(
        Zone("Dark Forest",   0xFF112200.toInt(), "Ancient cursed trees...", listOf(
            EnemyDef("Slime",       40,  10,  0,  30, 0),
            EnemyDef("Goblin",      65,  15,  2,  50, 1),
            EnemyDef("Bat",         55,  14,  1,  45, 5),
            EnemyDef("Forest Witch",90,  22,  3,  75, 6),
            EnemyDef("Troll",      130,  26,  7, 110, 1)
        ), unlockLevel = 1),
        Zone("Blood Marsh",   0xFF1A0000.toInt(), "Swamps of blood and rot...", listOf(
            EnemyDef("Zombie",      80,  18,  3,  65, 2),
            EnemyDef("Werewolf",   145,  32,  8, 115, 7),
            EnemyDef("Marsh Hag",  120,  28,  5,  95, 6),
            EnemyDef("Blood Bat",   70,  20,  2,  60, 5),
            EnemyDef("Blood Lord", 190,  40, 11, 160, 3)
        ), unlockLevel = 5),
        Zone("Bone Plains",   0xFF1A1400.toInt(), "Fields of the fallen...", listOf(
            EnemyDef("Skeleton",   120,  26,  6,  95, 2),
            EnemyDef("Revenant",   145,  33,  7, 118, 2),
            EnemyDef("Bone Archer",100,  30,  4,  80, 2),
            EnemyDef("Death Knight",175, 40, 14, 150, 3),
            EnemyDef("Lich",       210,  47, 11, 190, 6)
        ), unlockLevel = 10),
        Zone("Cursed Peaks",  0xFF0A0A1A.toInt(), "Mountains of darkness...", listOf(
            EnemyDef("Gargoyle",   155,  36, 12, 135, 1),
            EnemyDef("Shadow Wolf",165,  39,  9, 145, 7),
            EnemyDef("Dark Mage",  135,  44,  5, 125, 6),
            EnemyDef("Dark Knight",230,  52, 19, 210, 3),
            EnemyDef("Storm Lich", 260,  57, 13, 230, 6)
        ), unlockLevel = 15),
        Zone("Dragon Lair",   0xFF1A0500.toInt(), "The dragon's domain...", listOf(
            EnemyDef("Drake",      200,  46, 14, 178, 4),
            EnemyDef("Fire Drake", 250,  54, 17, 215, 4),
            EnemyDef("Dragon",     340,  62, 21, 420, 4)
        ), unlockLevel = 20),
        Zone("Void Rift",     0xFF0A001A.toInt(), "Edge of reality...", listOf(
            EnemyDef("Void Shade", 260,  60, 19, 250, 3),
            EnemyDef("Void Titan", 320,  68, 23, 320, 3),
            EnemyDef("Void Lord",  450,  78, 26, 550, 4)
        ), unlockLevel = 25)
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences("rpg_save_v3", Context.MODE_PRIVATE)
        load()
    }

    fun save() {
        lastSaveTime = System.currentTimeMillis()
        val ed = prefs.edit()
        ed.putInt("playerLevel", playerLevel)
        ed.putLong("gold", gold)
        ed.putInt("potions", potions)
        ed.putInt("currentZone", currentZoneIndex)
        ed.putInt("totalKills", totalKills)
        ed.putLong("lastSaveTime", lastSaveTime)
        // Save party
        ed.putInt("partySize", party.size)
        party.forEachIndexed { i, h ->
            ed.putString("p${i}class", h.heroClass.name)
            ed.putInt("p${i}level", h.level)
            ed.putInt("p${i}batk", h.bonusAtk)
            ed.putInt("p${i}bdef", h.bonusDef)
            ed.putInt("p${i}bhp", h.bonusHp)
        }
        // Save unlocked classes
        ed.putString("unlocked", unlockedClasses.joinToString(",") { it.name })
        ed.apply()
    }

    fun load() {
        playerLevel = prefs.getInt("playerLevel", 1)
        gold = prefs.getLong("gold", 100L)
        potions = prefs.getInt("potions", 5)
        currentZoneIndex = prefs.getInt("currentZone", 0)
        totalKills = prefs.getInt("totalKills", 0)
        lastSaveTime = prefs.getLong("lastSaveTime", System.currentTimeMillis())
        val partySize = prefs.getInt("partySize", -1)
        if (partySize > 0) {
            party.clear()
            for (i in 0 until partySize) {
                try {
                    val cls = HeroClass.valueOf(prefs.getString("p${i}class", "KNIGHT")!!)
                    party.add(PartyHero(cls,
                        prefs.getInt("p${i}level", 1),
                        prefs.getInt("p${i}batk", 0),
                        prefs.getInt("p${i}bdef", 0),
                        prefs.getInt("p${i}bhp", 0)))
                } catch (_: Exception) {}
            }
        }
        val unlStr = prefs.getString("unlocked", "KNIGHT,MAGE,ARCHER") ?: "KNIGHT,MAGE,ARCHER"
        unlockedClasses = unlStr.split(",").mapNotNull { n -> try { HeroClass.valueOf(n) } catch (_: Exception) { null } }.toMutableSet()
    }

    fun processIdleTime() {
        val now = System.currentTimeMillis()
        val elapsed = (now - lastSaveTime) / 1000L
        if (elapsed > 30) {
            val baseGold = (currentZoneIndex + 1) * 4L
            val ticks = elapsed / 10L
            gold += min(ticks * baseGold, baseGold * 720L)
            lastSaveTime = now
            save()
        }
    }

    fun gainExpAndGold(exp: Int, gld: Int) {
        gold += gld; totalKills++
        val playerExpNeeded = playerLevel * 80
        if (exp >= playerExpNeeded / 10) playerLevel++
        save()
    }

    fun isZoneUnlocked(idx: Int): Boolean = playerLevel >= ZONES[idx].unlockLevel
    fun currentZone(): Zone = ZONES[currentZoneIndex]

    fun heroUpgradeCost(heroIdx: Int, type: String): Long {
        val h = party.getOrNull(heroIdx) ?: return 9999L
        return when (type) {
            "atk" -> (h.heroClass.baseAtk + h.bonusAtk) * 9L
            "def" -> (h.heroClass.baseDef + h.bonusDef) * 12L + 25L
            "hp"  -> (h.heroClass.baseHp + h.bonusHp) / 8L * 5L
            "lvl" -> h.level * 60L
            else  -> 9999L
        }
    }
}
