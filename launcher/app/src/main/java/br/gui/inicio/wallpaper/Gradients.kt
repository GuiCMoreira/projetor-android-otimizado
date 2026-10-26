package br.gui.inicio.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

/** Os 4 degradês estilo macOS, desenhados por código (sem imagem no APK). */
object Gradients {
    private class Blob(val x: Float, val y: Float, val rx: Float, val ry: Float, val color: Int)
    private class Spec(val from: Int, val mid: Int, val to: Int, val blobs: List<Blob>)

    private fun c(hex: Long) = hex.toInt()

    private val SPECS = listOf(
        Spec(c(0xFF1B1F5E), c(0xFF3B1D6E), c(0xFF120F2E), listOf(
            Blob(0.15f, 0.85f, 0.60f, 0.80f, c(0xFFFF7A59)),
            Blob(0.85f, 0.20f, 0.70f, 0.90f, c(0xFF6A5CFF)),
            Blob(0.60f, 1.00f, 0.80f, 0.80f, c(0xFFE2408F)),
        )),
        Spec(c(0xFF0B2A4A), c(0xFF0A2140), c(0xFF08172E), listOf(
            Blob(0.20f, 0.20f, 0.70f, 0.90f, c(0xFF2FD3C4)),
            Blob(0.80f, 0.90f, 0.70f, 0.90f, c(0xFF2A6CF0)),
        )),
        Spec(c(0xFF3A1C1C), c(0xFF2A1416), c(0xFF1A0F12), listOf(
            Blob(0.80f, 0.20f, 0.70f, 0.90f, c(0xFFFFB347)),
            Blob(0.20f, 0.90f, 0.70f, 0.90f, c(0xFFFF5E62)),
        )),
        Spec(c(0xFF2B2D33), c(0xFF1E1F23), c(0xFF111214), emptyList()),
    )

    val COUNT: Int get() = SPECS.size

    fun render(index: Int, w: Int, h: Int): Bitmap {
        val spec = SPECS[index.coerceIn(0, SPECS.lastIndex)]
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        val canvas = Canvas(bmp)
        val base = Paint(Paint.DITHER_FLAG).apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), intArrayOf(spec.from, spec.mid, spec.to), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), base)
        for (b in spec.blobs) {
            val paint = Paint(Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(0f, 0f, 1f, b.color, b.color and 0x00FFFFFF, Shader.TileMode.CLAMP)
            }
            canvas.save()
            canvas.translate(b.x * w, b.y * h)
            canvas.scale(b.rx * w * 0.6f, b.ry * h * 0.6f) // como no CSS: transparente a 60% do raio
            canvas.drawCircle(0f, 0f, 1f, paint)
            canvas.restore()
        }
        return bmp
    }
}
