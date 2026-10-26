package br.gui.inicio.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/** Camada de painéis sobre a tela. Um painel por vez; o foco fica preso nele e volta para quem abriu ao fechar. */
class PanelHost(ctx: Context) : FrameLayout(ctx) {
    private var panel: View? = null
    private var opener: View? = null
    private val locked = mutableListOf<ViewGroup>()

    val isOpen: Boolean get() = panel != null

    /** [lockedWhileOpen]: áreas que não podem receber foco enquanto o painel estiver aberto. */
    fun show(panel: View, lp: FrameLayout.LayoutParams, opener: View?, lockedWhileOpen: List<ViewGroup>) {
        close()
        this.panel = panel
        this.opener = opener
        lockedWhileOpen.forEach {
            it.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            locked += it
        }
        addView(panel, lp)
        panel.alpha = 0f
        panel.animate().alpha(1f).setDuration(120).start()
        panel.post { panel.requestFocus() }
    }

    fun close() {
        val p = panel ?: return
        removeView(p)
        panel = null
        locked.forEach { it.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS }
        locked.clear()
        opener?.requestFocus()
        opener = null
    }

    companion object {
        private const val WRAP = FrameLayout.LayoutParams.WRAP_CONTENT

        /** Logo abaixo da barra de menu, a [start] px da esquerda ou a [end] px da direita. */
        fun below(start: Int? = null, end: Int? = null): FrameLayout.LayoutParams =
            FrameLayout.LayoutParams(WRAP, WRAP).apply {
                topMargin = Theme.si(64f)
                gravity = Gravity.TOP or (if (start != null) Gravity.START else Gravity.END)
                leftMargin = start ?: 0
                rightMargin = end ?: 0
            }

        fun centered(): FrameLayout.LayoutParams = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER)
    }
}
