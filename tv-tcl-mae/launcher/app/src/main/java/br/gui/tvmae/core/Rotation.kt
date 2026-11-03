package br.gui.tvmae.core

/** Troca automática da foto de fundo pelo horário: o mesmo cálculo em qualquer momento, sem nada rodando. */
object Rotation {
    /** Opções do menu, em minutos (0 = desligado). */
    val OPTIONS = listOf(0, 60, 180, 360, 1440)

    /** Índice da foto para o instante [nowMs]; -1 quando desligado ou sem fotos. */
    fun index(nowMs: Long, intervalMinutes: Int, count: Int): Int {
        if (intervalMinutes <= 0 || count <= 0) return -1
        return ((nowMs / (intervalMinutes * 60_000L)) % count).toInt()
    }
}
