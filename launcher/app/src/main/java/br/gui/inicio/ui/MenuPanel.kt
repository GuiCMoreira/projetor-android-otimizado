package br.gui.inicio.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

data class MenuEntry(
    val title: String,
    val enabled: Boolean = true,
    val separatorBefore: Boolean = false,
    val action: () -> Unit = {},
)

/** Menu de vidro em lista, como os menus do macOS. Item focado fica azul; item desabilitado fica esmaecido. */
class MenuPanel(ctx: Context, header: String?, entries: List<MenuEntry>, onPick: () -> Unit) : LinearLayout(ctx) {
    init {
        orientation = VERTICAL
        background = GlassDrawable(this, Theme.s(24f))
        val pad = Theme.si(12f)
        setPadding(pad, pad, pad, pad)
        minimumWidth = Theme.si(360f)
        if (header != null) {
            addView(Theme.text(ctx, 18f, 500, header).apply {
                alpha = 0.7f
                setPadding(Theme.si(20f), Theme.si(8f), Theme.si(20f), Theme.si(8f))
            })
        }
        entries.forEach { e ->
            if (e.separatorBefore) {
                addView(View(ctx).apply { setBackgroundColor(0x33FFFFFF) }, LayoutParams(LayoutParams.MATCH_PARENT, maxOf(1, Theme.si(1.5f))).apply {
                    setMargins(Theme.si(8f), Theme.si(6f), Theme.si(8f), Theme.si(6f))
                })
            }
            addView(item(ctx, e, onPick), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
    }

    private fun item(ctx: Context, e: MenuEntry, onPick: () -> Unit): TextView =
        Theme.text(ctx, 22f, 500, e.title).apply {
            setPadding(Theme.si(20f), Theme.si(12f), Theme.si(20f), Theme.si(12f))
            isFocusable = e.enabled
            alpha = if (e.enabled) 1f else 0.45f
            setOnFocusChangeListener { v, has -> v.background = if (has) Theme.rounded(Theme.SELECTION, 12f) else null }
            setOnClickListener {
                onPick()
                e.action()
            }
        }
}
