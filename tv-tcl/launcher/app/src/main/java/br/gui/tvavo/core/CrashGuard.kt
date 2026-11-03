package br.gui.tvavo.core

/** Quedas recentes da tela inicial. Com [LIMIT] quedas em [WINDOW_MS], ela abre a tela de emergência em vez de tentar de novo. */
object CrashGuard {
    const val LIMIT = 3
    const val WINDOW_MS = 5 * 60_000L

    /** Acrescenta [now] e descarta o que saiu da janela (ou ficou no futuro, se o relógio voltou). */
    fun record(history: List<Long>, now: Long): List<Long> = recent(history, now) + now

    fun shouldRescue(history: List<Long>, now: Long): Boolean = recent(history, now).size >= LIMIT

    fun format(history: List<Long>): String = history.joinToString(",")

    fun parse(text: String?): List<Long> = text.orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }

    private fun recent(history: List<Long>, now: Long): List<Long> = history.filter { it in (now - WINDOW_MS)..now }
}
