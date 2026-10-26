package br.gui.inicio.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoxBlurTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    @Test fun uniformImageStaysUniform() {
        val px = IntArray(16 * 9) { 0xFF336699.toInt() }
        assertArrayEquals(IntArray(16 * 9) { 0xFF336699.toInt() }, BoxBlur.blur(px, 16, 9, 2))
    }

    @Test fun singleBrightPixelSpreadsToNeighbours() {
        val w = 9
        val px = IntArray(w * w) { black }
        px[4 * w + 4] = white
        BoxBlur.blur(px, w, w, 1, passes = 1)
        val center = px[4 * w + 4] and 0xFF
        val neighbour = px[4 * w + 5] and 0xFF
        assertEquals(28, center) // 255 / 9
        assertEquals(28, neighbour)
        assertEquals(0, px[0] and 0xFF)
        assertEquals(255, px[4 * w + 4] ushr 24) // continua opaco
    }

    @Test fun radiusZeroIsNoOp() {
        val px = intArrayOf(black, white, black, white)
        assertArrayEquals(intArrayOf(black, white, black, white), BoxBlur.blur(px, 2, 2, 0))
    }

    @Test fun blurNeverLeavesChannelRange() {
        val px = IntArray(32 * 18) { if (it % 2 == 0) white else black }
        BoxBlur.blur(px, 32, 18, 5)
        assertTrue(px.all { (it ushr 24) == 255 })
    }
}
