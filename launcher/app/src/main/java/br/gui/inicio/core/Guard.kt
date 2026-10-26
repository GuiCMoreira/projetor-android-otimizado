package br.gui.inicio.core

/** Uma tela inicial não pode cair: qualquer erro em segundo plano (inclusive falta de memória) vira um valor de reserva. */
object Guard {
    inline fun <T> run(fallback: (Throwable) -> T, block: () -> T): T = try {
        block()
    } catch (t: Throwable) {
        fallback(t)
    }
}
