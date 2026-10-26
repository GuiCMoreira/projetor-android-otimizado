package br.gui.inicio.core

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ClockFormat {
    private val PT_BR = Locale.forLanguageTag("pt-BR")
    private val LONG_DATE = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR)
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", PT_BR)
    private val MENU_BAR = DateTimeFormatter.ofPattern("EEE d MMM HH:mm", PT_BR)

    fun longDate(t: LocalDateTime): String = LONG_DATE.format(t)

    fun time(t: LocalDateTime): String = TIME.format(t)

    /** O Java põe ponto nas abreviações em pt-BR ("seg."); o macOS não. */
    fun menuBar(t: LocalDateTime): String = MENU_BAR.format(t).replace(".", "")
}
