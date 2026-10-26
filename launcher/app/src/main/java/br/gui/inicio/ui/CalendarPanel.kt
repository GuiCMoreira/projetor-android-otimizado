package br.gui.inicio.ui

import android.content.Context
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import br.gui.inicio.core.CalendarMath
import java.time.LocalDate
import java.time.YearMonth

/** Calendário do mês, como o do macOS: hoje em vermelho, ← → trocam de mês, "Ajustar data e hora…" no fim. */
class CalendarPanel(ctx: Context, private val today: LocalDate, onAdjust: () -> Unit) : LinearLayout(ctx) {
    private var month: YearMonth = YearMonth.from(today)
    private val title = Theme.text(ctx, 24f, 700).apply { gravity = Gravity.CENTER }
    private val grid = GridLayout(ctx).apply { columnCount = 7 }

    init {
        orientation = VERTICAL
        background = GlassDrawable(this, Theme.s(32f))
        val pad = Theme.si(24f)
        setPadding(pad, pad, pad, pad)
        val wrap = LayoutParams.WRAP_CONTENT
        val match = LayoutParams.MATCH_PARENT

        val header = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            setPadding(Theme.si(12f), Theme.si(8f), Theme.si(12f), Theme.si(8f))
            addView(Theme.text(ctx, 26f, 600, "‹"))
            addView(title, LayoutParams(0, wrap, 1f))
            addView(Theme.text(ctx, 26f, 600, "›"))
            setOnFocusChangeListener { v, has -> v.background = if (has) Theme.rounded(Theme.FOCUS_FILL, 12f) else null }
            setOnKeyListener { _, code, ev ->
                if (ev.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                when (code) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { shift(-1); true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { shift(1); true }
                    KeyEvent.KEYCODE_DPAD_UP -> true
                    else -> false
                }
            }
        }
        addView(header, LayoutParams(match, wrap))
        addView(grid, LayoutParams(wrap, wrap).apply {
            topMargin = Theme.si(12f)
            gravity = Gravity.CENTER_HORIZONTAL
        })
        addView(View(ctx).apply { setBackgroundColor(0x33FFFFFF) }, LayoutParams(match, maxOf(1, Theme.si(1.5f))).apply {
            topMargin = Theme.si(12f)
            bottomMargin = Theme.si(8f)
        })
        addView(Theme.text(ctx, 20f, 500, "Ajustar data e hora…").apply {
            isFocusable = true
            setPadding(Theme.si(12f), Theme.si(10f), Theme.si(12f), Theme.si(10f))
            setOnFocusChangeListener { v, has -> v.background = if (has) Theme.rounded(Theme.SELECTION, 12f) else null }
            setOnClickListener { onAdjust() }
            setOnKeyListener { _, code, _ -> code == KeyEvent.KEYCODE_DPAD_DOWN || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT }
        }, LayoutParams(match, wrap))
        render()
    }

    private fun shift(delta: Long) {
        month = month.plusMonths(delta)
        render()
    }

    private fun render() {
        title.text = CalendarMath.title(month)
        grid.removeAllViews()
        listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { grid.addView(cell(it, 600, 0.6f, null)) }
        CalendarMath.grid(month, today).forEach { d ->
            grid.addView(cell(d.date.dayOfMonth.toString(), if (d.isToday) 700 else 400, if (d.inMonth) 1f else 0.35f, if (d.isToday) Theme.TODAY else null))
        }
    }

    private fun cell(text: String, weight: Int, alpha: Float, circle: Int?): TextView =
        Theme.text(context, 18f, weight, text).apply {
            gravity = Gravity.CENTER
            this.alpha = alpha
            if (circle != null) background = Theme.oval(circle)
            layoutParams = GridLayout.LayoutParams().apply {
                width = Theme.si(44f)
                height = Theme.si(44f)
                setMargins(Theme.si(2f), Theme.si(2f), Theme.si(2f), Theme.si(2f))
            }
        }
}
