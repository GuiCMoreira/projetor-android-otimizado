package br.gui.inicio.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import br.gui.inicio.ui.IconFactory
import java.text.Collator
import java.util.Locale

data class AppEntry(val pkg: String, val label: String, val component: ComponentName, val info: ActivityInfo)

/** Pergunta ao Android quais apps podem ser abertos e abre cada um. Não guarda cache: a lista é sempre a atual. */
class AppRepository(private val ctx: Context) {
    private val pm = ctx.packageManager
    private val collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"))

    /** Um item por pacote (preferindo a entrada de TV), em ordem alfabética, sem o próprio launcher. */
    fun launchable(): List<AppEntry> {
        val byPkg = LinkedHashMap<String, AppEntry>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val ai = ri.activityInfo
                if (ai.packageName == ctx.packageName || byPkg.containsKey(ai.packageName)) continue
                byPkg[ai.packageName] = AppEntry(
                    pkg = ai.packageName,
                    label = ri.loadLabel(pm).toString(),
                    component = ComponentName(ai.packageName, ai.name),
                    info = ai,
                )
            }
        }
        return byPkg.values.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun launch(entry: AppEntry): Boolean = start(
        Intent(Intent.ACTION_MAIN)
            .setComponent(entry.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
    )

    fun start(intent: Intent): Boolean = try {
        ctx.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    /** Ícone squircle; se o app só tiver um ícone pequeno, usa o banner de TV recortado. */
    fun iconBitmap(entry: AppEntry, size: Int): Bitmap {
        val icon = entry.info.loadIcon(pm)
        if (icon !is AdaptiveIconDrawable && icon.intrinsicWidth in 1 until size / 2) {
            entry.info.loadBanner(pm)?.let { return IconFactory.banner(it, size) }
        }
        return IconFactory.app(icon, size)
    }

    companion object {
        const val PLAY_STORE = "com.android.vending"
    }
}
