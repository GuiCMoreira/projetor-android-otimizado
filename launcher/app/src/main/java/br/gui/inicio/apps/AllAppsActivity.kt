package br.gui.inicio.apps

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import br.gui.inicio.core.Guard
import br.gui.inicio.core.NewAppsTracker
import br.gui.inicio.data.AppEntry
import br.gui.inicio.data.AppRepository
import br.gui.inicio.data.Prefs
import br.gui.inicio.ui.Backdrop
import br.gui.inicio.ui.MenuEntry
import br.gui.inicio.ui.MenuPanel
import br.gui.inicio.ui.PanelHost
import br.gui.inicio.ui.Theme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** "Todos os apps", estilo Launchpad: grade 6 x 3 por página, ponto azul em app novo, menu com Menu ou OK segurado. */
class AllAppsActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var repo: AppRepository
    private lateinit var panels: PanelHost
    private lateinit var grid: GridLayout
    private lateinit var dots: LinearLayout
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private var apps: List<AppEntry> = emptyList()
    private var page = 0
    private val pageCount: Int get() = maxOf(1, (apps.size + PAGE - 1) / PAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.init(this)
        prefs = Prefs(this)
        repo = AppRepository(this)
        val wrap = FrameLayout.LayoutParams.WRAP_CONTENT
        val match = FrameLayout.LayoutParams.MATCH_PARENT
        val root = FrameLayout(this).apply { clipChildren = false }
        Backdrop.attach(this, root, prefs, io)
        root.addView(
            Theme.text(this, 28f, 600, "Todos os apps").apply { alpha = 0.9f },
            FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = Theme.si(60f) },
        )
        grid = GridLayout(this).apply {
            columnCount = COLS
            clipChildren = false
            clipToPadding = false
        }
        root.addView(grid, FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = Theme.si(160f) })
        dots = LinearLayout(this).apply { gravity = Gravity.CENTER }
        root.addView(dots, FrameLayout.LayoutParams(wrap, wrap, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = Theme.si(56f) })
        panels = PanelHost(this)
        root.addView(panels, FrameLayout.LayoutParams(match, match))
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        io.execute {
            Guard.run({ Log.w(TAG, "falha ao listar os apps", it) }) {
                val list = repo.launchable()
                prefs.newApps = NewAppsTracker.ensureInitialized(prefs.newApps, System.currentTimeMillis())
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    apps = list
                    showPage(page.coerceIn(0, pageCount - 1), focusIndex = 0)
                }
            }
        }
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    private fun showPage(p: Int, focusIndex: Int) {
        page = p
        val slice = apps.drop(p * PAGE).take(PAGE)
        val size = Theme.si(144f)
        io.execute {
            Guard.run({ Log.w(TAG, "falha ao montar a página", it) }) {
                val icons = slice.map { repo.iconBitmap(it, size) }
                runOnUiThread {
                    if (isDestroyed || page != p) return@runOnUiThread
                    val state = prefs.newApps
                    grid.removeAllViews()
                    slice.forEachIndexed { i, entry -> grid.addView(cell(entry, icons[i], NewAppsTracker.isNew(state, entry.pkg, entry.installedAt))) }
                    renderDots()
                    if (slice.isNotEmpty()) grid.getChildAt(focusIndex.coerceIn(0, slice.lastIndex)).requestFocus()
                }
            }
        }
    }

    private fun cell(entry: AppEntry, icon: Bitmap, isNew: Boolean): View {
        val size = Theme.si(144f)
        val ring = GradientDrawable().apply {
            cornerRadius = size * 0.225f
            setStroke(Theme.si(4f), Color.WHITE)
        }
        val iconView = ImageView(this).apply { setImageBitmap(icon) }
        val frame = FrameLayout(this).apply {
            clipChildren = false
            addView(iconView, FrameLayout.LayoutParams(size, size))
        }
        if (isNew) {
            val dot = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Theme.SELECTION)
                    setStroke(Theme.si(3f), Color.WHITE)
                }
            }
            frame.addView(dot, FrameLayout.LayoutParams(Theme.si(26f), Theme.si(26f), Gravity.TOP or Gravity.END).apply {
                topMargin = -Theme.si(6f)
                rightMargin = -Theme.si(6f)
            })
        }
        val label = Theme.text(this, 20f, 500, entry.label).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = Theme.si(200f)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isFocusable = true
            clipChildren = false
            addView(frame, LinearLayout.LayoutParams(size, size))
            addView(label, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = Theme.si(14f) })
            layoutParams = GridLayout.LayoutParams().apply {
                width = Theme.si(220f)
                setMargins(0, 0, 0, Theme.si(44f))
            }
            setOnFocusChangeListener { _, has ->
                frame.animate().scaleX(if (has) 1.18f else 1f).scaleY(if (has) 1.18f else 1f).setDuration(150).start()
                iconView.foreground = if (has) ring else null
            }
            setOnClickListener { open(entry) }
            setOnLongClickListener {
                showMenu(entry, this)
                true
            }
        }
    }

    private fun renderDots() {
        dots.removeAllViews()
        if (pageCount < 2) return
        repeat(pageCount) { i ->
            dots.addView(View(this).apply { background = Theme.oval(if (i == page) Color.WHITE else 0x59FFFFFF) },
                LinearLayout.LayoutParams(Theme.si(14f), Theme.si(14f)).apply { setMargins(Theme.si(8f), 0, Theme.si(8f), 0) })
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (panels.isOpen) {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    panels.close()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
            val focused = grid.focusedChild
            if (focused != null) {
                val idx = grid.indexOfChild(focused)
                val col = idx % COLS
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> if (col == 0 && page > 0) {
                        showPage(page - 1, idx + COLS - 1)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> if ((col == COLS - 1 || idx == grid.childCount - 1) && page < pageCount - 1) {
                        showPage(page + 1, idx - col)
                        return true
                    }
                    KeyEvent.KEYCODE_MENU -> {
                        apps.getOrNull(page * PAGE + idx)?.let { showMenu(it, focused) }
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showMenu(entry: AppEntry, anchor: View) {
        val installed = apps.mapTo(HashSet()) { it.pkg }
        val dock = prefs.dock
        val dockEntry = when {
            entry.pkg in dock.visible(installed) -> MenuEntry("Remover do Dock") { prefs.dock = dock.remove(entry.pkg) }
            dock.isFull(installed) -> MenuEntry("Dock cheio", enabled = false)
            else -> MenuEntry("Adicionar ao Dock") { prefs.dock = dock.add(entry.pkg, installed) }
        }
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", entry.pkg, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val menu = MenuPanel(this, entry.label, listOf(
            dockEntry,
            MenuEntry("Abrir") { open(entry) },
            MenuEntry("Informações do app") { repo.start(details) },
        )) { panels.close() }
        panels.show(menu, PanelHost.centered(), anchor, listOf(grid))
    }

    private fun open(entry: AppEntry) {
        prefs.newApps = NewAppsTracker.markOpened(prefs.newApps, entry.pkg, System.currentTimeMillis())
        repo.launch(entry)
    }

    companion object {
        private const val TAG = "Inicio"
        const val COLS = 6
        const val ROWS = 3
        const val PAGE = COLS * ROWS
    }
}
