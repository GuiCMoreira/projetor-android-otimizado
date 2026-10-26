package br.gui.inicio.data

import android.content.Context
import br.gui.inicio.core.City
import br.gui.inicio.core.DockConfig
import br.gui.inicio.core.NewAppsState
import br.gui.inicio.core.Rotation
import br.gui.inicio.core.Screensaver
import br.gui.inicio.core.WallpaperChoice

/**
 * Tudo o que o launcher lembra. Cada leitura cai no padrão se o valor guardado estiver corrompido
 * (tipo trocado, por exemplo): uma preferência ruim nunca pode derrubar a tela inicial.
 */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("inicio", Context.MODE_PRIVATE)

    private fun string(key: String): String? = runCatching { sp.getString(key, null) }.getOrNull()
    private fun int(key: String, def: Int): Int = runCatching { sp.getInt(key, def) }.getOrDefault(def)
    private fun long(key: String, def: Long): Long = runCatching { sp.getLong(key, def) }.getOrDefault(def)

    var dock: DockConfig
        get() = DockConfig.parse(string(K_DOCK))
        set(v) { sp.edit().putString(K_DOCK, v.serialize()).apply() }

    var wallpaper: WallpaperChoice
        get() = WallpaperChoice.parse(string(K_WALLPAPER))
        set(v) { sp.edit().putString(K_WALLPAPER, v.serialize()).apply() }

    /** Troca automática de fundo, em minutos (0 = desligada). Padrão da 1.1: a cada 1 hora. */
    var rotateMinutes: Int
        get() = int(K_ROTATE, Rotation.OPTIONS[1])
        set(v) { sp.edit().putInt(K_ROTATE, v).apply() }

    /** Minutos sem tecla até o descanso de tela (0 = desligado). */
    var screensaverMinutes: Int
        get() = int(K_SCREENSAVER, Screensaver.DEFAULT)
        set(v) { sp.edit().putInt(K_SCREENSAVER, v).apply() }

    var weatherCity: City?
        get() = City.parse(string(K_CITY))
        set(v) { sp.edit().putString(K_CITY, v?.serialize()).apply() }

    /** Última resposta do Open-Meteo (JSON) e quando chegou. */
    var weatherJson: String?
        get() = string(K_WEATHER)
        set(v) { sp.edit().putString(K_WEATHER, v).apply() }

    var weatherAt: Long
        get() = long(K_WEATHER_AT, 0)
        set(v) { sp.edit().putLong(K_WEATHER_AT, v).apply() }

    val newApps: NewAppsState
        get() = runCatching {
            NewAppsState.fromStored(
                // Sem K_INIT_TIME (formato antigo) conta como primeira execução: senão tudo viraria "novo".
                initialized = sp.getBoolean(K_INIT, false) && sp.contains(K_INIT_TIME),
                initTime = sp.getLong(K_INIT_TIME, 0),
                stored = sp.getStringSet(K_OPENED, emptySet())!!.toSet(),
            )
        }.getOrDefault(NewAppsState())

    /** Ler, alterar e gravar num passo só: a lista (segundo plano) e o "abrir app" (tela) não se atropelam. */
    fun updateNewApps(change: (NewAppsState) -> NewAppsState) = synchronized(LOCK) {
        val v = change(newApps)
        sp.edit()
            .putBoolean(K_INIT, v.initialized)
            .putLong(K_INIT_TIME, v.initTime)
            .putStringSet(K_OPENED, HashSet(v.storedOpened()))
            .remove(K_KNOWN_OLD)
            .apply()
    }

    private companion object {
        val LOCK = Any()
        const val K_DOCK = "dock"
        const val K_WALLPAPER = "wallpaper"
        const val K_ROTATE = "wallpaper_rotate_minutes"
        const val K_SCREENSAVER = "screensaver_minutes"
        const val K_CITY = "weather_city"
        const val K_WEATHER = "weather_json"
        const val K_WEATHER_AT = "weather_at"
        const val K_INIT = "new_apps_initialized"
        const val K_INIT_TIME = "new_apps_init_time"
        const val K_OPENED = "new_apps_opened_at"
        const val K_KNOWN_OLD = "new_apps_known"
    }
}
