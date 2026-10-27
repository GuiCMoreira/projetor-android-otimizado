package br.gui.inicio.core

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
     * Ajuste fino para ícones adaptativos cujo logo passa da área segura: a Netflix desenha um N enorme e a Pluto 5.x
     * para TV corta o "tv".
     */
    private val FOREGROUND_SCALE = mapOf("com.netflix.mediaclient" to 0.8f, "tv.pluto.android" to 0.8f)

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

/** Um item da lista "Ver a seguir" do Android TV, publicada pelos apps de streaming. */
data class WatchNextItem(
    val pkg: String,
    val title: String,
    val season: String?,
    val episode: String?,
    val poster: String?,
    val positionMs: Long,
    val durationMs: Long,
    val intentUri: String?,
    val lastEngagedMs: Long,
    /** Mesma série pode vir duas vezes (o app publica o episódio novo com outro título); a série junta as duas. */
    val seriesId: String? = null,
)

object WatchNextList {
    /** Cabem 4 cartões inteiros na largura da tela (340 px + 28 de vão, a partir da margem de 110). */
    const val MAX = 4

    /** Códigos longos (20+ dígitos hexadecimais) da capa e do link: identificam o episódio mesmo quando o título muda. */
    private val LONG_ID = Regex("[0-9a-f]{20,}")

    fun ids(item: WatchNextItem): Set<String> =
        sequenceOf(item.poster, item.intentUri).filterNotNull().flatMap { LONG_ID.findAll(it.lowercase()).map { m -> m.value } }.toSet()

    /**
     * Mais recente primeiro, só de apps que abrem; no máximo [MAX]. Repetido = mesmo app e mesma série, mesmo título
     * ou um código de episódio em comum (a Pluto publica o mesmo episódio de novo com outro título, capa e link).
     */
    fun pick(items: List<WatchNextItem>, openable: (String) -> Boolean): List<WatchNextItem> {
        val seen = HashSet<String>()
        val kept = ArrayList<WatchNextItem>()
        for (item in items.filter { it.title.isNotBlank() && openable(it.pkg) }.sortedByDescending { it.lastEngagedMs }) {
            val keys = ids(item) + ("serie:" + (item.seriesId?.takeIf { it.isNotBlank() } ?: item.title.trim()))
            if (keys.any { "${item.pkg}|$it" in seen }) continue
            keys.forEach { seen += "${item.pkg}|$it" }
            kept += item
            if (kept.size == MAX) break
        }
        return kept
    }

    /** Fração assistida; null quando o app não informa. */
    fun progress(item: WatchNextItem): Float? =
        if (item.positionMs > 0 && item.durationMs > 0) (item.positionMs.toFloat() / item.durationMs).coerceAtMost(1f) else null

    /** "Pluto TV · Temporada 1 · Episódio 1 · faltam 19 min", só com o que o app informou. */
    fun meta(item: WatchNextItem, appName: String): String {
        val parts = mutableListOf(appName)
        if (!item.season.isNullOrBlank() && !item.episode.isNullOrBlank()) parts += "Temporada ${item.season}"
        if (!item.episode.isNullOrBlank()) parts += "Episódio ${item.episode}"
        if (progress(item) != null) {
            val left = item.durationMs - item.positionMs
            parts += if (left < 60_000) "falta menos de 1 min" else "faltam ${left / 60_000} min"
        }
        return parts.joinToString(" · ")
    }
}

/** Nome e limpeza das capas guardadas no aparelho (pasta de cache do app). */
object PosterCache {
    /** SHA-1 do endereço e do tamanho: estável entre aberturas e seguro como nome de arquivo. */
    fun fileName(uri: String, w: Int, h: Int): String {
        val digest = java.security.MessageDigest.getInstance("SHA-1").digest("$uri|$w|$h".toByteArray())
        return digest.joinToString("") { "%02x".format(it) } + ".jpg"
    }

    /** Arquivos de capas que não estão mais na fileira. */
    fun stale(existing: List<String>, keep: Set<String>): List<String> = existing.filterNot { it in keep }
}

/** O link de um episódio vem de outro app: só pode abrir o próprio app que publicou o episódio. */
object EpisodeLink {
    /** [target] = pacote do componente do link (null = qualquer app; aí a tela inicial fixa o [publisher]). */
    fun allowed(target: String?, publisher: String): Boolean = target == null || target == publisher
}
