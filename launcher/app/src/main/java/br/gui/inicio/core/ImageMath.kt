package br.gui.inicio.core

data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)

object ImageMath {
    /** Maior potência de 2 que ainda deixa a imagem decodificada >= destino nas duas dimensões. */
    fun sampleSize(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Int {
        require(srcW > 0 && srcH > 0 && dstW > 0 && dstH > 0) { "medidas devem ser positivas" }
        var size = 1
        while (srcW / (size * 2) >= dstW && srcH / (size * 2) >= dstH) size *= 2
        // Teto de memória: panoramas muito largos não passam de 4x os pixels da tela depois de decodificados.
        val maxPixels = dstW.toLong() * dstH * 4
        while ((srcW / size).toLong() * (srcH / size) > maxPixels) size *= 2
        return size
    }

    /** Recorte central da origem com a proporção do destino (efeito "cover"). */
    fun coverCrop(srcW: Int, srcH: Int, dstW: Int, dstH: Int): CropRect {
        require(srcW > 0 && srcH > 0 && dstW > 0 && dstH > 0) { "medidas devem ser positivas" }
        return if (srcW.toLong() * dstH > srcH.toLong() * dstW) {
            val w = (srcH.toLong() * dstW / dstH).toInt().coerceAtLeast(1)
            CropRect((srcW - w) / 2, 0, w, srcH)
        } else {
            val h = (srcW.toLong() * dstH / dstW).toInt().coerceAtLeast(1) // foto minúscula: nunca recorte vazio
            CropRect(0, (srcH - h) / 2, srcW, h)
        }
    }
}
