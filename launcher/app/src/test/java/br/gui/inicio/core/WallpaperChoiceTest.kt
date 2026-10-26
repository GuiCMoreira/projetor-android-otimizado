package br.gui.inicio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperChoiceTest {
    @Test fun nullOrGarbageFallsBackToDefault() {
        assertEquals(WallpaperChoice.DEFAULT, WallpaperChoice.parse(null))
        assertEquals(WallpaperChoice.DEFAULT, WallpaperChoice.parse("lixo"))
        assertEquals(WallpaperChoice.DEFAULT, WallpaperChoice.parse("grad:9"))
        assertEquals(WallpaperChoice.DEFAULT, WallpaperChoice.parse("video:a.jpg"))
    }

    @Test fun roundTripsEveryKind() {
        listOf(
            WallpaperChoice.Gradient(3),
            WallpaperChoice.Asset("montanha.jpg"),
            WallpaperChoice.UserFile("minha-foto_1.png"),
        ).forEach { assertEquals(it, WallpaperChoice.parse(it.serialize())) }
    }

    @Test fun rejectsPathTraversal() {
        assertEquals(WallpaperChoice.DEFAULT, WallpaperChoice.parse("file:../../segredo.jpg"))
        assertEquals(WallpaperChoice.DEFAULT, WallpaperChoice.parse("file:pasta/foto.jpg"))
        assertFalse(WallpaperChoice.isSafeName("..jpg"))
        assertFalse(WallpaperChoice.isSafeName(".oculta.jpg"))
        assertFalse(WallpaperChoice.isSafeName("foto.txt"))
        assertTrue(WallpaperChoice.isSafeName("Foto.JPG"))
    }
}
