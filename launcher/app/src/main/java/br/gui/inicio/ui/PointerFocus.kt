package br.gui.inicio.ui

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import br.gui.inicio.core.HoverTracker

/**
 * Mouse aéreo: o item sob o ponteiro ganha o foco (e com ele a lupa do Dock e a legenda), e a roda rola as
 * fileiras laterais, que sozinhas só respondem à roda horizontal. Cada tela chama [handle] no
 * dispatchGenericMotionEvent.
 */
class PointerFocus {
    private val tracker = HoverTracker<View>()
    private val rect = Rect()
    /** O último item que o ponteiro focou; foco em outro item quer dizer que as setas (ou um painel) mandaram. */
    private var focusedByPointer: View? = null

    /** Chamado quando o ponteiro deu foco a um item novo. */
    var onFocusByPointer: (() -> Unit)? = null

    /** true quando consumiu o evento (só a roda sobre uma fileira lateral). */
    fun handle(root: View, event: MotionEvent): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()
        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                // Campo de texto só ganha foco no clique: passar o ponteiro por cima não abre o teclado.
                val target = find(root, x, y) { it.isFocusable && it.isEnabled && it !is EditText }
                val current = root.findFocus()
                val foreign = current != null && current !== focusedByPointer
                // Neste firmware o clique do mouse liga o modo de toque, que apaga o foco: aí o ponteiro devolve o foco
                // ao item em que está, mesmo sem ter saído dele. Fora do modo de toque vale a regra do tremor.
                val focus = tracker.enter(target, event.x, event.y, foreign)
                    ?: target?.takeIf { root.isInTouchMode && !it.isFocused }
                    ?: return false
                // requestFocusFromTouch sai do modo de toque; o requestFocus comum é recusado nele.
                if (!focus.isFocused && focus.requestFocusFromTouch()) onFocusByPointer?.invoke()
                if (focus.isFocused) focusedByPointer = focus
            }
            MotionEvent.ACTION_HOVER_EXIT -> tracker.enter(null, event.x, event.y, foreignFocus = false)
            MotionEvent.ACTION_SCROLL -> {
                val wheel = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                if (wheel == 0f) return false
                val row = find(root, x, y) { it is HorizontalScrollView } as? HorizontalScrollView ?: return false
                row.smoothScrollBy((-wheel * row.width * WHEEL_STEP).toInt(), 0)
                return true
            }
        }
        return false
    }

    /** O item mais ao fundo e mais acima na pilha (último filho desenhado) que contém o ponto e atende [match]. */
    private fun find(v: View, x: Int, y: Int, match: (View) -> Boolean): View? {
        if (v.visibility != View.VISIBLE || !v.getGlobalVisibleRect(rect) || !rect.contains(x, y)) return null
        if (v is ViewGroup && v.descendantFocusability != ViewGroup.FOCUS_BLOCK_DESCENDANTS) {
            for (i in v.childCount - 1 downTo 0) find(v.getChildAt(i), x, y, match)?.let { return it }
        }
        return if (match(v)) v else null
    }

    private companion object {
        /** Cada clique da roda anda um terço da fileira. */
        const val WHEEL_STEP = 0.33f
    }
}
