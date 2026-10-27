package br.gui.inicio.data

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.tv.TvContract.WatchNextPrograms
import android.net.Uri
import android.util.Log
import br.gui.inicio.core.EpisodeLink
import br.gui.inicio.core.Guard
import br.gui.inicio.core.PosterCache
import br.gui.inicio.core.WatchNextItem
import br.gui.inicio.core.WatchNextList
import br.gui.inicio.ui.IconFactory
import br.gui.inicio.wallpaper.WallpaperLoader
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lista "Ver a seguir" do Android TV: cada app de streaming (Pluto TV, Crunchyroll, YouTube) publica o que
 * a pessoa estava assistindo, e a tela inicial só lê. Nada roda em segundo plano: a leitura acontece quando
 * a tela inicial aparece.
 */
class WatchNextRepository(private val ctx: Context, private val loader: WallpaperLoader) {
    private val pm = ctx.packageManager

    fun items(): List<WatchNextItem> = Guard.run({ emptyList() }) {
        val all = ArrayList<WatchNextItem>()
        ctx.contentResolver.query(WatchNextPrograms.CONTENT_URI, COLUMNS, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                if (c.int(WatchNextPrograms.COLUMN_BROWSABLE, 1) == 0) continue
                all += WatchNextItem(
                    pkg = c.str(WatchNextPrograms.COLUMN_PACKAGE_NAME) ?: continue,
                    title = c.str(WatchNextPrograms.COLUMN_TITLE) ?: continue,
                    season = c.str(WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER),
                    episode = c.str(WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER),
                    poster = c.str(WatchNextPrograms.COLUMN_POSTER_ART_URI) ?: c.str(WatchNextPrograms.COLUMN_THUMBNAIL_URI),
                    positionMs = c.long(WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS),
                    durationMs = c.long(WatchNextPrograms.COLUMN_DURATION_MILLIS),
                    intentUri = c.str(WatchNextPrograms.COLUMN_INTENT_URI),
                    lastEngagedMs = c.long(WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS),
                    seriesId = c.str(WatchNextPrograms.COLUMN_SERIES_ID),
                )
            }
        }
        WatchNextList.pick(all, ::isOpenable)
    }

    fun appName(pkg: String): String = Guard.run({ pkg }) { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }

    private val posters = File(ctx.cacheDir, "capas")

    /** Capa já guardada no aparelho, no tamanho do cartão; null se ainda não foi baixada. Leitura rápida, sem rede. */
    fun cachedPoster(item: WatchNextItem, w: Int, h: Int): Bitmap? {
        val uri = item.poster ?: return null
        val file = File(posters, PosterCache.fileName(uri, w, h))
        if (!file.exists()) return null
        return Guard.run({ Log.w(TAG, "capa guardada ilegível: $uri", it); file.delete(); null }) { BitmapFactory.decodeFile(file.path, ARGB) }
    }

    /**
     * Baixa a capa uma vez para um arquivo temporário, recorta no tamanho do cartão e guarda. A gravação é atômica
     * (temporário + renomear): se o processo morrer no meio, não fica capa pela metade no cache.
     * Bloqueante e sem prazo próprio: quem chama limita o tempo total.
     */
    fun downloadPoster(item: WatchNextItem, w: Int, h: Int): Boolean {
        val uri = item.poster ?: return false
        return Guard.run({ Log.w(TAG, "capa falhou: $uri", it); false }) {
            posters.mkdirs()
            val name = PosterCache.fileName(uri, w, h)
            val raw = File(posters, "$name.baixando")
            val tmp = File(posters, "$name.tmp")
            try {
                open(uri).use { input -> raw.outputStream().use { copyLimited(input, it) } }
                val bmp = loader.decodeCover({ raw.inputStream() }, w, h) ?: error("imagem inválida")
                tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                bmp.recycle()
                tmp.renameTo(File(posters, name))
            } finally {
                raw.delete()
                tmp.delete()
            }
        }
    }

    /** Apaga as capas de episódios que saíram da fileira. */
    fun prunePosters(items: List<WatchNextItem>, w: Int, h: Int) = Guard.run({ Unit }) {
        val keep = items.mapNotNull { it.poster?.let { uri -> PosterCache.fileName(uri, w, h) } }.toSet()
        PosterCache.stale(posters.list()?.toList() ?: emptyList(), keep).forEach { File(posters, it).delete() }
    }

    /** Selo do app de origem no canto do cartão. */
    fun badge(pkg: String, size: Int): Bitmap? = Guard.run({ null }) { IconFactory.app(pm.getApplicationIcon(pkg), size, pkg) }

    /** Abre direto no episódio, no ponto em que parou; se o app não aceitar, abre o app. */
    fun open(item: WatchNextItem): Boolean {
        val direct = item.intentUri?.let { Guard.run({ null }) { Intent.parseUri(it, Intent.URI_INTENT_SCHEME) } }
        // O link vem de outro app: só vale se abrir o próprio app que publicou o episódio (nunca a tela inicial ou um terceiro).
        val safe = direct?.takeIf { EpisodeLink.allowed(it.component?.packageName, item.pkg) }?.apply {
            selector = null
            setPackage(item.pkg)
        }
        Log.i(TAG, "abrir \"${item.title}\": ${if (safe == null) "sem link válido, abre o app" else "link ${safe.data}"}")
        // Tarefa nova: com o app já aberto, a Pluto ignora o link e fica na tela em que estava.
        if (safe != null && start(safe.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))) return true
        val app = pm.getLeanbackLaunchIntentForPackage(item.pkg) ?: pm.getLaunchIntentForPackage(item.pkg) ?: return false
        return start(app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun start(intent: Intent): Boolean = Guard.run({ false }) {
        ctx.startActivity(intent)
        true
    }

    private fun isOpenable(pkg: String): Boolean = Guard.run({ false }) {
        pm.getApplicationInfo(pkg, 0).enabled &&
            (pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)) != null
    }

    private fun open(uri: String): InputStream {
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            val conn = URL(uri).openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            return conn.inputStream
        }
        return ctx.contentResolver.openInputStream(Uri.parse(uri)) ?: error("capa vazia")
    }

    /** Capa de verdade tem poucas centenas de KB; acima de [MAX_POSTER_BYTES] desiste. */
    private fun copyLimited(input: InputStream, out: java.io.OutputStream) {
        val buf = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) return
            total += n
            if (total > MAX_POSTER_BYTES) error("capa grande demais")
            out.write(buf, 0, n)
        }
    }

    private fun Cursor.str(col: String): String? = getColumnIndex(col).takeIf { it >= 0 && !isNull(it) }?.let { getString(it) }

    private fun Cursor.long(col: String): Long = getColumnIndex(col).takeIf { it >= 0 && !isNull(it) }?.let { getLong(it) } ?: 0L

    private fun Cursor.int(col: String, default: Int): Int = getColumnIndex(col).takeIf { it >= 0 && !isNull(it) }?.let { getInt(it) } ?: default

    private companion object {
        const val TAG = "Inicio"
        const val TIMEOUT_MS = 8_000
        const val MAX_POSTER_BYTES = 8L * 1024 * 1024
        val ARGB = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val COLUMNS = arrayOf(
            WatchNextPrograms.COLUMN_PACKAGE_NAME,
            WatchNextPrograms.COLUMN_TITLE,
            WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER,
            WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER,
            WatchNextPrograms.COLUMN_POSTER_ART_URI,
            WatchNextPrograms.COLUMN_THUMBNAIL_URI,
            WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
            WatchNextPrograms.COLUMN_DURATION_MILLIS,
            WatchNextPrograms.COLUMN_INTENT_URI,
            WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS,
            WatchNextPrograms.COLUMN_BROWSABLE,
            WatchNextPrograms.COLUMN_SERIES_ID,
        )
    }
}
