package br.gui.tvmae.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Ícone do "Todos os apps": quadrado arredondado com 2 fileiras de 3 pontos, como no protótipo. */
class GridDots(ctx: Context) : View(ctx) {
    private val back = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3A4256.toInt() }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE9ECF2.toInt() }
    private val rect = RectF()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(0f, 0f, w, h)
        c.drawRoundRect(rect, w * 0.3f, w * 0.3f, back)
        val d = w * 0.18f
        val gap = w * 0.09f
        val x0 = (w - (3 * d + 2 * gap)) / 2
        val y0 = (h - (2 * d + gap)) / 2
        for (r in 0..1) for (col in 0..2) {
            val x = x0 + col * (d + gap)
            val y = y0 + r * (d + gap)
            rect.set(x, y, x + d, y + d)
            c.drawRoundRect(rect, d * 0.3f, d * 0.3f, dot)
        }
    }
}
