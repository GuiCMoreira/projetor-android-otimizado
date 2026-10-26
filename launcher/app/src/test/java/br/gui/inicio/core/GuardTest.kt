package br.gui.inicio.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GuardTest {
    @Test fun returnsBlockValueWhenNothingFails() = assertEquals(7, Guard.run({ -1 }) { 7 })

    @Test fun runtimeExceptionBecomesFallback() =
        assertEquals(-1, Guard.run({ -1 }) { throw IllegalStateException("ícone quebrado") })

    @Test fun outOfMemoryBecomesFallback() =
        assertEquals("degradê", Guard.run({ "degradê" }) { throw OutOfMemoryError("foto enorme") })

    @Test fun fallbackReceivesTheError() =
        assertEquals("boom", Guard.run({ it.message }) { throw RuntimeException("boom") })
}
