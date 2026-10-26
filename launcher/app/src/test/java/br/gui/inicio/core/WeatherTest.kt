package br.gui.inicio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherTest {
    private val forecast = """
        {"current":{"time":"2026-10-26T19:15","temperature_2m":15.8,"weather_code":2,"is_day":0},
         "daily":{"time":["2026-10-26"],"temperature_2m_max":[23.1],"temperature_2m_min":[13.5]}}
    """.trimIndent()

    @Test fun parsesForecast() {
        val w = WeatherParser.forecast(forecast)!!
        assertEquals(16, w.temp)
        assertEquals(23, w.max)
        assertEquals(14, w.min)
        assertEquals(2, w.code)
        assertEquals(false, w.isDay)
    }

    @Test fun brokenForecastIsNull() {
        assertNull(WeatherParser.forecast("{}"))
        assertNull(WeatherParser.forecast("não é json"))
    }

    @Test fun parsesCitySearch() {
        val json = """{"results":[{"name":"Campinas","latitude":-22.9,"longitude":-47.06,"admin1":"São Paulo","country":"Brasil"},
            {"name":"Sem estado","latitude":1.0,"longitude":2.0}]}"""
        val cities = WeatherParser.cities(json)
        assertEquals(2, cities.size)
        assertEquals(City("Campinas", "São Paulo", -22.9, -47.06), cities[0])
        assertEquals("", cities[1].region)
        assertTrue(WeatherParser.cities("{}").isEmpty())
    }

    @Test fun parsesIpLocation() {
        val json = """{"city":"Bragança Paulista","region":"São Paulo","latitude":"-22.9434","longitude":"-46.5581"}"""
        assertEquals(City("Bragança Paulista", "São Paulo", -22.9434, -46.5581), WeatherParser.ipCity(json))
        assertNull(WeatherParser.ipCity("""{"city":null}"""))
    }

    @Test fun describesWmoCodesInPortuguese() {
        assertEquals("Céu limpo", WeatherCodes.describe(0))
        assertEquals("Parcialmente nublado", WeatherCodes.describe(2))
        assertEquals("Chuva forte", WeatherCodes.describe(65))
        assertEquals("Trovoada", WeatherCodes.describe(95))
        assertEquals("Tempo indefinido", WeatherCodes.describe(1234))
    }

    @Test fun mapsCodesToSky() {
        assertEquals(Sky.CLEAR, WeatherCodes.sky(0))
        assertEquals(Sky.PARTLY, WeatherCodes.sky(2))
        assertEquals(Sky.CLOUDY, WeatherCodes.sky(3))
        assertEquals(Sky.FOG, WeatherCodes.sky(48))
        assertEquals(Sky.RAIN, WeatherCodes.sky(81))
        assertEquals(Sky.STORM, WeatherCodes.sky(99))
        assertEquals(Sky.CLOUDY, WeatherCodes.sky(1234))
    }
}
