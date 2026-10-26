package br.gui.inicio.core

/**
 * Estado do ponto azul. [initTime] = primeira execução do launcher; [opened] = quando cada app foi aberto pelo launcher.
 * "Novo" se baseia na data de instalação que o Android guarda, então funciona mesmo que o launcher não esteja aberto
 * quando um app é desinstalado e reinstalado (a reinstalação ganha uma data nova).
 */
data class NewAppsState(
    val initialized: Boolean = false,
    val initTime: Long = 0,
    val opened: Map<String, Long> = emptyMap(),
) {
    /** Formato salvo nas preferências: "pacote|instante". */
    fun storedOpened(): Set<String> = opened.mapTo(HashSet()) { (pkg, at) -> "$pkg|$at" }

    companion object {
        fun fromStored(initialized: Boolean, initTime: Long, stored: Set<String>): NewAppsState {
            val opened = stored.mapNotNull { item ->
                val pkg = item.substringBefore('|', "")
                val at = item.substringAfter('|', "").toLongOrNull()
                if (pkg.isEmpty() || at == null) null else pkg to at
            }.toMap()
            return NewAppsState(initialized, initTime, opened)
        }
    }
}

object NewAppsTracker {
    /** Na primeira execução tudo o que já está instalado conta como conhecido. Depois disso não muda. */
    fun ensureInitialized(state: NewAppsState, now: Long): NewAppsState =
        if (state.initialized) state else NewAppsState(initialized = true, initTime = now)

    fun isNew(state: NewAppsState, pkg: String, installedAt: Long): Boolean =
        state.initialized && installedAt > state.initTime && (state.opened[pkg] ?: Long.MIN_VALUE) < installedAt

    fun markOpened(state: NewAppsState, pkg: String, now: Long): NewAppsState =
        state.copy(opened = state.opened + (pkg to now))
}
