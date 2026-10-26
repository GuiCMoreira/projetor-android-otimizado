package br.gui.inicio.home

import android.content.Context
import android.graphics.Bitmap
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import br.gui.inicio.ui.GlassDrawable
import br.gui.inicio.ui.Theme

/** Item do Dock: app favorito, atalho fixo ou "Todos os apps". [groupStart] desenha uma divisória antes. */
data class DockItem(
    val key: String,
    val label: String,
    val icon: Bitmap,
    val groupStart: Boolean = false,
    val onOpen: () -> Unit,
)

/** Dock de vidro: o ícone em foco cresce 35% e sobe; a Home mostra o nome num balão. */
class DockView(ctx: Context) : LinearLayout(ctx) {
    var onUp: () -> Unit = {}
    var onFocusItem: (View, DockItem) -> Unit = { _, _ -> }
    private var icons: List<View> = emptyList()
    private var lastFocused: View? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.BOTTOM
        clipChildren = false
        clipToPadding = false
        background = GlassDrawable(this, Theme.s(40f))
        setPadding(Theme.si(22f), Theme.si(18f), Theme.si(22f), Theme.si(18f))
    }

    fun setItems(items: List<DockItem>) {
        val keepKey = lastFocused?.tag as? String
        removeAllViews()
        val size = Theme.si(112f)
        val gap = Theme.si(10f)
        val views = mutableListOf<View>()
        items.forEach { item ->
            if (item.groupStart && views.isNotEmpty()) {
                addView(View(context).apply { setBackgroundColor(0x4DFFFFFF) }, LayoutParams(maxOf(1, Theme.si(2f)), Theme.si(96f)).apply {
                    setMargins(Theme.si(12f), 0, Theme.si(12f), Theme.si(8f))
                })
            }
            val v = ImageView(context).apply {
                id = View.generateViewId()
                tag = item.key
                setImageBitmap(item.icon)
                contentDescription = item.label
                isFocusable = true
                // Cresce a partir da base, centralizado. O pivô só vale depois do layout (no 1º foco a largura ainda é 0).
                addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                    view.pivotX = view.width / 2f
                    view.pivotY = view.height.toFloat()
                }
                setOnFocusChangeListener { view, has ->
                    view.animate()
                        .scaleX(if (has) 1.35f else 1f)
                        .scaleY(if (has) 1.35f else 1f)
                        .translationY(if (has) -Theme.s(16f) else 0f)
                        .setDuration(150)
                        .start()
                    if (has) {
                        lastFocused = view
                        onFocusItem(view, item)
                    }
                }
                setOnClickListener { item.onOpen() }
                setOnKeyListener { _, code, ev ->
                    if (code == KeyEvent.KEYCODE_DPAD_UP && ev.action == KeyEvent.ACTION_DOWN) {
                        onUp()
                        true
                    } else {
                        code == KeyEvent.KEYCODE_DPAD_DOWN || code == KeyEvent.KEYCODE_DPAD_UP
                    }
                }
            }
            addView(v, LayoutParams(size, size).apply { setMargins(gap, 0, gap, 0) })
            views += v
        }
        views.firstOrNull()?.let { it.nextFocusLeftId = it.id }
        views.lastOrNull()?.let { it.nextFocusRightId = it.id }
        icons = views
        lastFocused = views.firstOrNull { it.tag == keepKey }
    }

    fun focusFirst() {
        icons.firstOrNull()?.requestFocus()
    }

    fun focusLast() {
        (lastFocused ?: icons.firstOrNull())?.requestFocus()
    }

    fun hasFocusedItem(): Boolean = icons.any { it.isFocused }
}
