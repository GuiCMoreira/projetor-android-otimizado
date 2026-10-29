package br.gui.inicio

import android.app.Application
import br.gui.inicio.rescue.CrashLog

/** Anota cada queda do app antes de o Android encerrá-lo; a tela inicial usa isso para não entrar em ciclo de quedas. */
class InicioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { CrashLog.record(this) }
            previous?.uncaughtException(thread, error)
        }
    }
}
