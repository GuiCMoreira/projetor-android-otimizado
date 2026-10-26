package br.gui.inicio.settings

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import br.gui.inicio.core.DockConfig
import br.gui.inicio.core.Guard
import br.gui.inicio.data.AppEntry
import br.gui.inicio.data.AppRepository
import br.gui.inicio.data.Prefs
import br.gui.inicio.ui.Backdrop
import br.gui.inicio.ui.Glyph
import br.gui.inicio.ui.GlyphView
import br.gui.inicio.ui.Theme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Lista de apps para montar o Dock: OK coloca ou tira, ← → mudam a posição. Play Store é atalho fixo e não aparece. */
class DockEditorActivity : Activity() {
    private class Row(val pkg: String, val position: TextView, val mark: FrameLayout, val check: GlyphView)

    private lateinit var prefs: Prefs
    private lateinit var repo: AppRepository
    private lateinit var list: LinearLayout
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private val rows = mutableListOf<Row>()
    private var installed: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.init(this)
        prefs = Prefs(this)
        repo = AppRepository(this)
        val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
        val match = LinearLayout.LayoutParams.MATCH_PARENT
        val root = FrameLayout(this)
        Backdrop.attach(this, root, prefs, io)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(Theme.text(this, 40f, 700, "Organizar Dock"))
        column.addView(
            Theme.text(this, 20f, 400, "OK coloca ou tira do Dock. ← → mudam a posição. Até ${DockConfig.MAX} apps.").apply { alpha = 0.75f },
            LinearLayout.LayoutParams(wrap, wrap).apply {
                topMargin = Theme.si(10f)
                bottomMargin = Theme.si(28f)
            },
        )
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            setPadding(0, Theme.si(8f), 0, Theme.si(80f))
            addView(list)
        }
        column.addView(scroll, LinearLayout.LayoutParams(match, 0, 1f))
        root.addView(column, FrameLayout.LayoutParams(Theme.si(900f), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL).apply { topMargin = Theme.si(70f) })
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        io.execute {
            Guard.run({ Log.w("Inicio", "falha ao listar os apps", it) }) {
                val all = repo.launchable()
                val shown = all.filter { it.pkg != AppRepository.PLAY_STORE }
                val icons = shown.map { repo.iconBitmap(it, Theme.si(72f)) }
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    installed = all.mapTo(HashSet()) { it.pkg }
                    build(shown, icons)
                }
            }
        }
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    private fun build(apps: List<AppEntry>, icons: List<Bitmap>) {
        list.removeAllViews()
        rows.clear()
        apps.forEachIndexed { i, entry -> list.addView(row(entry, icons[i])) }
        refresh()
        list.getChildAt(0)?.requestFocus()
    }

    private fun row(entry: AppEntry, icon: Bitmap): View {
        val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
        val position = Theme.text(this, 22f, 600).apply {
            gravity = Gravity.END
            minWidth = Theme.si(40f)
        }
        val check = GlyphView(this, Glyph.CHECK)
        val mark = FrameLayout(this).apply { addView(check, FrameLayout.LayoutParams(Theme.si(26f), Theme.si(26f), Gravity.CENTER)) }
        rows += Row(entry.pkg, position, mark, check)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            setPadding(Theme.si(16f), Theme.si(10f), Theme.si(20f), Theme.si(10f))
            addView(ImageView(context).apply { setImageBitmap(icon) }, LinearLayout.LayoutParams(Theme.si(72f), Theme.si(72f)))
            addView(Theme.text(context, 24f, 500, entry.label), LinearLayout.LayoutParams(0, wrap, 1f).apply { leftMargin = Theme.si(20f) })
            addView(position, LinearLayout.LayoutParams(wrap, wrap).apply { rightMargin = Theme.si(18f) })
            addView(mark, LinearLayout.LayoutParams(Theme.si(40f), Theme.si(40f)))
            setOnFocusChangeListener { v, has -> v.background = if (has) Theme.rounded(Theme.FOCUS_FILL, 18f) else null }
            setOnClickListener { toggle(entry.pkg) }
            setOnKeyListener { _, code, ev ->
                if (ev.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                when (code) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { move(entry.pkg, -1); true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { move(entry.pkg, 1); true }
                    else -> false
                }
            }
        }
    }

    private fun toggle(pkg: String) {
        val dock = prefs.dock
        prefs.dock = when {
            dock.contains(pkg) -> dock.remove(pkg)
            dock.isFull(installed) -> {
                Toast.makeText(this, "Dock cheio: tire um app antes de colocar outro", Toast.LENGTH_SHORT).show()
                dock
            }
            else -> dock.add(pkg, installed)
        }
        refresh()
    }

    private fun move(pkg: String, delta: Int) {
        val dock = prefs.dock
        if (!dock.contains(pkg)) return
        prefs.dock = dock.move(pkg, delta)
        refresh()
    }

    private fun refresh() {
        val order = prefs.dock.visible(installed)
        rows.forEach { r ->
            val pos = order.indexOf(r.pkg)
            r.position.text = if (pos >= 0) "${pos + 1}" else ""
            r.mark.background = if (pos >= 0) {
                Theme.oval(Theme.SELECTION)
            } else {
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setStroke(Theme.si(2f), 0x80FFFFFF.toInt())
                }
            }
            r.check.visibility = if (pos >= 0) View.VISIBLE else View.INVISIBLE
        }
    }
}
