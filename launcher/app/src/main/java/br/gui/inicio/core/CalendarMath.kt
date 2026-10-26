package br.gui.inicio.core

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CalendarDay(val date: LocalDate, val inMonth: Boolean, val isToday: Boolean)

object CalendarMath {
    private val TITLE = DateTimeFormatter.ofPattern("LLLL 'de' yyyy", Locale.forLanguageTag("pt-BR"))

    /** Seis semanas de sete dias, começando no domingo, como o calendário do macOS. */
    fun grid(month: YearMonth, today: LocalDate): List<CalendarDay> {
        val first = month.atDay(1)
        val offset = first.dayOfWeek.value % 7 // domingo = 0
        val start = first.minusDays(offset.toLong())
        return (0 until 42).map { i ->
            val d = start.plusDays(i.toLong())
            CalendarDay(d, YearMonth.from(d) == month, d == today)
        }
    }

    fun title(month: YearMonth): String = month.format(TITLE)
}
