package br.gui.tvmae.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import br.gui.tvmae.core.ImageMath
import java.io.InputStream

/** Decodifica fotos e capas já no tamanho de uso: nunca a imagem inteira na memória de uma TV de 940 MB. */
object Images {
    /** Lê só o necessário ([ImageMath.sampleSize]) e recorta para cobrir [w]x[h], como o "center crop". */
    fun decodeCover(open: () -> InputStream, w: Int, h: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = ImageMath.sampleSize(bounds.outWidth, bounds.outHeight, w, h)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val src = open().use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val crop = ImageMath.coverCrop(src.width, src.height, w, h)
        // Cor cheia: em 16 bits (RGB_565) céus e degradês de fotos ficam com faixas visíveis.
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(
            src,
            Rect(crop.left, crop.top, crop.left + crop.width, crop.top + crop.height),
            Rect(0, 0, w, h),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG),
        )
        src.recycle()
        return out
    }
}
