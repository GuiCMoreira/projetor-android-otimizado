package br.gui.inicio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockConfigTest {
    private val all = setOf("a.yt", "b.nf", "c.cr", "d.pl", "e.x", "f.x", "g.x", "h.x", "i.x")
    private fun cfg(vararg p: String) = DockConfig(p.toList())

    @Test fun parseNullReturnsDefault() = assertEquals(DockConfig.DEFAULT, DockConfig.parse(null))

    @Test fun parseEmptyMeansEmptyDock() = assertEquals(emptyList<String>(), DockConfig.parse("").packages)

    @Test fun parseDropsGarbageDuplicatesAndExtra() {
        val raw = "a.yt,,lixo,a.yt,../x,b.nf,c.cr,d.pl,e.x,f.x,g.x,h.x,i.x"
        assertEquals(
            listOf("a.yt", "b.nf", "c.cr", "d.pl", "e.x", "f.x", "g.x", "h.x"),
            DockConfig.parse(raw).packages,
        )
    }

    @Test fun serializeRoundTrip() {
        val c = cfg("a.yt", "b.nf")
        assertEquals(c, DockConfig.parse(c.serialize()))
    }

    @Test fun addAppendsAtEnd() = assertEquals(listOf("a.yt", "b.nf"), cfg("a.yt").add("b.nf", all).packages)

    @Test fun addIgnoresDuplicate() = assertEquals(listOf("a.yt"), cfg("a.yt").add("a.yt", all).packages)

    @Test fun addRefusesWhenFull() {
        val full = cfg("a.yt", "b.nf", "c.cr", "d.pl", "e.x", "f.x", "g.x", "h.x")
        assertTrue(full.isFull(all))
        assertEquals(full, full.add("i.x", all))
    }

    @Test fun uninstalledFavoriteFreesSlotOnAdd() {
        val full = cfg("a.yt", "b.nf", "c.cr", "d.pl", "e.x", "f.x", "g.x", "h.x")
        val installed = all - "c.cr"
        assertFalse(full.isFull(installed))
        assertEquals(
            listOf("a.yt", "b.nf", "d.pl", "e.x", "f.x", "g.x", "h.x", "i.x"),
            full.add("i.x", installed).packages,
        )
    }

    @Test fun visibleHidesUninstalledButKeepsIt() {
        val c = cfg("a.yt", "b.nf")
        assertEquals(listOf("a.yt"), c.visible(setOf("a.yt")))
        assertEquals(listOf("a.yt", "b.nf"), c.visible(all))
    }

    @Test fun removeDeletes() = assertEquals(listOf("b.nf"), cfg("a.yt", "b.nf").remove("a.yt").packages)

    @Test fun moveShiftsAndClampsAtEdges() {
        val c = cfg("a.yt", "b.nf", "c.cr")
        assertEquals(listOf("b.nf", "a.yt", "c.cr"), c.move("a.yt", 1, all).packages)
        assertEquals(c, c.move("c.cr", 1, all))
        assertEquals(c, c.move("a.yt", -1, all))
        assertEquals(c, c.move("zz.z", 1, all))
    }
}
