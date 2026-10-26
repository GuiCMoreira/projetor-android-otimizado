package br.gui.inicio.core

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Lógica pura da versão 1.1: pendências da revisão, troca de fundo, opções, timer e volume. */
class V11LogicTest {
    // M1: recorte nunca vazio
    @Test fun tinyPhotoCropNeverEmpty() = assertEquals(CropRect(0, 0, 1, 1), ImageMath.coverCrop(1, 1, 1920, 1080))

    // M2: teto de pixels decodificados (4x a tela)
    @Test fun panoramaRespectsPixelCap() = assertEquals(4, ImageMath.sampleSize(40000, 1000, 1920, 1080))

    @Test fun normalPhotoUnaffectedByCap() = assertEquals(2, ImageMath.sampleSize(3840, 2160, 1920, 1080))

    // M8: mover troca com o vizinho visível
    @Test fun moveSkipsHiddenFavorites() {
        val c = DockConfig(listOf("a.yt", "x.gone", "b.nf", "c.cr"))
        val installed = setOf("a.yt", "b.nf", "c.cr")
        assertEquals(listOf("b.nf", "x.gone", "a.yt", "c.cr"), c.move("a.yt", 1, installed).packages)
        assertEquals(c, c.move("a.yt", -1, installed))
        assertEquals(c, c.move("c.cr", 1, installed))
    }

    // M11: grade do calendário na virada do ano
    @Test fun january2027StartsOnDecember27() {
        val g = CalendarMath.grid(YearMonth.of(2027, 1), LocalDate.of(2026, 10, 26))
        assertEquals(LocalDate.of(2026, 12, 27), g[0].date)
        assertEquals(LocalDate.of(2027, 1, 1), g[5].date)
    }

    // Troca automática de fundo
    @Test fun rotationPicksPhotoByClock() {
        val hour = 60L * 60 * 1000
        assertEquals(0, Rotation.index(nowMs = 0, intervalMinutes = 60, count = 3))
        assertEquals(1, Rotation.index(nowMs = hour, intervalMinutes = 60, count = 3))
        assertEquals(0, Rotation.index(nowMs = 3 * hour + 5, intervalMinutes = 60, count = 3))
        assertEquals(-1, Rotation.index(nowMs = hour, intervalMinutes = 0, count = 3))
        assertEquals(-1, Rotation.index(nowMs = hour, intervalMinutes = 60, count = 0))
    }

    // Opções em ciclo (troca de fundo, descanso de tela, timer)
    @Test fun optionsCycleAndWrap() {
        val opts = listOf(0, 15, 30)
        assertEquals(15, Options.next(opts, 0))
        assertEquals(0, Options.next(opts, 30))
        assertEquals(0, Options.next(opts, 7)) // valor fora da lista volta ao começo
    }

    @Test fun sleepTimerMinutesLeftRoundsUp() {
        assertEquals(42, SleepTimer.minutesLeft(nowMs = 0, atMs = 41L * 60_000 + 1))
        assertEquals(0, SleepTimer.minutesLeft(nowMs = 10, atMs = 5))
    }

    @Test fun volumeStepsStayInRange() {
        assertEquals(15, Volume.step(15, 15, 1))
        assertEquals(0, Volume.step(0, 15, -1))
        assertEquals(8, Volume.step(7, 15, 1))
        assertEquals(47, Volume.percent(7, 15))
        assertEquals(0, Volume.percent(3, 0))
    }

    @Test fun cityRoundTripAndGarbage() {
        val c = City("Bragança Paulista", "São Paulo", -22.9434, -46.5581)
        assertEquals(c, City.parse(c.serialize()))
        assertNull(City.parse("lixo"))
        assertNull(City.parse(null))
    }
}
