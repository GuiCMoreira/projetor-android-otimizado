package br.gui.inicio.core

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockFormatTest {
    private val t = LocalDateTime.of(2026, 10, 26, 20, 45)

    @Test fun longDateInPortuguese() = assertEquals("segunda-feira, 26 de outubro", ClockFormat.longDate(t))

    @Test fun time24h() = assertEquals("20:45", ClockFormat.time(t))

    @Test fun earlyHourKeepsLeadingZero() = assertEquals("07:05", ClockFormat.time(t.withHour(7).withMinute(5)))

    @Test fun menuBarHasNoDots() = assertEquals("seg 26 out 20:45", ClockFormat.menuBar(t))
}
