package br.gui.inicio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mouse aéreo: o foco vai para o item sob o ponteiro, mas a mão tremendo sobre o mesmo item não o puxa de volta. */
class HoverTrackerTest {
    private fun HoverTracker<String>.at(target: String?, x: Float = 100f, foreign: Boolean = false) = enter(target, x, 100f, foreign)

    @Test fun enteringAnItemFocusesIt() = assertEquals("dock", HoverTracker<String>().at("dock"))

    @Test fun shakingOverTheSameItemDoesNothing() {
        val t = HoverTracker<String>()
        t.at("dock")
        assertNull(t.at("dock", x = 103f))
    }

    @Test fun leavingAndComingBackFocusesAgain() {
        val t = HoverTracker<String>()
        t.at("netflix")
        assertNull(t.at(null))
        assertEquals("netflix", t.at("netflix"))
    }

    @Test fun movingToAnotherItemFocusesIt() {
        val t = HoverTracker<String>()
        t.at("netflix")
        assertEquals("pluto", t.at("pluto"))
    }

    @Test fun afterArrowsTheParkedPointerDoesNotStealFocus() {
        // As setas levaram o foco para o Dock; o ponteiro parado sobre um cartão (até recriado) não o puxa de volta.
        val t = HoverTracker<String>()
        t.at("cartao")
        assertNull(t.at("cartao-novo", x = 100f, foreign = true))
        assertNull(t.at("cartao-novo", x = 106f, foreign = true))
    }

    @Test fun afterArrowsARealMoveGivesFocusBackToThePointer() {
        val t = HoverTracker<String>()
        t.at("cartao")
        t.at("cartao", x = 100f, foreign = true)
        assertEquals("cartao", t.at("cartao", x = 115f, foreign = true))
    }
}

/** No descanso de tela, só um movimento de verdade do mouse acorda; o tremor do controle parado, não. */
class PointerWakeTest {
    @Test fun firstEventOnlyMarksThePlace() = assertFalse(PointerWake().moved(500f, 500f))

    @Test fun smallJitterDoesNotWake() {
        val w = PointerWake()
        w.moved(500f, 500f)
        assertFalse(w.moved(520f, 490f))
    }

    @Test fun realMovementWakes() {
        val w = PointerWake()
        w.moved(500f, 500f)
        assertTrue(w.moved(600f, 500f))
    }

    @Test fun resetForgetsTheStartingPoint() {
        val w = PointerWake()
        w.moved(0f, 0f)
        w.reset()
        assertFalse(w.moved(900f, 900f))
    }
}

/** Aviso de pouca memória do Android: a tela inicial solta papel de parede e capas só quando não está à vista. */
class MemoryTrimTest {
    @Test fun nothingWhileTheHomeIsOnScreen() = assertFalse(MemoryTrim.shouldRelease(MemoryTrim.RUNNING_CRITICAL, homeVisible = true))

    @Test fun justHiddenKeepsEverythingForAnInstantReturn() = assertFalse(MemoryTrim.shouldRelease(MemoryTrim.UI_HIDDEN, homeVisible = false))

    @Test fun moderateMemoryWhileAnAppPlaysKeepsEverything() =
        assertFalse(MemoryTrim.shouldRelease(MemoryTrim.RUNNING_MODERATE, homeVisible = false))

    @Test fun lowMemoryWhileAnAppPlaysReleases() {
        listOf(MemoryTrim.RUNNING_LOW, MemoryTrim.RUNNING_CRITICAL).forEach {
            assertTrue(MemoryTrim.shouldRelease(it, homeVisible = false))
        }
    }

    @Test fun inBackgroundListReleases() {
        listOf(MemoryTrim.BACKGROUND, MemoryTrim.MODERATE, MemoryTrim.COMPLETE).forEach {
            assertTrue(MemoryTrim.shouldRelease(it, homeVisible = false))
        }
    }
}

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
