package br.gui.tvmae.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import br.gui.tvmae.core.Guard
import br.gui.tvmae.ui.Glyph

/**
 * Atalhos do canto superior direito. Caminhos conferidos nesta TCL (Android TV 11): a entrada abre o painel rápido
 * da TCL (TV, HDMI 1, HDMI 2, AV); o Bluetooth abre "adicionar acessório", porque a tela padrão do Android não existe
 * na TV. O timer de desligar ficou de fora: esta versão do sistema não tem uma tela de timer que dê para abrir de fora.
 */
enum class Shortcut(val label: String, val glyph: Glyph) {
    INPUT("Entrada", Glyph.INPUT),
    WIFI("Wi-Fi", Glyph.WIFI),
    BLUETOOTH("Bluetooth", Glyph.BLUETOOTH),
    SETTINGS("Ajustes", Glyph.GEAR),
}

class Shortcuts(private val ctx: Context) {
    fun open(s: Shortcut): Boolean {
        val intents = when (s) {
            Shortcut.INPUT -> listOf(Intent("com.tcl.suspension.VIEW_INPUTS"), Intent(Settings.ACTION_SETTINGS))
            Shortcut.WIFI -> listOf(Intent(Settings.ACTION_WIFI_SETTINGS), Intent(Settings.ACTION_SETTINGS))
            Shortcut.BLUETOOTH -> listOf(
                Intent().setComponent(ComponentName("com.android.tv.settings", "com.android.tv.settings.accessories.AddAccessoryActivity")),
                Intent(Settings.ACTION_SETTINGS),
            )
            Shortcut.SETTINGS -> listOf(Intent(Settings.ACTION_SETTINGS))
        }
        // O primeiro que abrir vale; o último de cada lista são os Ajustes, que existem em qualquer TV.
        return intents.any { start(it) }
    }

    private fun start(intent: Intent): Boolean = Guard.run({ false }) {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }
}
