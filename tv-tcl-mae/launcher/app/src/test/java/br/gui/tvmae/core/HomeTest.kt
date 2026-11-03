package br.gui.tvmae.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Bom dia", "Boa tarde" ou "Boa noite" com o nome dela, conforme a hora. */
class GreetingTest {
    private val name = "Ana e João"

    @Test fun morning() = assertEquals("Bom dia, Ana e João", Greeting.at(5, name))

    @Test fun lateMorning() = assertEquals("Bom dia, Ana e João", Greeting.at(11, name))

    @Test fun afternoon() = assertEquals("Boa tarde, Ana e João", Greeting.at(12, name))

    @Test fun evening() = assertEquals("Boa noite, Ana e João", Greeting.at(18, name))

    @Test fun earlyMorningIsStillNight() = assertEquals("Boa noite, Ana e João", Greeting.at(3, name))

    @Test fun withoutNameNoComma() = assertEquals("Boa tarde", Greeting.at(15, " "))
}

/**
 * Setas na tela da mãe: três fileiras (atalhos no alto, Continuar assistindo, faixa de apps). Cada fileira lembra
 * onde o foco estava, como nas smart TVs; fileira vazia é pulada.
 */
class HomeNavTest {
    private val sizes = intArrayOf(5, 4, 9) // atalhos, continuar assistindo, apps (8 fixos + "Todos os apps")

    @Test fun rightAlongTheRow() = assertEquals(HomeNav.Pos(2, 1), HomeNav().move(HomeNav.Pos(2, 0), Dir.RIGHT, sizes))

    @Test fun rightStopsAtTheEnd() = assertEquals(HomeNav.Pos(2, 8), HomeNav().move(HomeNav.Pos(2, 8), Dir.RIGHT, sizes))

    @Test fun leftStopsAtTheStart() = assertEquals(HomeNav.Pos(1, 0), HomeNav().move(HomeNav.Pos(1, 0), Dir.LEFT, sizes))

    @Test fun upGoesToTheRowAbove() = assertEquals(1, HomeNav().move(HomeNav.Pos(2, 0), Dir.UP, sizes).row)

    @Test fun eachRowRemembersWhereTheFocusWas() {
        val nav = HomeNav()
        nav.move(HomeNav.Pos(1, 2), Dir.DOWN, sizes) // sai do terceiro cartão
        assertEquals(HomeNav.Pos(1, 2), nav.move(HomeNav.Pos(2, 5), Dir.UP, sizes)) // volta para ele
    }

    @Test fun rememberedColumnIsClampedToTheRowSize() {
        val nav = HomeNav()
        nav.move(HomeNav.Pos(2, 7), Dir.UP, sizes)
        assertEquals(HomeNav.Pos(2, 2), nav.move(HomeNav.Pos(1, 0), Dir.DOWN, intArrayOf(5, 4, 3)))
    }

    @Test fun emptyRowIsSkipped() = assertEquals(0, HomeNav().move(HomeNav.Pos(2, 0), Dir.UP, intArrayOf(5, 0, 9)).row)

    @Test fun downFromTheBottomStays() = assertEquals(HomeNav.Pos(2, 3), HomeNav().move(HomeNav.Pos(2, 3), Dir.DOWN, sizes))

    @Test fun startsOnContinueWatchingWhenThereIsSomething() = assertEquals(HomeNav.Pos(1, 0), HomeNav.start(sizes))

    @Test fun startsOnTheAppsWhenNothingToContinue() = assertEquals(HomeNav.Pos(2, 0), HomeNav.start(intArrayOf(5, 0, 9)))
}

/** Apps fixos na faixa: começa com os apps que ela tem; no máximo 8 (mais o "Todos os apps"). */
class PinsTest {
    @Test fun defaultIsHerApps() = assertEquals(
        listOf("com.globo.globotv", "com.netflix.ninja", "com.google.android.youtube.tv"),
        Pins.DEFAULT,
    )

    @Test fun neverChosenUsesTheDefault() = assertEquals(Pins.DEFAULT, Pins.parse(null))

    @Test fun emptyChoiceStaysEmpty() = assertEquals(emptyList<String>(), Pins.parse(""))

    @Test fun roundTrip() = assertEquals(listOf("a", "b"), Pins.parse(Pins.format(listOf("a", "b"))))

    @Test fun addGoesToTheEnd() {
        val r = Pins.toggle(listOf("a"), "b")
        assertEquals(Pins.Change.ADDED, r.change)
        assertEquals(listOf("a", "b"), r.pins)
    }

    @Test fun toggleAgainRemoves() {
        val r = Pins.toggle(listOf("a", "b"), "a")
        assertEquals(Pins.Change.REMOVED, r.change)
        assertEquals(listOf("b"), r.pins)
    }

    @Test fun ninthIsRefused() {
        val eight = (1..8).map { "p$it" }
        val r = Pins.toggle(eight, "p9")
        assertEquals(Pins.Change.FULL, r.change)
        assertEquals(eight, r.pins)
    }

    @Test fun uninstalledAppsAreHiddenNotForgotten() =
        assertEquals(listOf("a", "c"), Pins.visible(listOf("a", "b", "c"), installed = setOf("a", "c")))

    @Test fun repeatedEntriesOnlyOnce() = assertEquals(listOf("a"), Pins.parse("a,a"))

    @Test fun brokenTextIgnoresBlanks() = assertNull(Pins.parse("a,,b").firstOrNull { it.isBlank() })
}

/** Revisão: app fixado e depois desinstalado não pode ocupar vaga que ninguém consegue liberar. */
class PinsInstalledTest {
    private val full = (1..8).map { "p$it" }

    @Test fun ghostDoesNotCountTowardTheLimit() {
        val r = Pins.toggle(full, "novo", installed = (full - "p3").toSet() + "novo")
        assertEquals(Pins.Change.ADDED, r.change)
        assertTrue("novo" in r.pins)
        assertFalse("p3" in r.pins)
    }

    @Test fun fullOfInstalledAppsIsStillFull() =
        assertEquals(Pins.Change.FULL, Pins.toggle(full, "novo", installed = full.toSet() + "novo").change)
}

/** Clima: guardado só vale por algumas horas; sem internet, tenta de novo com folga, e relógio que volta não trava. */
class WeatherPolicyTest {
    private val h = 3_600_000L

    @Test fun recentCacheIsShown() = assertTrue(WeatherPolicy.showCached(now = 10 * h, savedAt = 9 * h))

    @Test fun oldCacheIsHidden() = assertFalse(WeatherPolicy.showCached(now = 10 * h, savedAt = 6 * h))

    @Test fun neverSavedIsHidden() = assertFalse(WeatherPolicy.showCached(now = 10 * h, savedAt = 0))

    @Test fun freshWeatherIsNotFetched() = assertFalse(WeatherPolicy.shouldFetch(now = 10 * h, savedAt = 10 * h - 60_000, lastAttempt = 0))

    @Test fun oldWeatherIsFetched() = assertTrue(WeatherPolicy.shouldFetch(now = 10 * h, savedAt = 9 * h, lastAttempt = 0))

    @Test fun afterAFailureWaitsBeforeTryingAgain() =
        assertFalse(WeatherPolicy.shouldFetch(now = 10 * h, savedAt = 0, lastAttempt = 10 * h - 60_000))

    @Test fun afterTheWaitTriesAgain() =
        assertTrue(WeatherPolicy.shouldFetch(now = 10 * h, savedAt = 0, lastAttempt = 10 * h - 11 * 60_000))

    @Test fun clockGoingBackCountsAsOld() = assertTrue(WeatherPolicy.shouldFetch(now = 5 * h, savedAt = 9 * h, lastAttempt = 0))
}

/** "Todos os apps" esconde as peças da TCL que precisam continuar no sistema mas não servem para ela. */
class HiddenAppsTest {
    @Test fun tclSystemPiecesAreHidden() {
        listOf("com.tcl.guard", "com.tcl.suspension", "com.tcl.dashboard", "com.tcl.tv", "com.tcl.smartalexa").forEach {
            assertFalse(it, HiddenApps.listed(it))
        }
    }

    @Test fun herAppsAndTheGrandsonsToolsStay() {
        listOf("com.globo.globotv", "com.google.android.youtube.tv", "tv.pluto.android", "com.android.vending", "com.android.tv.settings")
            .forEach { assertTrue(it, HiddenApps.listed(it)) }
    }
}
