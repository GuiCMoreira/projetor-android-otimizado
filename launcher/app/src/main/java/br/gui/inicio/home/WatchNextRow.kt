package br.gui.inicio.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Outline
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import br.gui.inicio.ui.Theme

/** Um cartão pronto para a tela: capa, selo do app de origem, progresso e a linha de detalhes. */
class WatchNextCard(
    val title: String,
    val meta: String,
    val progress: Float?,
    val poster: Bitmap?,
    val badge: Bitmap?,
    val onOpen: () -> Unit,
)

/**
 * Fileira "Continuar assistindo" à esquerda, acima do Dock. O cartão em foco cresce 12% e o nome do episódio
 * aparece embaixo. Sem itens, a fileira some e a tela fica como antes.
 */
class WatchNextRow(ctx: Context) : LinearLayout(ctx) {
    var onUp: () -> Unit = {}
    var onDown: () -> Unit = {}
    private val strip = LinearLayout(ctx).apply {
        orientation = HORIZONTAL
        clipChildren = false
        clipToPadding = false
    }
    private val name = Theme.text(ctx, 30f, 700)
    private val meta = Theme.text(ctx, 20f, 500).apply { alpha = 0.78f }
    private var cards: List<View> = emptyList()
    private var focusedIndex = 0

    init {
        orientation = VERTICAL
        clipChildren = false
        clipToPadding = false
        val wrap = LayoutParams.WRAP_CONTENT
        addView(Theme.text(ctx, 18f, 700, "CONTINUAR ASSISTINDO").apply { letterSpacing = 0.08f; alpha = 0.78f })
        addView(strip, LayoutParams(wrap, wrap).apply { topMargin = Theme.si(16f) })
        addView(name, LayoutParams(wrap, wrap).apply { topMargin = Theme.si(20f) })
        addView(meta, LayoutParams(wrap, wrap).apply { topMargin = Theme.si(6f) })
        visibility = View.GONE
    }

    val hasItems: Boolean get() = cards.isNotEmpty()

    fun setItems(items: List<WatchNextCard>) {
        val hadFocus = hasFocusedCard()
        strip.removeAllViews()
        val w = Theme.si(CARD_W)
        val h = Theme.si(CARD_H)
        cards = items.mapIndexed { i, item ->
            card(item, w, h).also { v ->
                v.setOnFocusChangeListener { view, has ->
                    view.animate().scaleX(if (has) FOCUS_SCALE else 1f).scaleY(if (has) FOCUS_SCALE else 1f).setDuration(150).start()
                    (view as FrameLayout).foreground = if (has) Theme.ring(Theme.s(RADIUS), Theme.s(3f)) else null
                    if (has) {
                        focusedIndex = i
                        name.text = item.title
                        meta.text = item.meta
                    }
                }
                v.setOnClickListener { item.onOpen() }
                strip.addView(v, LayoutParams(w, h).apply { if (i > 0) leftMargin = Theme.si(28f) })
            }
        }
        cards.firstOrNull()?.let { it.nextFocusLeftId = it.id }
        cards.lastOrNull()?.let { it.nextFocusRightId = it.id }
        focusedIndex = focusedIndex.coerceIn(0, maxOf(0, cards.size - 1))
        items.getOrNull(focusedIndex)?.let { name.text = it.title; meta.text = it.meta }
        visibility = if (cards.isEmpty()) View.GONE else View.VISIBLE
        // Lista esvaziou com o foco nela (episódio terminado no app): o foco vai para o Dock, não fica solto.
        if (hadFocus) if (cards.isEmpty()) onDown() else focus()
    }

    fun focus() {
        cards.getOrNull(focusedIndex)?.requestFocus()
    }

    fun hasFocusedCard(): Boolean = cards.any { it.isFocused }

    private fun card(item: WatchNextCard, w: Int, h: Int): FrameLayout = FrameLayout(context).apply {
        id = View.generateViewId()
        isFocusable = true
        contentDescription = item.title
        background = Theme.rounded(Theme.TILE_FILL, RADIUS)
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) = outline.setRoundRect(0, 0, view.width, view.height, Theme.s(RADIUS))
        }
        clipToOutline = true
        elevation = Theme.s(10f)
        if (item.poster != null) {
            addView(ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(item.poster)
            }, FrameLayout.LayoutParams(w, h))
        } else {
            // Sem capa: o nome sobre o vidro, para o cartão nunca ficar vazio.
            addView(Theme.text(context, 24f, 700, item.title).apply {
                gravity = Gravity.CENTER
                maxLines = 3
                setPadding(Theme.si(20f), 0, Theme.si(20f), 0)
            }, FrameLayout.LayoutParams(w, h))
        }
        item.badge?.let { b ->
            addView(ImageView(context).apply { setImageBitmap(b) }, FrameLayout.LayoutParams(Theme.si(BADGE), Theme.si(BADGE)).apply {
                leftMargin = Theme.si(14f)
                topMargin = Theme.si(14f)
            })
        }
        item.progress?.let { p ->
            addView(ProgressLine(context, p), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, Theme.si(6f), Gravity.BOTTOM).apply {
                setMargins(Theme.si(16f), 0, Theme.si(16f), Theme.si(14f))
            })
        }
        setOnKeyListener { _, code, ev ->
            if (ev.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
            when (code) {
                KeyEvent.KEYCODE_DPAD_UP -> { onUp(); true }
                KeyEvent.KEYCODE_DPAD_DOWN -> { onDown(); true }
                else -> false
            }
        }
    }

    companion object {
        const val CARD_W = 340f
        const val CARD_H = 191f
        const val RADIUS = 20f
        const val BADGE = 40f
        const val FOCUS_SCALE = 1.12f
    }
}
