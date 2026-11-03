package br.gui.tvmae.core

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

    /** Combinado com o neto: um app (a Globoplay publica muito) não ocupa a fileira toda. */
    const val PER_APP = 2

    /** Códigos longos (20+ dígitos hexadecimais) da capa e do link: identificam o episódio mesmo quando o título muda. */
    private val LONG_ID = Regex("[0-9a-f]{20,}")

    fun ids(item: WatchNextItem): Set<String> =
        sequenceOf(item.poster, item.intentUri).filterNotNull().flatMap { LONG_ID.findAll(it.lowercase()).map { m -> m.value } }.toSet()

    /** Identifica a série (ou o título) dentro do app: é o que a tela inicial anota quando um cartão é aberto. */
    fun key(item: WatchNextItem): String = join(item.pkg, item.seriesId?.takeIf { it.isNotBlank() } ?: item.title)

    /** O que tocou dentro do app (sessão de mídia): só o título, sem série. null sem título. */
    fun playedKey(pkg: String, title: String?): String? = title?.takeIf { it.isNotBlank() }?.let { join(pkg, it) }

    private fun join(pkg: String, name: String) = pkg + "|" + name.trim().replace(Regex("[\\t\\n]"), " ")

    /** Horário mais novo em que o item foi aberto pelo cartão ou tocado dentro do app; null se nunca. */
    private fun openedAt(item: WatchNextItem, opened: Map<String, Long>): Long? =
        listOfNotNull(opened[key(item)], opened[join(item.pkg, item.title)]).maxOrNull()

    /**
     * Só o que foi assistido de verdade (algum progresso, ou aberto pelo cartão ou tocado no app: [opened], chave -> horário), dos apps
     * que abrem. Mais recente primeiro, valendo o mais novo entre o horário do app e o da abertura (o YouTube não
     * atualiza o horário de um vídeo revisto; a Globoplay carimba a lista toda na hora em que sincroniza). No máximo
     * [MAX], e [PER_APP] de cada app. Repetido = mesmo app e mesma série, mesmo título ou um código de episódio em
     * comum (a Pluto publica o mesmo episódio de novo com outro título, capa e link).
     */
    fun pick(items: List<WatchNextItem>, opened: Map<String, Long> = emptyMap(), openable: (String) -> Boolean): List<WatchNextItem> {
        val seen = HashSet<String>()
        val kept = ArrayList<WatchNextItem>()
        val perApp = HashMap<String, Int>()
        val candidates = items.filter { it.title.isNotBlank() && (it.positionMs > 0 || openedAt(it, opened) != null) && openable(it.pkg) }
        for (item in candidates.sortedByDescending { maxOf(it.lastEngagedMs, openedAt(it, opened) ?: 0L) }) {
            val keys = ids(item) + ("serie:" + (item.seriesId?.takeIf { it.isNotBlank() } ?: item.title.trim()))
            if (keys.any { "${item.pkg}|$it" in seen }) continue
            if ((perApp[item.pkg] ?: 0) >= PER_APP) continue
            perApp[item.pkg] = (perApp[item.pkg] ?: 0) + 1
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

/** O que foi aberto pelo cartão da tela inicial ou tocado dentro do app: chave ([WatchNextList.key]) -> horário. Guardado como "horário<TAB>chave" por linha. */
object OpenedLog {
    /** Bem mais que os 4 cartões: um item aberto e depois empurrado para fora ainda volta na ordem certa. */
    const val MAX = 20

    fun parse(raw: String?): Map<String, Long> =
        raw.orEmpty().lines().mapNotNull { line ->
            val tab = line.indexOf('\t')
            if (tab <= 0) return@mapNotNull null
            val at = line.substring(0, tab).toLongOrNull() ?: return@mapNotNull null
            line.substring(tab + 1).takeIf { it.isNotEmpty() }?.let { it to at }
        }.toMap()

    fun format(log: Map<String, Long>): String = log.entries.joinToString("\n") { "${it.value}\t${it.key}" }

    /** Anota (ou atualiza) [key] e fica só com os [MAX] mais recentes. */
    fun record(log: Map<String, Long>, key: String, now: Long): Map<String, Long> =
        (log + (key to now)).entries.sortedByDescending { it.value }.take(MAX).associate { it.key to it.value }
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
