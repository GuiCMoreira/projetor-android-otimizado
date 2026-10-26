package br.gui.inicio.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import br.gui.inicio.core.BoxBlur
import br.gui.inicio.core.Guard
import br.gui.inicio.core.ImageMath
import br.gui.inicio.core.Rotation
import br.gui.inicio.core.WallpaperChoice
import br.gui.inicio.ui.Theme
import java.io.File
import java.io.InputStream

/** Monta o papel de parede no tamanho exato da tela e a versão desfocada usada pelo vidro. Rodar fora da thread principal. */
class WallpaperLoader(private val ctx: Context) {
    /** null = não foi possível montar (tela fica no fundo preto, vidro sem desfoque). */
    data class Result(val full: Bitmap?, val blurred: Bitmap?)

    /** Nunca lança: qualquer falha (arquivo sumido, corrompido, memória) cai no degradê padrão e, no limite, em nada. */
    fun load(choice: WallpaperChoice, w: Int, h: Int): Result {
        val full = render(choice, w, h, fullScreen = true) ?: Guard.run({ null }) { Gradients.render(0, w / 2, h / 2) }
        val blurred = full?.let { f -> Guard.run({ null }) { blur(f, w, h) } }
        return Result(full, blurred)
    }

    /**
     * [fullScreen]: degradê sai em meia resolução e cor cheia (2 MB em vez de 8 MB); a tela amplia sem perda
     * visível porque não há detalhe fino. Fotos sempre saem no tamanho pedido.
     */
    fun render(choice: WallpaperChoice, w: Int, h: Int, fullScreen: Boolean = false): Bitmap? = try {
        when (choice) {
            is WallpaperChoice.Gradient -> if (fullScreen) Gradients.render(choice.index, w / 2, h / 2) else Gradients.render(choice.index, w, h)
            is WallpaperChoice.Asset -> decodeCover({ ctx.assets.open("wallpapers/${choice.name}") }, w, h)
            is WallpaperChoice.UserFile -> {
                val file = userDir()?.let { File(it, choice.name) }
                if (file != null && file.isFile) decodeCover({ file.inputStream() }, w, h, exifRotation(file)) else null
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

    /**
     * Fundo efetivo agora: com a troca automática ligada e fotos enviadas, a foto da vez pelo relógio;
     * senão, a escolha salva.
     */
    fun effective(saved: WallpaperChoice, rotateMinutes: Int, nowMs: Long): WallpaperChoice {
        val files = userFiles()
        val i = Rotation.index(nowMs, rotateMinutes, files.size)
        return if (i < 0) saved else WallpaperChoice.UserFile(files[i])
    }

    /** Graus para deixar a foto em pé (foto de celular copiada sem o script vem com a orientação só no EXIF). */
    private fun exifRotation(file: File): Int = Guard.run({ 0 }) {
        when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun decodeCover(open: () -> InputStream, w: Int, h: Int, rotation: Int = 0): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = ImageMath.sampleSize(bounds.outWidth, bounds.outHeight, w, h)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = open().use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val src = if (rotation == 0) decoded else {
            val m = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true).also { if (it !== decoded) decoded.recycle() }
        }
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

    /** Sempre 1/8 da tela ([w] x [h]), qualquer que seja a resolução de [full]. */
    private fun blur(full: Bitmap, w: Int, h: Int): Bitmap {
        val sw = maxOf(1, w / Theme.BLUR_DOWNSCALE)
        val sh = maxOf(1, h / Theme.BLUR_DOWNSCALE)
        val small = Bitmap.createScaledBitmap(full, sw, sh, true).copy(Bitmap.Config.ARGB_8888, true)
        val px = IntArray(sw * sh)
        small.getPixels(px, 0, sw, 0, 0, sw, sh)
        BoxBlur.blur(px, sw, sh, Theme.BLUR_RADIUS)
        small.setPixels(px, 0, sw, 0, 0, sw, sh)
        return small
    }
}
