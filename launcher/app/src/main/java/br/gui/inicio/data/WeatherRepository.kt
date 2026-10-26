package br.gui.inicio.data

import br.gui.inicio.core.City
import br.gui.inicio.core.Guard
import br.gui.inicio.core.Weather
import br.gui.inicio.core.WeatherParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Clima pelo Open-Meteo (gratuito, sem conta). A cidade vem das preferências; na primeira vez, da localização
 * da internet (get.geojs.io). Métodos de rede: só fora da thread principal.
 */
class WeatherRepository(private val prefs: Prefs) {

    /** Último clima guardado: aparece na hora, mesmo sem internet. */
    fun cached(): Weather? = prefs.weatherJson?.let { WeatherParser.forecast(it) }

    /** Busca o clima de agora e guarda. null = sem cidade ou sem resposta (o guardado continua valendo). */
    fun refresh(): Weather? {
        // A cidade detectada só vale se ninguém escolheu outra enquanto a consulta rodava.
        val city = prefs.weatherCity ?: detectCity()?.let { found -> prefs.weatherCity ?: found.also { prefs.weatherCity = it } } ?: return null
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${city.lat}&longitude=${city.lon}" +
            "&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min" +
            "&timezone=auto&forecast_days=1"
        val json = get(url) ?: return null
        val weather = WeatherParser.forecast(json) ?: return null
        if (prefs.weatherCity != city) return null // a cidade mudou durante a busca: não grava o clima da anterior
        prefs.weatherJson = json
        prefs.weatherAt = System.currentTimeMillis()
        return weather
    }

    fun search(name: String): List<City> {
        val q = URLEncoder.encode(name.trim(), "UTF-8")
        return get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=5&language=pt")
            ?.let { WeatherParser.cities(it) } ?: emptyList()
    }

    /** Troca a cidade (null = detectar de novo pela internet) e descarta o clima guardado da anterior. */
    fun setCity(city: City?) {
        prefs.weatherCity = city
        prefs.weatherJson = null
        prefs.weatherAt = 0
    }

    private fun detectCity(): City? = get("https://get.geojs.io/v1/ip/geo.json")?.let { WeatherParser.ipCity(it) }

    private fun get(url: String): String? = Guard.run({ null }) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("User-Agent", "Inicio-launcher")
            if (conn.responseCode == HttpURLConnection.HTTP_OK) conn.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val TIMEOUT_MS = 8_000
    }
}
