package br.gui.inicio.core

/**
 * Mouse aéreo: devolve o item que deve ganhar foco quando o ponteiro entra nele, e null enquanto continua no mesmo.
 * Assim o tremor da mão parada sobre um ícone não rouba o foco de quem está andando com as setas.
 *
 * [foreignFocus]: o foco atual foi posto por outra coisa (setas, painel que fechou). Aí o ponteiro só volta a mandar
 * depois de andar [MIN_MOVE_PX], mesmo que o item embaixo dele tenha sido recriado (capas que chegaram, Dock remontado).
 */
class HoverTracker<T : Any> {
    private var last: T? = null
    private var anchorX = Float.NaN
    private var anchorY = Float.NaN

    fun enter(target: T?, x: Float, y: Float, foreignFocus: Boolean): T? {
        if (foreignFocus) {
            if (anchorX.isNaN()) {
                anchorX = x
                anchorY = y
            }
            val dx = x - anchorX
            val dy = y - anchorY
            if (dx * dx + dy * dy < MIN_MOVE_PX * MIN_MOVE_PX) {
                last = target
                return null
            }
            anchorX = Float.NaN
            last = target
            return target
        }
        anchorX = Float.NaN
        if (target === last) return null
        last = target
        return target
    }

    private companion object {
        const val MIN_MOVE_PX = 12f
    }
}

/** Descanso de tela: o mouse só acorda depois de andar [MIN_PX] desde o primeiro movimento visto. */
class PointerWake {
    private var startX = Float.NaN
    private var startY = Float.NaN

    fun moved(x: Float, y: Float): Boolean {
        if (startX.isNaN()) {
            startX = x
            startY = y
            return false
        }
        val dx = x - startX
        val dy = y - startY
        return dx * dx + dy * dy >= MIN_PX * MIN_PX
    }

    fun reset() {
        startX = Float.NaN
        startY = Float.NaN
    }

    private companion object {
        const val MIN_PX = 60f
    }
}

/**
 * Aviso de pouca memória (onTrimMemory). Os níveis repetem os de ComponentCallbacks2, para testar sem Android.
 * Escondida há pouco (UI_HIDDEN), a tela inicial guarda tudo para voltar na hora; com memória baixa de verdade ou com o
 * processo já na lista de espera, solta papel de parede e capas.
 */
object MemoryTrim {
    const val RUNNING_MODERATE = 5
    const val RUNNING_LOW = 10
    const val RUNNING_CRITICAL = 15
    const val UI_HIDDEN = 20
    const val BACKGROUND = 40
    const val MODERATE = 60
    const val COMPLETE = 80

    fun shouldRelease(level: Int, homeVisible: Boolean): Boolean =
        !homeVisible && (level >= BACKGROUND || level == RUNNING_LOW || level == RUNNING_CRITICAL)
}

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
