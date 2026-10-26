package br.gui.inicio.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

/** Gera os ícones squircle (raio 22,5%) do Dock, do Launchpad e das telas de ajuste. */
object IconFactory {
    fun squirclePath(size: Float): Path = Path().apply {
        addRoundRect(0f, 0f, size, size, size * 0.225f, size * 0.225f, Path.Direction.CW)
    }

    /** Ícone de app: adaptativo usa as próprias camadas; ícone comum vai centralizado sobre fundo branco. */
    fun app(icon: Drawable, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.clipPath(squirclePath(size.toFloat()))
        if (icon is AdaptiveIconDrawable) {
            val inset = size / 4 // camadas de 108dp com 72dp visíveis: 18/72 = 0,25
            icon.setBounds(-inset, -inset, size + inset, size + inset)
            icon.draw(c)
        } else {
            c.drawColor(Color.WHITE)
            val pad = (size * 0.14f).toInt()
            icon.setBounds(pad, pad, size - pad, size - pad)
            icon.draw(c)
        }
        return bmp
    }

    /** Banner de TV (16:9) recortado no centro, para apps que só têm ícone de baixa resolução. */
    fun banner(banner: Drawable, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.clipPath(squirclePath(size.toFloat()))
        val w = size * 16 / 9
        val left = (size - w) / 2
        banner.setBounds(left, 0, left + w, size)
        banner.draw(c)
        return bmp
    }

    /** Ícone de atalho do sistema: squircle com degradê vertical e um glifo branco. */
    fun glyph(g: Glyph, top: Int, bottom: Int, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, size.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        }
        c.drawPath(squirclePath(size.toFloat()), paint)
        Glyphs.draw(c, g, size / 2f, size / 2f, size * 0.52f, Color.WHITE)
        return bmp
    }
}
