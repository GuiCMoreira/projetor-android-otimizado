package br.gui.tvavo.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import br.gui.tvavo.core.Guard
import br.gui.tvavo.core.HiddenApps
import br.gui.tvavo.core.IconFill
import br.gui.tvavo.core.IconStyle
import java.text.Collator
import java.util.Locale

data class AppEntry(val pkg: String, val label: String, val component: ComponentName, val info: ActivityInfo)

/** Apps que abrem pela tela inicial, e o ícone de cada um. Sem cache: a lista é sempre a atual. */
class Apps(private val ctx: Context) {
    private val pm = ctx.packageManager
    private val collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"))

    /** Um item por pacote (preferindo a entrada de TV), em ordem alfabética, sem a própria tela inicial nem telas internas do sistema. */
    fun launchable(): List<AppEntry> {
        val byPkg = LinkedHashMap<String, AppEntry>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val ai = ri.activityInfo
                if (ai.packageName == ctx.packageName || byPkg.containsKey(ai.packageName)) continue
                if (!HiddenApps.listed(ai.packageName)) continue
                // App de sistema só com entrada de celular ("Caixa de Mensagens", "copyDataToTV"...) é tela interna:
                // fica de fora. Apps instalados entram sempre, tenham ou não entrada de TV.
                if (category == Intent.CATEGORY_LAUNCHER && ai.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) continue
                // Um app com rótulo quebrado não pode derrubar a lista inteira: ele fica de fora.
                Guard.run({ null }) {
                    AppEntry(ai.packageName, ri.loadLabel(pm).toString(), ComponentName(ai.packageName, ai.name), ai)
                }?.let { byPkg[ai.packageName] = it }
            }
        }
        return byPkg.values.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun launch(entry: AppEntry): Boolean = try {
        ctx.startActivity(
            Intent(Intent.ACTION_MAIN).setComponent(entry.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        )
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    /** Ícone em quadrado de cantos arredondados; se o desenho do app falhar, um quadrado cinza. */
    fun icon(entry: AppEntry, size: Int): Bitmap = Guard.run({ blank(size) }) { draw(sharpIcon(entry), size, entry.pkg) }

    /**
     * A TV pede os ícones na densidade dela (baixa, 213), e alguns apps entregam uma imagem pequena que fica borrada
     * no botão grande (o Globoplay). Pede a versão xxhdpi, que basta para os 150 px do maior botão; xxxhdpi podia
     * ampliar um ícone grande de densidade menor para milhões de pixels numa TV de 940 MB. Sem ela, a de sempre.
     */
    private fun sharpIcon(entry: AppEntry): Drawable {
        val res = entry.info.iconResource
        if (res != 0) {
            Guard.run({ null }) {
                pm.getResourcesForApplication(entry.info.applicationInfo).getDrawableForDensity(res, DisplayMetrics.DENSITY_XXHIGH, null)
            }?.let { return it }
        }
        return entry.info.loadIcon(pm)
    }

    /**
     * Como no projetor: ícone adaptativo usa as próprias camadas (a da frente pode encolher, ver
     * [IconStyle.foregroundScale]); ícone comum opaco até a borda ocupa o quadrado inteiro, com os cantos na cor dele;
     * logo solto vai centralizado sobre o branco.
     */
    private fun draw(icon: Drawable, size: Int, pkg: String): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = size * CORNER
        c.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), r, r, Path.Direction.CW) })
        if (icon is AdaptiveIconDrawable) {
            // Camadas de 108 unidades das quais só as 72 do meio aparecem: desenha 1,5x maior, centralizado.
            val bleed = size / 4
            icon.background?.apply { setBounds(-bleed, -bleed, size + bleed, size + bleed); draw(c) }
            val half = (size / 2f + bleed) * IconStyle.foregroundScale(pkg)
            val mid = size / 2f
            icon.foreground?.apply { setBounds((mid - half).toInt(), (mid - half).toInt(), (mid + half).toInt(), (mid + half).toInt()); draw(c) }
            return bmp
        }
        when (val fill = IconStyle.classify(edgeColors(icon))) {
            is IconFill.Full -> {
                c.drawColor(fill.color)
                icon.setBounds(0, 0, size, size)
            }
            IconFill.Padded -> {
                c.drawColor(Color.WHITE)
                val pad = (size * 0.12f).toInt()
                icon.setBounds(pad, pad, size - pad, size - pad)
            }
        }
        icon.draw(c)
        return bmp
    }

    /** Cores da borda do ícone, desenhado pequeno só para a amostra. */
    private fun edgeColors(icon: Drawable): IntArray {
        val probe = Bitmap.createBitmap(PROBE, PROBE, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, PROBE, PROBE)
        icon.draw(Canvas(probe))
        val colors = IconStyle.edgeSamples(PROBE, PROBE).map { (x, y) -> probe.getPixel(x, y) }.toIntArray()
        probe.recycle()
        return colors
    }

    private fun blank(size: Int): Bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        eraseColor(0xFF3A4256.toInt())
    }

    private companion object {
        /** Mesmo arredondamento do protótipo (26 de raio num logo de 150). */
        const val CORNER = 0.17f
        const val PROBE = 64
    }
}
