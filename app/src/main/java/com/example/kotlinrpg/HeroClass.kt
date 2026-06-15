package com.example.kotlinrpg

enum class HeroClass(
    val displayName: String,
    val description: String,
    val baseHp: Int,
    val baseAtk: Int,
    val baseDef: Int,
    val critChance: Float,
    val attackLabel: String,
    val skillLabel: String
) {
    KNIGHT("Knight", "Iron will & heavy armor\nHigh DEF, balanced ATK", 130, 16, 10, 0.15f, "SLASH", "BLOCK"),
    MAGE(  "Mage",   "Arcane destruction\nHigh ATK magic, fragile",     85, 26,  3, 0.12f, "CAST",  "CHANNEL"),
    ARCHER("Archer", "Swift & precise\nHigh CRIT, ranged attacks",      100, 20,  6, 0.28f, "SHOOT", "EVADE")
}
