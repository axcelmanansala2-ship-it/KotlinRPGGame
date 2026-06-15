package com.example.kotlinrpg

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        // Hero
        val SKIN   = 0xFFFFCC88.toInt()
        val HARMO  = 0xFF2255CC.toInt()
        val HDARK  = 0xFF102288.toInt()
        val HGOLD  = 0xFFFFCC00.toInt()
        val EYE_B  = 0xFF1133FF.toInt()
        val MOUTH  = 0xFF221100.toInt()
        // Slime
        val SLG    = 0xFF33BB33.toInt()
        val SLGH   = 0xFF99FF99.toInt()
        val SLDK   = 0xFF227722.toInt()
        // Goblin
        val GOBSK  = 0xFF44AA33.toInt()
        val GOBDK  = 0xFF225511.toInt()
        val GOBBR  = 0xFF996633.toInt()
        val GOBYE  = 0xFFFFFF00.toInt()
        // Skeleton
        val BONE   = 0xFFEEEECC.toInt()
        val BSHD   = 0xFFBBBBAA.toInt()
        val BRED   = 0xFFFF2200.toInt()
        // Dark Knight
        val DKARM  = 0xFF222244.toInt()
        val DKSHD  = 0xFF111133.toInt()
        val DKRED  = 0xFFAA0000.toInt()
        val DKGLD  = 0xFFAA8800.toInt()
        val STEEL  = 0xFFCCCCDD.toInt()
        // Dragon
        val DRGSC  = 0xFF336633.toInt()
        val DRGHI  = 0xFF55AA55.toInt()
        val DRGDK  = 0xFF224422.toInt()
        val DRGWN  = 0xFF558833.toInt()
        val DRGBL  = 0xFFFFCC44.toInt()
        val DRGFR  = 0xFFFF4411.toInt()
        val DRGEY  = 0xFFFFFF00.toInt()
        // General
        val BK     = 0xFF111111.toInt()
        val WH     = 0xFFFFFFFF.toInt()
        val FIREO  = 0xFFFF8800.toInt()
        val FIREY  = 0xFFFFEE00.toInt()
    }

    data class EnemyDef(
        val name: String, val maxHp: Int, val attack: Int,
        val defense: Int, val expReward: Int, val spriteId: Int
    )

    val STAGES = listOf(
        EnemyDef("Slime",       40,  10, 0,  30,  0),
        EnemyDef("Goblin",      75,  18, 3,  60,  1),
        EnemyDef("Skeleton",   120,  26, 6,  95,  2),
        EnemyDef("Dark Knight",170,  38, 14, 145, 3),
        EnemyDef("Dragon",     270,  50, 18, 350, 4)
    )

    // Player
    var playerMaxHp = 100; var playerHp = 100
    var playerAtk = 15;    var playerDef = 5
    var playerLevel = 1;   var playerExp = 0; var playerExpNext = 100
    var playerPotions = 3

    // Enemy
    var currentStage = 0
    var enemyHp = 0;       var enemyMaxHp = 0
    var currentEnemy: EnemyDef? = null

    enum class Phase { PLAYER_TURN, PLAYER_ATTACKING, ENEMY_ATTACKING, STAGE_CLEAR, GAME_OVER }
    var phase = Phase.PLAYER_TURN

    // Animation
    var heroOffX = 0f;  var heroOffY = 0f
    var enemyOffX = 0f; var enemyOffY = 0f
    var heroFlash = 0;  var enemyFlash = 0
    var idleTick = 0L
    var animStep = 0

    // Callbacks
    var onLog: ((String) -> Unit)? = null
    var onStatsChanged: ((Int, Int, Int) -> Unit)? = null
    var onStageClear: ((Int, Int) -> Unit)? = null
    var onGameOver: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())

    private val runFrame = object : Runnable {
        override fun run() {
            idleTick++
            invalidate()
            postDelayed(this, 50)
        }
    }

    init { post { handler.post(runFrame) } }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(runFrame)
    }

    private fun ps() = width / 96f  // pixel size

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val p = ps()
        val groundY = h * 0.75f

        drawBackground(canvas, w, h, groundY, p)

        val heroBaseX = w * 0.04f
        val heroBaseY = groundY - p * 20f
        drawHero(canvas, heroBaseX + heroOffX, heroBaseY + heroOffY, p,
            if (heroFlash > 0 && heroFlash % 2 == 0) WH else 0)

        val enemy = currentEnemy ?: return
        val esh = enemySpriteH() * p
        val enemyBaseX = w * 0.54f
        val enemyBaseY = groundY - esh
        if (enemyHp > 0)
            drawEnemy(canvas, enemyBaseX + enemyOffX, enemyBaseY + enemyOffY, p,
                if (enemyFlash > 0 && enemyFlash % 2 == 0) WH else 0)

        // HP Bars
        val barH = p * 2.5f
        drawBar(canvas, w*0.02f, groundY+p*1f, w*0.44f, barH,
            playerHp, playerMaxHp, 0xFF33CC33.toInt(), "HERO")
        drawBar(canvas, w*0.54f, groundY+p*1f, w*0.44f, barH,
            enemyHp, enemyMaxHp, 0xFFCC3333.toInt(), enemy.name)

        if (heroFlash > 0) heroFlash--
        if (enemyFlash > 0) enemyFlash--
    }

    private fun drawBar(c: Canvas, x:Float, y:Float, w:Float, h:Float, hp:Int, max:Int, color:Int, name:String){
        if(max<=0) return
        paint.color = 0xFF111111.toInt(); c.drawRect(x,y,x+w,y+h,paint)
        val pct = hp.toFloat()/max
        paint.color = if(pct>0.4f) color else 0xFFCC3311.toInt()
        c.drawRect(x,y,x+w*pct,y+h,paint)
        paint.color = 0xFF444444.toInt(); paint.style=Paint.Style.STROKE; paint.strokeWidth=1.5f
        c.drawRect(x,y,x+w,y+h,paint); paint.style=Paint.Style.FILL
        paint.color=WH; paint.textSize=h*0.72f; paint.typeface=Typeface.MONOSPACE
        c.drawText("$name $hp/$max",x+3f,y+h-2f,paint)
    }

    private fun enemySpriteH() = when(currentEnemy?.spriteId){ 0->9f; 1->18f; 2->21f; 3->25f; else->22f }

    private fun drawBackground(canvas: Canvas, w:Float, h:Float, gy:Float, p:Float){
        val skyShader = LinearGradient(0f,0f,0f,gy, 0xFF060618.toInt(), 0xFF1A3060.toInt(), Shader.TileMode.CLAMP)
        paint.shader = skyShader; canvas.drawRect(0f,0f,w,gy,paint); paint.shader=null

        // Stars
        paint.color = 0xFFFFFFFF.toInt()
        listOf(0.08f to 0.06f, 0.22f to 0.13f, 0.38f to 0.04f, 0.55f to 0.09f,
               0.71f to 0.15f, 0.87f to 0.05f, 0.12f to 0.28f, 0.44f to 0.22f,
               0.78f to 0.3f,  0.93f to 0.18f, 0.3f to 0.38f,  0.62f to 0.35f).forEach{ (sx,sy) ->
            canvas.drawRect(sx*w, sy*gy, sx*w+p, sy*gy+p, paint)
        }

        // Moon
        paint.color = 0xFFFFFFAA.toInt()
        canvas.drawCircle(w*0.82f, gy*0.18f, p*4.5f, paint)
        paint.color = 0xFF060618.toInt()
        canvas.drawCircle(w*0.84f+p, gy*0.16f, p*3.3f, paint)

        // Mountains
        paint.color = 0xFF252540.toInt()
        val mtn = Path()
        mtn.moveTo(0f, gy)
        listOf(0.0f to 1.0f, 0.08f to 0.55f, 0.18f to 0.85f, 0.28f to 0.48f,
               0.38f to 0.78f, 0.5f to 0.42f, 0.62f to 0.75f, 0.72f to 0.52f,
               0.83f to 0.8f, 0.92f to 0.47f, 1.0f to 0.78f).forEach{ (mx,my) ->
            mtn.lineTo(mx*w, my*gy)
        }
        mtn.lineTo(w, gy); mtn.close(); canvas.drawPath(mtn, paint)

        // Ground
        val gndShader = LinearGradient(0f,gy,0f,h, 0xFF2D1B0A.toInt(), 0xFF1A0D05.toInt(), Shader.TileMode.CLAMP)
        paint.shader = gndShader; canvas.drawRect(0f,gy,w,h,paint); paint.shader=null
        paint.color = 0xFF2D5A27.toInt(); canvas.drawRect(0f,gy,w,gy+p*2,paint)
        paint.color = 0xFF3A7A33.toInt()
        var gx=0f; while(gx<w){ canvas.drawRect(gx,gy-p,gx+p,gy,paint); gx+=p*4 }

        // Ground grid lines (pixel art floor)
        paint.color = 0xFF3D2510.toInt()
        var fx=0f; while(fx<w){ canvas.drawRect(fx,gy+p*2,fx+1,h,paint); fx+=p*6 }
    }

    // ═══════════════════════════════════════════════════
    //   HERO SPRITE  (12 wide × 20 tall)
    // ═══════════════════════════════════════════════════
    private fun drawHero(canvas: Canvas, x:Float, y:Float, p:Float, flash:Int){
        val bob = (sin(idleTick*0.18)*p*0.6f).toFloat()
        fun px(c:Int,r:Int,color:Int,w:Int=1,h:Int=1){
            paint.color = if(flash!=0) flash else color
            canvas.drawRect(x+c*p, y+r*p+bob, x+(c+w)*p, y+(r+h)*p+bob, paint)
        }
        // helmet gold crown
        px(2,0,HGOLD,8)
        // helmet row 1: gold|armor|gold
        px(2,1,HGOLD); px(3,1,HARMO,6); px(9,1,HGOLD)
        // visor row 2: gold|dark|gold
        px(2,2,HGOLD); px(3,2,HDARK,6); px(9,2,HGOLD)
        // visor slit eyes (glow white)
        px(4,2,0xFFAABBFF.toInt(),2); px(7,2,0xFFAABBFF.toInt(),2)
        // helmet row 3: gold|armor|gold
        px(2,3,HGOLD); px(3,3,HARMO,6); px(9,3,HGOLD)
        // neck/face (rows 4-7)
        px(2,4,SKIN,8,3)
        // eyes
        px(3,5,EYE_B,2); px(7,5,EYE_B,2)
        px(3,5,BK); px(8,5,BK)     // pupils
        // brows
        px(3,4,MOUTH,3); px(7,4,MOUTH,3)
        // mouth
        px(4,7,MOUTH,1); px(7,7,MOUTH,1)
        px(2,7,SKIN,2); px(6,7,SKIN,2); px(9,7,SKIN)
        // neck
        px(4,8,SKIN,4)
        // shoulders (full width gold strip)
        px(0,9,HGOLD,12)
        // pauldrons (raised shoulder pads)
        px(0,8,HGOLD,3,2); px(9,8,HGOLD,3,2)
        // body armor rows 10-13
        px(0,10,HGOLD,2,4); px(10,10,HGOLD,2,4)   // side trim
        px(2,10,HARMO,8,4)
        // chest detail
        px(4,10,HDARK,4,2)    // chest gem area
        px(5,11,HGOLD,2)      // gem
        // belt
        px(2,14,HDARK,8); px(3,14,HGOLD,6)
        // legs
        px(2,15,HDARK,4,4); px(6,15,HDARK,4,4)
        // leg detail
        px(2,17,HGOLD,2); px(8,17,HGOLD,2)
        // boots
        px(1,19,HDARK,5); px(6,19,HDARK,5)
        px(1,19,HGOLD,1); px(10,19,HGOLD,1)

        // Attack pose: sword arm
        if(phase==Phase.PLAYER_ATTACKING && animStep>0){
            // Sword extending right
            px(12,8,HGOLD,1,3)     // handle
            px(11,9,HGOLD,3)       // guard
            px(12,5,STEEL,1,4)     // blade
            px(12,4,WH)            // tip
        }
    }

    // ═══════════════════════════════════════════════════
    //   SLIME SPRITE (12 wide × 9 tall)
    // ═══════════════════════════════════════════════════
    private fun drawSlime(canvas: Canvas, x:Float, y:Float, p:Float, flash:Int){
        val bob=(sin(idleTick*0.14)*p*0.5f).toFloat()
        val squish=(sin(idleTick*0.14)*p*0.3f).toFloat()
        fun px(c:Int,r:Int,color:Int,w:Int=1,h:Int=1){
            paint.color=if(flash!=0) flash else color
            canvas.drawRect(x+c*p,y+r*p+bob,x+(c+w)*p,y+(r+h)*p+bob+squish,paint)
        }
        px(3,0,SLG,6)
        px(1,1,SLG,10)
        px(0,2,SLG,12); px(1,1,SLGH,4); px(7,1,SLGH,2)
        px(0,3,SLG,12)
        // eyes
        px(1,3,BK,3,2); px(8,3,BK,3,2)
        px(2,3,SLGH); px(9,3,SLGH)
        px(0,4,SLG,12); px(0,5,SLG,12)
        px(1,6,SLG,10)
        px(3,7,SLDK,2); px(7,7,SLDK,2)   // tentacle stubs
        px(4,8,SLDK,4)
    }

    // ═══════════════════════════════════════════════════
    //   GOBLIN SPRITE (12 wide × 18 tall)
    // ═══════════════════════════════════════════════════
    private fun drawGoblin(canvas: Canvas, x:Float, y:Float, p:Float, flash:Int){
        val bob=(sin(idleTick*0.16)*p*0.4f).toFloat()
        fun px(c:Int,r:Int,color:Int,w:Int=1,h:Int=1){
            paint.color=if(flash!=0) flash else color
            canvas.drawRect(x+c*p,y+r*p+bob,x+(c+w)*p,y+(r+h)*p+bob,paint)
        }
        // big ears
        px(0,2,GOBSK,2,3); px(10,2,GOBSK,2,3)
        px(0,1,GOBSK); px(11,1,GOBSK)
        // head
        px(2,0,GOBSK,8,7)
        // hair
        px(2,0,GOBDK,2); px(4,0,GOBSK,2); px(6,0,GOBDK,4)
        // eyes (angry)
        px(3,2,GOBYE,2); px(8,2,GOBYE,2)
        px(3,2,BK); px(9,2,BK)
        px(3,1,GOBDK,3); px(7,1,GOBDK,3)   // angry brows
        // big nose
        px(5,3,GOBDK,2,2)
        // grin
        px(3,6,BK,7)
        px(4,6,WH); px(6,6,WH); px(8,6,WH)   // teeth
        // neck
        px(4,7,GOBSK,4,1)
        // body (ragged tunic)
        px(3,8,GOBBR,6,5)
        px(1,8,GOBSK,2,5)   // left arm
        px(9,8,GOBSK,2,5)   // right arm
        px(4,9,BK,4)        // tunic seam
        // dagger in right hand
        px(10,11,STEEL,1,3); px(9,11,GOBBR,1,1); px(10,10,HGOLD)
        // legs
        px(3,13,GOBDK,3,5); px(6,13,GOBDK,3,5)
        px(2,17,GOBDK,4); px(6,17,GOBDK,5)   // big feet
    }

    // ═══════════════════════════════════════════════════
    //   SKELETON SPRITE (12 wide × 21 tall)
    // ═══════════════════════════════════════════════════
    private fun drawSkeleton(canvas: Canvas, x:Float, y:Float, p:Float, flash:Int){
        val bob=(sin(idleTick*0.12)*p*0.3f).toFloat()
        fun px(c:Int,r:Int,color:Int,w:Int=1,h:Int=1){
            paint.color=if(flash!=0) flash else color
            canvas.drawRect(x+c*p,y+r*p+bob,x+(c+w)*p,y+(r+h)*p+bob,paint)
        }
        // skull
        px(3,0,BONE,6); px(2,1,BONE,8,4)
        // eye sockets (glowing red)
        px(3,2,BRED,2,2); px(7,2,BRED,2,2)
        px(3,2,0xFFFF6644.toInt()); px(8,2,0xFFFF6644.toInt())   // glow inner
        // cheekbones
        px(2,4,BSHD,2); px(8,4,BSHD,2)
        // nose cavity
        px(5,3,BK,2,2)
        // teeth (top jaw)
        px(3,5,BONE,6)
        // jaw
        px(3,6,BONE,6,2)
        px(3,7,BK,1,1); px(5,7,BK,1,1); px(7,7,BK,1,1)   // gaps between teeth
        // neck spine
        px(5,8,BONE,2,2)
        // ribcage
        px(2,10,BONE,8,5)
        for(i in 0..4){ px(2,10+i,BSHD); px(9,10+i,BSHD) }  // shadow sides
        // rib detail
        px(3,10,BK); px(8,10,BK); px(3,12,BK); px(8,12,BK); px(3,14,BK); px(8,14,BK)
        // arms (thin bones)
        px(0,10,BONE,2,8); px(10,10,BONE,2,8)
        // hands
        px(0,18,BONE); px(1,18,BSHD)
        px(10,18,BONE); px(11,18,BSHD)
        // pelvis
        px(2,15,BONE,8,2)
        // legs (bone columns)
        px(2,17,BONE,3,4); px(7,17,BONE,3,4)
        // feet
        px(1,21,BONE,4); px(7,21,BONE,4)
    }

    // ═══════════════════════════════════════════════════
    //   DARK KNIGHT SPRITE (14 wide × 25 tall)
    // ═══════════════════════════════════════════════════
    private fun drawDarkKnight(canvas: Canvas, x:Float, y:Float, p:Float, flash:Int){
        val bob=(sin(idleTick*0.10)*p*0.2f).toFloat()
        fun px(c:Int,r:Int,color:Int,w:Int=1,h:Int=1){
            paint.color=if(flash!=0) flash else color
            canvas.drawRect(x+c*p,y+r*p+bob,x+(c+w)*p,y+(r+h)*p+bob,paint)
        }
        // horns
        px(3,0,DKARM,2,3); px(9,0,DKARM,2,3)
        px(3,-1,DKARM); px(10,-1,DKARM)   // horn tips
        // gold crown band
        px(2,3,DKGLD,10)
        // helmet (big dark)
        px(1,4,DKARM,12,7)
        px(1,4,DKSHD,2,7); px(11,4,DKSHD,2,7)   // side shadow
        // visor eye slit (glowing red)
        px(3,6,DKRED,4); px(7,6,DKRED,4)
        px(3,6,0xFFFF4444.toInt(),2); px(9,6,0xFFFF4444.toInt(),2)
        // visor lines
        px(2,5,DKSHD,10); px(2,10,DKSHD,10)
        // neck
        px(4,11,DKARM,6,2)
        // shoulder pads (wide + spiky)
        px(0,12,DKARM,4,4); px(10,12,DKARM,4,4)
        px(0,12,DKGLD,4); px(10,12,DKGLD,4)     // gold top
        px(1,11,DKARM,2,2); px(11,11,DKARM,2,2) // spike up
        // chest
        px(1,13,DKARM,12,8)
        px(1,13,DKSHD,2,8); px(11,13,DKSHD,2,8)
        // chest rune (cross)
        px(5,14,DKRED,4,5)
        px(4,16,DKRED,6)
        // gold waist
        px(1,21,DKGLD,12)
        // arms
        px(0,15,DKARM,2,6); px(12,15,DKARM,2,6)
        // gauntlets
        px(0,21,DKGLD,3,2); px(11,21,DKGLD,3,2)
        // sword (right side)
        px(13,17,DKGLD,1,2)   // pommel
        px(13,19,DKGLD,1,3)   // grip
        px(12,22,DKGLD,3)     // crossguard
        px(13,23,STEEL,1,8)   // blade
        px(13,31,WH)           // tip
        // legs
        px(2,22,DKARM,4,5); px(8,22,DKARM,4,5)
        // boots
        px(1,27,DKSHD,6); px(7,27,DKSHD,6)
        px(1,28,DKARM,7); px(7,28,DKARM,7)
    }

    // ═══════════════════════════════════════════════════
    //   DRAGON SPRITE (26 wide × 22 tall)
    // ═══════════════════════════════════════════════════
    private fun drawDragon(canvas: Canvas, x:Float, y:Float, p:Float, flash:Int){
        val bob=(sin(idleTick*0.08)*p*0.6f).toFloat()
        fun px(c:Int,r:Int,color:Int,w:Int=1,h:Int=1){
            paint.color=if(flash!=0) flash else color
            canvas.drawRect(x+c*p,y+r*p+bob,x+(c+w)*p,y+(r+h)*p+bob,paint)
        }
        // Wings (background)
        px(0,0,DRGDK,7,2)
        px(0,2,DRGWN,6,3); px(0,5,DRGWN,5,2); px(0,7,DRGWN,4,2)
        px(18,0,DRGDK,8,2)
        px(20,2,DRGWN,7,3); px(21,5,DRGWN,5,2); px(22,7,DRGWN,4,2)
        // Wing membranes veins
        px(2,2,DRGDK,1,3); px(4,2,DRGDK,1,4)
        px(21,2,DRGDK,1,3); px(23,2,DRGDK,1,4)
        // Tail
        px(0,9,DRGSC,4,2); px(0,11,DRGSC,3,2); px(1,13,DRGSC,2); px(2,14,DRGSC)
        // Body (big chunky)
        px(5,5,DRGSC,15,10)
        // Scale highlights
        px(6,6,DRGHI,4,2); px(12,6,DRGHI,4,2)
        px(7,9,DRGHI,3,2); px(13,9,DRGHI,3,2)
        px(10,7,DRGHI,2,1)
        // Belly
        px(7,7,DRGBL,11,6)
        px(8,8,0xFFEEBB00.toInt(),4); px(13,8,0xFFEEBB00.toInt(),3)
        px(9,10,0xFFEEBB00.toInt(),7)
        // Neck
        px(11,2,DRGSC,7,4); px(12,1,DRGSC,5,2)
        // Head
        px(14,0,DRGSC,11,6)
        px(13,1,DRGSC,12,4)
        // Snout
        px(21,1,DRGSC,5,4)
        // nostril
        px(23,1,DRGDK,2)
        // Eye (big yellow)
        px(15,0,DRGEY,4,4)
        px(16,1,BK,2,2)        // pupil (slit)
        px(15,1,0xFFFFFF88.toInt(),1)  // highlight
        // Horn
        px(14,0,DRGSC,2,2)
        px(14,-1,DRGSC,1,2)
        px(15,-2,DRGSC,1,1)
        // Teeth (lower jaw)
        px(22,5,WH,1,2); px(24,5,WH,1,2); px(23,4,WH,1,2)
        // FIRE BREATH (during attack)
        if(phase==Phase.ENEMY_ATTACKING && animStep>=1){
            val fireColors = listOf(FIREY,FIREO,DRGFR,FIREO,FIREY,FIREO,DRGFR)
            for(i in 0..6){ px(26+i*2,2+(i%3),fireColors[i],2,2) }
            px(26,1,FIREY,3)
        }
        // Front legs
        px(6,14,DRGSC,5,5); px(14,14,DRGSC,5,5)
        // Claws
        px(5,18,DRGDK,2); px(8,18,DRGDK,2); px(11,18,DRGDK,2)
        px(13,18,DRGDK,2); px(16,18,DRGDK,2); px(19,18,DRGDK,2)
    }

    private fun drawEnemy(canvas: Canvas, x:Float, y:Float, p:Float, flash:Int){
        when(currentEnemy?.spriteId){
            0 -> drawSlime(canvas, x, y, p, flash)
            1 -> drawGoblin(canvas, x, y, p, flash)
            2 -> drawSkeleton(canvas, x, y, p, flash)
            3 -> drawDarkKnight(canvas, x, y, p, flash)
            4 -> drawDragon(canvas, x, y, p, flash)
        }
    }

    // ═══════════════════════════════════════════════════
    //   GAME LOGIC
    // ═══════════════════════════════════════════════════
    fun startGame() { loadStage(0) }

    fun loadStage(stage: Int) {
        currentStage = stage
        currentEnemy = STAGES[stage]
        enemyMaxHp = STAGES[stage].maxHp; enemyHp = enemyMaxHp
        heroOffX=0f; heroOffY=0f; enemyOffX=0f; enemyOffY=0f
        heroFlash=0; enemyFlash=0; animStep=0
        phase = Phase.PLAYER_TURN
        onStatsChanged?.invoke(playerHp, enemyHp, playerPotions)
    }

    fun playerAttack() {
        if(phase != Phase.PLAYER_TURN) return
        phase = Phase.PLAYER_ATTACKING; animStep=0
        animHeroAttack()
    }

    fun playerDefend() {
        if(phase != Phase.PLAYER_TURN) return
        onLog?.invoke("You raise your shield! (damage -80%)")
        animEnemyAttack(defending=true)
    }

    fun playerHeal() {
        if(phase != Phase.PLAYER_TURN || playerPotions<=0) return
        val heal = minOf((playerMaxHp*0.30).toInt(), playerMaxHp-playerHp)
        playerHp += heal; playerPotions--
        onLog?.invoke("You drink a potion! +$heal HP  (${playerPotions} left)")
        onStatsChanged?.invoke(playerHp, enemyHp, playerPotions)
        animEnemyAttack(defending=false)
    }

    private fun animHeroAttack() {
        val steps = 10; var step = 0
        val targetOffset = width * 0.42f
        fun frame() {
            step++; animStep = step
            when {
                step <= steps/2 -> {
                    heroOffX = targetOffset * (step.toFloat()/(steps/2))
                    handler.postDelayed(::frame, 35)
                }
                step == steps/2+1 -> {
                    val crit = Random.nextFloat() < 0.15f
                    val raw  = maxOf(1, playerAtk - (currentEnemy?.defense?:0) + Random.nextInt(-3,4))
                    val dmg  = if(crit) (raw*1.8).toInt() else raw
                    enemyHp  = maxOf(0, enemyHp-dmg)
                    enemyFlash = 10
                    onLog?.invoke(if(crit) "★ CRITICAL HIT! $dmg damage!" else "Hero attacks for $dmg damage!")
                    onStatsChanged?.invoke(playerHp, enemyHp, playerPotions)
                    handler.postDelayed(::frame, 100)
                }
                step <= steps -> {
                    heroOffX = targetOffset * (1f - (step-steps/2-1).toFloat()/(steps/2))
                    handler.postDelayed(::frame, 35)
                }
                else -> {
                    heroOffX=0f; animStep=0
                    if(enemyHp<=0) onEnemyDefeated() else animEnemyAttack(defending=false)
                }
            }
        }
        handler.postDelayed(::frame, 50)
    }

    private fun animEnemyAttack(defending: Boolean) {
        phase = Phase.ENEMY_ATTACKING
        val steps=10; var step=0
        val targetOffset = -(width*0.34f)
        handler.postDelayed({
            fun frame(){
                step++; animStep=step
                when{
                    step<=steps/2 -> {
                        enemyOffX = targetOffset*(step.toFloat()/(steps/2))
                        handler.postDelayed(::frame, 35)
                    }
                    step==steps/2+1 -> {
                        val raw  = maxOf(1, (currentEnemy?.attack?:10) - playerDef + Random.nextInt(-3,4))
                        val dmg  = if(defending) maxOf(1,(raw*0.2).toInt()) else raw
                        playerHp = maxOf(0, playerHp-dmg)
                        heroFlash = 10
                        val msg  = if(defending) "Blocked! Only $dmg damage taken!" else "${currentEnemy?.name} hits for $dmg damage!"
                        onLog?.invoke(msg)
                        onStatsChanged?.invoke(playerHp, enemyHp, playerPotions)
                        handler.postDelayed(::frame, 100)
                    }
                    step<=steps -> {
                        enemyOffX = targetOffset*(1f-(step-steps/2-1).toFloat()/(steps/2))
                        handler.postDelayed(::frame, 35)
                    }
                    else -> {
                        enemyOffX=0f; animStep=0
                        if(playerHp<=0){ phase=Phase.GAME_OVER; onGameOver?.invoke() }
                        else { phase=Phase.PLAYER_TURN }
                    }
                }
            }
            frame()
        }, 350)
    }

    private fun onEnemyDefeated() {
        val exp = currentEnemy?.expReward?:0
        playerExp += exp; onLog?.invoke("${currentEnemy?.name} defeated!  +$exp EXP")
        while(playerExp >= playerExpNext){
            playerLevel++; playerExp -= playerExpNext
            playerExpNext = (playerExpNext*1.5).toInt()
            playerMaxHp+=20; playerHp=minOf(playerHp+25,playerMaxHp)
            playerAtk+=5; playerDef+=2
            onLog?.invoke("★ LEVEL UP! Lv.$playerLevel  ATK+5  DEF+2  HP+20")
        }
        onStageClear?.invoke(exp, playerLevel)
    }
}
