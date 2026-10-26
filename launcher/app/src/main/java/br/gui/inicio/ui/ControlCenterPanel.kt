package br.gui.inicio.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioManager
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import br.gui.inicio.core.Guard
import br.gui.inicio.core.Volume
import br.gui.inicio.data.SystemShortcut

/** Bloco "Timer" da Central: o texto da vez e o que o OK faz. */
interface TimerControl {
    fun subtitle(): String
    fun cycle()
}

/**
 * Central de Controle: volume de verdade no topo (← → mudam o volume de mídia) e a grade de blocos
 * que abrem as telas oficiais do projetor, mais o bloco do timer para desligar.
 */
class ControlCenterPanel(
    ctx: Context,
    items: List<SystemShortcut>,
    private val timer: TimerControl,
    onPick: (SystemShortcut) -> Unit,
) : LinearLayout(ctx) {
    private val audio = ctx.getSystemService(AudioManager::class.java)

    init {
        orientation = VERTICAL
        background = GlassDrawable(this, Theme.s(32f))
        val pad = Theme.si(20f)
        setPadding(pad, pad, pad, pad)
        addView(volumeRow(ctx), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(Theme.si(8f), Theme.si(8f), Theme.si(8f), Theme.si(8f))
        })
        val grid = GridLayout(ctx).apply { columnCount = 2 }
        items.forEach { grid.addView(tile(ctx, it.title, { it.subtitle }, it.glyph) { onPick(it) }) }
        lateinit var timerTile: View
        timerTile = tile(ctx, "Timer", { timer.subtitle() }, Glyph.TIMER) {
            timer.cycle()
            (timerTile.tag as? () -> Unit)?.invoke() // reescreve o subtítulo sem fechar a Central
        }
        grid.addView(timerTile)
        addView(grid)
    }

    private fun volumeRow(ctx: Context): View {
        val max = Guard.run({ 0 }) { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
        var current = Guard.run({ 0 }) { audio.getStreamVolume(AudioManager.STREAM_MUSIC) }
        val bar = VolumeBar(ctx)
        val percent = Theme.text(ctx, 20f, 500).apply { gravity = Gravity.END; minWidth = Theme.si(64f) }
        val icon = GlyphView(ctx, Glyph.SOUND)
        fun render(focused: Boolean) {
            bar.fraction = if (max > 0) current / max.toFloat() else 0f
            bar.focused = focused
            percent.text = "${Volume.percent(current, max)}%"
            val textColor = if (focused) 0xFF111111.toInt() else Color.WHITE
            percent.setTextColor(textColor)
            icon.color = textColor
        }
        return LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            setPadding(Theme.si(22f), Theme.si(16f), Theme.si(22f), Theme.si(16f))
            background = Theme.rounded(Theme.TILE_FILL, 22f)
            addView(icon, LayoutParams(Theme.si(30f), Theme.si(30f)))
            addView(bar, LayoutParams(0, Theme.si(14f), 1f).apply { setMargins(Theme.si(18f), 0, Theme.si(18f), 0) })
            addView(percent)
            render(false)
            setOnFocusChangeListener { _, has ->
                background = Theme.rounded(if (has) Color.WHITE else Theme.TILE_FILL, 22f)
                percent.setShadowLayer(if (has) 0f else Theme.s(6f), 0f, if (has) 0f else Theme.s(1f), if (has) 0 else Theme.TEXT_SHADOW)
                render(has)
            }
            setOnKeyListener { _, code, ev ->
                val delta = when (code) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> -1
                    KeyEvent.KEYCODE_DPAD_RIGHT -> 1
                    else -> return@setOnKeyListener false
                }
                if (ev.action == KeyEvent.ACTION_DOWN) {
                    current = Volume.step(current, max, delta)
                    Guard.run({ Unit }) { audio.setStreamVolume(AudioManager.STREAM_MUSIC, current, 0) }
                    render(true)
                }
                true
            }
        }
    }

    private fun tile(ctx: Context, titleText: String, subtitleText: () -> String, glyphKind: Glyph, onClick: () -> Unit): View {
        val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
        val idle = 0x38FFFFFF
        val glyph = GlyphView(ctx, glyphKind)
        val circle = FrameLayout(ctx).apply {
            background = Theme.oval(idle)
            addView(glyph, FrameLayout.LayoutParams(Theme.si(30f), Theme.si(30f), Gravity.CENTER))
        }
        // Uma linha só: se um texto passar da largura, termina em "…" em vez de quebrar e ser cortado.
        val title = Theme.text(ctx, 20f, 500, titleText).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val subtitle: TextView = Theme.text(ctx, 15f, 400, subtitleText()).apply {
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
            tag = { subtitle.text = subtitleText() }
            setOnFocusChangeListener { _, has ->
                background = Theme.rounded(if (has) Color.WHITE else Theme.TILE_FILL, 22f)
                val textColor = if (has) 0xFF111111.toInt() else Color.WHITE
                listOf(title, subtitle).forEach {
                    it.setTextColor(textColor)
                    if (has) it.setShadowLayer(0f, 0f, 0f, 0) else it.setShadowLayer(Theme.s(6f), 0f, Theme.s(1f), Theme.TEXT_SHADOW)
                }
                circle.background = Theme.oval(if (has) Theme.SELECTION else idle)
            }
            setOnClickListener { onClick() }
        }
    }

    /** Barra do volume: trilho translúcido e preenchimento (azul com foco, branco sem). */
    private class VolumeBar(ctx: Context) : View(ctx) {
        var fraction = 0f
            set(v) { field = v.coerceIn(0f, 1f); invalidate() }
        var focused = false
            set(v) { field = v; invalidate() }
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(c: Canvas) {
            val r = height / 2f
            paint.color = if (focused) 0x33000000 else 0x40FFFFFF
            c.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), r, r, paint)
            paint.color = if (focused) Theme.SELECTION else Color.WHITE
            val w = maxOf(height.toFloat(), width * fraction)
            if (fraction > 0f) c.drawRoundRect(0f, 0f, w, height.toFloat(), r, r, paint)
        }
    }
}
