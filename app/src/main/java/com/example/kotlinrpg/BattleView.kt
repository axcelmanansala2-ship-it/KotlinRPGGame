package com.example.kotlinrpg

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class BattleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, def: Int = 0
) : View(context, attrs, def) {

    // ═══════════════════ DATA CLASSES ═══════════════════════════════
    data class BUnit(
        val id: Int, val isHero: Boolean,
        val heroClass: HeroClass? = null,
        val enemyDef: GameState.EnemyDef? = null,
        val slot: Int,
        var maxHp: Int, var hp: Int, var atk: Int, var def: Int,
        var critChance: Float = 0.15f, var atkSpeed: Int = 28,
        var isAlive: Boolean = true, var flashTimer: Int = 0,
        var atkTimer: Int = 0, var isDying: Boolean = false, var dyingAlpha: Float = 1f
    )
    data class Proj(
        val isHeroAtk: Boolean, val fromSlot: Int, val toSlot: Int,
        var x: Float, var y: Float, val tx: Float, val ty: Float,
        val color: Int, val damage: Int, val isCrit: Boolean,
        var progress: Float = 0f, var done: Boolean = false
    )
    data class FloatNum(val text: String, var x: Float, var y: Float, var life: Int, val color: Int, val isCrit: Boolean)

    // ═══════════════════ STATE ══════════════════════════════════════
    private val heroes  = mutableListOf<BUnit>()
    private val enemies = mutableListOf<BUnit>()
    private val projs   = mutableListOf<Proj>()
    private val floats  = mutableListOf<FloatNum>()
    private var tick    = 0L
    private var waveDone = false
    private var waveDelay = 0

    var onBattleLog: ((String) -> Unit)? = null
    var onWaveCleared: ((Int, Int) -> Unit)? = null   // exp, gold
    var onAllDefeated: (() -> Unit)? = null
    var onStatsChanged: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())

    // Hero formation slots (fraction of w, h)
    private val hSlots = arrayOf(0.14f to 0.30f, 0.14f to 0.52f, 0.14f to 0.72f)
    // Enemy formation slots
    private val eSlots = arrayOf(0.72f to 0.30f, 0.72f to 0.52f, 0.72f to 0.72f)

    private val frameRunner = object : Runnable {
        override fun run() {
            tick++
            updateProjectiles()
            updateDeaths()
            updateAttackTimers()
            floats.forEach { it.y -= 1.8f; it.life-- }
            floats.removeAll { it.life <= 0 }
            if (waveDelay > 0) { waveDelay--; if (waveDelay == 0) onWaveCleared?.invoke(0, 0) }
            invalidate()
            handler.postDelayed(this, 50)
        }
    }

    init { post { handler.post(frameRunner) } }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); handler.removeCallbacksAndMessages(null) }

    // ═══════════════════ PUBLIC API ═════════════════════════════════
    fun loadParty() {
        heroes.clear()
        GameState.party.forEachIndexed { i, ph ->
            heroes.add(BUnit(i, true, heroClass = ph.heroClass, slot = i,
                maxHp = ph.maxHp, hp = ph.maxHp, atk = ph.atk, def = ph.def,
                critChance = ph.critChance, atkSpeed = ph.atkSpeed,
                atkTimer = (i * 8) % ph.atkSpeed))
        }
    }

    fun loadEnemyWave(wave: List<GameState.EnemyDef>) {
        enemies.clear()
        wave.forEachIndexed { i, ed ->
            enemies.add(BUnit(i + 100, false, enemyDef = ed, slot = i,
                maxHp = ed.maxHp, hp = ed.maxHp, atk = ed.attack, def = ed.defense,
                atkSpeed = 32, atkTimer = (i * 10)))
        }
        waveDone = false
    }

    fun restoreHeroHp() { heroes.forEach { it.hp = min(it.hp + it.maxHp / 4, it.maxHp) } }

    private fun updateAttackTimers() {
        if (waveDone) return
        val aliveEnemies = enemies.filter { it.isAlive && !it.isDying }
        val aliveHeroes  = heroes.filter { it.isAlive && !it.isDying }
        if (aliveEnemies.isEmpty() || aliveHeroes.isEmpty()) return

        heroes.filter { it.isAlive && !it.isDying }.forEach { h ->
            h.atkTimer++
            if (h.atkTimer >= h.atkSpeed) {
                h.atkTimer = 0
                val target = aliveEnemies.minByOrNull { it.hp }!!
                fireAtk(h, target)
            }
        }
        enemies.filter { it.isAlive && !it.isDying }.forEach { e ->
            e.atkTimer++
            if (e.atkTimer >= e.atkSpeed) {
                e.atkTimer = 0
                val target = aliveHeroes.minByOrNull { it.hp } ?: return@forEach
                fireEnemyAtk(e, target)
            }
        }

        // Priest heals allies every 60 ticks
        if (tick % 60 == 0L) {
            heroes.filter { it.heroClass == HeroClass.PRIEST && it.isAlive }.forEach { _ ->
                heroes.filter { it.isAlive }.forEach { h ->
                    val heal = (h.maxHp * 0.06).toInt()
                    h.hp = min(h.hp + heal, h.maxHp)
                    addFloat("+$heal", slotX(true, h.slot), slotY(true, h.slot) - 20f, 0xFF88FFCC.toInt(), false)
                }
            }
        }
    }

    private fun fireAtk(hero: BUnit, target: BUnit) {
        val fromX = slotX(true, hero.slot) + unitW(true) / 2
        val fromY = slotY(true, hero.slot) + unitH(true) / 2
        val toX   = slotX(false, target.slot) + unitW(false) / 2
        val toY   = slotY(false, target.slot) + unitH(false) / 2
        val crit  = Random.nextFloat() < hero.critChance
        val raw   = maxOf(1, hero.atk - target.def + Random.nextInt(-2, 3))
        val dmg   = if (crit) (raw * 2.0).toInt() else raw
        val col   = hero.heroClass?.spriteColor ?: 0xFFFFFFFF.toInt()
        projs.add(Proj(true, hero.slot, target.slot, fromX, fromY, toX, toY, col, dmg, crit))
    }

    private fun fireEnemyAtk(enemy: BUnit, target: BUnit) {
        val fromX = slotX(false, enemy.slot) + unitW(false) / 2
        val fromY = slotY(false, enemy.slot) + unitH(false) / 2
        val toX   = slotX(true, target.slot) + unitW(true) / 2
        val toY   = slotY(true, target.slot) + unitH(true) / 2
        val raw   = maxOf(1, enemy.atk - target.def + Random.nextInt(-2, 3))
        projs.add(Proj(false, enemy.slot, target.slot, fromX, fromY, toX, toY, 0xFFFF4433.toInt(), raw, false))
    }

    private fun updateProjectiles() {
        projs.forEach { p ->
            if (p.done) return@forEach
            p.progress += 0.07f
            val t = p.progress.coerceIn(0f, 1f)
            p.x = p.x + (p.tx - p.x) * 0.07f
            p.y = p.y + (p.ty - p.y) * 0.07f
            if (t >= 1f || dist(p.x, p.y, p.tx, p.ty) < 12f) {
                p.done = true
                if (p.isHeroAtk) {
                    val target = enemies.find { it.slot == p.toSlot && it.isAlive } ?: return@forEach
                    target.hp = maxOf(0, target.hp - p.damage)
                    target.flashTimer = 6
                    addFloat(if (p.isCrit) "★${p.damage}" else "${p.damage}", p.tx, p.ty - 10f, if (p.isCrit) 0xFFFFDD00.toInt() else 0xFFFF6644.toInt(), p.isCrit)
                    onBattleLog?.invoke(if (p.isCrit) "CRIT! ${p.damage} dmg!" else "${p.damage} dmg!")
                    if (target.hp <= 0) { target.isDying = true; target.isAlive = false }
                    checkWaveCleared()
                } else {
                    val target = heroes.find { it.slot == p.toSlot && it.isAlive } ?: return@forEach
                    target.hp = maxOf(0, target.hp - p.damage)
                    target.flashTimer = 6
                    addFloat("-${p.damage}", p.tx, p.ty - 10f, 0xFFFF2222.toInt(), false)
                    onStatsChanged?.invoke()
                    if (target.hp <= 0) { target.isDying = true; target.isAlive = false }
                    if (heroes.none { it.isAlive && !it.isDying }) onAllDefeated?.invoke()
                }
            }
        }
        projs.removeAll { it.done }
    }

    private fun updateDeaths() {
        enemies.filter { it.isDying && it.dyingAlpha > 0 }.forEach { it.dyingAlpha -= 0.03f }
        enemies.removeAll { it.isDying && it.dyingAlpha <= 0 }
    }

    private fun checkWaveCleared() {
        if (enemies.none { it.isAlive } && !waveDone) {
            waveDone = true
            val totalExp = enemies.sumOf { it.enemyDef?.expReward ?: 0 }
            val totalGold = enemies.sumOf { it.enemyDef?.goldReward ?: 0 }
            onBattleLog?.invoke("Wave cleared! +$totalExp EXP  +$totalGold Gold")
            GameState.gainExpAndGold(totalExp, totalGold)
            waveDelay = 40
        }
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float) = sqrt((x2-x1).pow(2) + (y2-y1).pow(2))
    private fun addFloat(t: String, x: Float, y: Float, c: Int, crit: Boolean) = floats.add(FloatNum(t, x, y, 28, c, crit))
    private fun slotX(hero: Boolean, slot: Int): Float { val s = if (hero) hSlots[slot.coerceIn(0,2)] else eSlots[slot.coerceIn(0,2)]; return s.first * width }
    private fun slotY(hero: Boolean, slot: Int): Float { val s = if (hero) hSlots[slot.coerceIn(0,2)] else eSlots[slot.coerceIn(0,2)]; return s.second * height * 0.88f }
    private fun unitW(hero: Boolean) = width * (if (hero) 0.18f else 0.22f)
    private fun unitH(hero: Boolean) = height * (if (hero) 0.20f else 0.20f)

    // ═══════════════════ DRAWING ════════════════════════════════════
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        drawBackground(canvas, w, h)
        drawDivider(canvas, w, h)
        enemies.forEach { drawEnemyUnit(canvas, it, w, h) }
        heroes.forEach  { drawHeroUnit(canvas, it, w, h) }
        drawProjectiles(canvas)
        drawFloatNums(canvas)
    }

    private fun drawBackground(canvas: Canvas, w: Float, h: Float) {
        val bh = h * 0.82f
        // Sky gradient - deep dark blue/purple
        val sky = LinearGradient(0f, 0f, 0f, bh, 0xFF040410.toInt(), 0xFF0E0822.toInt(), Shader.TileMode.CLAMP)
        paint.shader = sky; canvas.drawRect(0f, 0f, w, bh, paint); paint.shader = null
        // Stars
        paint.color = 0x99FFFFFF.toInt()
        val stars = listOf(0.05f to 0.04f, 0.14f to 0.09f, 0.23f to 0.03f, 0.38f to 0.07f, 0.54f to 0.04f,
            0.67f to 0.10f, 0.80f to 0.03f, 0.93f to 0.08f, 0.08f to 0.18f, 0.46f to 0.15f,
            0.73f to 0.20f, 0.88f to 0.13f, 0.29f to 0.24f, 0.60f to 0.22f, 0.96f to 0.21f)
        val sp = w / 280f
        stars.forEach { (sx, sy) -> canvas.drawCircle(sx * w, sy * bh, sp * 1.5f, paint) }
        // Blood moon
        paint.color = 0xFFBB1100.toInt(); canvas.drawCircle(w * 0.85f, bh * 0.13f, w * 0.072f, paint)
        paint.color = 0xFF040410.toInt(); canvas.drawCircle(w * 0.873f, bh * 0.10f, w * 0.048f, paint)
        // Far mountains
        paint.color = 0xFF130820.toInt()
        val mtn = Path(); mtn.moveTo(0f, bh * 0.68f)
        listOf(0.0f to 0.68f, 0.08f to 0.42f, 0.18f to 0.60f, 0.28f to 0.38f, 0.40f to 0.55f,
            0.50f to 0.30f, 0.60f to 0.50f, 0.72f to 0.40f, 0.84f to 0.56f, 0.94f to 0.36f, 1.0f to 0.58f)
            .forEach { (mx, my) -> mtn.lineTo(mx * w, my * bh) }
        mtn.lineTo(w, bh); mtn.close(); canvas.drawPath(mtn, paint)
        // Castle silhouette
        paint.color = 0xFF0C0518.toInt()
        canvas.drawRect(w * 0.38f, bh * 0.44f, w * 0.62f, bh * 0.72f, paint) // main wall
        canvas.drawRect(w * 0.36f, bh * 0.38f, w * 0.44f, bh * 0.58f, paint) // left tower
        canvas.drawRect(w * 0.56f, bh * 0.38f, w * 0.64f, bh * 0.58f, paint) // right tower
        val crenW = w * 0.014f; val crenGap = w * 0.01f
        for (i in 0..4) {
            val cx = w * 0.36f + i * (crenW + crenGap)
            canvas.drawRect(cx, bh * 0.33f, cx + crenW, bh * 0.39f, paint)
        }
        for (i in 0..4) {
            val cx = w * 0.56f + i * (crenW + crenGap)
            canvas.drawRect(cx, bh * 0.33f, cx + crenW, bh * 0.39f, paint)
        }
        // Castle windows (glowing)
        paint.color = 0xFFFF6600.toInt()
        canvas.drawRect(w * 0.465f, bh * 0.50f, w * 0.497f, bh * 0.58f, paint)
        canvas.drawRect(w * 0.503f, bh * 0.50f, w * 0.535f, bh * 0.58f, paint)
        paint.color = 0xFFFF8800.toInt()
        canvas.drawRect(w * 0.389f, bh * 0.45f, w * 0.411f, bh * 0.54f, paint)
        canvas.drawRect(w * 0.589f, bh * 0.45f, w * 0.611f, bh * 0.54f, paint)
        // Dead trees (left and right of center)
        paint.color = 0xFF0C0518.toInt()
        drawDeadTree(canvas, w * 0.22f, bh * 0.85f, bh * 0.25f, w * 0.006f)
        drawDeadTree(canvas, w * 0.78f, bh * 0.85f, bh * 0.22f, w * 0.006f)
        drawDeadTree(canvas, w * 0.07f, bh * 0.88f, bh * 0.18f, w * 0.005f)
        drawDeadTree(canvas, w * 0.93f, bh * 0.88f, bh * 0.16f, w * 0.005f)
        // Ground
        val gnd = LinearGradient(0f, bh, 0f, h, 0xFF1C0A14.toInt(), 0xFF100810.toInt(), Shader.TileMode.CLAMP)
        paint.shader = gnd; canvas.drawRect(0f, bh * 0.80f, w, h, paint); paint.shader = null
        // Mist
        val mist = LinearGradient(0f, bh * 0.78f, 0f, bh * 0.90f, 0x00331844.toInt(), 0xFF1C0A14.toInt(), Shader.TileMode.CLAMP)
        paint.shader = mist; canvas.drawRect(0f, bh * 0.78f, w, bh * 0.90f, paint); paint.shader = null
        // Ground glowing lines
        paint.color = 0xFF2A0D22.toInt()
        canvas.drawRect(0f, bh * 0.86f, w, bh * 0.88f, paint)
    }

    private fun drawDeadTree(canvas: Canvas, bx: Float, by: Float, th: Float, tw: Float) {
        canvas.drawRect(bx - tw, by - th, bx + tw, by, paint)
        canvas.drawRect(bx - tw * 3f, by - th * 0.65f, bx - tw, by - th * 0.50f, paint)
        canvas.drawRect(bx + tw,     by - th * 0.65f, bx + tw * 3f, by - th * 0.50f, paint)
        canvas.drawRect(bx - tw * 4f, by - th * 0.40f, bx - tw, by - th * 0.28f, paint)
        canvas.drawRect(bx + tw,     by - th * 0.40f, bx + tw * 4f, by - th * 0.28f, paint)
    }

    private fun drawDivider(canvas: Canvas, w: Float, h: Float) {
        // Center divider - glowing rune line
        val cx = w / 2f; val bh = h * 0.88f
        paint.color = 0x33AA44FF.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f
        canvas.drawLine(cx, bh * 0.12f, cx, bh * 0.92f, paint)
        paint.style = Paint.Style.FILL
        // VS text
        paint.color = 0xFF553366.toInt(); paint.textSize = w * 0.04f; paint.typeface = Typeface.MONOSPACE; paint.textAlign = Paint.Align.CENTER
        canvas.drawText("VS", cx, bh * 0.5f, paint); paint.textAlign = Paint.Align.LEFT
    }

    // ─── Hero Unit Drawing ────────────────────────────────────────
    private fun drawHeroUnit(canvas: Canvas, u: BUnit, w: Float, h: Float) {
        val x = slotX(true, u.slot); val y = slotY(true, u.slot)
        val uw = unitW(true); val uh = unitH(true)
        val alpha = if (u.isDying) (u.dyingAlpha.coerceIn(0f, 1f) * 255).toInt() else 255
        paint.alpha = alpha
        val bob = (sin(tick * 0.12 + u.slot * 1.1) * 3.0).toFloat()
        val flash = u.flashTimer > 0 && u.flashTimer % 2 == 0
        if (u.flashTimer > 0) u.flashTimer--
        val p = uw / 14f
        // Selected glow behind active hero
        if (!u.isDying) {
            val glow = RadialGradient(x + uw * 0.5f, y + uh * 0.7f + bob, uw * 0.7f,
                intArrayOf(0x22FFFFFF.toInt(), 0x00000000.toInt()), null, Shader.TileMode.CLAMP)
            paint.shader = glow; canvas.drawCircle(x + uw * 0.5f, y + uh * 0.7f + bob, uw * 0.7f, paint); paint.shader = null
        }
        paint.alpha = alpha
        when (u.heroClass) {
            HeroClass.KNIGHT   -> drawKnight(canvas, x, y + bob, p, flash)
            HeroClass.MAGE     -> drawMage(canvas, x, y + bob, p, flash)
            HeroClass.ARCHER   -> drawArcher(canvas, x, y + bob, p, flash)
            HeroClass.ASSASSIN -> drawAssassin(canvas, x, y + bob, p, flash)
            HeroClass.PRIEST   -> drawPriest(canvas, x, y + bob, p, flash)
            null -> {}
        }
        drawHpBarUnit(canvas, x, y + uh + 4f, uw, u.hp, u.maxHp, u.heroClass?.spriteColor ?: 0xFF44FF88.toInt())
        paint.alpha = 255
    }

    // ─── Enemy Unit Drawing ───────────────────────────────────────
    private fun drawEnemyUnit(canvas: Canvas, u: BUnit, w: Float, h: Float) {
        val x = slotX(false, u.slot); val y = slotY(false, u.slot)
        val uw = unitW(false); val uh = unitH(false)
        val alpha = if (u.isDying) (u.dyingAlpha * 255).toInt().coerceIn(0, 255) else 255
        paint.alpha = alpha
        val bob = (sin(tick * 0.10 + u.slot * 0.9 + Math.PI) * 2.5).toFloat()
        val flash = u.flashTimer > 0 && u.flashTimer % 2 == 0
        if (u.flashTimer > 0) u.flashTimer--
        val p = uw / 16f
        // Mirror enemies (they face left)
        canvas.save()
        canvas.scale(-1f, 1f, x + uw / 2f, 0f)
        when (u.enemyDef?.spriteId) {
            0 -> drawSlime(canvas, x, y + bob, p, flash)
            1 -> drawGoblin(canvas, x, y + bob, p, flash)
            2 -> drawSkeleton(canvas, x, y + bob, p, flash)
            3 -> drawDarkKnight(canvas, x, y + bob, p, flash)
            4 -> drawDragon(canvas, x - uw * 0.3f, y + bob, p * 0.7f, flash)
            5 -> drawBat(canvas, x, y + bob + uh * 0.1f, p, flash)
            6 -> drawWitch(canvas, x, y + bob, p, flash)
            7 -> drawWerewolf(canvas, x, y + bob, p, flash)
            else -> drawSlime(canvas, x, y + bob, p, flash)
        }
        canvas.restore()
        if (u.isAlive || u.isDying) {
            drawHpBarUnit(canvas, x, y + uh + 4f, uw, u.hp, u.maxHp, 0xFFFF2244.toInt())
            // Name label
            paint.textSize = p * 2.2f; paint.typeface = Typeface.MONOSPACE; paint.color = 0xFFCCAA88.toInt(); paint.alpha = alpha
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(u.enemyDef?.name ?: "", x + uw / 2f, y + uh + p * 5.5f, paint)
            paint.textAlign = Paint.Align.LEFT
        }
        paint.alpha = 255
    }

    private fun drawHpBarUnit(canvas: Canvas, x: Float, y: Float, w: Float, hp: Int, max: Int, col: Int) {
        if (max <= 0) return
        paint.color = 0xFF111111.toInt(); canvas.drawRect(x, y, x + w, y + 6f, paint)
        val pct = (hp.toFloat() / max).coerceIn(0f, 1f)
        paint.color = when { pct > 0.6f -> col; pct > 0.3f -> 0xFFFFAA00.toInt(); else -> 0xFFDD2200.toInt() }
        canvas.drawRect(x, y, x + w * pct, y + 6f, paint)
        paint.color = 0xFF333333.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f
        canvas.drawRect(x, y, x + w, y + 6f, paint); paint.style = Paint.Style.FILL
    }

    private fun drawProjectiles(canvas: Canvas) {
        projs.forEach { pr ->
            val r = if (pr.isCrit) 12f else 8f
            val glow = RadialGradient(pr.x, pr.y, r * 2.5f, intArrayOf(pr.color, pr.color and 0x00FFFFFF.toInt()), floatArrayOf(0.3f, 1f), Shader.TileMode.CLAMP)
            paint.shader = glow; canvas.drawCircle(pr.x, pr.y, r * 2.5f, paint); paint.shader = null
            paint.color = pr.color; paint.alpha = 220; canvas.drawCircle(pr.x, pr.y, r * 0.6f, paint)
            paint.color = 0xFFFFFFFF.toInt(); paint.alpha = 160; canvas.drawCircle(pr.x, pr.y, r * 0.25f, paint)
            paint.alpha = 255
        }
    }

    private fun drawFloatNums(canvas: Canvas) {
        floats.forEach { fn ->
            val a = (fn.life * 9).coerceIn(0, 255)
            paint.alpha = a; paint.color = fn.color
            paint.textSize = if (fn.isCrit) 36f else 26f
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(fn.text, fn.x, fn.y, paint)
        }
        paint.alpha = 255; paint.textAlign = Paint.Align.LEFT
    }

    // ═══════════════════ SPRITE LIBRARY ═════════════════════════════
    private val BK=0xFF080810.toInt(); val WH=0xFFFFFFFF.toInt()
    private val SKIN=0xFFFFCC88.toInt(); val MSKIN=0xFF44AA44.toInt()
    private val STEEL=0xFFCCCCDD.toInt(); val GOLD=0xFFFFCC00.toInt()
    private val EYB=0xFF2244FF.toInt(); val MOUTH=0xFF221100.toInt()
    // Knight
    private val HBLU=0xFF2255CC.toInt(); val HDARK=0xFF102288.toInt()
    // Mage
    private val MHAT=0xFF221133.toInt(); val MHATB=0xFF443355.toInt()
    private val MROBE=0xFF3322AA.toInt(); val MORB=0xFFBB44FF.toInt(); val MSTAFF=0xFF884422.toInt(); val MBEARD=0xFFEEEEDD.toInt(); val MEYE=0xFF44FFAA.toInt()
    // Archer
    private val AHOOD=0xFF336622.toInt(); val ATUN=0xFF886633.toInt(); val ABOW=0xFF663311.toInt()
    // Assassin
    private val ACLK=0xFF1A0022.toInt(); val ACLKD=0xFF110018.toInt(); val ADGR=0xFFAA88CC.toInt()
    // Priest
    private val PROBE=0xFFDDEEFF.toInt(); val PRDK=0xFFAABBCC.toInt(); val PHALO=0xFFFFEE88.toInt()
    // Enemies
    private val SLG=0xFF33BB33.toInt(); val SLGH=0xFF99FF99.toInt(); val SLDK=0xFF227722.toInt()
    private val GOBSK=0xFF44AA33.toInt(); val GOBDK=0xFF225511.toInt(); val GOBBR=0xFF996633.toInt(); val GOBYE=0xFFFFFF00.toInt()
    private val BONE=0xFFEEEECC.toInt(); val BSHD=0xFFBBBBAA.toInt(); val BRED=0xFFFF2200.toInt()
    private val DKARM=0xFF222244.toInt(); val DKSHD=0xFF111133.toInt(); val DKRED=0xFFAA0000.toInt(); val DKGLD=0xFFAA8800.toInt()
    private val DRGSC=0xFF336633.toInt(); val DRGHI=0xFF55AA55.toInt(); val DRGWN=0xFF558833.toInt(); val DRGBL=0xFFFFCC44.toInt(); val DRGFR=0xFFFF4411.toInt()
    private val BATB=0xFF331144.toInt(); val BATW=0xFF220033.toInt(); val BATEY=0xFFFF4400.toInt()
    private val WITCHH=0xFF110022.toInt(); val WITCHR=0xFF440088.toInt(); val WITCHORB=0xFF00FFAA.toInt()
    private val WFUR=0xFF553322.toInt(); val WFURH=0xFF775544.toInt(); val WCLW=0xFFDDCCAA.toInt(); val WEYE=0xFFFF2200.toInt()

    private fun px(canvas: Canvas, cx: Float, cy: Float, col: Int, c: Int, r: Int, pw: Int=1, ph: Int=1) {
        paint.color = col; canvas.drawRect(cx + c * pw, cy + r * ph, cx + (c + pw), cy + (r + ph), paint)
    }
    private fun pxf(canvas: Canvas, cx: Float, cy: Float, col: Int, c: Float, r: Float, pw: Float, ph: Float) {
        paint.color = col; canvas.drawRect(cx + c * pw, cy + r * ph, cx + (c + 1f) * pw, cy + (r + 1f) * ph, paint)
    }

    private fun drawKnight(canvas: Canvas, x: Float, y: Float, p: Float, flash: Boolean) {
        val fc = if (flash) WH else 0
        fun block(c: Int, r: Int, col: Int, w: Int=1, h: Int=1) { paint.color = if (fc!=0) fc else col; canvas.drawRect(x+c*p, y+r*p, x+(c+w)*p, y+(r+h)*p, paint) }
        block(2,0,GOLD,8); block(2,1,GOLD); block(3,1,HBLU,6); block(9,1,GOLD)
        block(2,2,GOLD); block(3,2,HDARK,6); block(9,2,GOLD)
        block(4,2,0xFFAABBFF.toInt(),2); block(7,2,0xFFAABBFF.toInt(),2)
        block(2,3,GOLD); block(3,3,HBLU,6); block(9,3,GOLD)
        block(2,4,SKIN,8,3)
        block(3,5,EYB,2); block(7,5,EYB,2); block(3,5,BK); block(8,5,BK)
        block(4,7,MOUTH,4)
        block(0,9,GOLD,12); block(0,8,GOLD,3,2); block(9,8,GOLD,3,2)
        block(0,10,GOLD,2,4); block(10,10,GOLD,2,4); block(2,10,HBLU,8,4)
        block(4,10,HDARK,4,2); block(5,11,GOLD,2)
        block(2,14,HDARK,8); block(3,14,GOLD,6)
        block(2,15,HDARK,4,4); block(6,15,HDARK,4,4)
        block(2,17,GOLD,2); block(8,17,GOLD,2)
        block(1,19,HDARK,5); block(6,19,HDARK,5)
        // Sword
        block(12,8,GOLD,1,3); block(11,9,GOLD,3); block(12,5,STEEL,1,4); block(12,4,WH)
        // Shield hint
        block(-2,9,GOLD,2,5); block(-3,10,HBLU,2,4)
    }

    private fun drawMage(canvas: Canvas, x: Float, y: Float, p: Float, flash: Boolean) {
        val fc = if (flash) WH else 0
        fun block(c: Int, r: Int, col: Int, w: Int=1, h: Int=1) { paint.color = if (fc!=0) fc else col; canvas.drawRect(x+c*p, y+r*p, x+(c+w)*p, y+(r+h)*p, paint) }
        block(5,0,MHAT,2); block(4,1,MHAT,4); block(3,2,MHAT,6); block(2,3,MHAT,8); block(1,4,MHATB,10)
        block(3,4,0xFF660099.toInt(),2)
        block(3,5,SKIN,6,3); block(4,6,MEYE,2); block(7,6,MEYE); block(5,8,MBEARD,4); block(3,9,MBEARD,6)
        block(5,10,SKIN,2)
        block(2,11,MROBE,8); block(1,12,MROBE,10,5)
        block(0,13,0x992222AA.toInt()/0x99,2,4); block(9,13,0xFF221188.toInt(),2,4)
        block(4,12,0xFF221188.toInt(),4); block(0,17,MROBE,12,2); block(1,19,MROBE,10)
        block(12,7,MORB,2,2); block(13,8,MORB); block(12,9,MSTAFF,1,10); block(11,9,MSTAFF)
        // Orb glow
        paint.color = if (fc!=0) fc else 0x44BB44FF.toInt()
        canvas.drawCircle(x + 13f * p, y + 7.5f * p, p * 3f, paint)
    }

    private fun drawArcher(canvas: Canvas, x: Float, y: Float, p: Float, flash: Boolean) {
        val fc = if (flash) WH else 0
        fun block(c: Int, r: Int, col: Int, w: Int=1, h: Int=1) { paint.color = if (fc!=0) fc else col; canvas.drawRect(x+c*p, y+r*p, x+(c+w)*p, y+(r+h)*p, paint) }
        block(3,0,AHOOD,6); block(2,1,AHOOD,8,4); block(2,2,0xFF224411.toInt(),8)
        block(3,3,SKIN,6,3); block(4,4,0xFF224411.toInt(),2); block(7,4,0xFF224411.toInt()); block(5,6,MOUTH,2)
        block(5,7,SKIN,2)
        block(2,8,ATUN,8,5); block(1,9,AHOOD,2,4); block(9,9,AHOOD,2,4)
        block(10,8,0xFF663300.toInt(),2,5); block(11,8,GOLD); block(11,9,GOLD); block(11,10,GOLD)
        block(2,13,0xFF553322.toInt(),4,4); block(6,13,0xFF553322.toInt(),4,4)
        block(1,17,0xFF442211.toInt(),5); block(6,17,0xFF442211.toInt(),5)
        block(0,5,ABOW,1,8); block(0,5,0xFFEEEECC.toInt()); block(0,13,0xFFEEEECC.toInt())
    }

    private fun drawAssassin(canvas: Canvas, x: Float, y: Float, p: Float, flash: Boolean) {
        val fc = if (flash) WH else 0
        fun block(c: Int, r: Int, col: Int, w: Int=1, h: Int=1) { paint.color = if (fc!=0) fc else col; canvas.drawRect(x+c*p, y+r*p, x+(c+w)*p, y+(r+h)*p, paint) }
        // Hood - dark
        block(3,0,ACLK,6); block(2,1,ACLK,8); block(1,2,ACLK,10); block(2,3,ACLK,8)
        // Face (shadowed)
        block(3,3,BK,6,3); block(4,4,0xFFFF4488.toInt(),2); block(7,4,0xFFFF4488.toInt()) // eyes
        // Cloak body
        block(1,6,ACLK,10,8); block(2,6,ACLKD,8,6)
        block(0,7,ACLK,2,6); block(10,7,ACLK,2,6)
        // Legs (tight)
        block(3,14,ACLK,3,5); block(6,14,ACLK,3,5)
        block(2,19,ACLKD,4); block(6,19,ACLKD,4)
        // Daggers
        block(12,6,ADGR,1,5); block(12,5,WH); block(12,11,ACLKD)
        block(-2,6,ADGR,1,5); block(-2,5,WH); block(-2,11,ACLKD)
    }

    private fun drawPriest(canvas: Canvas, x: Float, y: Float, p: Float, flash: Boolean) {
        val fc = if (flash) WH else 0
        fun block(c: Int, r: Int, col: Int, w: Int=1, h: Int=1) { paint.color = if (fc!=0) fc else col; canvas.drawRect(x+c*p, y+r*p, x+(c+w)*p, y+(r+h)*p, paint) }
        // Halo
        paint.color = if (fc!=0) fc else 0x66FFEE88.toInt()
        canvas.drawCircle(x + 6f*p, y + 1f*p, p * 4f, paint)
        // Hat/hood
        block(3,0,PROBE,6,2); block(2,1,PROBE,8)
        // Face
        block(3,3,SKIN,6,3); block(4,4,0xFF4466AA.toInt(),2); block(7,4,0xFF4466AA.toInt())
        block(5,6,MOUTH,2); block(3,7,0xFFEEDDCC.toInt(),6) // beard
        block(5,8,SKIN,2)
        // Robe (white/light)
        block(2,9,PROBE,8); block(1,10,PROBE,10,6)
        block(0,11,PRDK,2,5); block(10,11,PRDK,2,5)
        block(4,10,0xFF8899BB.toInt(),4,3) // cross symbol
        // Holy symbol glow
        paint.color = if (fc!=0) fc else 0x55FFEE44.toInt()
        canvas.drawCircle(x + 6f*p, y + 12f*p, p * 3f, paint)
        block(2,16,PROBE,8,2); block(2,18,PROBE,4,4); block(6,18,PROBE,4,4)
        block(1,22,PROBE,5); block(6,22,PROBE,5)
        // Staff
        block(12,3,PHALO,2,2)
        paint.color = if (fc!=0) fc else 0x66FFEE44.toInt(); canvas.drawCircle(x+13f*p, y+4f*p, p*2.5f, paint)
        block(12,5,0xFF884422.toInt(),1,12)
    }

    // ─── Enemy Sprites ────────────────────────────────────────────
    private fun drawSlime(canvas: Canvas, x: Float, y: Float, p: Float, flash: Boolean) {
        val fc = if (flash) WH else 0
        val sq = (sin(tick * 0.14) * p * 0.4f).toFloat()
        fun block(c: Int, r: Int, col: Int, w: Int=1, h: Int=1) { paint.color = if (fc!=0) fc else col; canvas.drawRect(x+c*p, y+r*p, x+(c+w)*p, y+(r+h)*p+sq, paint) }
        block(3,0,SLG,6); block(1,1,SLG,10); block(0,2,SLG,12); block(1,1,SLGH,4); block(7,1,SLGH,2)
        block(0,3,SLG,12); block(1,3,BK,3,2); block(8,3,BK,3,2); block(2,3,SLGH); block(9,3,SLGH)
        block(0,4,SLG,12); block(0,5,SLG,12); block(1,6,SLG,10); block(3,7,SLDK,2); block(7,7,SLDK,2); block(4,8,SLDK,4)
    }

    private fun drawGoblin(canvas: Canvas, x: Float, y: Float, p: Float, flash: Boolean) {
        val fc = if (flash) WH else 0
        fun block(c: Int, r: Int, col: Int, w: Int=1, h: Int=1) { paint.color = if (fc!=0) fc else col; canvas.drawRect(x+c*p, y+r*p, x+(c+w)*p, y+(r+h)*p, paint) }
        block(0,2,GOBSK,2,3); block(10,2,GOBSK,2,3)
        block(2,0,GOBSK,8,7); block(2,0,GOBDK,2); block(6,0,GOBDK,4)
        block(3,2,GOBYE,2); block(8,2,GOBYE,2); block(3,2,BK); block(9,2,BK)
        block(5,3,GOBDK,2,2); block(3,6,BK,7); block(4,6,WH); block(6,6,WH); block(8,6,WH)
        block(4,7,GOBSK,4); block(3,8,GOBBR,6,5); block(1,8,GOBSK,2,5); block(9,8,GOBSK,2,5)
        block(4,9,BK,4); block(10,11,STEEL,1,3); block(10,10,GOLD)
        block(3,13,GOBDK,3,5); block(6,13,GOBDK,3,5)
    }

    private fun drawSkeleton(canvas: Canvas, x: Float, y: Float, p: Float, flash: Boolean) {
        val fc = if (flash) WH else 0
        fun block(c: Int, r: Int, col: Int, w: Int=1, h: Int=1) { paint.color = if (fc!=0) fc else col; canvas.drawRect(x+c*p, y+r*p, x+(c+w)*p, y+(r+h)*p, paint) }
        block(3,0,BONE,6); block(2,1,BONE,8,4)
        block(3,2,BRED,2,2); block(7,2,BRED,2,2); block(3,2,0xFFFF6644.toInt()); block(8,2,0xFFFF6644.toInt())
        block(2,4,BSHD,2); block(8,4,BSHD,2); block(5,3,BK,2,2); block(3,5,BONE,6); block(3,6,BONE,6,2)
        block(3,7,BK); block(5,7,BK); block(7,7,BK); block(5,8,BONE,2,2)
        block(2,10,BONE,8,5)
        for (i in 0..4) { block(2,10+i,BSHD); block(9,10+i,BSHD) }
        block(3,10,BK); block(8,10,BK); block(3,12,BK); block(8,12,BK); block(3,14,BK); block(8,14,BK)
        block(0,10,BONE,2,8); block(10,10,BONE,2,8)
        block(2,15,BONE,8,2); block(2,17,BONE,3,4); block(7,17,BONE,3,4)
        block(1,20,BONE,4); block(7,20,BONE,4)
    }

    private fun drawDarkKnight(canvas: Canvas, x: Float, y: Float, p: Float, flash: Boolean) {
        val fc = if (flash) WH else 0
        fun block(c: Int, r: Int, col: Int, w: Int=1, h: Int=1) { paint.color = if (fc!=0) fc else col; canvas.drawRect(x+c*p, y+r*p, x+(c+w)*p, y+(r+h)*p, paint) }
        block(3,0,DKARM,2,3); block(9,0,DKARM,2,3)
        block(2,3,DKGLD,10); block(1,4,DKARM,12,7)
        block(1,4,DKSHD,2,7); block(11,4,DKSHD,2,7)
        block(3,6,DKRED,4); block(7,6,DKRED,4); block(3,6,0xFFFF4444.toInt(),2); block(9,6,0xFFFF4444.toInt(),2)
        block(2,5,DKSHD,10); block(2,10,DKSHD,10); block(4,11,DKARM,6,2)
        block(0,12,DKARM,4,4); block(10,12,DKARM,4,4); block(0,12,DKGLD,4); block(10,12,DKGLD,4)
        block(1,13,DKARM,12,8); block(1,13,DKSHD,2,8); block(11,13,DKSHD,2,8)
        block(5,14,DKRED,4,5); block(4,16,DKRED,6); block(1,21,DKGLD,12)
        block(0,15,DKARM,2,6); block(12,15,DKARM,2,6); block(2,22,DKARM,4,5); block(8,22,DKARM,4,5)
        block(13,17,DKGLD,1,5); block(12,22,DKGLD,3); block(13,23,STEEL,1,8); block(13,31,WH)
    }

    private fun drawDragon(canvas: Canvas, x: Float, y: Float, p: Float, flash: Boolean) {
        val fc = if (flash) WH else 0
        fun block(c: Int, r: Int, col: Int, w: Int=1, h: Int=1) { paint.color = if (fc!=0) fc else col; canvas.drawRect(x+c*p, y+r*p, x+(c+w)*p, y+(r+h)*p, paint) }
        block(0,0,0xFF224422.toInt(),7,2); block(18,0,0xFF224422.toInt(),8,2)
        block(0,2,DRGWN,6,3); block(20,2,DRGWN,7,3)
        block(5,5,DRGSC,15,10); block(6,6,DRGHI,4,2); block(12,6,DRGHI,4,2)
        block(7,9,DRGHI,3,2); block(13,9,DRGHI,3,2); block(7,7,DRGBL,11,6)
        block(11,2,DRGSC,7,4); block(14,0,DRGSC,11,6); block(15,0,0xFFFF3300.toInt(),2,2)
        block(21,1,DRGSC,5,4); block(6,14,DRGSC,5,5); block(14,14,DRGSC,5,5)
        block(16,1,DRGBL,4,3)
    }

    private fun drawBat(canvas: Canvas, x: Float, y: Float, p: Float, flash: Boolean) {
        val fc = if (flash) WH else 0
        val flap = (sin(tick * 0.35) * p * 1.5f).toFloat()
        fun block(c: Int, r: Int, col: Int, w: Int=1, h: Int=1) { paint.color = if (fc!=0) fc else col; canvas.drawRect(x+c*p, y+r*p, x+(c+w)*p, y+(r+h)*p+if(r<3)flap else 0f, paint) }
        block(0,1,BATW,3,2); block(9,1,BATW,3,2); block(1,0,BATW,2); block(9,0,BATW,2)
        block(0,3,BATW,4); block(8,3,BATW,4); block(4,2,BATB,4,4)
        block(4,3,BATEY,2); block(7,3,BATEY); block(4,1,BATB,1,2); block(7,1,BATB,1,2)
        block(5,6,WH); block(6,6,WH); block(5,7,BATB); block(6,7,BATB)
    }

    private fun drawWitch(canvas: Canvas, x: Float, y: Float, p: Float, flash: Boolean) {
        val fc = if (flash) WH else 0
        fun block(c: Int, r: Int, col: Int, w: Int=1, h: Int=1) { paint.color = if (fc!=0) fc else col; canvas.drawRect(x+c*p, y+r*p, x+(c+w)*p, y+(r+h)*p, paint) }
        block(5,0,WITCHH); block(4,1,WITCHH,2); block(3,2,WITCHH,4); block(2,3,WITCHH,6); block(1,4,WITCHR,10)
        block(3,5,MSKIN,6,3); block(4,6,0xFF00AA44.toInt(),2); block(7,6,0xFF00AA44.toInt())
        block(3,8,0xFF226622.toInt(),7); block(7,7,MSKIN,2); block(8,8,MSKIN)
        block(5,9,MSKIN,2)
        block(2,10,WITCHR,8); block(1,11,WITCHR,10,6); block(0,12,0xFF330066.toInt(),2,5); block(9,12,0xFF330066.toInt(),2,5)
        block(0,17,WITCHR,12,2); block(1,19,WITCHR,10)
        block(12,5,WITCHORB,2,2); block(13,6,WITCHORB)
        paint.color = if (fc!=0) fc else 0x8800FFAA.toInt(); canvas.drawCircle(x+13f*p, y+6f*p, p*1.8f, paint)
        block(12,7,0xFF884422.toInt(),1,10)
        block(5,4,GOLD); block(6,3,GOLD); block(7,4,GOLD)
    }

    private fun drawWerewolf(canvas: Canvas, x: Float, y: Float, p: Float, flash: Boolean) {
        val fc = if (flash) WH else 0
        fun block(c: Int, r: Int, col: Int, w: Int=1, h: Int=1) { paint.color = if (fc!=0) fc else col; canvas.drawRect(x+c*p, y+r*p, x+(c+w)*p, y+(r+h)*p, paint) }
        block(3,0,WFUR,2,3); block(9,0,WFUR,2,3); block(4,1,WFURH); block(9,1,WFURH)
        block(2,2,WFUR,10,6); block(2,2,WFURH,2,6); block(10,2,WFURH,2,6)
        block(3,3,WEYE,2,2); block(9,3,WEYE,2,2); block(4,4,0xFFFF4400.toInt()); block(9,4,0xFFFF4400.toInt())
        block(4,6,WFUR,6,3); block(5,7,0xFF221100.toInt(),4); block(6,7,BK,2)
        block(5,9,WH,1,2); block(8,9,WH,1,2); block(4,8,WFUR,6,2)
        block(1,10,WFUR,12,7); block(1,10,WFURH,2,7); block(11,10,WFURH,2,7); block(4,11,0xFF221100.toInt(),6,3)
        block(0,10,WFUR,2,8); block(12,10,WFUR,2,8)
        block(0,17,WCLW); block(0,18,WCLW); block(1,18,WCLW); block(13,17,WCLW); block(13,18,WCLW); block(14,18,WCLW)
        block(1,17,0xFF332211.toInt(),12); block(2,18,WFUR,5,5); block(7,18,WFUR,5,5)
        block(1,22,WFUR,6); block(7,22,WFUR,6)
    }
}
