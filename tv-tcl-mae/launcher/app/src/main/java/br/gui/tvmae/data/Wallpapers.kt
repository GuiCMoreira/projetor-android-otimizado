package br.gui.tvmae.data

import android.content.Context
import android.graphics.Bitmap
import br.gui.tvmae.core.Guard
import br.gui.tvmae.core.Rotation
import br.gui.tvmae.ui.Images
import java.io.File

/**
 * Fotos de fundo: ficam no aparelho, na pasta do app (Android/data/br.gui.tvmae/files/wallpapers), mandadas pelo
 * computador com `scripts/enviar-fotos.sh`; não vêm dentro do app. Troca de hora em hora pelo relógio
 * ([Rotation.index]), sem nada rodando entre uma troca e outra.
 */
class Wallpapers(private val ctx: Context) {
    /**
     * Resolvida a cada leitura: a tela inicial é a primeira a abrir quando a TV liga, às vezes antes de o armazenamento
     * estar montado; guardada no construtor, a pasta ficaria null e a TV sem foto até reiniciar o app.
     */
    private val dir get() = ctx.getExternalFilesDir("wallpapers")

    fun files(): List<File> = Guard.run({ emptyList() }) {
        dir?.listFiles { f -> f.isFile && f.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
            ?.sortedBy { it.name } ?: emptyList()
    }

    /** A foto da hora [nowMs]; null sem fotos (aí fica o degradê escuro). */
    fun current(nowMs: Long): File? {
        val all = files()
        val i = Rotation.index(nowMs, INTERVAL_MINUTES, all.size)
        return all.getOrNull(i)
    }

    fun decode(file: File, w: Int, h: Int): Bitmap? = Guard.run({ null }) { Images.decodeCover({ file.inputStream() }, w, h) }

    companion object {
        /** Combinado com o neto: troca a cada 1 hora. */
        const val INTERVAL_MINUTES = 60
    }
}
