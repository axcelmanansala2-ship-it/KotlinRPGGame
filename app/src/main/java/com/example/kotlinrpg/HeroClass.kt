package com.example.kotlinrpg

enum class HeroClass(
    val displayName: String,
    val description: String,
    val baseHp: Int,
    val baseAtk: Int,
    val baseDef: Int,
    val critChance: Float,
    val atkSpeed: Int,   // frames between attacks (lower = faster)
    val spriteColor: Int // orb/projectile color
) {
    KNIGHT  ("Knight",   "Tank\nHigh DEF + HP",         130, 16, 10, 0.15f, 28, 0xFF5577FF.toInt()),
    MAGE    ("Mage",     "AOE Burst\nHigh ATK, fragile",  85, 28,  3, 0.12f, 36, 0xFFBB44FF.toInt()),
    ARCHER  ("Archer",   "Fast DPS\nHigh CRIT",          100, 20,  6, 0.30f, 20, 0xFF44FF88.toInt()),
    ASSASSIN("Assassin", "Glass Cannon\nMax CRIT, slow",   75, 32,  2, 0.45f, 34, 0xFFFF4488.toInt()),
    PRIEST  ("Priest",   "Support\nHeals party/tick",    110, 12,  7, 0.10f, 40, 0xFFFFEE44.toInt())
}
