package br.gui.inicio.core

import br.gui.inicio.core.WakeKey.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Correções da revisão final da 1.1 (I1 e C2). */
class V11FixesTest {
    private val ok = 23
    private val right = 22

    // I1: a tecla que acorda do descanso é consumida inteira, inclusive repetições
    @Test fun wakeKeyIsSwallowedUntilItsUp() {
        val k = WakeKey()
        assertEquals(Decision.WAKE, k.handle(down = true, keyCode = ok, repeat = 0, saverRunning = true))
        assertEquals(Decision.CONSUME, k.handle(down = true, keyCode = ok, repeat = 1, saverRunning = false))
        assertEquals(Decision.CONSUME, k.handle(down = false, keyCode = ok, repeat = 0, saverRunning = false))
        assertEquals(Decision.PASS, k.handle(down = true, keyCode = ok, repeat = 0, saverRunning = false))
    }

    @Test fun lostUpDoesNotEatTheNextKey() {
        val k = WakeKey()
        k.handle(down = true, keyCode = right, repeat = 0, saverRunning = true) // UP de RIGHT nunca chega
        assertEquals(Decision.PASS, k.handle(down = true, keyCode = ok, repeat = 0, saverRunning = false))
        assertEquals(Decision.PASS, k.handle(down = false, keyCode = ok, repeat = 0, saverRunning = false))
    }

    @Test fun normalKeysPassWhenSaverIsOff() =
        assertEquals(Decision.PASS, WakeKey().handle(down = true, keyCode = ok, repeat = 0, saverRunning = false))

    // C2: clima não tenta a cada minuto depois de falhar; relógio que volta conta como velho
    private val min = 60_000L

    @Test fun freshWeatherIsNotFetched() = assertFalse(WeatherSchedule.shouldFetch(nowMs = 100 * min, weatherAt = 90 * min, lastAttemptMs = 0))

    @Test fun staleWeatherIsFetched() = assertTrue(WeatherSchedule.shouldFetch(nowMs = 100 * min, weatherAt = 60 * min, lastAttemptMs = 0))

    @Test fun failedAttemptWaitsTenMinutes() {
        assertFalse(WeatherSchedule.shouldFetch(nowMs = 100 * min, weatherAt = 0, lastAttemptMs = 95 * min))
        assertTrue(WeatherSchedule.shouldFetch(nowMs = 100 * min, weatherAt = 0, lastAttemptMs = 89 * min))
    }

    @Test fun clockGoingBackCountsAsStale() = assertTrue(WeatherSchedule.shouldFetch(nowMs = 10 * min, weatherAt = 500 * min, lastAttemptMs = 0))
}
