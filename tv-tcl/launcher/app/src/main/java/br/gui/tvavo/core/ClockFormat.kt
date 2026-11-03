package br.gui.tvavo.core

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ClockFormat {
    private val PT_BR = Locale.forLanguageTag("pt-BR")
    private val LONG_DATE = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR)
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", PT_BR)

    fun longDate(t: LocalDateTime): String = LONG_DATE.format(t)

    fun time(t: LocalDateTime): String = TIME.format(t)

}
