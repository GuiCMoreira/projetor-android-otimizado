package br.gui.inicio.data

import android.content.Context
import br.gui.inicio.core.DockConfig
import br.gui.inicio.core.NewAppsState
import br.gui.inicio.core.WallpaperChoice

/** Tudo o que o launcher lembra: ordem do Dock, papel de parede e o estado do ponto azul. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("inicio", Context.MODE_PRIVATE)

    var dock: DockConfig
        get() = DockConfig.parse(sp.getString(K_DOCK, null))
        set(v) { sp.edit().putString(K_DOCK, v.serialize()).apply() }

    var wallpaper: WallpaperChoice
        get() = WallpaperChoice.parse(sp.getString(K_WALLPAPER, null))
        set(v) { sp.edit().putString(K_WALLPAPER, v.serialize()).apply() }

    var newApps: NewAppsState
        get() = NewAppsState.fromStored(
            // Sem K_INIT_TIME (formato antigo) conta como primeira execução: senão tudo viraria "novo".
            initialized = sp.getBoolean(K_INIT, false) && sp.contains(K_INIT_TIME),
            initTime = sp.getLong(K_INIT_TIME, 0),
            stored = sp.getStringSet(K_OPENED, emptySet())!!.toSet(),
        )
        set(v) {
            sp.edit()
                .putBoolean(K_INIT, v.initialized)
                .putLong(K_INIT_TIME, v.initTime)
                .putStringSet(K_OPENED, HashSet(v.storedOpened()))
                .remove(K_KNOWN_OLD)
                .apply()
        }

    private companion object {
        const val K_DOCK = "dock"
        const val K_WALLPAPER = "wallpaper"
        const val K_INIT = "new_apps_initialized"
        const val K_INIT_TIME = "new_apps_init_time"
        const val K_OPENED = "new_apps_opened_at"
        const val K_KNOWN_OLD = "new_apps_known"
    }
}
