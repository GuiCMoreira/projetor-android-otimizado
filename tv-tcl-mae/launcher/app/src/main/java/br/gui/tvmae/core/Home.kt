package br.gui.tvmae.core

/** Saudação do topo da tela: muda sozinha com a hora do dia. */
object Greeting {
    fun at(hour: Int, name: String): String {
        val word = when (hour) {
            in 5..11 -> "Bom dia"
            in 12..17 -> "Boa tarde"
            else -> "Boa noite"
        }
        return if (name.isBlank()) word else "$word, ${name.trim()}"
    }
}

enum class Dir { LEFT, RIGHT, UP, DOWN }

/**
 * Setas na tela inicial da mãe: três fileiras, de cima para baixo os atalhos (entrada, Wi-Fi, Bluetooth, timer,
 * ajustes), o Continuar assistindo e a faixa de apps (o último item é o "Todos os apps"). Cada fileira lembra a
 * coluna onde o foco estava, como nas smart TVs; fileira vazia é pulada; as setas laterais não mudam de fileira.
 */
class HomeNav {
    data class Pos(val row: Int, val col: Int)

    private val remembered = HashMap<Int, Int>()

    fun move(from: Pos, d: Dir, sizes: IntArray): Pos {
        val size = sizes.getOrElse(from.row) { 0 }
        return when (d) {
            Dir.LEFT -> Pos(from.row, (from.col - 1).coerceAtLeast(0))
            Dir.RIGHT -> Pos(from.row, (from.col + 1).coerceAtMost((size - 1).coerceAtLeast(0)))
            Dir.UP, Dir.DOWN -> {
                val step = if (d == Dir.UP) -1 else 1
                var row = from.row + step
                while (row in sizes.indices && sizes[row] == 0) row += step
                if (row !in sizes.indices) return from
                remembered[from.row] = from.col
                Pos(row, (remembered[row] ?: 0).coerceIn(0, sizes[row] - 1))
            }
        }
    }

    companion object {
        const val SHORTCUTS = 0
        const val WATCH = 1
        const val APPS = 2

        /** Começa no que ela estava vendo; sem nada para continuar, na faixa de apps. */
        fun start(sizes: IntArray): Pos = if (sizes.getOrElse(WATCH) { 0 } > 0) Pos(WATCH, 0) else Pos(APPS, 0)
    }
}

/** Apps fixos na faixa, guardados como texto ("pacote,pacote"), em ordem. */
object Pins {
    /** Cabem 8 na faixa de 1280 px, mais o "Todos os apps". */
    const val MAX = 8

    /** Os apps que ela usa: Globoplay, Netflix e YouTube. HBO Max, Prime Video, Twitch, Red Bull TV e YouTube Music saíram da TV. */
    val DEFAULT = listOf("com.globo.globotv", "com.netflix.ninja", "com.google.android.youtube.tv")

    enum class Change { ADDED, REMOVED, FULL }

    class Result(val pins: List<String>, val change: Change)

    /** null = nunca escolhido (usa [DEFAULT]); "" = escolheu não fixar nada. */
    fun parse(raw: String?): List<String> =
        raw?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct() ?: DEFAULT

    fun format(pins: List<String>): String = pins.joinToString(",")

    /**
     * Fixa ou tira [pkg]. A lotação conta só os [installed] (null = todos): um app fixado e depois desinstalado não
     * aparece em lugar nenhum, então não pode ocupar vaga; ao fixar outro, ele é descartado.
     */
    fun toggle(pins: List<String>, pkg: String, installed: Set<String>? = null): Result {
        if (pkg in pins) return Result(pins - pkg, Change.REMOVED)
        val live = if (installed == null) pins else pins.filter { it in installed }
        if (live.size >= MAX) return Result(pins, Change.FULL)
        return Result(live + pkg, Change.ADDED)
    }

    /** Só os instalados, na ordem escolhida. Um app desinstalado some da tela mas continua lembrado. */
    fun visible(pins: List<String>, installed: Set<String>): List<String> = pins.filter { it in installed }.take(MAX)
}

/**
 * Quando mostrar e quando buscar o clima. O guardado só vale por [SHOW_MS]: clima de dias atrás, mostrado como de
 * agora, engana. Sem internet, espera [RETRY_MS] entre tentativas (antes tentava a cada minuto, o dia todo). Relógio
 * que voltou no tempo (gravado "no futuro") conta como vencido, senão o clima pararia de atualizar.
 */
object WeatherPolicy {
    const val SHOW_MS = 3 * 60 * 60_000L
    const val MAX_AGE_MS = 30 * 60_000L
    const val RETRY_MS = 10 * 60_000L

    fun showCached(now: Long, savedAt: Long): Boolean = savedAt > 0 && now >= savedAt && now - savedAt < SHOW_MS

    fun shouldFetch(now: Long, savedAt: Long, lastAttempt: Long): Boolean {
        val fresh = savedAt in 1..now && now - savedAt < MAX_AGE_MS
        val waiting = lastAttempt in 1..now && now - lastAttempt < RETRY_MS
        return !fresh && !waiting
    }
}

/**
 * Peças da TCL que precisam continuar no sistema mas não servem para ela, escondidas do "Todos os apps": o Protetor
 * de Segurança (permissões), o Quick Panel (botão do controle), a TCL Home, a TV (antena e HDMI, que ela não usa) e
 * a Alexa (o atualizador da TCL a religa a cada reinício). Os botões do controle continuam abrindo cada uma.
 */
object HiddenApps {
    private val HIDDEN = setOf(
        "com.tcl.guard", "com.tcl.suspension", "com.tcl.dashboard", "com.tcl.tv", "com.tcl.smartalexa",
        "com.tcl.partnercustomizer",
    )

    fun listed(pkg: String): Boolean = pkg !in HIDDEN
}
