package br.gui.inicio.home

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import br.gui.inicio.apps.AllAppsActivity
import br.gui.inicio.core.ClockFormat
import br.gui.inicio.core.Guard
import br.gui.inicio.core.NewAppsTracker
import br.gui.inicio.core.WallpaperChoice
import br.gui.inicio.data.AppEntry
import br.gui.inicio.data.AppRepository
import br.gui.inicio.data.Prefs
import br.gui.inicio.data.ShortcutResolver
import br.gui.inicio.data.Shortcuts
import br.gui.inicio.settings.DockEditorActivity
import br.gui.inicio.settings.WallpaperActivity
import br.gui.inicio.ui.CalendarPanel
import br.gui.inicio.ui.ControlCenterPanel
import br.gui.inicio.ui.GlassDrawable
import br.gui.inicio.ui.GlassSource
import br.gui.inicio.ui.Glyph
import br.gui.inicio.ui.IconFactory
import br.gui.inicio.ui.MenuEntry
import br.gui.inicio.ui.MenuPanel
import br.gui.inicio.ui.PanelHost
import br.gui.inicio.ui.Theme
import br.gui.inicio.ui.invalidateTree
import br.gui.inicio.wallpaper.WallpaperLoader
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Tela inicial estilo macOS. Nada roda com ela oculta: receptores ligados em onStart e desligados em onStop. */
class HomeActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var repo: AppRepository
    private lateinit var shortcuts: ShortcutResolver
    private lateinit var loader: WallpaperLoader
    private lateinit var root: FrameLayout
    private lateinit var wallpaperView: ImageView
    private lateinit var clock: ClockView
    private lateinit var menuBar: MenuBarView
    private lateinit var dock: DockView
    private lateinit var tooltip: TextView
    private lateinit var panels: PanelHost
    private lateinit var status: StatusMonitor
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private var loadedChoice: WallpaperChoice? = null
    private var dockLoaded = false

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = tick()
    }
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = refreshApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.init(this)
        prefs = Prefs(this)
        repo = AppRepository(this)
        shortcuts = ShortcutResolver(this)
        loader = WallpaperLoader(this)
        val match = FrameLayout.LayoutParams.MATCH_PARENT
        val wrap = FrameLayout.LayoutParams.WRAP_CONTENT

        root = FrameLayout(this).apply {
            clipChildren = false
            setBackgroundColor(Color.BLACK)
        }
        wallpaperView = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        root.addView(wallpaperView, FrameLayout.LayoutParams(match, match))
        clock = ClockView(this)
        root.addView(clock, FrameLayout.LayoutParams(match, wrap, Gravity.TOP).apply { topMargin = Theme.si(170f) })
        menuBar = MenuBarView(this, ::onBarItem)
        root.addView(menuBar, FrameLayout.LayoutParams(match, Theme.si(56f), Gravity.TOP))
        dock = DockView(this)
        root.addView(dock, FrameLayout.LayoutParams(wrap, wrap, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = Theme.si(46f) })
        tooltip = Theme.text(this, 22f, 500).apply {
            visibility = View.INVISIBLE
            setPadding(Theme.si(18f), Theme.si(8f), Theme.si(18f), Theme.si(8f))
        }
        tooltip.background = GlassDrawable(tooltip, Theme.s(14f))
        root.addView(tooltip, FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.START))
        panels = PanelHost(this)
        root.addView(panels, FrameLayout.LayoutParams(match, match))
        setContentView(root)

        status = StatusMonitor(this) { wifi, bt -> menuBar.setStatus(wifi, bt) }
        dock.onUp = { menuBar.focusFirst() }
        dock.onFocusItem = ::showTooltip
        menuBar.onDown = { dock.focusLast() }
        root.viewTreeObserver.addOnGlobalFocusChangeListener { _, now ->
            if (now == null || now.parent !== dock) tooltip.visibility = View.INVISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        val time = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        val packages = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(timeReceiver, time, Context.RECEIVER_NOT_EXPORTED)
        registerReceiver(packageReceiver, packages, Context.RECEIVER_NOT_EXPORTED)
        status.start()
        tick()
        loadWallpaper()
        refreshApps()
    }

    override fun onStop() {
        unregisterReceiver(timeReceiver)
        unregisterReceiver(packageReceiver)
        status.stop()
        super.onStop()
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    /** Botão Início com a tela já aberta: fecha o painel e volta o foco ao Dock. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        panels.close()
        dock.focusFirst()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_DOWN && panels.isOpen) panels.close()
            return true // na tela inicial, Voltar nunca sai
        }
        if (event.keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_DOWN) {
            if (!panels.isOpen) openStartMenu(menuBar.startItem)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun tick() {
        val now = LocalDateTime.now()
        clock.update(now)
        menuBar.setTime(ClockFormat.menuBar(now))
    }

    private fun loadWallpaper() {
        val choice = prefs.wallpaper
        if (choice == loadedChoice && GlassSource.blurred != null) return
        val m = resources.displayMetrics
        io.execute {
            Guard.run({ Log.w(TAG, "falha ao carregar o papel de parede", it) }) {
                val result = loader.load(choice, m.widthPixels, m.heightPixels)
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    wallpaperView.setImageBitmap(result.full)
                    GlassSource.blurred = result.blurred
                    loadedChoice = choice
                    root.invalidateTree()
                }
            }
        }
    }

    private fun refreshApps() {
        io.execute {
            // Se a montagem falhar, o Dock anterior continua na tela; a Home nunca cai.
            Guard.run({ Log.w(TAG, "falha ao montar o Dock", it) }) {
                val apps = repo.launchable()
                val installed = apps.mapTo(HashSet()) { it.pkg }
                prefs.newApps = NewAppsTracker.ensureInitialized(prefs.newApps, System.currentTimeMillis())
                val items = buildDock(apps, installed)
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    // Na primeira carga o foco vai para o Dock, mesmo que a barra tenha pego o foco antes.
                    val hadFocus = !dockLoaded || dock.hasFocusedItem() || currentFocus == null
                    dock.setItems(items)
                    dockLoaded = true
                    if (hadFocus && !panels.isOpen) dock.focusLast()
                }
            }
        }
    }

    private fun buildDock(apps: List<AppEntry>, installed: Set<String>): List<DockItem> {
        val size = Theme.si(112f)
        val byPkg = apps.associateBy { it.pkg }
        val items = mutableListOf<DockItem>()
        prefs.dock.visible(installed).mapNotNull { byPkg[it] }.forEach { e ->
            items += DockItem(e.pkg, e.label, repo.iconBitmap(e, size)) { openApp(e) }
        }
        val fixed = mutableListOf<DockItem>()
        byPkg[AppRepository.PLAY_STORE]?.let { e ->
            fixed += DockItem(e.pkg, e.label, repo.iconBitmap(e, size), groupStart = fixed.isEmpty()) { openApp(e) }
        }
        if (shortcuts.isAvailable(Shortcuts.SOURCE)) {
            fixed += DockItem("hdmi", "HDMI", IconFactory.glyph(Glyph.INPUT, BLUE_TOP, BLUE_BOTTOM, size), groupStart = fixed.isEmpty()) {
                shortcuts.open(Shortcuts.SOURCE)
            }
        }
        if (shortcuts.isAvailable(Shortcuts.SETTINGS)) {
            fixed += DockItem("settings", "Ajustes", IconFactory.glyph(Glyph.GEAR, GRAY_TOP, GRAY_BOTTOM, size), groupStart = fixed.isEmpty()) {
                shortcuts.open(Shortcuts.SETTINGS)
            }
        }
        items += fixed
        items += DockItem("all", "Todos os apps", IconFactory.glyph(Glyph.GRID, LIGHT_TOP, LIGHT_BOTTOM, size), groupStart = true) {
            startActivity(Intent(this, AllAppsActivity::class.java))
        }
        return items
    }

    private fun openApp(e: AppEntry) {
        prefs.newApps = NewAppsTracker.markOpened(prefs.newApps, e.pkg, System.currentTimeMillis())
        if (!repo.launch(e)) Toast.makeText(this, "Não foi possível abrir ${e.label}", Toast.LENGTH_SHORT).show()
    }

    private fun showTooltip(anchor: View, item: DockItem) {
        tooltip.text = item.label
        tooltip.visibility = View.VISIBLE
        tooltip.post {
            val loc = IntArray(2)
            anchor.getLocationOnScreen(loc)
            tooltip.x = loc[0] + anchor.width / 2f - tooltip.width / 2f
            tooltip.y = loc[1] - anchor.height * 0.35f - Theme.s(16f) - tooltip.height - Theme.s(18f)
            tooltip.invalidate() // o vidro depende da posição na tela
        }
    }

    private fun locked(): List<ViewGroup> = listOf(menuBar, dock)

    private fun onBarItem(item: BarItem, anchor: View) {
        when (item) {
            BarItem.START -> openStartMenu(anchor)
            BarItem.BLUETOOTH -> shortcuts.open(Shortcuts.BLUETOOTH)
            BarItem.WIFI -> shortcuts.open(Shortcuts.WIFI)
            BarItem.CONTROL -> openControlCenter(anchor)
            BarItem.CLOCK -> openCalendar(anchor)
        }
    }

    private fun openStartMenu(anchor: View) {
        val entries = mutableListOf(
            MenuEntry("Papel de parede…") { startActivity(Intent(this, WallpaperActivity::class.java)) },
            MenuEntry("Organizar Dock…") { startActivity(Intent(this, DockEditorActivity::class.java)) },
        )
        val firstOfGroup = entries.size
        if (shortcuts.isAvailable(Shortcuts.SETTINGS)) {
            entries += MenuEntry("Ajustes do projetor", separatorBefore = entries.size == firstOfGroup) { shortcuts.open(Shortcuts.SETTINGS) }
        }
        if (shortcuts.isAvailable(Shortcuts.SOURCE)) {
            entries += MenuEntry("Fontes de entrada (HDMI)", separatorBefore = entries.size == firstOfGroup) { shortcuts.open(Shortcuts.SOURCE) }
        }
        if (shortcuts.isAvailable(Shortcuts.ORIGINAL_LAUNCHER)) {
            entries += MenuEntry("Launcher original do fabricante", separatorBefore = true) { shortcuts.open(Shortcuts.ORIGINAL_LAUNCHER) }
        }
        val loc = IntArray(2).also { anchor.getLocationOnScreen(it) }
        panels.show(MenuPanel(this, null, entries) { panels.close() }, PanelHost.below(start = loc[0]), anchor, locked())
    }

    private fun openControlCenter(anchor: View) {
        val loc = IntArray(2).also { anchor.getLocationOnScreen(it) }
        val end = resources.displayMetrics.widthPixels - (loc[0] + anchor.width)
        val panel = ControlCenterPanel(this, shortcuts.available(Shortcuts.CONTROL_CENTER)) { s ->
            panels.close()
            shortcuts.open(s)
        }
        panels.show(panel, PanelHost.below(end = end), anchor, locked())
    }

    private fun openCalendar(anchor: View) {
        val panel = CalendarPanel(this, LocalDate.now()) {
            panels.close()
            shortcuts.open(Shortcuts.DATE)
        }
        panels.show(panel, PanelHost.below(end = Theme.si(16f)), anchor, locked())
    }

    private companion object {
        const val TAG = "Inicio"
        const val BLUE_TOP = 0xFF3A7BFD.toInt()
        const val BLUE_BOTTOM = 0xFF1F4FD8.toInt()
        const val GRAY_TOP = 0xFF9A9CA3.toInt()
        const val GRAY_BOTTOM = 0xFF5D5F66.toInt()
        const val LIGHT_TOP = 0xFFC7C9D1.toInt()
        const val LIGHT_BOTTOM = 0xFF8E9099.toInt()
    }
}
