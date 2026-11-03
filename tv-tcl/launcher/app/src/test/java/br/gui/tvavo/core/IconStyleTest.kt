package br.gui.tvavo.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ícones: logo opaco até a borda preenche o quadrado; logo solto fica sobre o branco. Vindo do projetor (V12Test). */
class IconStyleTest {
    private val yellow = 0xFFFFF301.toInt()
    private val clear = 0x00000000

    // Ícones: logo opaco até a borda (Pluto, Crunchyroll) preenche; logo solto (Play Store) fica sobre o branco
    @Test fun opaqueEdgesFillWithTheirColor() =
        assertEquals(IconFill.Full(yellow), IconStyle.classify(IntArray(12) { yellow }))

    @Test fun transparentEdgesKeepWhitePadding() =
        assertEquals(IconFill.Padded, IconStyle.classify(IntArray(12) { clear }))

    @Test fun twoTransparentSamplesStillFill() {
        val s = IntArray(12) { yellow }.also { it[0] = clear; it[5] = clear }
        assertEquals(IconFill.Full(yellow), IconStyle.classify(s))
    }

    @Test fun mostlyTransparentKeepsPadding() {
        val s = IntArray(12) { clear }.also { it[1] = yellow; it[4] = yellow }
        assertEquals(IconFill.Padded, IconStyle.classify(s))
    }

    @Test fun fillUsesTheMostCommonEdgeColor() {
        val orange = 0xFFFF5E00.toInt()
        val s = IntArray(12) { orange }.also { it[3] = 0xFFFFFFFF.toInt() }
        assertEquals(IconFill.Full(orange), IconStyle.classify(s))
    }

    @Test fun edgeSamplesStayInsideTheImage() {
        val pts = IconStyle.edgeSamples(48, 48)
        assertEquals(12, pts.size)
        assertTrue(pts.all { (x, y) -> x in 0 until 48 && y in 0 until 48 })
    }

    @Test fun netflixTvKeepsItsSize() {
        assertEquals(1f, IconStyle.foregroundScale("com.netflix.ninja"), 0f)
        assertEquals(1f, IconStyle.foregroundScale("com.crunchyroll.crunchyroid"), 0f)
    }

    /** A Pluto 5.x para TV tem ícone adaptativo com o logo maior que a área segura: o "tv" saía cortado. */
    @Test fun plutoLogoFitsTheSquare() = assertEquals(0.8f, IconStyle.foregroundScale("tv.pluto.android"), 0f)
}
