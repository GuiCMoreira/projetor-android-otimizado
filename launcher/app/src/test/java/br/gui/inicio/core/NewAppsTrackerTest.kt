package br.gui.inicio.core

import br.gui.inicio.core.NewAppsTracker.ensureInitialized
import br.gui.inicio.core.NewAppsTracker.isNew
import br.gui.inicio.core.NewAppsTracker.markOpened
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tempos em milissegundos; o launcher roda pela primeira vez em t = 1000. */
class NewAppsTrackerTest {
    private val start = ensureInitialized(NewAppsState(), now = 1000)

    @Test fun nothingIsNewBeforeInitialization() = assertFalse(isNew(NewAppsState(), "x.new", installedAt = 5000))

    @Test fun firstRunMarksInstalledAsKnown() = assertFalse(isNew(start, "a.yt", installedAt = 500))

    @Test fun ensureInitializedRunsOnlyOnce() = assertEquals(start, ensureInitialized(start, now = 9000))

    @Test fun appInstalledAfterFirstRunIsNew() = assertTrue(isNew(start, "x.new", installedAt = 2000))

    @Test fun openedAppIsNoLongerNew() =
        assertFalse(isNew(markOpened(start, "x.new", now = 3000), "x.new", installedAt = 2000))

    @Test fun reinstalledAppIsNewAgain() {
        val opened = markOpened(start, "x.new", now = 3000)
        // Desinstalado e reinstalado sem passar pelo launcher: o Android dá uma nova data de instalação.
        assertTrue(isNew(opened, "x.new", installedAt = 4000))
    }

    @Test fun factoryAppReinstalledIsNew() = assertTrue(isNew(start, "a.yt", installedAt = 4000))

    @Test fun openedAtSurvivesSerialization() {
        val s = markOpened(markOpened(start, "x.new", now = 3000), "y.other", now = 3500)
        assertEquals(s, NewAppsState.fromStored(s.initialized, s.initTime, s.storedOpened()))
    }

    @Test fun garbageInStorageIsIgnored() {
        val s = NewAppsState.fromStored(true, 1000, setOf("x.new|3000", "lixo", "y|abc", "|5"))
        assertEquals(mapOf("x.new" to 3000L), s.opened)
    }
}
