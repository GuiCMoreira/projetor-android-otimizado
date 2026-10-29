package br.gui.inicio.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** Zoom lento do descanso de tela, calculado por tempo: o resultado não depende de quantos quadros foram desenhados. */
class KenBurnsTest {
    private val duration = 20_000L

    @Test fun startsWithoutZoom() {
        val f = KenBurns.at(0, duration, dx = 60f)
        assertEquals(1f, f.scale, 0.0001f)
        assertEquals(0f, f.translationX, 0.0001f)
    }

    @Test fun halfwayIsHalfTheWay() {
        val f = KenBurns.at(10_000, duration, dx = 60f)
        assertEquals(1.06f, f.scale, 0.0001f)
        assertEquals(30f, f.translationX, 0.0001f)
    }

    @Test fun stopsAtTheEnd() {
        val f = KenBurns.at(99_000, duration, dx = -60f)
        assertEquals(KenBurns.MAX_SCALE, f.scale, 0.0001f)
        assertEquals(-60f, f.translationX, 0.0001f)
    }

    @Test fun clockGoingBackDoesNotShrinkBelowTheStart() = assertEquals(1f, KenBurns.at(-500, duration, dx = 60f).scale, 0.0001f)

    @Test fun twentyFramesPerSecond() = assertEquals(50L, KenBurns.STEP_MS)
}
