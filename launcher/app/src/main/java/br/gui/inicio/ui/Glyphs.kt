package br.gui.inicio.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

enum class Glyph { GEAR, INPUT, GRID, WIFI, BLUETOOTH, CONTROLS, FOCUS, KEYSTONE, ZOOM, PROJECTION, SOUND, CHECK, TIMER, LOCATION }

/** Ícones desenhados em código, para não depender de fontes de símbolos que o aparelho não tem. */
object Glyphs {
    fun draw(c: Canvas, g: Glyph, cx: Float, cy: Float, size: Float, color: Int) {
        val r = size / 2
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = size * 0.08f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val f = Paint(p).apply { style = Paint.Style.FILL }
        when (g) {
            Glyph.GEAR -> {
                c.drawCircle(cx, cy, r * 0.62f, p)
                c.drawCircle(cx, cy, r * 0.28f, p)
                for (i in 0 until 8) {
                    c.save()
                    c.rotate(i * 45f, cx, cy)
                    c.drawRoundRect(cx - r * 0.13f, cy - r * 0.95f, cx + r * 0.13f, cy - r * 0.6f, r * 0.05f, r * 0.05f, f)
                    c.restore()
                }
            }
            Glyph.INPUT -> {
                c.drawRoundRect(cx - r * 0.9f, cy - r * 0.6f, cx + r * 0.9f, cy + r * 0.45f, r * 0.12f, r * 0.12f, p)
                c.drawLine(cx - r * 0.35f, cy + r * 0.8f, cx + r * 0.35f, cy + r * 0.8f, p)
                c.drawLine(cx - r * 0.4f, cy - r * 0.08f, cx + r * 0.25f, cy - r * 0.08f, p)
                c.drawLine(cx + r * 0.05f, cy - r * 0.3f, cx + r * 0.27f, cy - r * 0.08f, p)
                c.drawLine(cx + r * 0.05f, cy + r * 0.14f, cx + r * 0.27f, cy - r * 0.08f, p)
            }
            Glyph.GRID -> {
                val q = r * 0.42f
                val gap = r * 0.62f
                for (i in -1..1) for (j in -1..1) {
                    val x = cx + i * gap
                    val y = cy + j * gap
                    c.drawRoundRect(x - q / 2, y - q / 2, x + q / 2, y + q / 2, q * 0.28f, q * 0.28f, f)
                }
            }
            Glyph.WIFI -> {
                val base = cy + r * 0.55f
                for (k in 1..3) {
                    val rr = r * 0.32f * k
                    c.drawArc(cx - rr, base - rr, cx + rr, base + rr, 225f, 90f, false, p)
                }
                c.drawCircle(cx, base, r * 0.1f, f)
            }
            Glyph.BLUETOOTH -> {
                val path = Path().apply {
                    moveTo(cx - r * 0.45f, cy - r * 0.4f)
                    lineTo(cx + r * 0.4f, cy + r * 0.38f)
                    lineTo(cx, cy + r * 0.8f)
                    lineTo(cx, cy - r * 0.8f)
                    lineTo(cx + r * 0.4f, cy - r * 0.38f)
                    lineTo(cx - r * 0.45f, cy + r * 0.4f)
                }
                c.drawPath(path, p)
            }
            Glyph.CONTROLS -> {
                for ((dy, knobLeft) in listOf(-0.4f to false, 0.4f to true)) {
                    val y = cy + r * dy
                    c.drawRoundRect(cx - r * 0.85f, y - r * 0.27f, cx + r * 0.85f, y + r * 0.27f, r * 0.27f, r * 0.27f, p)
                    c.drawCircle(if (knobLeft) cx - r * 0.58f else cx + r * 0.58f, y, r * 0.15f, f)
                }
            }
            Glyph.FOCUS -> {
                c.drawCircle(cx, cy, r * 0.3f, p)
                c.drawCircle(cx, cy, r * 0.72f, p)
                for (i in 0 until 4) {
                    c.save()
                    c.rotate(i * 90f, cx, cy)
                    c.drawLine(cx, cy - r * 0.98f, cx, cy - r * 0.82f, p)
                    c.restore()
                }
            }
            Glyph.KEYSTONE -> {
                val path = Path().apply {
                    moveTo(cx - r * 0.6f, cy - r * 0.6f)
                    lineTo(cx + r * 0.6f, cy - r * 0.6f)
                    lineTo(cx + r * 0.9f, cy + r * 0.6f)
                    lineTo(cx - r * 0.9f, cy + r * 0.6f)
                    close()
                }
                c.drawPath(path, p)
            }
            Glyph.ZOOM -> {
                c.drawRect(cx - r * 0.38f, cy - r * 0.38f, cx + r * 0.38f, cy + r * 0.38f, p)
                for ((sx, sy) in listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)) {
                    c.drawLine(cx + sx * r * 0.58f, cy + sy * r * 0.58f, cx + sx * r * 0.92f, cy + sy * r * 0.92f, p)
                }
            }
            Glyph.PROJECTION -> {
                c.drawRoundRect(cx - r * 0.85f, cy - r * 0.3f, cx + r * 0.25f, cy + r * 0.45f, r * 0.1f, r * 0.1f, p)
                c.drawCircle(cx - r * 0.3f, cy + r * 0.07f, r * 0.18f, p)
                c.drawLine(cx + r * 0.4f, cy - r * 0.1f, cx + r * 0.9f, cy - r * 0.5f, p)
                c.drawLine(cx + r * 0.4f, cy + r * 0.2f, cx + r * 0.9f, cy + r * 0.45f, p)
            }
            Glyph.SOUND -> {
                val path = Path().apply {
                    moveTo(cx - r * 0.9f, cy - r * 0.25f)
                    lineTo(cx - r * 0.5f, cy - r * 0.25f)
                    lineTo(cx - r * 0.1f, cy - r * 0.65f)
                    lineTo(cx - r * 0.1f, cy + r * 0.65f)
                    lineTo(cx - r * 0.5f, cy + r * 0.25f)
                    lineTo(cx - r * 0.9f, cy + r * 0.25f)
                    close()
                }
                c.drawPath(path, f)
                for (k in 1..2) {
                    val rr = r * 0.32f * k
                    c.drawArc(cx + r * 0.05f - rr, cy - rr, cx + r * 0.05f + rr, cy + rr, -45f, 90f, false, p)
                }
            }
            Glyph.TIMER -> {
                c.drawCircle(cx, cy + r * 0.08f, r * 0.72f, p)
                c.drawLine(cx, cy + r * 0.08f, cx, cy - r * 0.32f, p)
                c.drawLine(cx, cy + r * 0.08f, cx + r * 0.3f, cy + r * 0.25f, p)
                c.drawLine(cx - r * 0.2f, cy - r * 0.9f, cx + r * 0.2f, cy - r * 0.9f, p)
            }
            Glyph.LOCATION -> {
                val path = Path().apply {
                    moveTo(cx, cy + r * 0.85f)
                    cubicTo(cx - r * 0.95f, cy - r * 0.05f, cx - r * 0.55f, cy - r * 0.85f, cx, cy - r * 0.85f)
                    cubicTo(cx + r * 0.55f, cy - r * 0.85f, cx + r * 0.95f, cy - r * 0.05f, cx, cy + r * 0.85f)
                }
                c.drawPath(path, p)
                c.drawCircle(cx, cy - r * 0.25f, r * 0.2f, f)
            }
            Glyph.CHECK -> {
                c.drawLine(cx - r * 0.5f, cy, cx - r * 0.1f, cy + r * 0.4f, p)
                c.drawLine(cx - r * 0.1f, cy + r * 0.4f, cx + r * 0.55f, cy - r * 0.4f, p)
            }
        }
    }
}

/** View que desenha um [Glyph] centralizado no tamanho da própria view. */
class GlyphView(ctx: Context, glyph: Glyph) : View(ctx) {
    var glyph: Glyph = glyph
        set(v) { field = v; invalidate() }
    var color: Int = Color.WHITE
        set(v) { field = v; invalidate() }

    override fun onDraw(canvas: Canvas) {
        Glyphs.draw(canvas, glyph, width / 2f, height / 2f, minOf(width, height).toFloat(), color)
    }
}
