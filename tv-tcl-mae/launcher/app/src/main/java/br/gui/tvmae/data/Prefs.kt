package br.gui.tvmae.data

import android.content.Context
import br.gui.tvmae.core.City
import br.gui.tvmae.core.OpenedLog
import br.gui.tvmae.core.Pins

/** Preferências da tela inicial. Leitura defensiva: um valor corrompido vira o padrão, nunca uma queda. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("tvmae", Context.MODE_PRIVATE)

    private fun string(key: String): String? = runCatching { sp.getString(key, null) }.getOrNull()
    private fun long(key: String, def: Long): Long = runCatching { sp.getLong(key, def) }.getOrDefault(def)

    /** Apps fixos na tela inicial, em ordem. Nunca escolhidos = Globoplay, Netflix e YouTube. */
    var pins: List<String>
        get() = Pins.parse(string(K_PINS))
        set(v) { sp.edit().putString(K_PINS, Pins.format(v)).apply() }

    var weatherCity: City?
        get() = City.parse(string(K_CITY))
        set(v) { sp.edit().putString(K_CITY, v?.serialize()).apply() }

    var weatherJson: String?
        get() = string(K_WEATHER)
        set(v) { sp.edit().putString(K_WEATHER, v).apply() }

    var weatherAt: Long
        get() = long(K_WEATHER_AT, 0)
        set(v) { sp.edit().putLong(K_WEATHER_AT, v).apply() }

    /** Cartões do Continuar assistindo abertos pela tela inicial e quando. */
    var opened: Map<String, Long>
        get() = OpenedLog.parse(string(K_OPENED))
        set(v) { sp.edit().putString(K_OPENED, OpenedLog.format(v)).apply() }

    private companion object {
        const val K_OPENED = "watch_opened"
        const val K_PINS = "pins"
        const val K_CITY = "weather_city"
        const val K_WEATHER = "weather_json"
        const val K_WEATHER_AT = "weather_at"
    }
}
