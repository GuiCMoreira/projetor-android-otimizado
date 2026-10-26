package br.gui.inicio.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup

/** Papel de parede reduzido (1/8) e desfocado uma única vez; é a "matéria" do vidro. */
object GlassSource {
    @Volatile var blurred: Bitmap? = null
}

/**
 * Vidro fosco barato: desenha o pedaço do fundo já desfocado que fica atrás de [host],
 * mais um véu branco, borda e um brilho no topo. Nenhum desfoque é calculado por quadro.
 */
class GlassDrawable(private val host: View, private val radiusPx: Float) : Drawable() {
    private val loc = IntArray(2)
    private val rect = RectF()
    private val path = Path()
    private val matrix = Matrix()
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Theme.GLASS_FILL }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Theme.GLASS_BORDER
    }
    private val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
        color = Theme.GLASS_HIGHLIGHT
    }

    override fun draw(canvas: Canvas) {
        rect.set(bounds)
        path.reset()
        path.addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(path)
        GlassSource.blurred?.let { bmp ->
            host.getLocationOnScreen(loc)
            val scale = host.resources.displayMetrics.widthPixels.toFloat() / bmp.width
            matrix.setScale(scale, scale)
            matrix.postTranslate(-loc[0].toFloat(), -loc[1].toFloat())
            canvas.drawBitmap(bmp, matrix, bitmapPaint)
        }
        canvas.drawRect(rect, fill)
        canvas.restore()
        canvas.drawPath(path, border)
        canvas.drawLine(rect.left + radiusPx, rect.top + 1f, rect.right - radiusPx, rect.top + 1f, highlight)
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** Redesenha a view e todos os filhos (o vidro precisa redesenhar quando o fundo muda). */
fun View.invalidateTree() {
    invalidate()
    if (this is ViewGroup) for (i in 0 until childCount) getChildAt(i).invalidateTree()
}
