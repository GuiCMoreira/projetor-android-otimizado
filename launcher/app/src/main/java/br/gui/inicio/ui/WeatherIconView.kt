package br.gui.inicio.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import br.gui.inicio.core.Sky

/** Ícone de clima colorido, desenhado em código (sol amarelo, nuvem branca, chuva azul), no estilo do macOS. */
class WeatherIconView(ctx: Context) : View(ctx) {
    var sky: Sky = Sky.CLEAR
        set(v) { field = v; invalidate() }
    var isDay: Boolean = true
        set(v) { field = v; invalidate() }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        stroke.strokeWidth = s * 0.07f
        when (sky) {
            Sky.CLEAR -> if (isDay) sun(c, cx, cy, s * 0.24f, rays = true) else moon(c, cx, cy, s * 0.3f)
            Sky.PARTLY -> {
                if (isDay) sun(c, cx + s * 0.14f, cy - s * 0.14f, s * 0.17f, rays = true) else moon(c, cx + s * 0.14f, cy - s * 0.14f, s * 0.2f)
                cloud(c, cx - s * 0.05f, cy + s * 0.1f, s * 0.78f)
            }
            Sky.CLOUDY -> cloud(c, cx, cy, s * 0.9f)
            Sky.FOG -> {
                cloud(c, cx, cy - s * 0.12f, s * 0.8f)
                stroke.color = 0xFFD1D1D6.toInt()
                for (k in 0..1) c.drawLine(cx - s * 0.32f, cy + s * (0.2f + k * 0.12f), cx + s * 0.32f, cy + s * (0.2f + k * 0.12f), stroke)
            }
            Sky.DRIZZLE, Sky.RAIN -> {
                cloud(c, cx, cy - s * 0.12f, s * 0.8f)
                drops(c, cx, cy + s * 0.22f, s, if (sky == Sky.RAIN) 3 else 2)
            }
            Sky.SNOW -> {
                cloud(c, cx, cy - s * 0.12f, s * 0.8f)
                fill.color = 0xFFFFFFFF.toInt()
                for (k in -1..1) c.drawCircle(cx + k * s * 0.18f, cy + s * 0.26f + (k and 1) * s * 0.06f, s * 0.045f, fill)
            }
            Sky.STORM -> {
                cloud(c, cx, cy - s * 0.12f, s * 0.8f)
                fill.color = 0xFFFFD60A.toInt()
                val bolt = Path().apply {
                    moveTo(cx + s * 0.02f, cy + s * 0.08f)
                    lineTo(cx - s * 0.1f, cy + s * 0.28f)
                    lineTo(cx, cy + s * 0.28f)
                    lineTo(cx - s * 0.06f, cy + s * 0.44f)
                    lineTo(cx + s * 0.12f, cy + s * 0.2f)
                    lineTo(cx + s * 0.02f, cy + s * 0.2f)
                    close()
                }
                c.drawPath(bolt, fill)
            }
        }
    }

    private fun sun(c: Canvas, x: Float, y: Float, r: Float, rays: Boolean) {
        fill.color = 0xFFFFD60A.toInt()
        c.drawCircle(x, y, r, fill)
        if (!rays) return
        stroke.color = 0xFFFFD60A.toInt()
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val r1 = r * 1.35f
            val r2 = r * 1.75f
            c.drawLine(x + (r1 * Math.cos(a)).toFloat(), y + (r1 * Math.sin(a)).toFloat(), x + (r2 * Math.cos(a)).toFloat(), y + (r2 * Math.sin(a)).toFloat(), stroke)
        }
    }

    private fun moon(c: Canvas, x: Float, y: Float, r: Float) {
        val path = Path().apply {
            addCircle(x, y, r, Path.Direction.CW)
            val cut = Path().apply { addCircle(x + r * 0.55f, y - r * 0.35f, r * 0.85f, Path.Direction.CW) }
            op(cut, Path.Op.DIFFERENCE)
        }
        fill.color = 0xFFF2F2F7.toInt()
        c.drawPath(path, fill)
    }

    private fun cloud(c: Canvas, x: Float, y: Float, w: Float) {
        fill.color = 0xFFFFFFFF.toInt()
        val h = w * 0.32f
        c.drawRoundRect(x - w / 2, y, x + w / 2, y + h, h / 2, h / 2, fill)
        c.drawCircle(x - w * 0.14f, y + h * 0.05f, w * 0.2f, fill)
        c.drawCircle(x + w * 0.12f, y - h * 0.05f, w * 0.26f, fill)
    }

    private fun drops(c: Canvas, x: Float, y: Float, s: Float, n: Int) {
        stroke.color = 0xFF64D2FF.toInt()
        val start = -(n - 1) / 2f
        for (k in 0 until n) {
            val dx = x + (start + k) * s * 0.18f
            c.drawLine(dx, y, dx - s * 0.05f, y + s * 0.16f, stroke)
        }
    }
}
