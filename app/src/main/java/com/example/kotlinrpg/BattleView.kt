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
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    companion object {
        // Hero palette
        val SKIN=0xFFFFCC88.toInt(); val HARMO=0xFF2255CC.toInt(); val HDARK=0xFF102288.toInt()
        val HGOLD=0xFFFFCC00.toInt(); val EYE_B=0xFF1133FF.toInt(); val STEEL=0xFFCCCCDD.toInt()
        val MOUTH=0xFF221100.toInt()
        // Mage palette
        val MHAT=0xFF221133.toInt(); val MHATB=0xFF443355.toInt(); val MROBE=0xFF3322AA.toInt()
        val MROBEDK=0xFF221188.toInt(); val MORB=0xFFBB44FF.toInt(); val MSTAFF=0xFF884422.toInt()
        val MBEARD=0xFFEEEEDD.toInt(); val MEYE=0xFF44FFAA.toInt()
        // Archer palette
        val AHOOD=0xFF336622.toInt(); val ATUN=0xFF886633.toInt()
        val ABOW=0xFF663311.toInt(); val ABSTR=0xFFEEEECC.toInt()
        // Enemy shared
        val SLG=0xFF33BB33.toInt(); val SLGH=0xFF99FF99.toInt(); val SLDK=0xFF227722.toInt()
        val GOBSK=0xFF44AA33.toInt(); val GOBDK=0xFF225511.toInt(); val GOBBR=0xFF996633.toInt(); val GOBYE=0xFFFFFF00.toInt()
        val BONE=0xFFEEEECC.toInt(); val BSHD=0xFFBBBBAA.toInt(); val BRED=0xFFFF2200.toInt()
        val DKARM=0xFF222244.toInt(); val DKSHD=0xFF111133.toInt(); val DKRED=0xFFAA0000.toInt(); val DKGLD=0xFFAA8800.toInt()
        val DRGSC=0xFF336633.toInt(); val DRGHI=0xFF55AA55.toInt(); val DRGWN=0xFF558833.toInt()
        val DRGBL=0xFFFFCC44.toInt(); val DRGFR=0xFFFF4411.toInt(); val DRGEY=0xFFFFFF00.toInt()
        val BATB=0xFF331144.toInt(); val BATW=0xFF220033.toInt(); val BATEY=0xFFFF4400.toInt()
        val WITCHH=0xFF110022.toInt(); val WITCHR=0xFF440088.toInt(); val WITCHSK=0xFF88CC88.toInt(); val WITCHORB=0xFF00FFAA.toInt()
        val WFUR=0xFF553322.toInt(); val WFURH=0xFF775544.toInt(); val WCLW=0xFFDDCCAA.toInt(); val WEYE=0xFFFF2200.toInt()
        val BK=0xFF111111.toInt(); val WH=0xFFFFFFFF.toInt()
        val FIREO=0xFFFF8800.toInt(); val FIREY=0xFFFFEE00.toInt()
    }

    var currentEnemy: GameState.EnemyDef? = null
    var enemyHp = 0; var enemyMaxHp = 0
    private var heroOffX = 0f; private var heroOffY = 0f
    private var enemyOffX = 0f; private var enemyOffY = 0f
    private var heroFlash = 0; private var enemyFlash = 0
    private var idleTick = 0L
    private var attackAnimStep = 0
    private var isPlayerAttacking = false
    private var isEnemyAttacking = false

    data class FloatText(val text: String, var x: Float, var y: Float, var life: Int, val color: Int)
    private val floatTexts = mutableListOf<FloatText>()

    var onBattleLog: ((String) -> Unit)? = null
    var onStatsChanged: (() -> Unit)? = null
    var onEnemyDefeated: ((Int, Int) -> Unit)? = null
    var onPlayerDefeated: (() -> Unit)? = null
    var onLevelUp: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private var battleRunning = false

    private val animFrame = object : Runnable {
        override fun run() {
            idleTick++
            floatTexts.forEach { it.y -= 1.5f; it.life-- }
            floatTexts.removeAll { it.life <= 0 }
            invalidate()
            handler.postDelayed(this, 50)
        }
    }

    private val battleTick = object : Runnable {
        override fun run() {
            if (!battleRunning || currentEnemy == null) return
            if (!isPlayerAttacking && !isEnemyAttacking) doPlayerAttack()
            handler.postDelayed(this, 1600)
        }
    }

    init { post { handler.post(animFrame) } }

    fun startBattle(enemy: GameState.EnemyDef) {
        currentEnemy = enemy
        enemyMaxHp = enemy.maxHp; enemyHp = enemy.maxHp
        heroOffX = 0f; heroOffY = 0f; enemyOffX = 0f; enemyOffY = 0f
        heroFlash = 0; enemyFlash = 0; attackAnimStep = 0
        isPlayerAttacking = false; isEnemyAttacking = false
        battleRunning = true
        handler.removeCallbacks(battleTick)
        handler.postDelayed(battleTick, 800)
    }

    fun stopBattle() {
        battleRunning = false
        handler.removeCallbacks(battleTick)
    }

    fun resumeBattle() {
        if (currentEnemy != null && enemyHp > 0) {
            battleRunning = true
            handler.removeCallbacks(battleTick)
            handler.postDelayed(battleTick, 800)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(animFrame)
        handler.removeCallbacks(battleTick)
    }

    private fun ps() = width / 96f

    private fun doPlayerAttack() {
        isPlayerAttacking = true
        val steps = 8; var step = 0
        val target = width * 0.40f
        fun frame() {
            step++; attackAnimStep = step
            when {
                step <= steps / 2 -> { heroOffX = target * (step.toFloat() / (steps / 2)); handler.postDelayed(::frame, 40) }
                step == steps / 2 + 1 -> {
                    val cls = GameState.heroClass
                    val crit = Random.nextFloat() < cls.critChance
                    val raw = maxOf(1, GameState.heroAtk - (currentEnemy?.defense ?: 0) + Random.nextInt(-3, 4))
                    val dmg = if (crit) (raw * 1.8).toInt() else raw
                    enemyHp = maxOf(0, enemyHp - dmg)
                    enemyFlash = 8
                    val msg = if (crit) "CRITICAL! $dmg damage!" else "${GameState.heroClass.displayName} attacks for $dmg!"
                    onBattleLog?.invoke(msg)
                    addFloat(if (crit) "★$dmg" else "$dmg", width * 0.6f, height * 0.4f, if (crit) 0xFFFFFF00.toInt() else 0xFFFF4444.toInt())
                    handler.postDelayed(::frame, 80)
                }
                step <= steps -> { heroOffX = target * (1f - (step - steps / 2 - 1).toFloat() / (steps / 2)); handler.postDelayed(::frame, 40) }
                else -> {
                    heroOffX = 0f; attackAnimStep = 0; isPlayerAttacking = false
                    if (enemyHp <= 0) handleEnemyDefeated()
                    else { handler.postDelayed({ doEnemyAttack() }, 300) }
                }
            }
        }
        handler.postDelayed(::frame, 30)
    }

    private fun doEnemyAttack() {
        if (enemyHp <= 0 || currentEnemy == null) return
        isEnemyAttacking = true
        val steps = 8; var step = 0
        val target = -(width * 0.30f)
        fun frame() {
            step++
            when {
                step <= steps / 2 -> { enemyOffX = target * (step.toFloat() / (steps / 2)); handler.postDelayed(::frame, 40) }
                step == steps / 2 + 1 -> {
                    val raw = maxOf(1, (currentEnemy?.attack ?: 10) - GameState.heroDef + Random.nextInt(-3, 4))
                    GameState.heroHp = maxOf(0, GameState.heroHp - raw)
                    heroFlash = 8
                    onBattleLog?.invoke("${currentEnemy?.name} hits for $raw!")
                    addFloat("-$raw", width * 0.15f, height * 0.4f, 0xFFFF2222.toInt())
                    onStatsChanged?.invoke()
                    handler.postDelayed(::frame, 80)
                }
                step <= steps -> { enemyOffX = target * (1f - (step - steps / 2 - 1).toFloat() / (steps / 2)); handler.postDelayed(::frame, 40) }
                else -> {
                    enemyOffX = 0f; isEnemyAttacking = false
                    if (GameState.heroHp <= 0) onPlayerDefeated?.invoke()
                }
            }
        }
        handler.postDelayed(::frame, 30)
    }

    private fun handleEnemyDefeated() {
        val enemy = currentEnemy ?: return
        val leveled = GameState.gainExpAndGold(enemy.expReward, enemy.goldReward)
        if (leveled) { onLevelUp?.invoke(); onBattleLog?.invoke("★ LEVEL UP! Now Lv.${GameState.heroLevel}!") }
        onEnemyDefeated?.invoke(enemy.expReward, enemy.goldReward)
        battleRunning = false
    }

    private fun addFloat(text: String, x: Float, y: Float, color: Int) {
        floatTexts.add(FloatText(text, x, y, 30, color))
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val p = ps()
        val gy = h * 0.72f
        drawDarkBg(canvas, w, h, gy, p)
        val hby = gy - p * 22f
        drawHero(canvas, w * 0.06f + heroOffX, hby + heroOffY, p)
        if (enemyHp > 0 && currentEnemy != null) {
            val esh = enemySpriteH() * p
            drawEnemy(canvas, w * 0.52f + enemyOffX, gy - esh + enemyOffY, p)
        }
        drawBars(canvas, w, gy, p)
        drawFloatTexts(canvas, p)
        if (heroFlash > 0) heroFlash--
        if (enemyFlash > 0) enemyFlash--
    }

    private fun drawBars(canvas: Canvas, w: Float, gy: Float, p: Float) {
        val bh = p * 2.8f
        drawBar(canvas, w * 0.02f, gy + p * 0.5f, w * 0.44f, bh, GameState.heroHp, GameState.heroMaxHp, 0xFF33DD33.toInt(), "LV${GameState.heroLevel} ${GameState.heroClass.displayName}")
        val en = currentEnemy
        if (en != null) drawBar(canvas, w * 0.54f, gy + p * 0.5f, w * 0.44f, bh, enemyHp, enemyMaxHp, 0xFFDD2233.toInt(), en.name)
    }

    private fun drawBar(c: Canvas, x: Float, y: Float, w: Float, h: Float, hp: Int, max: Int, col: Int, name: String) {
        if (max <= 0) return
        paint.color = 0xFF111111.toInt(); c.drawRect(x, y, x + w, y + h, paint)
        val pct = hp.toFloat() / max
        paint.color = if (pct > 0.4f) col else 0xFFCC3311.toInt()
        c.drawRect(x, y, x + w * pct, y + h, paint)
        paint.color = 0xFF333333.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f
        c.drawRect(x, y, x + w, y + h, paint); paint.style = Paint.Style.FILL
        paint.color = WH; paint.textSize = h * 0.68f; paint.typeface = Typeface.MONOSPACE
        c.drawText("$name  $hp/$max", x + 3f, y + h - 2f, paint)
    }

    private fun drawFloatTexts(canvas: Canvas, p: Float) {
        paint.textSize = p * 4f; paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        floatTexts.forEach { ft ->
            paint.alpha = (ft.life * 8).coerceIn(0, 255)
            paint.color = ft.color
            canvas.drawText(ft.text, ft.x, ft.y, paint)
        }
        paint.alpha = 255
    }

    private fun enemySpriteH() = when (currentEnemy?.spriteId) { 0 -> 9f; 1 -> 18f; 2 -> 21f; 3 -> 26f; 4 -> 22f; 5 -> 8f; 6 -> 20f; 7 -> 22f; else -> 18f }

    private fun drawDarkBg(canvas: Canvas, w: Float, h: Float, gy: Float, p: Float) {
        val sky = LinearGradient(0f, 0f, 0f, gy, 0xFF030308.toInt(), 0xFF0D0820.toInt(), Shader.TileMode.CLAMP)
        paint.shader = sky; canvas.drawRect(0f, 0f, w, gy, paint); paint.shader = null
        // stars
        paint.color = 0x88FFFFFF.toInt()
        listOf(0.06f to 0.05f, 0.18f to 0.12f, 0.34f to 0.04f, 0.52f to 0.09f, 0.7f to 0.14f, 0.86f to 0.06f,
            0.11f to 0.27f, 0.45f to 0.21f, 0.77f to 0.3f, 0.93f to 0.17f, 0.29f to 0.38f, 0.61f to 0.34f,
            0.07f to 0.42f, 0.88f to 0.38f, 0.55f to 0.46f).forEach { (sx, sy) ->
            canvas.drawRect(sx * w, sy * gy, sx * w + p, sy * gy + p, paint)
        }
        // moon (blood red)
        paint.color = 0xFFCC2211.toInt(); canvas.drawCircle(w * 0.8f, gy * 0.16f, p * 5f, paint)
        paint.color = 0xFF030308.toInt(); canvas.drawCircle(w * 0.82f + p, gy * 0.14f, p * 3.5f, paint)
        // mountains dark
        paint.color = 0xFF1A0A20.toInt()
        val mtn = Path(); mtn.moveTo(0f, gy)
        listOf(0.0f to 1.0f, 0.07f to 0.52f, 0.17f to 0.82f, 0.27f to 0.45f, 0.38f to 0.75f, 0.50f to 0.38f,
            0.62f to 0.72f, 0.72f to 0.48f, 0.83f to 0.78f, 0.93f to 0.44f, 1.0f to 0.75f).forEach { (mx, my) ->
            mtn.lineTo(mx * w, my * gy)
        }
        mtn.lineTo(w, gy); mtn.close(); canvas.drawPath(mtn, paint)
        // dead trees silhouette
        paint.color = 0xFF0D0515.toInt()
        for (tx in 0..6) {
            val bx = (tx * w / 7f) + p * 2
            val by = gy * (0.58f + (tx % 3) * 0.06f)
            canvas.drawRect(bx, by, bx + p, gy, paint)
            canvas.drawRect(bx - p * 2, by + p * 2, bx + p * 3, by + p, paint)
        }
        // ground
        val gnd = LinearGradient(0f, gy, 0f, h, 0xFF1A0A08.toInt(), 0xFF0A0508.toInt(), Shader.TileMode.CLAMP)
        paint.shader = gnd; canvas.drawRect(0f, gy, w, h, paint); paint.shader = null
        paint.color = 0xFF2A1A2A.toInt(); canvas.drawRect(0f, gy, w, gy + p * 2, paint)
    }

    // ═══════════════ HERO SPRITES ═══════════════
    private fun drawHero(canvas: Canvas, x: Float, y: Float, p: Float) {
        when (GameState.heroClass) {
            HeroClass.KNIGHT -> drawKnight(canvas, x, y, p)
            HeroClass.MAGE -> drawMage(canvas, x, y, p)
            HeroClass.ARCHER -> drawArcher(canvas, x, y, p)
        }
    }

    private fun drawKnight(canvas: Canvas, x: Float, y: Float, p: Float) {
        val bob = (sin(idleTick * 0.18) * p * 0.55f).toFloat()
        val fl = if (heroFlash > 0 && heroFlash % 2 == 0) WH else 0
        fun px(c: Int, r: Int, col: Int, w: Int = 1, h: Int = 1) {
            paint.color = if (fl != 0) fl else col
            canvas.drawRect(x + c * p, y + r * p + bob, x + (c + w) * p, y + (r + h) * p + bob, paint)
        }
        px(2,0,HGOLD,8); px(2,1,HGOLD); px(3,1,HARMO,6); px(9,1,HGOLD)
        px(2,2,HGOLD); px(3,2,HDARK,6); px(9,2,HGOLD)
        px(4,2,0xFFAABBFF.toInt(),2); px(7,2,0xFFAABBFF.toInt(),2)
        px(2,3,HGOLD); px(3,3,HARMO,6); px(9,3,HGOLD)
        px(2,4,SKIN,8,3); px(3,5,EYE_B,2); px(7,5,EYE_B,2); px(3,5,BK); px(8,5,BK)
        px(3,4,MOUTH,3); px(7,4,MOUTH,3); px(4,7,MOUTH); px(7,7,MOUTH)
        px(2,7,SKIN,2); px(6,7,SKIN,2); px(9,7,SKIN); px(4,8,SKIN,4)
        px(0,9,HGOLD,12); px(0,8,HGOLD,3,2); px(9,8,HGOLD,3,2)
        px(0,10,HGOLD,2,4); px(10,10,HGOLD,2,4); px(2,10,HARMO,8,4)
        px(4,10,HDARK,4,2); px(5,11,HGOLD,2); px(2,14,HDARK,8); px(3,14,HGOLD,6)
        px(2,15,HDARK,4,4); px(6,15,HDARK,4,4); px(2,17,HGOLD,2); px(8,17,HGOLD,2)
        px(1,19,HDARK,5); px(6,19,HDARK,5); px(1,19,HGOLD); px(10,19,HGOLD)
        if (isPlayerAttacking && attackAnimStep > 0) {
            px(12,8,HGOLD,1,3); px(11,9,HGOLD,3); px(12,5,STEEL,1,4); px(12,4,WH)
        }
    }

    private fun drawMage(canvas: Canvas, x: Float, y: Float, p: Float) {
        val bob = (sin(idleTick * 0.16) * p * 0.5f).toFloat()
        val fl = if (heroFlash > 0 && heroFlash % 2 == 0) WH else 0
        fun px(c: Int, r: Int, col: Int, w: Int = 1, h: Int = 1) {
            paint.color = if (fl != 0) fl else col
            canvas.drawRect(x + c * p, y + r * p + bob, x + (c + w) * p, y + (r + h) * p + bob, paint)
        }
        // Hat
        px(5,0,MHAT,2); px(4,1,MHAT,4); px(3,2,MHAT,6); px(2,3,MHAT,8); px(1,4,MHATB,10)
        px(3,4,0xFF660099.toInt(),2); // star on hat
        // Face
        px(3,5,SKIN,6,3); px(4,6,MEYE,2); px(7,6,MEYE); px(5,8,MBEARD,4); px(3,9,MBEARD,6)
        // Neck
        px(5,10,SKIN,2)
        // Robe
        px(2,11,MROBE,8); px(1,12,MROBE,10,5); px(0,13,MROBEDK,2,4); px(9,13,MROBEDK,2,4)
        px(4,12,MROBEDK,4); px(0,17,MROBE,12,2); px(1,19,MROBE,10)
        // Staff (right side)
        px(12,7,MORB,2,2); px(13,8,MORB); px(12,9,MSTAFF,1,10); px(11,9,MSTAFF)
        if (isPlayerAttacking && attackAnimStep > 0) {
            paint.color = if (fl != 0) fl else 0x88BB44FF.toInt()
            canvas.drawCircle(x + 15 * p, y + 8 * p + bob, p * 2.5f, paint)
        }
    }

    private fun drawArcher(canvas: Canvas, x: Float, y: Float, p: Float) {
        val bob = (sin(idleTick * 0.20) * p * 0.5f).toFloat()
        val fl = if (heroFlash > 0 && heroFlash % 2 == 0) WH else 0
        fun px(c: Int, r: Int, col: Int, w: Int = 1, h: Int = 1) {
            paint.color = if (fl != 0) fl else col
            canvas.drawRect(x + c * p, y + r * p + bob, x + (c + w) * p, y + (r + h) * p + bob, paint)
        }
        // Hood
        px(3,0,AHOOD,6); px(2,1,AHOOD,8,4); px(2,2,0xFF224411.toInt(),8)
        // Face
        px(3,3,SKIN,6,3); px(4,4,0xFF224411.toInt(),2); px(7,4,0xFF224411.toInt()); px(5,6,MOUTH,2)
        // Neck
        px(5,7,SKIN,2)
        // Tunic
        px(2,8,ATUN,8,5); px(1,9,AHOOD,2,4); px(9,9,AHOOD,2,4)
        // Quiver on back
        px(10,8,0xFF663300.toInt(),2,5); px(11,8,0xFFFFCC00.toInt()); px(11,9,0xFFFFCC00.toInt()); px(11,10,0xFFFFCC00.toInt())
        // Legs
        px(2,13,0xFF553322.toInt(),4,4); px(6,13,0xFF553322.toInt(),4,4)
        px(1,17,0xFF442211.toInt(),5); px(6,17,0xFF442211.toInt(),5)
        // Bow (left side)
        px(0,5,ABOW,1,8); px(0,5,ABSTR); px(0,13,ABSTR)
        if (isPlayerAttacking && attackAnimStep > 0) {
            // Arrow flying
            paint.color = if (fl != 0) fl else 0xFFFFCC00.toInt()
            canvas.drawRect(x + 14 * p, y + 9 * p + bob, x + 20 * p, y + 9.5f * p + bob, paint)
            paint.color = STEEL
            canvas.drawRect(x + 20 * p, y + 8.5f * p + bob, x + 21 * p, y + 10.5f * p + bob, paint)
        }
    }

    // ═══════════════ ENEMY SPRITES ═══════════════
    private fun drawEnemy(canvas: Canvas, x: Float, y: Float, p: Float) {
        when (currentEnemy?.spriteId) {
            0 -> drawSlime(canvas, x, y, p)
            1 -> drawGoblin(canvas, x, y, p)
            2 -> drawSkeleton(canvas, x, y, p)
            3 -> drawDarkKnight(canvas, x, y, p)
            4 -> drawDragon(canvas, x, y, p)
            5 -> drawBat(canvas, x, y, p)
            6 -> drawWitch(canvas, x, y, p)
            7 -> drawWerewolf(canvas, x, y, p)
        }
    }

    private fun drawSlime(canvas: Canvas, x: Float, y: Float, p: Float) {
        val bob = (sin(idleTick * 0.14) * p * 0.5f).toFloat()
        val sq = (sin(idleTick * 0.14) * p * 0.3f).toFloat()
        val fl = if (enemyFlash > 0 && enemyFlash % 2 == 0) WH else 0
        fun px(c: Int, r: Int, col: Int, w: Int = 1, h: Int = 1) { paint.color = if (fl != 0) fl else col; canvas.drawRect(x + c * p, y + r * p + bob, x + (c + w) * p, y + (r + h) * p + bob + sq, paint) }
        px(3,0,SLG,6); px(1,1,SLG,10); px(0,2,SLG,12); px(1,1,SLGH,4); px(7,1,SLGH,2)
        px(0,3,SLG,12); px(1,3,BK,3,2); px(8,3,BK,3,2); px(2,3,SLGH); px(9,3,SLGH)
        px(0,4,SLG,12); px(0,5,SLG,12); px(1,6,SLG,10); px(3,7,SLDK,2); px(7,7,SLDK,2); px(4,8,SLDK,4)
    }

    private fun drawGoblin(canvas: Canvas, x: Float, y: Float, p: Float) {
        val bob = (sin(idleTick * 0.16) * p * 0.4f).toFloat()
        val fl = if (enemyFlash > 0 && enemyFlash % 2 == 0) WH else 0
        fun px(c: Int, r: Int, col: Int, w: Int = 1, h: Int = 1) { paint.color = if (fl != 0) fl else col; canvas.drawRect(x + c * p, y + r * p + bob, x + (c + w) * p, y + (r + h) * p + bob, paint) }
        px(0,2,GOBSK,2,3); px(10,2,GOBSK,2,3); px(0,1,GOBSK); px(11,1,GOBSK)
        px(2,0,GOBSK,8,7); px(2,0,GOBDK,2); px(4,0,GOBSK,2); px(6,0,GOBDK,4)
        px(3,2,GOBYE,2); px(8,2,GOBYE,2); px(3,2,BK); px(9,2,BK); px(3,1,GOBDK,3); px(7,1,GOBDK,3)
        px(5,3,GOBDK,2,2); px(3,6,BK,7); px(4,6,WH); px(6,6,WH); px(8,6,WH)
        px(4,7,GOBSK,4); px(3,8,GOBBR,6,5); px(1,8,GOBSK,2,5); px(9,8,GOBSK,2,5)
        px(4,9,BK,4); px(10,11,STEEL,1,3); px(9,11,GOBBR); px(10,10,HGOLD)
        px(3,13,GOBDK,3,5); px(6,13,GOBDK,3,5); px(2,17,GOBDK,4); px(6,17,GOBDK,5)
    }

    private fun drawSkeleton(canvas: Canvas, x: Float, y: Float, p: Float) {
        val bob = (sin(idleTick * 0.12) * p * 0.3f).toFloat()
        val fl = if (enemyFlash > 0 && enemyFlash % 2 == 0) WH else 0
        fun px(c: Int, r: Int, col: Int, w: Int = 1, h: Int = 1) { paint.color = if (fl != 0) fl else col; canvas.drawRect(x + c * p, y + r * p + bob, x + (c + w) * p, y + (r + h) * p + bob, paint) }
        px(3,0,BONE,6); px(2,1,BONE,8,4); px(3,2,BRED,2,2); px(7,2,BRED,2,2)
        px(3,2,0xFFFF6644.toInt()); px(8,2,0xFFFF6644.toInt()); px(2,4,BSHD,2); px(8,4,BSHD,2)
        px(5,3,BK,2,2); px(3,5,BONE,6); px(3,6,BONE,6,2)
        px(3,7,BK); px(5,7,BK); px(7,7,BK); px(5,8,BONE,2,2)
        px(2,10,BONE,8,5)
        for (i in 0..4) { px(2,10+i,BSHD); px(9,10+i,BSHD) }
        px(3,10,BK); px(8,10,BK); px(3,12,BK); px(8,12,BK); px(3,14,BK); px(8,14,BK)
        px(0,10,BONE,2,8); px(10,10,BONE,2,8); px(0,18,BONE); px(1,18,BSHD); px(10,18,BONE); px(11,18,BSHD)
        px(2,15,BONE,8,2); px(2,17,BONE,3,4); px(7,17,BONE,3,4)
        px(1,20,BONE,4); px(7,20,BONE,4)
    }

    private fun drawDarkKnight(canvas: Canvas, x: Float, y: Float, p: Float) {
        val bob = (sin(idleTick * 0.10) * p * 0.2f).toFloat()
        val fl = if (enemyFlash > 0 && enemyFlash % 2 == 0) WH else 0
        fun px(c: Int, r: Int, col: Int, w: Int = 1, h: Int = 1) { paint.color = if (fl != 0) fl else col; canvas.drawRect(x + c * p, y + r * p + bob, x + (c + w) * p, y + (r + h) * p + bob, paint) }
        px(3,0,DKARM,2,3); px(9,0,DKARM,2,3); px(3,-1,DKARM); px(10,-1,DKARM)
        px(2,3,DKGLD,10); px(1,4,DKARM,12,7); px(1,4,DKSHD,2,7); px(11,4,DKSHD,2,7)
        px(3,6,DKRED,4); px(7,6,DKRED,4); px(3,6,0xFFFF4444.toInt(),2); px(9,6,0xFFFF4444.toInt(),2)
        px(2,5,DKSHD,10); px(2,10,DKSHD,10); px(4,11,DKARM,6,2)
        px(0,12,DKARM,4,4); px(10,12,DKARM,4,4); px(0,12,DKGLD,4); px(10,12,DKGLD,4)
        px(1,11,DKARM,2,2); px(11,11,DKARM,2,2); px(1,13,DKARM,12,8)
        px(1,13,DKSHD,2,8); px(11,13,DKSHD,2,8); px(5,14,DKRED,4,5); px(4,16,DKRED,6)
        px(1,21,DKGLD,12); px(0,15,DKARM,2,6); px(12,15,DKARM,2,6)
        px(0,21,DKGLD,3,2); px(11,21,DKGLD,3,2)
        px(13,17,DKGLD,1,2); px(13,19,DKGLD,1,3); px(12,22,DKGLD,3); px(13,23,STEEL,1,8); px(13,31,WH)
        px(2,22,DKARM,4,5); px(8,22,DKARM,4,5); px(1,27,DKSHD,6); px(7,27,DKSHD,6)
    }

    private fun drawDragon(canvas: Canvas, x: Float, y: Float, p: Float) {
        val bob = (sin(idleTick * 0.08) * p * 0.6f).toFloat()
        val fl = if (enemyFlash > 0 && enemyFlash % 2 == 0) WH else 0
        fun px(c: Int, r: Int, col: Int, w: Int = 1, h: Int = 1) { paint.color = if (fl != 0) fl else col; canvas.drawRect(x + c * p, y + r * p + bob, x + (c + w) * p, y + (r + h) * p + bob, paint) }
        px(0,0,0xFF224422.toInt(),7,2); px(18,0,0xFF224422.toInt(),8,2)
        px(0,2,DRGWN,6,3); px(0,5,DRGWN,5,2); px(0,7,DRGWN,4,2)
        px(20,2,DRGWN,7,3); px(21,5,DRGWN,5,2); px(22,7,DRGWN,4,2)
        px(0,9,DRGSC,4,2); px(0,11,DRGSC,3,2); px(1,13,DRGSC,2); px(2,14,DRGSC)
        px(5,5,DRGSC,15,10); px(6,6,DRGHI,4,2); px(12,6,DRGHI,4,2); px(7,9,DRGHI,3,2); px(13,9,DRGHI,3,2)
        px(7,7,DRGBL,11,6); px(11,2,DRGSC,7,4); px(12,1,DRGSC,5,2)
        px(14,0,DRGSC,11,6); px(13,1,DRGSC,12,4); px(21,1,DRGSC,5,4); px(23,1,0xFF224422.toInt(),2)
        px(15,0,DRGEY,4,4); px(16,1,BK,2,2); px(15,1,0xFFFFFF88.toInt())
        px(14,0,DRGSC,2,2); px(14,-1,DRGSC,1,2); px(15,-2,DRGSC)
        px(22,5,WH,1,2); px(24,5,WH,1,2); px(23,4,WH,1,2)
        if (isEnemyAttacking) { listOf(FIREY,FIREO,DRGFR,FIREO,FIREY,FIREO,DRGFR).forEachIndexed { i, c -> px(26+i*2,2+(i%3),c,2,2) }; px(26,1,FIREY,3) }
        px(6,14,DRGSC,5,5); px(14,14,DRGSC,5,5)
    }

    private fun drawBat(canvas: Canvas, x: Float, y: Float, p: Float) {
        val flap = (sin(idleTick * 0.35) * p * 1.5f).toFloat()
        val fl = if (enemyFlash > 0 && enemyFlash % 2 == 0) WH else 0
        fun px(c: Int, r: Int, col: Int, w: Int = 1, h: Int = 1) { paint.color = if (fl != 0) fl else col; canvas.drawRect(x + c * p, y + r * p, x + (c + w) * p, y + (r + h) * p + (if (r < 3) flap * 0.5f else 0f), paint) }
        // Wings
        px(0,1,BATW,3,2); px(9,1,BATW,3,2); px(1,0,BATW,2); px(9,0,BATW,2)
        px(0,3,BATW,4); px(8,3,BATW,4)
        // Body
        px(4,2,BATB,4,4)
        // Eyes
        px(4,3,BATEY,2); px(7,3,BATEY); px(4,3,0xFFFF6600.toInt()); px(7,3,0xFFFF6600.toInt())
        // Ears
        px(4,1,BATB,1,2); px(7,1,BATB,1,2)
        // Fangs
        px(5,6,WH); px(6,6,WH)
        // Feet
        px(5,7,BATB,1,1); px(6,7,BATB,1,1)
    }

    private fun drawWitch(canvas: Canvas, x: Float, y: Float, p: Float) {
        val bob = (sin(idleTick * 0.14) * p * 0.45f).toFloat()
        val fl = if (enemyFlash > 0 && enemyFlash % 2 == 0) WH else 0
        fun px(c: Int, r: Int, col: Int, w: Int = 1, h: Int = 1) { paint.color = if (fl != 0) fl else col; canvas.drawRect(x + c * p, y + r * p + bob, x + (c + w) * p, y + (r + h) * p + bob, paint) }
        // Hat
        px(5,0,WITCHH); px(4,1,WITCHH,2); px(3,2,WITCHH,4); px(2,3,WITCHH,6); px(1,4,WITCHR,10)
        // Face (green skin)
        px(3,5,WITCHSK,6,3); px(4,6,0xFF00AA44.toInt(),2); px(7,6,0xFF00AA44.toInt())
        px(4,7,WITCHSK,2); px(7,7,WITCHSK); px(3,8,0xFF226622.toInt(),7) // chin/jaw
        // Nose (pointy)
        px(7,7,WITCHSK,2); px(8,8,WITCHSK)
        // Neck
        px(5,9,WITCHSK,2)
        // Robe
        px(2,10,WITCHR,8); px(1,11,WITCHR,10,6); px(0,12,0xFF330066.toInt(),2,5); px(9,12,0xFF330066.toInt(),2,5)
        px(3,11,0xFF660099.toInt(),6)
        px(0,17,WITCHR,12,2); px(1,19,WITCHR,10)
        // Staff + orb
        px(12,5,WITCHORB,2,2); px(13,6,WITCHORB)
        paint.color = if (fl != 0) fl else 0x8800FFAA.toInt()
        canvas.drawCircle(x + 13f * p, y + 6f * p + bob, p * 1.8f, paint)
        px(12,7,0xFF884422.toInt(),1,10)
        // Hat band star
        px(5,4,0xFFFFFF00.toInt()); px(6,3,0xFFFFFF00.toInt()); px(7,4,0xFFFFFF00.toInt())
    }

    private fun drawWerewolf(canvas: Canvas, x: Float, y: Float, p: Float) {
        val bob = (sin(idleTick * 0.12) * p * 0.35f).toFloat()
        val fl = if (enemyFlash > 0 && enemyFlash % 2 == 0) WH else 0
        fun px(c: Int, r: Int, col: Int, w: Int = 1, h: Int = 1) { paint.color = if (fl != 0) fl else col; canvas.drawRect(x + c * p, y + r * p + bob, x + (c + w) * p, y + (r + h) * p + bob, paint) }
        // Ears
        px(3,0,WFUR,2,3); px(9,0,WFUR,2,3); px(4,1,WFURH); px(9,1,WFURH)
        // Head
        px(2,2,WFUR,10,6); px(2,2,WFURH,2,6); px(10,2,WFURH,2,6)
        // Eyes (glowing red)
        px(3,3,WEYE,2,2); px(9,3,WEYE,2,2); px(4,4,0xFFFF4400.toInt()); px(9,4,0xFFFF4400.toInt())
        // Snout
        px(4,6,WFUR,6,3); px(5,7,0xFF221100.toInt(),4); px(6,7,0xFF111111.toInt(),2)
        // Fangs
        px(5,9,WH,1,2); px(8,9,WH,1,2)
        // Neck
        px(4,8,WFUR,6,2)
        // Body (burly)
        px(1,10,WFUR,12,7); px(1,10,WFURH,2,7); px(11,10,WFURH,2,7)
        px(4,11,0xFF221100.toInt(),6,3)
        // Arms (thick)
        px(0,10,WFUR,2,8); px(12,10,WFUR,2,8)
        // Claws
        px(0,17,WCLW); px(0,18,WCLW); px(1,18,WCLW); px(13,17,WCLW); px(13,18,WCLW); px(14,18,WCLW)
        // Belt
        px(1,17,0xFF332211.toInt(),12); px(2,17,0xFF887744.toInt(),4); px(7,17,0xFF887744.toInt(),3)
        // Legs
        px(2,18,WFUR,5,5); px(7,18,WFUR,5,5)
        // Feet/paws
        px(1,22,WFUR,6); px(7,22,WFUR,6); px(0,23,WFURH,3); px(9,23,WFURH,3)
    }
}
