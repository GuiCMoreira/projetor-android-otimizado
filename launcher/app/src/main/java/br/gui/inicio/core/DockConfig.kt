package br.gui.inicio.core

/** Favoritos do Dock, na ordem escolhida. Imutável: cada operação devolve uma cópia. */
data class DockConfig(val packages: List<String>) {

    /** Favoritos ainda instalados, na ordem salva. Desinstalado some, mas volta se for reinstalado. */
    fun visible(installed: Set<String>): List<String> = packages.filter { it in installed }

    fun isFull(installed: Set<String>): Boolean = visible(installed).size >= MAX

    fun contains(pkg: String): Boolean = pkg in packages

    /** Adiciona no fim. Antes descarta favoritos desinstalados, para a vaga deles ficar livre. */
    fun add(pkg: String, installed: Set<String>): DockConfig {
        val kept = visible(installed)
        if (pkg in kept || kept.size >= MAX) return copy(packages = kept)
        return copy(packages = kept + pkg)
    }

    fun remove(pkg: String): DockConfig = copy(packages = packages - pkg)

    fun move(pkg: String, delta: Int): DockConfig {
        val from = packages.indexOf(pkg)
        if (from < 0) return this
        val to = (from + delta).coerceIn(0, packages.lastIndex)
        if (to == from) return this
        val list = packages.toMutableList().apply {
            removeAt(from)
            add(to, pkg)
        }
        return copy(packages = list)
    }

    fun serialize(): String = packages.joinToString(SEP)

    companion object {
        const val MAX = 8
        private const val SEP = ","
        private val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")

        val DEFAULT = DockConfig(
            listOf(
                "com.google.android.youtube.tv",
                "com.netflix.mediaclient",
                "com.crunchyroll.crunchyroid",
                "tv.pluto.android",
            ),
        )

        /** null = nunca salvo (usa o padrão). Texto vazio = Dock vazio de propósito. */
        fun parse(raw: String?): DockConfig {
            if (raw == null) return DEFAULT
            val list = raw.split(SEP)
                .map { it.trim() }
                .filter { PACKAGE_NAME.matches(it) }
                .distinct()
                .take(MAX)
            return DockConfig(list)
        }
    }
}
