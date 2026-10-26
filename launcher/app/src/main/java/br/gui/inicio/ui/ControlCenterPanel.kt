package br.gui.inicio.ui

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import br.gui.inicio.data.SystemShortcut

/** Central de Controle: grade 2 x 4 de blocos que abrem as telas oficiais do projetor. */
class ControlCenterPanel(ctx: Context, items: List<SystemShortcut>, onPick: (SystemShortcut) -> Unit) : GridLayout(ctx) {
    init {
        columnCount = 2
        background = GlassDrawable(this, Theme.s(32f))
        val pad = Theme.si(20f)
        setPadding(pad, pad, pad, pad)
        items.forEach { addView(tile(ctx, it, onPick)) }
    }

    private fun tile(ctx: Context, s: SystemShortcut, onPick: (SystemShortcut) -> Unit): View {
        val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
        val idle = 0x38FFFFFF
        val glyph = GlyphView(ctx, s.glyph)
        val circle = FrameLayout(ctx).apply {
            background = Theme.oval(idle)
            addView(glyph, FrameLayout.LayoutParams(Theme.si(30f), Theme.si(30f), Gravity.CENTER))
        }
        // Uma linha só: se um texto passar da largura, termina em "…" em vez de quebrar e ser cortado.
        val title = Theme.text(ctx, 20f, 500, s.title).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val subtitle = Theme.text(ctx, 15f, 400, s.subtitle).apply {
            alpha = 0.7f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val texts = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(subtitle, LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = Theme.si(4f) })
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            setPadding(Theme.si(18f), Theme.si(16f), Theme.si(18f), Theme.si(16f))
            background = Theme.rounded(Theme.TILE_FILL, 22f)
            addView(circle, LinearLayout.LayoutParams(Theme.si(52f), Theme.si(52f)))
            // Peso 1: a coluna de textos ocupa todo o resto do bloco (com WRAP ela media pelo subtítulo e cortava o título).
            addView(texts, LinearLayout.LayoutParams(0, wrap, 1f).apply { leftMargin = Theme.si(16f) })
            layoutParams = GridLayout.LayoutParams().apply {
                width = Theme.si(330f)
                setMargins(Theme.si(8f), Theme.si(8f), Theme.si(8f), Theme.si(8f))
            }
            setOnFocusChangeListener { _, has ->
                background = Theme.rounded(if (has) Color.WHITE else Theme.TILE_FILL, 22f)
                val textColor = if (has) 0xFF111111.toInt() else Color.WHITE
                listOf(title, subtitle).forEach {
                    it.setTextColor(textColor)
                    if (has) it.setShadowLayer(0f, 0f, 0f, 0) else it.setShadowLayer(Theme.s(6f), 0f, Theme.s(1f), Theme.TEXT_SHADOW)
                }
                circle.background = Theme.oval(if (has) Theme.SELECTION else idle)
            }
            setOnClickListener { onPick(s) }
        }
    }
}
