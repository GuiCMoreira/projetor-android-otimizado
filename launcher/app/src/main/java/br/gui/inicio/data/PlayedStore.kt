package br.gui.inicio.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import br.gui.inicio.core.Guard
import br.gui.inicio.core.PlayedItem
import br.gui.inicio.core.PlayedLog
import java.io.File

/**
 * Onde fica o que tocou ([PlayedLog]): um arquivo de texto na pasta do app e as capas salvas ao lado. Escrito pelo
 * [PlayingListener] e lido pela tela inicial, no mesmo processo.
 */
object PlayedStore {
    /** A tela inicial à vista se inscreve aqui para refazer a fileira quando algo novo é anotado. */
    @Volatile var onChange: (() -> Unit)? = null

    private val lock = Any()
    private val main = Handler(Looper.getMainLooper())

    fun artDir(ctx: Context) = File(ctx.filesDir, "tocados")

    private fun file(ctx: Context) = File(ctx.filesDir, "tocados.txt")

    fun load(ctx: Context): List<PlayedItem> = synchronized(lock) {
        Guard.run({ Log.w(TAG, "lista de tocados ilegível", it); emptyList() }) {
            file(ctx).takeIf { it.isFile }?.readText().let { PlayedLog.parse(it) }
        }
    }

    fun record(ctx: Context, item: PlayedItem) {
        synchronized(lock) {
            Guard.run({ Log.w(TAG, "falha ao anotar o que tocou", it) }) {
                val log = PlayedLog.record(load(ctx), item)
                // Temporário + renomear: se o processo morrer no meio da gravação, a lista anterior continua inteira.
                val tmp = File(ctx.filesDir, "tocados.txt.tmp")
                tmp.writeText(PlayedLog.format(log))
                tmp.renameTo(file(ctx))
                val keep = log.mapNotNull { it.art }.toSet()
                artDir(ctx).listFiles()?.filter { "file://${it.path}" !in keep }?.forEach { it.delete() }
            }
        }
        main.post { onChange?.invoke() }
    }

    private const val TAG = "Inicio"
}
