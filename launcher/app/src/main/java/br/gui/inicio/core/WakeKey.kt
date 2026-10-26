package br.gui.inicio.core

/**
 * A tecla que acorda do descanso de tela é consumida inteira: o DOWN, as repetições de quem segura
 * e o UP. Se o UP se perder, o próximo toque novo (repetição 0) zera o estado.
 */
class WakeKey {
    enum class Decision { WAKE, CONSUME, PASS }

    private var key: Int? = null

    fun handle(down: Boolean, keyCode: Int, repeat: Int, saverRunning: Boolean): Decision {
        if (down && saverRunning) {
            key = keyCode
            return Decision.WAKE
        }
        if (down && repeat == 0) key = null
        if (key == keyCode) {
            if (!down) key = null
            return Decision.CONSUME
        }
        return Decision.PASS
    }
}

/** Quando buscar o clima: guardado com mais de 30 min, e no máximo uma tentativa a cada 10 min. */
object WeatherSchedule {
    const val REFRESH_MS = 30 * 60_000L
    const val RETRY_MS = 10 * 60_000L

    /** Relógio que voltou no tempo (horário acertado depois do boot) também conta como velho. */
    fun isStale(nowMs: Long, weatherAt: Long): Boolean = weatherAt > nowMs || nowMs - weatherAt > REFRESH_MS

    fun shouldFetch(nowMs: Long, weatherAt: Long, lastAttemptMs: Long): Boolean =
        isStale(nowMs, weatherAt) && (lastAttemptMs > nowMs || nowMs - lastAttemptMs >= RETRY_MS)
}
