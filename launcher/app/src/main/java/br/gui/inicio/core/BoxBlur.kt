package br.gui.inicio.core

/** Desfoque de caixa em passadas horizontais e verticais (3 passadas aproximam um gaussiano). */
object BoxBlur {
    fun blur(pixels: IntArray, w: Int, h: Int, radius: Int, passes: Int = 3): IntArray {
        require(pixels.size == w * h) { "tamanho não confere" }
        if (radius < 1) return pixels
        val tmp = IntArray(pixels.size)
        repeat(passes) {
            pass(pixels, tmp, length = w, lines = h, step = 1, lineStride = w, radius = radius)
            pass(tmp, pixels, length = h, lines = w, step = w, lineStride = 1, radius = radius)
        }
        return pixels
    }

    /** Média móvel ao longo de cada linha (ou coluna), repetindo a borda nos extremos. */
    private fun pass(src: IntArray, dst: IntArray, length: Int, lines: Int, step: Int, lineStride: Int, radius: Int) {
        val div = 2 * radius + 1
        for (line in 0 until lines) {
            val base = line * lineStride
            fun at(i: Int) = src[base + i.coerceIn(0, length - 1) * step]
            var a = 0; var r = 0; var g = 0; var b = 0
            for (i in -radius..radius) {
                val p = at(i)
                a += p ushr 24; r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF
            }
            for (i in 0 until length) {
                dst[base + i * step] = ((a / div) shl 24) or ((r / div) shl 16) or ((g / div) shl 8) or (b / div)
                val out = at(i - radius)
                val inn = at(i + radius + 1)
                a += (inn ushr 24) - (out ushr 24)
                r += ((inn shr 16) and 0xFF) - ((out shr 16) and 0xFF)
                g += ((inn shr 8) and 0xFF) - ((out shr 8) and 0xFF)
                b += (inn and 0xFF) - (out and 0xFF)
            }
        }
    }
}
