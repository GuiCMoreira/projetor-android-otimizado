package br.gui.tvavo.apps

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import br.gui.tvavo.core.Guard
import br.gui.tvavo.core.HomeGrid
import br.gui.tvavo.core.Pins
import br.gui.tvavo.data.AppEntry
import br.gui.tvavo.data.Apps
import br.gui.tvavo.data.Prefs
import br.gui.tvavo.ui.Theme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Todos os apps em botões grandes, 4 por fileira. OK abre o app; segurar OK fixa ou tira da tela inicial (até
 * [HomeGrid.MAX]). É por aqui que o neto escolhe o que a avó vê na tela inicial.
 */
class AllAppsActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var apps: Apps
    private lateinit var grid: GridLayout
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private val badges = HashMap<String, TextView>()
    private val iconViews = HashMap<String, ImageView>()
    private var installed: Set<String> = emptySet()
    /** Quando a tela abriu: um OK repetido logo em seguida é do botão que abriu esta tela, não um pedido novo. */
    private var openedAt = 0L
    /** OK segurado já fixou ou tirou um app: o resto das repetições e o soltar não fazem mais nada (nem abrem o app). */
    private var holdHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openedAt = SystemClock.uptimeMillis()
        Theme.init(this)
        prefs = Prefs(this)
        apps = Apps(this)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Theme.si(SIDE), Theme.si(44f), Theme.si(SIDE), Theme.si(40f))
            clipChildren = false
            clipToPadding = false
        }
        column.addView(Theme.text(this, 40f, 600, value = "Todos os apps"))
        column.addView(Theme.text(this, 22f, 500, Theme.MUTED, "OK abre o app. Segure OK para fixar ou tirar da tela inicial (até ${HomeGrid.MAX})."),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = Theme.si(10f)
                bottomMargin = Theme.si(30f)
            })
        grid = GridLayout(this).apply {
            columnCount = COLS
            clipChildren = false
            clipToPadding = false
        }
        column.addView(grid)
        setContentView(ScrollView(this).apply {
            background = Theme.background()
            isVerticalScrollBarEnabled = false
            clipChildren = false
            addView(column)
        })
        load()
    }

    /**
     * O primeiro item já vem selecionado (em ordem alfabética, as Configurações): um segundo OK sem querer, dado no
     * "Todos os apps" do Início, abria as Configurações da TV. Por [DOUBLE_OK_MS] depois de abrir, o OK é ignorado.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val ok = event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER
        if (ok && SystemClock.uptimeMillis() - openedAt < DOUBLE_OK_MS) return true
        // OK segurado, reconhecido pela repetição da tecla (todo controle repete quem fica apertado). Mais firme que o
        // clique longo do Android, que depende de o soltar chegar depois do tempo certo.
        if (ok && event.action == KeyEvent.ACTION_DOWN && event.repeatCount > 0) {
            if (!holdHandled) {
                holdHandled = true
                (currentFocus?.tag as? AppEntry)?.let { togglePin(it) }
            }
            return true
        }
        if (ok && event.action == KeyEvent.ACTION_UP && holdHandled) {
            holdHandled = false
            currentFocus?.isPressed = false
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    private fun load() {
        io.execute {
            Guard.run({ Log.w("TvAvo", "falha ao listar os apps", it) }) {
                // Os botões aparecem logo, só com o nome; os ícones chegam depois, em blocos. Numa TV lenta, esperar
                // todos os ícones deixava a tela vazia por segundos, e ela achava que tinha travado.
                val list = apps.launchable()
                runOnUiThread { if (!isDestroyed) show(list) }
                list.chunked(ICON_BATCH).forEach { batch ->
                    val icons = batch.map { it.pkg to apps.icon(it, Theme.si(ICON)) }
                    runOnUiThread { if (!isDestroyed) icons.forEach { (pkg, bmp) -> iconViews[pkg]?.setImageBitmap(bmp) } }
                }
            }
        }
    }

    private fun show(list: List<AppEntry>) {
        grid.removeAllViews()
        badges.clear()
        iconViews.clear()
        installed = list.mapTo(HashSet()) { it.pkg }
        val pins = prefs.pins
        val w = Theme.si((1280f - 2 * SIDE - GAP * (COLS - 1)) / COLS)
        list.forEachIndexed { i, e ->
            val item = FrameLayout(this).apply {
                background = Theme.rounded(Theme.TILE, 28f)
                isFocusable = true
                isClickable = true
                tag = e
                contentDescription = e.label
                setOnClickListener { if (!apps.launch(e)) toast("Não foi possível abrir ${e.label}") }
                onFocusChangeListener = View.OnFocusChangeListener { v, has ->
                    v.animate().scaleX(if (has) 1.05f else 1f).scaleY(if (has) 1.05f else 1f).setDuration(140).start()
                    v.foreground = if (has) Theme.ring(28f, 6f) else null
                    v.elevation = if (has) Theme.s(16f) else 0f
                }
            }
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            val icon = ImageView(this).apply { background = Theme.rounded(0xFF3A4256.toInt(), ICON * 0.17f) }
            iconViews[e.pkg] = icon
            content.addView(icon, LinearLayout.LayoutParams(Theme.si(ICON), Theme.si(ICON)))
            content.addView(Theme.text(this, 26f, 600, value = e.label).apply {
                maxLines = 2
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(Theme.si(12f), 0, Theme.si(12f), 0)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = Theme.si(14f)
            })
            item.addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            val badge = Theme.text(this, 18f, 600, value = "Na tela inicial").apply {
                background = Theme.rounded(0xFF3478F6.toInt(), 12f)
                setPadding(Theme.si(12f), Theme.si(5f), Theme.si(12f), Theme.si(5f))
                visibility = if (e.pkg in pins) View.VISIBLE else View.GONE
            }
            item.addView(badge, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
                topMargin = Theme.si(12f)
                rightMargin = Theme.si(12f)
            })
            badges[e.pkg] = badge
            grid.addView(item, GridLayout.LayoutParams().apply {
                width = w
                height = Theme.si(TILE_H)
                rightMargin = if (i % COLS == COLS - 1) 0 else Theme.si(GAP)
                bottomMargin = Theme.si(GAP)
            })
        }
        grid.getChildAt(0)?.requestFocus()
    }

    private fun togglePin(e: AppEntry) {
        val result = Pins.toggle(prefs.pins, e.pkg, installed)
        prefs.pins = result.pins
        badges[e.pkg]?.visibility = if (e.pkg in result.pins) View.VISIBLE else View.GONE
        toast(
            when (result.change) {
                Pins.Change.ADDED -> "${e.label} está na tela inicial"
                Pins.Change.REMOVED -> "${e.label} saiu da tela inicial"
                Pins.Change.FULL -> "A tela inicial já tem ${HomeGrid.MAX} apps. Tire um antes."
            },
        )
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    private companion object {
        const val COLS = 4
        const val SIDE = 80f
        const val GAP = 30f
        const val ICON = 110f
        const val TILE_H = 220f
        const val ICON_BATCH = 4
        const val DOUBLE_OK_MS = 700L
    }
}
