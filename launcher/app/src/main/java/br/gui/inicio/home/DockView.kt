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

/**
 * Dock de vidro: o ícone em foco cresce 30% e sobe, e os vizinhos se afastam (como no macOS) para nunca encostarem nele.
 * A Home mostra o nome num balão.
 */
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
        // Margem lateral = margem normal + afastamento, para os ícones das pontas não saírem do vidro ao se afastarem.
        val side = Theme.si(22f + PUSH)
        setPadding(side, Theme.si(18f), side, Theme.si(18f))
    }

    /** Afasta para os lados tudo o que estiver à esquerda e à direita de [focused]; null volta todos ao lugar. */
    private fun spreadAround(focused: View?) {
        val center = focused?.let { indexOfChild(it) } ?: -1
        val push = Theme.s(PUSH)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child === focused) continue
            val tx = when {
                center < 0 -> 0f
                i < center -> -push
                else -> push
            }
            child.animate().translationX(tx).setDuration(150).start()
        }
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
                        .scaleX(if (has) FOCUS_SCALE else 1f)
                        .scaleY(if (has) FOCUS_SCALE else 1f)
                        .translationY(if (has) -Theme.s(16f) else 0f)
                        .apply { if (has) translationX(0f) }
                        .setDuration(150)
                        .start()
                    if (has) {
                        lastFocused = view
                        spreadAround(view)
                        onFocusItem(view, item)
                    } else {
                        // Foco saiu do Dock (foi para a barra ou um painel): todos voltam ao lugar.
                        post { if (icons.none { it.isFocused }) spreadAround(null) }
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

    companion object {
        const val FOCUS_SCALE = 1.3f

        /**
         * Quanto os vizinhos se afastam, em px de uma tela 1920. O ícone de 112 px cresce 16,8 px de cada lado
         * (30%); com 20 px de vão normal + 24 de afastamento, sobram ~27 px de respiro entre o ícone em foco e o vizinho.
         */
        const val PUSH = 24f
    }
}
