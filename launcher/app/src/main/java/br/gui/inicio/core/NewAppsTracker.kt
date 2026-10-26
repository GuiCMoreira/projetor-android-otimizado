package br.gui.inicio.core

/** Estado do ponto azul. known = apps presentes na primeira execução; opened = apps já abertos pelo launcher. */
data class NewAppsState(
    val initialized: Boolean = false,
    val known: Set<String> = emptySet(),
    val opened: Set<String> = emptySet(),
)

object NewAppsTracker {
    /** Na primeira execução tudo o que já está instalado conta como conhecido. Depois disso não muda. */
    fun ensureInitialized(state: NewAppsState, installed: Set<String>): NewAppsState =
        if (state.initialized) state else NewAppsState(initialized = true, known = installed)

    /** Esquece apps desinstalados, para que uma reinstalação apareça como nova. */
    fun prune(state: NewAppsState, installed: Set<String>): NewAppsState =
        state.copy(known = state.known intersect installed, opened = state.opened intersect installed)

    fun isNew(state: NewAppsState, pkg: String): Boolean =
        state.initialized && pkg !in state.known && pkg !in state.opened

    fun markOpened(state: NewAppsState, pkg: String): NewAppsState =
        state.copy(opened = state.opened + pkg)
}
