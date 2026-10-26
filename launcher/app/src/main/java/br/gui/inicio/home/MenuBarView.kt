package br.gui.inicio.home

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import br.gui.inicio.ui.GlassDrawable
import br.gui.inicio.ui.Glyph
import br.gui.inicio.ui.GlyphView
import br.gui.inicio.ui.Theme

enum class BarItem { START, BLUETOOTH, WIFI, CONTROL, CLOCK }

/** Barra de menu de vidro. Todos os itens recebem foco; ↓ volta ao Dock. Desligado = ícone esmaecido. */
class MenuBarView(ctx: Context, private val onItem: (BarItem, View) -> Unit) : LinearLayout(ctx) {
    var onDown: () -> Unit = {}
    private val items = LinkedHashMap<BarItem, View>()
    private val bluetooth = GlyphView(ctx, Glyph.BLUETOOTH)
    private val wifi = GlyphView(ctx, Glyph.WIFI)
    private val clock = Theme.text(ctx, 20f, 500).apply { gravity = Gravity.CENTER_VERTICAL }

    val startItem: View get() = items.getValue(BarItem.START)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = GlassDrawable(this, 0f)
        setPadding(Theme.si(16f), 0, Theme.si(16f), 0)
        val wrap = LayoutParams.WRAP_CONTENT

        add(BarItem.START, Theme.text(ctx, 20f, 700, "Início").apply { gravity = Gravity.CENTER_VERTICAL }, wrap)
        addView(View(ctx), LayoutParams(0, 1, 1f))
        add(BarItem.BLUETOOTH, glyphBox(bluetooth), Theme.si(56f))
        add(BarItem.WIFI, glyphBox(wifi), Theme.si(56f))
        val control = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(GlyphView(ctx, Glyph.CONTROLS), LayoutParams(Theme.si(26f), Theme.si(26f)))
            addView(Theme.text(ctx, 20f, 500, "Controle"), LayoutParams(wrap, wrap).apply { leftMargin = Theme.si(8f) })
        }
        add(BarItem.CONTROL, control, wrap)
        add(BarItem.CLOCK, clock, wrap)

        val all = items.values.toList()
        all.first().let { it.nextFocusLeftId = it.id }
        all.last().let { it.nextFocusRightId = it.id }
    }

    private fun glyphBox(g: GlyphView) = FrameLayout(context).apply {
        addView(g, FrameLayout.LayoutParams(Theme.si(26f), Theme.si(26f), Gravity.CENTER))
    }

    private fun add(item: BarItem, v: View, width: Int) {
        v.id = View.generateViewId()
        v.isFocusable = true
        v.setPadding(Theme.si(14f), 0, Theme.si(14f), 0)
        v.setOnFocusChangeListener { view, has -> view.background = if (has) Theme.rounded(Theme.FOCUS_FILL, 10f) else null }
        v.setOnClickListener { onItem(item, v) }
        v.setOnKeyListener { _, code, ev ->
            when (code) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (ev.action == KeyEvent.ACTION_DOWN) onDown()
                    true
                }
                KeyEvent.KEYCODE_DPAD_UP -> true
                else -> false
            }
        }
        items[item] = v
        addView(v, LayoutParams(width, Theme.si(42f)).apply { leftMargin = Theme.si(6f) })
    }

    fun setTime(text: String) {
        clock.text = text
    }

    /** [bluetoothOn] null = estado ilegível sem permissão: o item some. */
    fun setStatus(wifiConnected: Boolean, bluetoothOn: Boolean?) {
        wifi.color = if (wifiConnected) Color.WHITE else 0x80FFFFFF.toInt()
        items.getValue(BarItem.BLUETOOTH).visibility = if (bluetoothOn == null) View.GONE else View.VISIBLE
        bluetooth.color = if (bluetoothOn == true) Color.WHITE else 0x80FFFFFF.toInt()
    }

    fun focusFirst() {
        startItem.requestFocus()
    }
}
