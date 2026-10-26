package br.gui.inicio.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.TextView
import br.gui.inicio.R

/** Tokens visuais da especificação (seção 3). Medidas pensadas para 1920x1080 e convertidas por [s]. */
object Theme {
    const val SELECTION = 0xFF3478F6.toInt()
    const val TODAY = 0xFFFF3B30.toInt()
    const val GLASS_FILL = 0x29FFFFFF
    const val GLASS_BORDER = 0x38FFFFFF
    const val GLASS_HIGHLIGHT = 0x40FFFFFF
    const val FOCUS_FILL = 0x4DFFFFFF
    const val TILE_FILL = 0x24FFFFFF
    const val TEXT_SHADOW = 0x59000000
    const val DIM = 0x73000000
    const val BLUR_DOWNSCALE = 8
    const val BLUR_RADIUS = 5

    private var factor = 1f
    private val fonts = HashMap<Int, Typeface>()

    fun init(ctx: Context) {
        factor = ctx.resources.displayMetrics.widthPixels / 1920f
    }

    fun s(px: Float): Float = px * factor

    fun si(px: Float): Int = Math.round(px * factor)

    fun font(ctx: Context, weight: Int): Typeface = fonts.getOrPut(weight) {
        val res = when {
            weight >= 700 -> R.font.inter_bold
            weight >= 600 -> R.font.inter_semibold
            weight >= 500 -> R.font.inter_medium
            else -> R.font.inter_regular
        }
        ctx.resources.getFont(res)
    }

    /** Texto branco com a sombra suave que garante contraste sobre qualquer fundo. */
    fun text(ctx: Context, sizePx: Float, weight: Int, value: CharSequence = ""): TextView =
        TextView(ctx).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_PX, s(sizePx))
            typeface = font(ctx, weight)
            setTextColor(Color.WHITE)
            setShadowLayer(s(6f), 0f, s(1f), TEXT_SHADOW)
            includeFontPadding = false
        }

    fun rounded(color: Int, radiusPx: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = s(radiusPx)
    }

    fun oval(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }
}
