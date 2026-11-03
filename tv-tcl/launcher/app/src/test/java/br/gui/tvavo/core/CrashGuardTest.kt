package br.gui.tvavo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 3 quedas em 5 minutos: a tela inicial para de tentar abrir e mostra a tela de emergência. */
class CrashGuardTest {
    private val min = 60_000L

    @Test fun twoCrashesStillTryTheHome() = assertFalse(CrashGuard.shouldRescue(listOf(0L, min), now = 2 * min))

    @Test fun threeCrashesInFiveMinutesRescue() = assertTrue(CrashGuard.shouldRescue(listOf(0L, min, 2 * min), now = 3 * min))

    @Test fun oldCrashesDoNotCount() = assertFalse(CrashGuard.shouldRescue(listOf(0L, min, 2 * min), now = 8 * min))

    @Test fun recordKeepsOnlyTheWindow() = assertEquals(listOf(6 * min, 7 * min), CrashGuard.record(listOf(0L, 6 * min), now = 7 * min))

    @Test fun clockGoingBackDoesNotKeepOldCrashesForever() =
        assertEquals(listOf(1_000L), CrashGuard.record(listOf(50 * min), now = 1_000L))

    @Test fun storedTextRoundTrips() = assertEquals(listOf(1L, 2L), CrashGuard.parse(CrashGuard.format(listOf(1L, 2L))))

    @Test fun brokenTextIsIgnored() = assertEquals(listOf(5L), CrashGuard.parse("x,5,,"))
}
