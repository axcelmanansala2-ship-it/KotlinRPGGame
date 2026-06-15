package com.example.kotlinrpg

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.sin

class MapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var onZoneSelected: ((Int) -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var tick = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val animLoop = object : Runnable {
        override fun run() { tick++; invalidate(); handler.postDelayed(this, 60) }
    }

    // Zone positions as fractions of width/height
    private val zonePos = listOf(
        0.18f to 0.30f,  // 0 Dark Forest
        0.12f to 0.58f,  // 1 Blood Marsh
        0.45f to 0.52f,  // 2 Bone Plains
        0.72f to 0.25f,  // 3 Cursed Peaks
        0.80f to 0.55f,  // 4 Dragon Lair
        0.50f to 0.80f   // 5 Void Rift
    )

    private val zoneColors = listOf(
        0xFF225511.toInt(), // Dark Forest - dark green
        0xFF661111.toInt(), // Blood Marsh - blood red
        0xFF776633.toInt(), // Bone Plains - bone yellow
        0xFF223366.toInt(), // Cursed Peaks - dark blue
        0xFF882211.toInt(), // Dragon Lair - fire red
        0xFF440066.toInt()  // Void Rift - deep purple
    )

    private val zoneIcons = listOf("🌲", "🦟", "💀", "⛰", "🐉", "🌀")

    // Path connections between zones
    private val paths = listOf(0 to 1, 0 to 2, 1 to 2, 2 to 3, 2 to 4, 2 to 5, 3 to 4, 4 to 5)

    init { post { handler.post(animLoop) } }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(animLoop)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        drawBackground(canvas, w, h)
        drawPaths(canvas, w, h)
        drawZones(canvas, w, h)
        drawLegend(canvas, w, h)
    }

    private fun drawBackground(canvas: Canvas, w: Float, h: Float) {
        val bg = LinearGradient(0f, 0f, 0f, h, 0xFF050510.toInt(), 0xFF0A0518.toInt(), Shader.TileMode.CLAMP)
        paint.shader = bg; canvas.drawRect(0f, 0f, w, h, paint); paint.shader = null

        // Grid lines (dark)
        paint.color = 0x11AAAAFF.toInt(); paint.strokeWidth = 1f; paint.style = Paint.Style.STROKE
        var gx = 0f; while (gx < w) { canvas.drawLine(gx, 0f, gx, h, paint); gx += w / 12f }
        var gy = 0f; while (gy < h) { canvas.drawLine(0f, gy, w, gy, paint); gy += h / 16f }
        paint.style = Paint.Style.FILL

        // Continent shape
        paint.color = 0xFF0D1A0D.toInt()
        val land = Path()
        land.moveTo(w * 0.05f, h * 0.15f)
        land.cubicTo(w * 0.25f, h * 0.05f, w * 0.75f, h * 0.08f, w * 0.92f, h * 0.18f)
        land.cubicTo(w * 0.98f, h * 0.45f, w * 0.95f, h * 0.75f, w * 0.85f, h * 0.92f)
        land.cubicTo(w * 0.60f, h * 0.98f, w * 0.30f, h * 0.96f, w * 0.08f, h * 0.88f)
        land.cubicTo(w * 0.02f, h * 0.65f, w * 0.01f, h * 0.35f, w * 0.05f, h * 0.15f)
        canvas.drawPath(land, paint)

        // Stars
        paint.color = 0x66FFFFFF.toInt()
        val starPos = listOf(0.03f to 0.04f, 0.15f to 0.08f, 0.88f to 0.03f, 0.95f to 0.11f, 0.07f to 0.92f, 0.93f to 0.88f)
        starPos.forEach { (sx, sy) -> canvas.drawCircle(sx * w, sy * h, 2f, paint) }
    }

    private fun drawPaths(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f; paint.pathEffect = DashPathEffect(floatArrayOf(12f, 8f), (tick % 20) * 1f)
        paths.forEach { (a, b) ->
            val (ax, ay) = zonePos[a]; val (bx, by) = zonePos[b]
            val unlockA = GameState.isZoneUnlocked(a); val unlockB = GameState.isZoneUnlocked(b)
            paint.color = if (unlockA && unlockB) 0x88AA6633.toInt() else 0x33555555.toInt()
            canvas.drawLine(ax * w, ay * h, bx * w, by * h, paint)
        }
        paint.pathEffect = null; paint.style = Paint.Style.FILL
    }

    private fun drawZones(canvas: Canvas, w: Float, h: Float) {
        val zones = GameState.ZONES
        zones.forEachIndexed { i, zone ->
            val (fx, fy) = zonePos[i]
            val cx = fx * w; val cy = fy * h
            val r = w * 0.072f
            val unlocked = GameState.isZoneUnlocked(i)
            val isCurrent = i == GameState.currentZoneIndex

            // Glow for current
            if (isCurrent && unlocked) {
                val glowAlpha = ((sin(tick * 0.08) + 1.0) / 2.0 * 120 + 60).toInt()
                paint.color = (zoneColors[i] and 0x00FFFFFF) or (glowAlpha shl 24)
                canvas.drawCircle(cx, cy, r * 1.6f, paint)
            }

            // Outer ring
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f
            paint.color = if (unlocked) zoneColors[i] else 0xFF333333.toInt()
            if (isCurrent) paint.color = 0xFFFFDD00.toInt()
            canvas.drawCircle(cx, cy, r, paint); paint.style = Paint.Style.FILL

            // Fill
            paint.color = if (unlocked) (zoneColors[i] and 0x00FFFFFF) or 0xCC000000.toInt() else 0xCC1A1A1A.toInt()
            canvas.drawCircle(cx, cy, r * 0.92f, paint)

            // Lock icon or zone icon
            paint.textSize = r * 0.9f; paint.textAlign = Paint.Align.CENTER
            if (!unlocked) {
                paint.color = 0xFF444444.toInt()
                canvas.drawText("🔒", cx, cy + paint.textSize * 0.35f, paint)
            } else {
                canvas.drawText(zoneIcons[i], cx, cy + paint.textSize * 0.35f, paint)
            }

            // Zone name
            paint.textSize = r * 0.38f; paint.textAlign = Paint.Align.CENTER
            paint.color = if (unlocked) 0xFFCCBBAA.toInt() else 0xFF555555.toInt()
            if (isCurrent) paint.color = 0xFFFFDD00.toInt()
            canvas.drawText(zone.name, cx, cy + r * 1.35f, paint)

            // Unlock level if locked
            if (!unlocked) {
                paint.textSize = r * 0.30f; paint.color = 0xFF886644.toInt()
                canvas.drawText("Lv.${zone.unlockLevel}", cx, cy + r * 1.7f, paint)
            }
        }
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawLegend(canvas: Canvas, w: Float, h: Float) {
        val zone = GameState.currentZone()
        paint.color = 0xCC060610.toInt()
        canvas.drawRect(0f, h - h * 0.12f, w, h, paint)
        paint.color = 0xFFFFDD88.toInt(); paint.textSize = h * 0.032f; paint.typeface = Typeface.MONOSPACE
        canvas.drawText("Current: ${zone.name}  •  ${zone.description}", w * 0.03f, h - h * 0.072f, paint)
        paint.color = 0xFF889988.toInt(); paint.textSize = h * 0.026f
        canvas.drawText("Tap a zone to travel there", w * 0.03f, h - h * 0.038f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val w = width.toFloat(); val h = height.toFloat()
        val r = w * 0.072f
        zonePos.forEachIndexed { i, (fx, fy) ->
            val dx = event.x - fx * w; val dy = event.y - fy * h
            if (dx * dx + dy * dy <= r * r * 2.5f) {
                if (GameState.isZoneUnlocked(i)) {
                    GameState.currentZoneIndex = i; GameState.save()
                    onZoneSelected?.invoke(i); invalidate()
                }
                return true
            }
        }
        return true
    }
}
