package br.gui.tvavo.data

import android.content.Context
import br.gui.tvavo.core.City
import br.gui.tvavo.core.Pins

/** Preferências da tela inicial. Leitura defensiva: um valor corrompido vira o padrão, nunca uma queda. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("tvavo", Context.MODE_PRIVATE)

    private fun string(key: String): String? = runCatching { sp.getString(key, null) }.getOrNull()
    private fun long(key: String, def: Long): Long = runCatching { sp.getLong(key, def) }.getOrDefault(def)

    /** Apps fixos na tela inicial, em ordem. Nunca escolhidos = Globoplay, YouTube e Pluto TV. */
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

    private companion object {
        const val K_PINS = "pins"
        const val K_CITY = "weather_city"
        const val K_WEATHER = "weather_json"
        const val K_WEATHER_AT = "weather_at"
    }
}
