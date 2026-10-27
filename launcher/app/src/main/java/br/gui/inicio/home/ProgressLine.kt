package br.gui.inicio.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Barrinha de progresso: trilho claro e a parte assistida em branco. Usada nos cartões e no descanso. */
class ProgressLine(ctx: Context, private val fraction: Float) : View(ctx) {
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x59FFFFFF }
    private val done = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val r = RectF()

    override fun onDraw(c: Canvas) {
        val rad = height / 2f
        r.set(0f, 0f, width.toFloat(), height.toFloat())
        c.drawRoundRect(r, rad, rad, track)
        r.right = maxOf(height.toFloat(), width * fraction)
        c.drawRoundRect(r, rad, rad, done)
    }
}
