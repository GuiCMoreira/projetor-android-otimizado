package br.gui.inicio.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.provider.Settings
import br.gui.inicio.ui.Glyph

/** Atalho para uma tela do sistema ou do fabricante. [intents] em ordem de preferência (o primeiro que existir). */
data class SystemShortcut(
    val id: String,
    val title: String,
    val subtitle: String,
    val glyph: Glyph,
    val intents: List<Intent>,
)

/** Telas verificadas no projetor em 26/10/2026 (ver especificação, seções 2.3 e 2.7). */
object Shortcuts {
    private fun action(a: String) = Intent(a)

    val SETTINGS = SystemShortcut("settings", "Todos os ajustes", "Menu do projetor", Glyph.GEAR, listOf(action(Settings.ACTION_SETTINGS)))
    val SOURCE = SystemShortcut(
        "source", "Fontes", "HDMI e outras", Glyph.INPUT,
        listOf(
            action("android.settings.SOURCE"),
            Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput("com.sstar.tv.inputservice/.HdmiInputService/HW1")),
        ),
    )
    val WIFI = SystemShortcut("wifi", "Wi-Fi", "Redes", Glyph.WIFI, listOf(action(Settings.ACTION_WIFI_SETTINGS)))
    val BLUETOOTH = SystemShortcut("bluetooth", "Bluetooth", "Aparelhos", Glyph.BLUETOOTH, listOf(action(Settings.ACTION_BLUETOOTH_SETTINGS)))
    val DATE = SystemShortcut("date", "Data e hora", "", Glyph.GEAR, listOf(action(Settings.ACTION_DATE_SETTINGS)))
    val AUTOFOCUS = SystemShortcut("autofocus", "Foco automático", "Ajusta a nitidez", Glyph.FOCUS, listOf(action("com.zhiying.AUTO_FOCUS")))
    val KEYSTONE = SystemShortcut("keystone", "Keystone", "Corrigir 4 cantos", Glyph.KEYSTONE, listOf(action("android.intent.action.FOUR_POINT")))
    val ZOOM = SystemShortcut("zoom", "Zoom", "Tamanho da imagem", Glyph.ZOOM, listOf(action("android.intent.action.Scale")))
    val PICTURE = SystemShortcut("picture", "Modo de imagem", "Cores e brilho", Glyph.PICTURE, listOf(action("com.zhiying.PICTUREMODE")))
    val PROJECTION = SystemShortcut("projection", "Projeção", "Frontal, teto, traseira", Glyph.PROJECTION, listOf(action("android.settings.PROJECTION")))
    val SOUND = SystemShortcut("sound", "Som", "Saída e volume", Glyph.SOUND, listOf(action(Settings.ACTION_SOUND_SETTINGS)))
    /** Timer para desligar: abre "Outros ajustes" do projetor, onde fica o "Desligamento programado" oficial. */
    val SLEEP_TIMER = SystemShortcut("sleep", "Timer para desligar", "Desligamento programado", Glyph.TIMER, listOf(action("android.settings.OTHER_SETTINGS")))
    val ORIGINAL_LAUNCHER = SystemShortcut(
        "original", "Launcher original do fabricante", "", Glyph.GRID,
        listOf(Intent(Intent.ACTION_MAIN).setComponent(ComponentName("com.zeasn.launcher.projector", "com.zeasn.launcher.ui.MainActivity"))),
    )

    val CONTROL_CENTER = listOf(AUTOFOCUS, KEYSTONE, ZOOM, PICTURE, PROJECTION, SOUND, SOURCE, SLEEP_TIMER, SETTINGS)
}

class ShortcutResolver(private val ctx: Context) {
    /** Primeiro intent que existe neste aparelho; null esconde o atalho. Desativado também conta como inexistente. */
    fun intentFor(s: SystemShortcut): Intent? =
        s.intents.firstOrNull { it.resolveActivity(ctx.packageManager) != null }
            ?.let { Intent(it).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    fun isAvailable(s: SystemShortcut): Boolean = intentFor(s) != null

    fun available(list: List<SystemShortcut>): List<SystemShortcut> = list.filter(::isAvailable)

    fun open(s: SystemShortcut): Boolean {
        val intent = intentFor(s) ?: return false
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
