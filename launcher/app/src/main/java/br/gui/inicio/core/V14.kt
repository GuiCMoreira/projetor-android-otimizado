package br.gui.inicio.core

/**
 * Zoom lento (Ken Burns) do descanso de tela, calculado pelo tempo passado. Desenhado a cada [STEP_MS] (20 quadros
 * por segundo) em vez de a cada quadro da tela: a foto anda menos de 1 px por passo, então o olho não vê diferença.
 * Medido no projetor: um terço menos quadros e uns 30% menos processador (launcher mais compositor de tela).
 */
object KenBurns {
    const val MAX_SCALE = 1.12f
    const val STEP_MS = 50L

    class Frame(val scale: Float, val translationX: Float)

    fun at(elapsedMs: Long, durationMs: Long, dx: Float): Frame {
        val t = (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)
        return Frame(1f + (MAX_SCALE - 1f) * t, dx * t)
    }
}
