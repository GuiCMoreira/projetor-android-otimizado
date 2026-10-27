package br.gui.inicio.core

import kotlin.math.roundToInt
import org.json.JSONObject

enum class Sky { CLEAR, PARTLY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM }

/** Clima de agora (graus inteiros) e máxima e mínima do dia. */
data class Weather(val temp: Int, val code: Int, val isDay: Boolean, val max: Int, val min: Int)

/** Códigos WMO usados pelo Open-Meteo. */
object WeatherCodes {
    fun sky(code: Int): Sky = when (code) {
        0, 1 -> Sky.CLEAR
        2 -> Sky.PARTLY
        3 -> Sky.CLOUDY
        45, 48 -> Sky.FOG
        51, 53, 55, 56, 57 -> Sky.DRIZZLE
        61, 63, 65, 66, 67, 80, 81, 82 -> Sky.RAIN
        71, 73, 75, 77, 85, 86 -> Sky.SNOW
        95, 96, 99 -> Sky.STORM
        else -> Sky.CLOUDY
    }

    /** [isDay] falso troca o "sol" do código 1 (à noite o ícone já é a lua). */
    fun describe(code: Int, isDay: Boolean = true): String = when (code) {
        0 -> "Céu limpo"
        1 -> if (isDay) "Predomínio de sol" else "Céu quase limpo"
        2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Neblina"
        51, 53, 55 -> "Garoa"
        56, 57 -> "Garoa congelante"
        61 -> "Chuva fraca"
        63 -> "Chuva"
        65 -> "Chuva forte"
        66, 67 -> "Chuva congelante"
        71, 73, 75, 77 -> "Neve"
        80, 81, 82 -> "Pancadas de chuva"
        85, 86 -> "Pancadas de neve"
        95 -> "Trovoada"
        96, 99 -> "Trovoada com granizo"
        else -> "Tempo indefinido"
    }
}

/** Lê as respostas do Open-Meteo e do geojs. Qualquer formato inesperado vira null ou lista vazia. */
object WeatherParser {
    fun forecast(json: String): Weather? = runCatching {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        Weather(
            temp = current.getDouble("temperature_2m").roundToInt(),
            code = current.getInt("weather_code"),
            isDay = current.optInt("is_day", 1) == 1,
            max = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
            min = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
        )
    }.getOrNull()

    fun cities(json: String): List<City> = runCatching {
        val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
        (0 until results.length()).mapNotNull { i ->
            val r = results.getJSONObject(i)
            val name = r.optString("name")
            if (name.isBlank()) null else City(name, r.optString("admin1", ""), r.getDouble("latitude"), r.getDouble("longitude"))
        }
    }.getOrDefault(emptyList())

    /** Resposta do get.geojs.io (latitude e longitude vêm como texto). */
    fun ipCity(json: String): City? = runCatching {
        val r = JSONObject(json)
        val name = if (r.isNull("city")) "" else r.optString("city")
        if (name.isBlank()) return null
        City(name, r.optString("region", ""), r.getString("latitude").toDouble(), r.getString("longitude").toDouble())
    }.getOrNull()
}
