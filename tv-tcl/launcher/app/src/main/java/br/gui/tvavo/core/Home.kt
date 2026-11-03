package br.gui.tvavo.core

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
 * Onde fica cada botão de app, em pixels de uma tela 1280x720 (a da TV), como no protótipo: até 4 apps numa fileira
 * de botões grandes (ícone em cima, nome embaixo); 5 ou 6 em duas fileiras de 3, mais baixas, com o ícone ao lado do
 * nome ([Cell.compact]). O "Todos os apps" fica sempre embaixo de tudo, separado.
 */
object HomeGrid {
    const val MAX = 6
    const val WIDTH = 1280
    const val SIDE = 80
    const val TOP = 262
    private const val GAP = 35
    private const val ROW_GAP = 20
    private const val BIG_H = 300
    private const val SMALL_H = 145
    /** Um ou dois apps não viram faixas gigantes: cada botão tem no máximo a largura de quando são 3. */
    private const val MAX_W = (WIDTH - 2 * SIDE - 2 * GAP) / 3

    data class Cell(val left: Int, val top: Int, val width: Int, val height: Int, val compact: Boolean)

    fun columns(n: Int): Int = if (n <= 4) n.coerceAtLeast(1) else 3

    fun cells(n: Int): List<Cell> {
        val count = n.coerceIn(0, MAX)
        if (count == 0) return emptyList()
        val cols = columns(count)
        val compact = count > 4
        val h = if (compact) SMALL_H else BIG_H
        val w = minOf((WIDTH - 2 * SIDE - GAP * (cols - 1)) / cols, MAX_W)
        // Centraliza a fileira quando os botões não ocupam a largura toda (1 ou 2 apps).
        val rowWidth = w * cols + GAP * (cols - 1)
        val left0 = (WIDTH - rowWidth) / 2
        return (0 until count).map { i ->
            Cell(left0 + (i % cols) * (w + GAP), TOP + (i / cols) * (h + ROW_GAP), w, h, compact)
        }
    }

    /**
     * Próximo item para a seta [d]. Índices 0 até n-1 são os apps; [n] é o "Todos os apps". Subindo dele, volta para
     * o app de onde veio ([last]). As setas laterais não pulam de fileira.
     */
    fun move(index: Int, d: Dir, n: Int, last: Int): Int {
        val count = n.coerceIn(0, MAX)
        if (count == 0) return 0
        val all = count
        if (index >= all) return if (d == Dir.UP) last.coerceIn(0, count - 1) else all
        val cols = columns(count)
        val col = index % cols
        return when (d) {
            Dir.LEFT -> if (col > 0) index - 1 else index
            Dir.RIGHT -> if (col < cols - 1 && index + 1 < count) index + 1 else index
            Dir.DOWN -> if (index + cols < count) index + cols else all
            Dir.UP -> if (index - cols >= 0) index - cols else index
        }
    }
}

/** Apps fixos na tela inicial, guardados como texto ("pacote,pacote"), em ordem. */
object Pins {
    /** Globoplay, YouTube e Pluto TV: o que a avó vai usar, como no projetor. */
    val DEFAULT = listOf("com.globo.globotv", "com.google.android.youtube.tv", "tv.pluto.android")

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
        if (live.size >= HomeGrid.MAX) return Result(pins, Change.FULL)
        return Result(live + pkg, Change.ADDED)
    }

    /** Só os instalados, na ordem escolhida. Um app desinstalado some da tela mas continua lembrado. */
    fun visible(pins: List<String>, installed: Set<String>): List<String> = pins.filter { it in installed }.take(HomeGrid.MAX)
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
