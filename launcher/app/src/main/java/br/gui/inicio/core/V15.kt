package br.gui.inicio.core

/** O que tocou num app, como ele informou ao sistema enquanto tocava (sessão de mídia). Anotado pela tela inicial. */
data class PlayedItem(
    val pkg: String,
    val title: String,
    val subtitle: String?,
    /** Identificador que o app dá ao item; no YouTube é o código do vídeo. */
    val mediaId: String?,
    val positionMs: Long,
    val durationMs: Long,
    val playedAtMs: Long,
    /** Capa: endereço que o app informou ou arquivo salvo pela tela inicial; null sem imagem. */
    val art: String?,
)

/**
 * Continuar assistindo dos apps que não publicam na lista do Android neste projetor (YouTube, Netflix, Crunchyroll,
 * Spotify): a tela inicial anota o que toca e monta os cartões com isso.
 */
object PlayedLog {
    /** Bem mais que os 4 cartões: um item empurrado para fora volta quando os da frente saem. */
    const val MAX = 30
    const val MAX_AGE_MS = 14 * 24 * 60 * 60_000L

    /** Menos que isso é prévia que toca sozinha na tela do app ou um trailer, não "assistir". */
    const val MIN_WATCHED_MS = 30_000L

    /** Faltando menos que isso, o episódio acabou (sobram os créditos). */
    const val FINISHED_LEFT_MS = 2 * 60_000L

    /** Música ocupa um cartão só, o da última que tocou (combinado com o dono). */
    val MUSIC = setOf("com.spotify.tv.android", "com.spotify.music")

    private val YOUTUBE = setOf("com.google.android.youtube.tv", "com.google.android.youtube")
    private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

    fun watched(positionMs: Long): Boolean = positionMs >= MIN_WATCHED_MS

    /** Segunda linha do cartão. Igual ao título só fica em música ("ATliens", do ATliens); em vídeo, repetiria. */
    fun secondLine(pkg: String, title: String, raw: String?): String? {
        val line = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return if (pkg !in MUSIC && line == title.trim()) null else line
    }

    private fun key(item: PlayedItem) = if (item.pkg in MUSIC) item.pkg else item.pkg + "|" + item.title.trim()

    /** Anota [item] no lugar do mesmo título (ou da música anterior), mais recente primeiro, no máximo [MAX]. */
    fun record(log: List<PlayedItem>, item: PlayedItem): List<PlayedItem> =
        (listOf(item) + log.filter { key(it) != key(item) }).sortedByDescending { it.playedAtMs }.take(MAX)

    /** O que vira cartão: recente, não terminado e só de apps que não publicam na lista do Android ([publishers]). */
    fun visible(log: List<PlayedItem>, now: Long, publishers: Set<String>): List<PlayedItem> = log.filter {
        it.pkg !in publishers &&
            now - it.playedAtMs <= MAX_AGE_MS &&
            (it.pkg in MUSIC || it.durationMs <= 0 || it.durationMs - it.positionMs >= FINISHED_LEFT_MS)
    }

    /** Link direto para o vídeo, no ponto em que parou. Só o YouTube tem um código que dá para abrir de fora. */
    fun link(pkg: String, mediaId: String?, positionMs: Long): String? {
        if (pkg !in YOUTUBE || mediaId == null || !VIDEO_ID.matches(mediaId)) return null
        return "https://www.youtube.com/watch?v=$mediaId&t=${positionMs / 1000}s"
    }

    fun toWatchNext(p: PlayedItem) = WatchNextItem(
        pkg = p.pkg,
        title = p.title,
        season = null,
        episode = null,
        poster = p.art,
        positionMs = p.positionMs,
        durationMs = p.durationMs,
        intentUri = link(p.pkg, p.mediaId, p.positionMs),
        lastEngagedMs = p.playedAtMs,
        subtitle = p.subtitle,
    )

    /** Uma linha por item, campos separados por TAB; TAB, quebra de linha e barra invertida escapados. */
    fun format(log: List<PlayedItem>): String = log.joinToString("\n") { p ->
        listOf(p.pkg, p.title, p.subtitle.orEmpty(), p.mediaId.orEmpty(), p.positionMs.toString(), p.durationMs.toString(),
            p.playedAtMs.toString(), p.art.orEmpty()).joinToString("\t") { escape(it) }
    }

    fun parse(raw: String?): List<PlayedItem> = raw.orEmpty().lines().mapNotNull { line ->
        val f = line.split('\t').map { unescape(it) }
        if (f.size != 8 || f[0].isEmpty() || f[1].isEmpty()) return@mapNotNull null
        PlayedItem(
            pkg = f[0],
            title = f[1],
            subtitle = f[2].ifEmpty { null },
            mediaId = f[3].ifEmpty { null },
            positionMs = f[4].toLongOrNull() ?: return@mapNotNull null,
            durationMs = f[5].toLongOrNull() ?: return@mapNotNull null,
            playedAtMs = f[6].toLongOrNull() ?: return@mapNotNull null,
            art = f[7].ifEmpty { null },
        )
    }

    private fun escape(s: String) = s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

    private fun unescape(s: String): String {
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                out.append(when (s[i + 1]) { 't' -> '\t'; 'n' -> '\n'; 'r' -> '\r'; else -> s[i + 1] })
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }
}

/**
 * Séries tiradas do Continuar assistindo (segurar OK no cartão). Os apps não dizem de qual perfil é o item (o
 * Crunchyroll publica o que foi visto no perfil de outra pessoa), então quem decide é o dono. Chave: app e título.
 */
object HiddenShows {
    fun key(item: WatchNextItem): String = item.pkg + "|" + item.title.trim()

    fun add(hidden: Set<String>, key: String): Set<String> = hidden + key

    fun filter(items: List<WatchNextItem>, hidden: Set<String>): List<WatchNextItem> = items.filter { key(it) !in hidden }

    fun format(hidden: Set<String>): String = hidden.joinToString("\n")

    fun parse(raw: String?): Set<String> = raw.orEmpty().lines().filter { it.contains('|') }.toSet()
}
