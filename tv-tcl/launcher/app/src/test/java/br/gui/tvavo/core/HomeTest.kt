package br.gui.tvavo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Bom dia", "Boa tarde" ou "Boa noite" com o nome dela, conforme a hora. */
class GreetingTest {
    private val name = "Dona Cida"

    @Test fun morning() = assertEquals("Bom dia, Dona Cida", Greeting.at(5, name))

    @Test fun lateMorning() = assertEquals("Bom dia, Dona Cida", Greeting.at(11, name))

    @Test fun afternoon() = assertEquals("Boa tarde, Dona Cida", Greeting.at(12, name))

    @Test fun evening() = assertEquals("Boa noite, Dona Cida", Greeting.at(18, name))

    @Test fun earlyMorningIsStillNight() = assertEquals("Boa noite, Dona Cida", Greeting.at(3, name))

    @Test fun withoutNameNoComma() = assertEquals("Boa tarde", Greeting.at(15, " "))
}

/** Até 4 apps numa fileira de botões grandes; 5 ou 6 em duas fileiras de 3, com o ícone ao lado do nome. */
class HomeGridTest {
    @Test fun threeAppsOneRowOfThree() {
        val cells = HomeGrid.cells(3)
        assertEquals(3, cells.size)
        assertEquals(1, cells.map { it.top }.distinct().size)
        assertFalse(cells.any { it.compact })
    }

    @Test fun fourAppsStillOneRow() = assertEquals(1, HomeGrid.cells(4).map { it.top }.distinct().size)

    @Test fun sixAppsTwoRowsOfThreeCompact() {
        val cells = HomeGrid.cells(6)
        assertEquals(2, cells.map { it.top }.distinct().size)
        assertEquals(3, cells.count { it.top == cells[0].top })
        assertTrue(cells.all { it.compact })
    }

    @Test fun neverMoreThanSix() = assertEquals(HomeGrid.MAX, HomeGrid.cells(9).size)

    @Test fun rowFitsTheScreenWithMargins() {
        for (n in 1..6) {
            val cells = HomeGrid.cells(n)
            assertTrue("n=$n começa na margem", cells.minOf { it.left } >= HomeGrid.SIDE)
            assertTrue("n=$n cabe na tela", cells.maxOf { it.left + it.width } <= HomeGrid.WIDTH - HomeGrid.SIDE)
        }
    }

    @Test fun oneAppIsNotAGiantStrip() = assertTrue(HomeGrid.cells(1)[0].width <= HomeGrid.cells(3)[0].width)
}

/** Setas na tela inicial; o índice [n] é o "Todos os apps", embaixo de tudo. */
class HomeNavTest {
    private fun move(i: Int, d: Dir, n: Int, last: Int = 0) = HomeGrid.move(i, d, n, last)

    @Test fun rightAlongTheRow() = assertEquals(1, move(0, Dir.RIGHT, 3))

    @Test fun rightStopsAtTheEnd() = assertEquals(2, move(2, Dir.RIGHT, 3))

    @Test fun leftStopsAtTheStart() = assertEquals(0, move(0, Dir.LEFT, 3))

    @Test fun downFromTheRowGoesToAllApps() = assertEquals(3, move(1, Dir.DOWN, 3))

    @Test fun upFromAllAppsGoesBackWhereItCameFrom() = assertEquals(2, move(3, Dir.UP, 3, last = 2))

    @Test fun downInTwoRowsGoesToTheSecondRow() = assertEquals(4, move(1, Dir.DOWN, 6))

    @Test fun downFromTheSecondRowGoesToAllApps() = assertEquals(6, move(4, Dir.DOWN, 6))

    @Test fun downWhenTheCellBelowIsEmptyGoesToAllApps() = assertEquals(5, move(2, Dir.DOWN, 5))

    @Test fun upFromTheSecondRow() = assertEquals(0, move(3, Dir.UP, 6))

    @Test fun rightDoesNotWrapToTheNextRow() = assertEquals(2, move(2, Dir.RIGHT, 6))

    @Test fun noAppsOnlyAllApps() = assertEquals(0, move(0, Dir.UP, 0))
}

/** Apps fixos na tela inicial: começa com Globoplay, YouTube e Pluto TV; no máximo 6. */
class PinsTest {
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

    @Test fun seventhIsRefused() {
        val six = (1..6).map { "p$it" }
        val r = Pins.toggle(six, "p7")
        assertEquals(Pins.Change.FULL, r.change)
        assertEquals(six, r.pins)
    }

    @Test fun uninstalledAppsAreHiddenNotForgotten() =
        assertEquals(listOf("a", "c"), Pins.visible(listOf("a", "b", "c"), installed = setOf("a", "c")))

    @Test fun repeatedEntriesOnlyOnce() = assertEquals(listOf("a"), Pins.parse("a,a"))

    @Test fun brokenTextIgnoresBlanks() = assertNull(Pins.parse("a,,b").firstOrNull { it.isBlank() })
}

/** Revisão: app fixado e depois desinstalado não pode ocupar vaga que ninguém consegue liberar. */
class PinsInstalledTest {
    private val six = (1..6).map { "p$it" }

    @Test fun ghostDoesNotCountTowardTheLimit() {
        val r = Pins.toggle(six, "novo", installed = (six - "p3").toSet() + "novo")
        assertEquals(Pins.Change.ADDED, r.change)
        assertTrue("novo" in r.pins)
        assertFalse("p3" in r.pins)
    }

    @Test fun fullOfInstalledAppsIsStillFull() =
        assertEquals(Pins.Change.FULL, Pins.toggle(six, "novo", installed = six.toSet() + "novo").change)
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
