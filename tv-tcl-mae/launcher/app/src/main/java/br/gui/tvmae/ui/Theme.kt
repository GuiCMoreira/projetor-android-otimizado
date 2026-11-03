package br.gui.tvmae.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.TextView
import br.gui.tvmae.R

/**
 * Cores e medidas do protótipo (docs/prototipos/tela-inicial.html). Medidas em pixels de uma tela 1280x720, a da TV,
 * convertidas por [s] para qualquer outra.
 */
object Theme {
    const val BG_TOP = 0xFF1B2233.toInt()
    const val BG_BOTTOM = 0xFF0D1018.toInt()
    const val TILE = 0xFF262D3D.toInt()
    const val MUTED = 0xFFB9BFCC.toInt()
    const val TEXT = 0xFFF5F6F8.toInt()

    private var factor = 1f
    private val fonts = HashMap<Int, Typeface>()

    fun init(ctx: Context) {
        factor = ctx.resources.displayMetrics.widthPixels / 1280f
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

    fun text(ctx: Context, sizePx: Float, weight: Int, color: Int = TEXT, value: CharSequence = ""): TextView =
        TextView(ctx).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_PX, s(sizePx))
            typeface = font(ctx, weight)
            setTextColor(color)
            includeFontPadding = false
        }

    fun rounded(color: Int, radiusPx: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = s(radiusPx)
    }

    /** Borda branca grossa do foco, sem preenchimento: o item em foco tem que saltar aos olhos. */
    fun ring(radiusPx: Float, widthPx: Float) = GradientDrawable().apply {
        setColor(Color.TRANSPARENT)
        setStroke(si(widthPx).coerceAtLeast(2), Color.WHITE)
        cornerRadius = s(radiusPx)
    }

    /** Fundo da tela: degradê escuro e calmo, sem foto, para nada competir com os botões. */
    fun background() = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(BG_TOP, BG_BOTTOM))
}
