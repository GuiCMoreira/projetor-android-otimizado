package br.gui.inicio.core

/**
 * "Liberar memória" da Central de Controle: quais apps podem ser fechados e como contar o que voltou.
 * Só apps de uso; a tela inicial, o Android, o Google (serviços e teclado) e as peças do projetor ficam.
 */
object MemoryClean {
    private val PROTECTED = listOf(
        "android", "com.android.", "com.google.android.gms", "com.google.android.gsf",
        "com.google.android.inputmethod.", "com.newlink.", "com.sstar.", "com.zhiying.",
    )

    fun targets(installed: List<String>, own: String): List<String> =
        installed.distinct().filter { pkg -> pkg != own && PROTECTED.none { pkg == it || pkg.startsWith(it) } }

    /** Abaixo de 10 MB é ruído da medição: diz que já estava livre em vez de anunciar um ganho que não houve. */
    fun message(beforeKb: Long, afterKb: Long): String {
        val mb = (afterKb - beforeKb) / 1024
        return if (mb >= 10) "Liberados $mb MB" else "A memória já estava livre"
    }
}
