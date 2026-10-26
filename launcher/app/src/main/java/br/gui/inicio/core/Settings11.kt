package br.gui.inicio.core

import kotlin.math.roundToInt

/** Troca automática de papel de parede: a foto da vez sai do relógio, sem nada rodando em segundo plano. */
object Rotation {
    /** Opções do menu, em minutos (0 = desligado). */
    val OPTIONS = listOf(0, 60, 180, 360, 1440)

    /** Índice da foto para o instante [nowMs]; -1 quando desligado ou sem fotos. */
    fun index(nowMs: Long, intervalMinutes: Int, count: Int): Int {
        if (intervalMinutes <= 0 || count <= 0) return -1
        return ((nowMs / (intervalMinutes * 60_000L)) % count).toInt()
    }
}

/** Valores que o controle percorre com OK (último volta ao primeiro). */
object Options {
    fun next(options: List<Int>, current: Int): Int {
        val i = options.indexOf(current)
        return if (i < 0) options.first() else options[(i + 1) % options.size]
    }
}

object SleepTimer {
    /** Minutos do bloco "Timer" (0 = desligado). */
    val OPTIONS = listOf(0, 15, 30, 60, 90, 120)

    /** Minutos que faltam, arredondando para cima; 0 se já passou. */
    fun minutesLeft(nowMs: Long, atMs: Long): Int =
        if (atMs <= nowMs) 0 else ((atMs - nowMs + 59_999) / 60_000).toInt()
}

object Screensaver {
    /** Minutos sem tecla até o descanso de tela (0 = desligado). */
    val OPTIONS = listOf(0, 5, 10, 15, 30)
    const val DEFAULT = 5
}

object Volume {
    fun step(current: Int, max: Int, delta: Int): Int = (current + delta).coerceIn(0, max.coerceAtLeast(0))

    fun percent(current: Int, max: Int): Int = if (max <= 0) 0 else (current * 100f / max).roundToInt()
}

/** Cidade do widget de clima. Guardada nas preferências como "nome|região|lat|lon". */
data class City(val name: String, val region: String, val lat: Double, val lon: Double) {
    fun serialize(): String = listOf(name, region, lat.toString(), lon.toString()).joinToString(SEP)

    companion object {
        private const val SEP = "|"

        fun parse(raw: String?): City? {
            val p = raw?.split(SEP) ?: return null
            if (p.size != 4 || p[0].isBlank()) return null
            val lat = p[2].toDoubleOrNull() ?: return null
            val lon = p[3].toDoubleOrNull() ?: return null
            return City(p[0], p[1], lat, lon)
        }
    }
}
