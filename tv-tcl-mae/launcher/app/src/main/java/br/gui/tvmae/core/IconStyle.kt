package br.gui.tvmae.core

/** Como preencher o quadrado de um ícone comum (não adaptativo). */
sealed interface IconFill {
    /** Logo solto, com fundo transparente: vai centralizado sobre o branco. */
    data object Padded : IconFill

    /** Logo opaco até a borda: ocupa o quadrado inteiro, e o que sobrar dos cantos ganha [color]. */
    data class Full(val color: Int) : IconFill
}

object IconStyle {
    /** A partir de quantos pontos opacos (de 12) o ícone é tratado como quadrado cheio. */
    private const val MIN_OPAQUE = 9

    /**
     * Ajuste fino para ícones adaptativos cujo logo passa da área segura: a Pluto 5.x para TV corta o "tv"
     * (mesmo valor do projetor).
     */
    private val FOREGROUND_SCALE = mapOf("tv.pluto.android" to 0.8f)

    /** 12 pontos a 2 px da borda, em 1/4, 1/2 e 3/4 de cada lado (longe dos cantos arredondados). */
    fun edgeSamples(w: Int, h: Int): List<Pair<Int, Int>> {
        val xs = listOf(w / 4, w / 2, w * 3 / 4)
        val ys = listOf(h / 4, h / 2, h * 3 / 4)
        val inX = minOf(2, w - 1)
        val inY = minOf(2, h - 1)
        return xs.map { it to inY } + xs.map { it to h - 1 - inY } + ys.map { inX to it } + ys.map { w - 1 - inX to it }
    }

    /** [samples] em ARGB, na ordem de [edgeSamples]. */
    fun classify(samples: IntArray): IconFill {
        val opaque = samples.filter { (it ushr 24) > 200 }
        if (opaque.size < MIN_OPAQUE) return IconFill.Padded
        val color = opaque.groupingBy { it }.eachCount().maxBy { it.value }.key
        return IconFill.Full(color)
    }

    fun foregroundScale(pkg: String): Float = FOREGROUND_SCALE[pkg] ?: 1f
}
