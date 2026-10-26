package br.gui.inicio.core

import br.gui.inicio.core.NewAppsTracker.ensureInitialized
import br.gui.inicio.core.NewAppsTracker.isNew
import br.gui.inicio.core.NewAppsTracker.markOpened
import br.gui.inicio.core.NewAppsTracker.prune
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewAppsTrackerTest {
    private val base = setOf("a.yt", "b.nf")

    @Test fun nothingIsNewBeforeInitialization() = assertFalse(isNew(NewAppsState(), "x.new"))

    @Test fun firstRunMarksInstalledAsKnown() {
        val s = ensureInitialized(NewAppsState(), base)
        assertFalse(isNew(s, "a.yt"))
        assertFalse(isNew(s, "b.nf"))
    }

    @Test fun ensureInitializedRunsOnlyOnce() {
        val first = ensureInitialized(NewAppsState(), base)
        val second = ensureInitialized(first, base + "x.new")
        assertEquals(first, second)
        assertTrue(isNew(second, "x.new"))
    }

    @Test fun openedAppIsNoLongerNew() {
        val s = markOpened(ensureInitialized(NewAppsState(), base), "x.new")
        assertFalse(isNew(s, "x.new"))
    }

    @Test fun reinstalledAppIsNewAgain() {
        var s = markOpened(ensureInitialized(NewAppsState(), base), "x.new")
        s = prune(s, base)                 // x.new desinstalado
        assertTrue(isNew(s, "x.new"))      // reinstalado: novo de novo
        s = prune(s, base - "a.yt")        // app de fábrica desinstalado
        assertTrue(isNew(s, "a.yt"))
    }
}
