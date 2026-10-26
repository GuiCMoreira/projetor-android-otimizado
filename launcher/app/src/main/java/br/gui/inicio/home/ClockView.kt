package br.gui.inicio.home

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import br.gui.inicio.core.ClockFormat
import br.gui.inicio.ui.Theme
import java.time.LocalDateTime

/** Data por extenso e hora grande, centralizadas, como a tela de bloqueio do Mac. */
class ClockView(ctx: Context) : LinearLayout(ctx) {
    private val date = Theme.text(ctx, 34f, 600).apply { alpha = 0.9f }
    private val time = Theme.text(ctx, 220f, 700).apply {
        alpha = 0.92f
        letterSpacing = -0.04f
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        addView(date, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(time, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = Theme.si(8f) })
    }

    fun update(now: LocalDateTime) {
        date.text = ClockFormat.longDate(now)
        time.text = ClockFormat.time(now)
    }
}
