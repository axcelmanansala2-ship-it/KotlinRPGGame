package com.example.kotlinrpg

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.min

object GameState {
    private lateinit var prefs: SharedPreferences

    var heroClass: HeroClass = HeroClass.KNIGHT
    var heroLevel: Int = 1
    var heroMaxHp: Int = 130
    var heroHp: Int = 130
    var heroAtk: Int = 16
    var heroDef: Int = 10
    var heroExp: Int = 0
    var heroExpNext: Int = 100
    var gold: Long = 50L
    var potions: Int = 5
    var currentZoneIndex: Int = 0
    var totalKills: Int = 0
    var lastSaveTime: Long = 0L
    var isClassSelected: Boolean = false

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
        Zone("Dark Forest",   0xFF112200.toInt(), "Ancient cursed trees hide unknown horrors...", listOf(
            EnemyDef("Slime",       40,  10,  0,  30, 0),
            EnemyDef("Goblin",      65,  15,  2,  50, 1),
            EnemyDef("Bat",         55,  14,  1,  45, 5),
            EnemyDef("Forest Witch",90,  22,  3,  75, 6),
            EnemyDef("Troll",      130,  26,  7, 110, 1)
        ), unlockLevel = 1),
        Zone("Blood Marsh",   0xFF1A0000.toInt(), "The swamp reeks of blood and rot...", listOf(
            EnemyDef("Zombie",      80,  18,  3,  65, 2),
            EnemyDef("Werewolf",   145,  32,  8, 115, 7),
            EnemyDef("Marsh Hag",  120,  28,  5,  95, 6),
            EnemyDef("Blood Bat",   70,  20,  2,  60, 5),
            EnemyDef("Blood Lord", 190,  40, 11, 160, 3)
        ), unlockLevel = 5),
        Zone("Bone Plains",   0xFF1A1400.toInt(), "Fields of the fallen warriors...", listOf(
            EnemyDef("Skeleton",   120,  26,  6,  95, 2),
            EnemyDef("Revenant",   145,  33,  7, 118, 2),
            EnemyDef("Bone Archer",100,  30,  4,  80, 2),
            EnemyDef("Death Knight",175, 40, 14, 150, 3),
            EnemyDef("Lich",       210,  47, 11, 190, 6)
        ), unlockLevel = 10),
        Zone("Cursed Peaks",  0xFF0A0A1A.toInt(), "Mountains of eternal darkness...", listOf(
            EnemyDef("Gargoyle",   155,  36, 12, 135, 1),
            EnemyDef("Shadow Wolf",165,  39,  9, 145, 7),
            EnemyDef("Dark Mage",  135,  44,  5, 125, 6),
            EnemyDef("Dark Knight",230,  52, 19, 210, 3),
            EnemyDef("Storm Lich", 260,  57, 13, 230, 6)
        ), unlockLevel = 15),
        Zone("Dragon Lair",   0xFF1A0500.toInt(), "The dragon's eternal domain of fire...", listOf(
            EnemyDef("Drake",      200,  46, 14, 178, 4),
            EnemyDef("Fire Drake", 250,  54, 17, 215, 4),
            EnemyDef("Dragon",     340,  62, 21, 420, 4)
        ), unlockLevel = 20),
        Zone("Void Rift",     0xFF0A001A.toInt(), "The edge of reality tears apart...", listOf(
            EnemyDef("Void Shade", 260,  60, 19, 250, 3),
            EnemyDef("Void Titan", 320,  68, 23, 320, 3),
            EnemyDef("Void Lord",  450,  78, 26, 550, 4)
        ), unlockLevel = 25)
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences("rpg_save_v2", Context.MODE_PRIVATE)
        load()
    }

    fun setClass(cls: HeroClass) {
        heroClass = cls
        heroMaxHp = cls.baseHp; heroHp = cls.baseHp
        heroAtk = cls.baseAtk; heroDef = cls.baseDef
        heroLevel = 1; heroExp = 0; heroExpNext = 100
        gold = 50L; potions = 5
        currentZoneIndex = 0; totalKills = 0
        isClassSelected = true
        save()
    }

    fun save() {
        lastSaveTime = System.currentTimeMillis()
        prefs.edit()
            .putString("heroClass", heroClass.name)
            .putInt("heroLevel", heroLevel)
            .putInt("heroMaxHp", heroMaxHp)
            .putInt("heroHp", heroHp)
            .putInt("heroAtk", heroAtk)
            .putInt("heroDef", heroDef)
            .putInt("heroExp", heroExp)
            .putInt("heroExpNext", heroExpNext)
            .putLong("gold", gold)
            .putInt("potions", potions)
            .putInt("currentZone", currentZoneIndex)
            .putInt("totalKills", totalKills)
            .putLong("lastSaveTime", lastSaveTime)
            .putBoolean("isClassSelected", isClassSelected)
            .apply()
    }

    fun load() {
        heroClass = try { HeroClass.valueOf(prefs.getString("heroClass", "KNIGHT")!!) } catch (e: Exception) { HeroClass.KNIGHT }
        heroLevel = prefs.getInt("heroLevel", 1)
        heroMaxHp = prefs.getInt("heroMaxHp", heroClass.baseHp)
        heroHp = prefs.getInt("heroHp", heroClass.baseHp)
        heroAtk = prefs.getInt("heroAtk", heroClass.baseAtk)
        heroDef = prefs.getInt("heroDef", heroClass.baseDef)
        heroExp = prefs.getInt("heroExp", 0)
        heroExpNext = prefs.getInt("heroExpNext", 100)
        gold = prefs.getLong("gold", 50L)
        potions = prefs.getInt("potions", 5)
        currentZoneIndex = prefs.getInt("currentZone", 0)
        totalKills = prefs.getInt("totalKills", 0)
        lastSaveTime = prefs.getLong("lastSaveTime", System.currentTimeMillis())
        isClassSelected = prefs.getBoolean("isClassSelected", false)
    }

    fun processIdleTime() {
        if (!isClassSelected) return
        val now = System.currentTimeMillis()
        val elapsed = (now - lastSaveTime) / 1000L
        if (elapsed > 30) {
            val baseGold = (currentZoneIndex + 1) * 3L
            val ticks = elapsed / 10L
            val idleGold = ticks * baseGold
            val cappedGold = min(idleGold, baseGold * 720L)
            gold += cappedGold
            val baseExp = (currentZoneIndex + 1) * 2
            val idleExp = (ticks * baseExp).toInt()
            heroExp += idleExp
            checkLevelUp()
            lastSaveTime = now
            save()
        }
    }

    fun gainExpAndGold(exp: Int, gld: Int): Boolean {
        heroExp += exp
        gold += gld
        totalKills++
        return checkLevelUp()
    }

    fun checkLevelUp(): Boolean {
        var leveled = false
        while (heroExp >= heroExpNext) {
            heroLevel++
            heroExp -= heroExpNext
            heroExpNext = (heroExpNext * 1.5).toInt()
            heroMaxHp += 15 + heroLevel
            heroHp = min(heroHp + 25, heroMaxHp)
            heroAtk += 3 + heroLevel / 4
            heroDef += 1 + heroLevel / 6
            leveled = true
        }
        return leveled
    }

    fun isZoneUnlocked(idx: Int): Boolean = heroLevel >= ZONES[idx].unlockLevel

    fun currentZone(): Zone = ZONES[currentZoneIndex]

    fun resetHpPartial() { heroHp = heroMaxHp / 3 }
}
