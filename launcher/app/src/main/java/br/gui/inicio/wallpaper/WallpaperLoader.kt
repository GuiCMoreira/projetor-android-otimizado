package br.gui.inicio.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import br.gui.inicio.core.BoxBlur
import br.gui.inicio.core.ImageMath
import br.gui.inicio.core.WallpaperChoice
import br.gui.inicio.ui.Theme
import java.io.File
import java.io.InputStream

/** Monta o papel de parede no tamanho exato da tela e a versão desfocada usada pelo vidro. Rodar fora da thread principal. */
class WallpaperLoader(private val ctx: Context) {
    data class Result(val full: Bitmap, val blurred: Bitmap)

    /** Nunca lança: qualquer falha (arquivo sumido, corrompido, memória) cai no degradê padrão. */
    fun load(choice: WallpaperChoice, w: Int, h: Int): Result {
        val full = render(choice, w, h) ?: Gradients.render(0, w, h)
        return Result(full, blur(full))
    }

    fun render(choice: WallpaperChoice, w: Int, h: Int): Bitmap? = try {
        when (choice) {
            is WallpaperChoice.Gradient -> Gradients.render(choice.index, w, h)
            is WallpaperChoice.Asset -> decodeCover({ ctx.assets.open("wallpapers/${choice.name}") }, w, h)
            is WallpaperChoice.UserFile -> {
                val file = userDir()?.let { File(it, choice.name) }
                if (file != null && file.isFile) decodeCover({ file.inputStream() }, w, h) else null
            }
        }
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }

    fun assetNames(): List<String> =
        ctx.assets.list("wallpapers")?.filter { WallpaperChoice.isSafeName(it) }?.sorted() ?: emptyList()

    /** Fotos enviadas pelo computador (scripts/enviar-wallpaper.sh). Pasta própria do app: sem permissão. */
    fun userFiles(): List<String> =
        userDir()?.listFiles()?.map { it.name }?.filter { WallpaperChoice.isSafeName(it) }?.sorted() ?: emptyList()

    private fun userDir(): File? = ctx.getExternalFilesDir("wallpapers")

    private fun decodeCover(open: () -> InputStream, w: Int, h: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = ImageMath.sampleSize(bounds.outWidth, bounds.outHeight, w, h)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val src = open().use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val crop = ImageMath.coverCrop(src.width, src.height, w, h)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        Canvas(out).drawBitmap(
            src,
            Rect(crop.left, crop.top, crop.left + crop.width, crop.top + crop.height),
            Rect(0, 0, w, h),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG),
        )
        src.recycle()
        return out
    }

    private fun blur(full: Bitmap): Bitmap {
        val sw = maxOf(1, full.width / Theme.BLUR_DOWNSCALE)
        val sh = maxOf(1, full.height / Theme.BLUR_DOWNSCALE)
        val small = Bitmap.createScaledBitmap(full, sw, sh, true).copy(Bitmap.Config.ARGB_8888, true)
        val px = IntArray(sw * sh)
        small.getPixels(px, 0, sw, 0, 0, sw, sh)
        BoxBlur.blur(px, sw, sh, Theme.BLUR_RADIUS)
        small.setPixels(px, 0, sw, 0, 0, sw, sh)
        return small
    }
}
