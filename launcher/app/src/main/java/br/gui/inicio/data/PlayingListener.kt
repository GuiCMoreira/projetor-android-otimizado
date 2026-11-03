package br.gui.inicio.data

import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.util.Log
import br.gui.inicio.core.Guard
import br.gui.inicio.core.PlayedItem
import br.gui.inicio.core.PlayedLog
import br.gui.inicio.core.PosterCache
import java.io.File
import java.util.concurrent.Executors

/**
 * Anota o que toca nos apps (título, segunda linha, capa e ponto em que parou, como cada app informa ao sistema na
 * sessão de mídia) para o Continuar assistindo: neste projetor, YouTube, Netflix e Crunchyroll não publicam na lista
 * do Android. Não lê notificações (o filtro do manifesto não deixa chegar nenhuma); o acesso a notificações é só o que
 * o Android exige para ver as sessões de mídia. Liberado pelo ADB:
 * `cmd notification allow_listener br.gui.inicio/br.gui.inicio.data.PlayingListener`.
 */
class PlayingListener : NotificationListenerService() {
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val watches = HashMap<MediaSession.Token, Watch>()
    private var sessions: MediaSessionManager? = null

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { follow(it.orEmpty()) }

    /** Tocando por muito tempo sem pausar não gera aviso nenhum: a cada [TICK_MS] anota o ponto atual. */
    private val tick = object : Runnable {
        override fun run() {
            watches.values.forEach { it.snapshot() }
            if (watches.values.any { it.playing }) main.postDelayed(this, TICK_MS)
        }
    }

    override fun onListenerConnected() {
        Guard.run({ Log.w(TAG, "sem acesso às sessões de mídia", it) }) {
            val msm = getSystemService(MediaSessionManager::class.java)
            val me = ComponentName(this, PlayingListener::class.java)
            msm.addOnActiveSessionsChangedListener(sessionsChanged, me, main)
            sessions = msm
            follow(msm.getActiveSessions(me))
        }
    }

    override fun onListenerDisconnected() {
        Guard.run({ Unit }) { sessions?.removeOnActiveSessionsChangedListener(sessionsChanged) }
        sessions = null
        follow(emptyList())
        main.removeCallbacks(tick)
    }

    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }

    /** Acompanha só as sessões ativas; ao sumir, a sessão é anotada uma última vez. */
    private fun follow(active: List<MediaController>) {
        val tokens = active.map { it.sessionToken }.toSet()
        watches.keys.filter { it !in tokens }.forEach { watches.remove(it)?.stop() }
        active.filter { it.sessionToken !in watches }.forEach { c -> watches[c.sessionToken] = Watch(c).also { it.snapshot() } }
    }

    private fun startTicking() {
        main.removeCallbacks(tick)
        main.postDelayed(tick, TICK_MS)
    }

    private inner class Watch(private val c: MediaController) : MediaController.Callback() {
        var playing = false
            private set
        private var artFor: String? = null
        private var art: String? = null

        init { c.registerCallback(this, main) }

        fun stop() = Guard.run({ Unit }) {
            snapshot()
            c.unregisterCallback(this)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) { snapshot() }
        override fun onMetadataChanged(metadata: MediaMetadata?) { snapshot() }

        fun snapshot(): Unit = Guard.run({ Log.w(TAG, "falha ao ler o que toca", it); Unit }) {
            val state = c.playbackState ?: return@run
            val wasPlaying = playing
            playing = state.state == PlaybackState.STATE_PLAYING
            if (playing && !wasPlaying) startTicking()
            // Só tocando ou no instante em que para: uma sessão pausada que reaparece horas depois não é "assistir agora".
            if (!playing && !wasPlaying) return@run
            val md = c.metadata ?: return@run
            val title = text(md, MediaMetadata.METADATA_KEY_TITLE, MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: md.description?.title?.toString()?.takeIf { it.isNotBlank() } ?: return@run
            val position = positionNow(state)
            if (!PlayedLog.watched(position)) return@run
            val subtitle = PlayedLog.secondLine(c.packageName, title, text(md, MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, MediaMetadata.METADATA_KEY_ARTIST, MediaMetadata.METADATA_KEY_ALBUM)
                ?: md.description?.subtitle?.toString())
            val key = "${c.packageName}|$title"
            if (key != artFor) {
                artFor = key
                art = artOf(md, key)
                Log.i(TAG, "tocando: ${c.packageName} | $title | $subtitle | id=${md.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)} | campos=${md.keySet().joinToString { k -> "${k.substringAfterLast('.')}=${Guard.run({ "?" }) { md.getText(k)?.toString()?.take(60) }}" }}")
            }
            val item = PlayedItem(
                pkg = c.packageName,
                title = title.trim(),
                subtitle = subtitle,
                mediaId = md.getString(MediaMetadata.METADATA_KEY_MEDIA_ID),
                positionMs = position,
                durationMs = md.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0),
                playedAtMs = System.currentTimeMillis(),
                art = art,
            )
            io.execute { PlayedStore.record(this@PlayingListener, item) }
        }

        private fun text(md: MediaMetadata, vararg keys: String): String? =
            keys.firstNotNullOfOrNull { k -> md.getString(k)?.takeIf { it.isNotBlank() } }

        /** Endereço da capa, se o app informou; senão a imagem que veio junto, salva como arquivo. */
        private fun artOf(md: MediaMetadata, key: String): String? {
            val uri = text(md, MediaMetadata.METADATA_KEY_ART_URI, MediaMetadata.METADATA_KEY_ALBUM_ART_URI, MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
            if (uri != null && (uri.startsWith("http://") || uri.startsWith("https://"))) return uri
            val bmp = sequenceOf(MediaMetadata.METADATA_KEY_ART, MediaMetadata.METADATA_KEY_ALBUM_ART, MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                .mapNotNull { md.getBitmap(it) }.firstOrNull() ?: return uri
            val dir = PlayedStore.artDir(this@PlayingListener).apply { mkdirs() }
            val file = File(dir, PosterCache.fileName(key, bmp.width, bmp.height))
            val copy = bmp.copy(Bitmap.Config.ARGB_8888, false)
            io.execute { Guard.run({ Unit }) { file.outputStream().use { copy.compress(Bitmap.CompressFormat.JPEG, 90, it) }; copy.recycle() } }
            return "file://${file.path}"
        }

        /** O app só avisa o ponto quando muda de estado; tocando, soma o tempo que passou desde o aviso. */
        private fun positionNow(state: PlaybackState): Long {
            if (state.state != PlaybackState.STATE_PLAYING || state.lastPositionUpdateTime <= 0) return state.position.coerceAtLeast(0)
            val elapsed = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
            return (state.position + elapsed * state.playbackSpeed).toLong().coerceAtLeast(0)
        }
    }

    private companion object {
        const val TAG = "Inicio"
        const val TICK_MS = 30_000L
    }
}
