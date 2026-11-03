package br.gui.tvmae.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** Troca de foto por horário (vindo do projetor). */
class RotationTest {
    private val hour = 3_600_000L

    @Test fun hourlyRotation() {
        assertEquals(0, Rotation.index(nowMs = 0, intervalMinutes = 60, count = 3))
        assertEquals(1, Rotation.index(nowMs = hour, intervalMinutes = 60, count = 3))
        assertEquals(0, Rotation.index(nowMs = 3 * hour + 5, intervalMinutes = 60, count = 3))
        assertEquals(-1, Rotation.index(nowMs = hour, intervalMinutes = 0, count = 3))
        assertEquals(-1, Rotation.index(nowMs = hour, intervalMinutes = 60, count = 0))
    }
}
