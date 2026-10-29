package br.gui.inicio.rescue

import android.content.Context
import br.gui.inicio.core.CrashGuard

/** Horários das últimas quedas, num arquivo de preferências só deles: um defeito nas preferências do Início não os apaga. */
object CrashLog {
    private const val FILE = "crash_guard"
    private const val KEY = "crashes"
    /** Marcado ao abrir a tela de emergência e apagado quando ela aparece: se ficou marcado, foi ela que caiu. */
    private const val RESCUE_STARTED = "rescue_started"

    /** Chamado pelo tratador de falhas: grava na hora (commit), porque o processo morre logo depois. */
    fun record(context: Context) {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val next = CrashGuard.record(CrashGuard.parse(sp.getString(KEY, null)), System.currentTimeMillis())
        sp.edit().putString(KEY, CrashGuard.format(next)).commit()
    }

    fun shouldRescue(context: Context): Boolean {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return CrashGuard.shouldRescue(CrashGuard.parse(sp.getString(KEY, null)), System.currentTimeMillis())
    }

    fun clear(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove(KEY).remove(RESCUE_STARTED).commit()
    }

    fun rescueStarted(context: Context) = setRescue(context, true)

    fun rescueShown(context: Context) = setRescue(context, false)

    fun rescueFailed(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(RESCUE_STARTED, false)

    /** A tela inicial abriu normal: uma queda antiga da emergência não vale mais. */
    fun homeOpened(context: Context) {
        if (rescueFailed(context)) setRescue(context, false)
    }

    private fun setRescue(context: Context, started: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(RESCUE_STARTED, started).commit()
    }
}
