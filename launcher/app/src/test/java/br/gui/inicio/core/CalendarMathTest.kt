package br.gui.inicio.core

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarMathTest {
    private val today = LocalDate.of(2026, 10, 26)

    @Test fun october2026StartsOnSundaySeptember27() {
        val g = CalendarMath.grid(YearMonth.of(2026, 10), today)
        assertEquals(42, g.size)
        assertEquals(LocalDate.of(2026, 9, 27), g[0].date)
        assertFalse(g[0].inMonth)
        assertEquals(LocalDate.of(2026, 10, 1), g[4].date)
        assertTrue(g[4].inMonth)
    }

    @Test fun todayIsMarkedOnlyOnce() {
        val g = CalendarMath.grid(YearMonth.of(2026, 10), today)
        assertEquals(listOf(29), g.indices.filter { g[it].isToday })
        assertTrue(CalendarMath.grid(YearMonth.of(2026, 11), today).none { it.isToday })
    }

    @Test fun leapFebruary2028() {
        val g = CalendarMath.grid(YearMonth.of(2028, 2), today)
        assertEquals(LocalDate.of(2028, 2, 29), g[30].date)
        assertTrue(g[30].inMonth)
        assertEquals(LocalDate.of(2028, 3, 1), g[31].date)
        assertFalse(g[31].inMonth)
    }

    @Test fun decemberToJanuaryTitle() {
        assertEquals("outubro de 2026", CalendarMath.title(YearMonth.of(2026, 10)))
        assertEquals("janeiro de 2027", CalendarMath.title(YearMonth.of(2026, 12).plusMonths(1)))
    }
}
