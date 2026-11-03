package br.gui.tvmae.data

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import br.gui.tvmae.core.Guard
import br.gui.tvmae.core.OpenedLog
import br.gui.tvmae.core.WatchNextList

/**
 * Anota o que toca dentro dos apps (título da sessão de mídia), para o Continuar assistindo pôr em primeiro o que ela
 * acabou de ver mesmo quando escolheu o vídeo dentro do app: o YouTube não atualiza o horário nem o ponto de um vídeo
 * revisto. Não lê notificação nenhuma; o acesso a notificações é só o que o Android exige para ver as sessões de mídia.
 * Liberado pelo ADB: `cmd notification allow_listener br.gui.tvmae/br.gui.tvmae.data.PlayingListener`.
 */
class PlayingListener : NotificationListenerService() {
    private val controllers = HashMap<MediaSession.Token, Watch>()
    private var sessions: MediaSessionManager? = null

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { follow(it.orEmpty()) }

    override fun onListenerConnected() {
        Guard.run({ Log.w(TAG, "sem acesso às sessões de mídia", it) }) {
            val msm = getSystemService(MediaSessionManager::class.java)
            val me = ComponentName(this, PlayingListener::class.java)
            msm.addOnActiveSessionsChangedListener(sessionsChanged, me)
            sessions = msm
            follow(msm.getActiveSessions(me))
        }
    }

    override fun onListenerDisconnected() {
        Guard.run({ Unit }) { sessions?.removeOnActiveSessionsChangedListener(sessionsChanged) }
        sessions = null
        follow(emptyList())
    }

    /** Acompanha só as sessões ativas: as que sumiram perdem o acompanhamento. */
    private fun follow(active: List<MediaController>) {
        val tokens = active.map { it.sessionToken }.toSet()
        controllers.keys.filter { it !in tokens }.forEach { controllers.remove(it)?.stop() }
        active.filter { it.sessionToken !in controllers }.forEach { c -> controllers[c.sessionToken] = Watch(c).also { it.check() } }
    }

    private inner class Watch(private val c: MediaController) : MediaController.Callback() {
        private var lastKey: String? = null

        init { c.registerCallback(this) }

        fun stop() = Guard.run({ Unit }) { c.unregisterCallback(this) }

        override fun onPlaybackStateChanged(state: PlaybackState?) = check()
        override fun onMetadataChanged(metadata: MediaMetadata?) = check()

        /** Tocando com título: anota uma vez por vídeo (o pausa e continua do mesmo vídeo não regrava). */
        fun check() = Guard.run({ Unit }) {
            if (c.playbackState?.state != PlaybackState.STATE_PLAYING) return@run
            val title = c.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: c.metadata?.description?.title?.toString()
            val key = WatchNextList.playedKey(c.packageName, title) ?: return@run
            if (key == lastKey) return@run
            lastKey = key
            val prefs = Prefs(this@PlayingListener)
            prefs.opened = OpenedLog.record(prefs.opened, key, System.currentTimeMillis())
            Log.i(TAG, "tocando: $key")
        }
    }

    private companion object {
        const val TAG = "TvMae"
    }
}
