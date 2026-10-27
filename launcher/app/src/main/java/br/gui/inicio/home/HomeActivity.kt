package br.gui.inicio.home

import android.app.Activity
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.graphics.drawable.TransitionDrawable
import android.os.Handler
import android.os.Looper
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import br.gui.inicio.core.Options
import br.gui.inicio.core.City
import br.gui.inicio.core.Screensaver
import br.gui.inicio.core.WakeKey
import br.gui.inicio.core.WatchNextItem
import br.gui.inicio.core.WatchNextList
import br.gui.inicio.core.WeatherSchedule
import br.gui.inicio.data.WatchNextRepository
import br.gui.inicio.data.WeatherRepository
import br.gui.inicio.settings.CityActivity
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
import java.util.concurrent.TimeUnit

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
    /** Rede do clima em fila própria: DNS sem resposta não segura o Dock nem o papel de parede. */
    private val net: ExecutorService = Executors.newSingleThreadExecutor()
    /** Capas do "Continuar assistindo" em fila própria, para não esperar atrás do clima. */
    private val media: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var watchNext: WatchNextRepository
    private lateinit var watchRow: WatchNextRow
    private var loadedChoice: WallpaperChoice? = null
    private var dockLoaded = false
    private lateinit var weatherView: WeatherView
    private lateinit var weather: WeatherRepository
    private lateinit var screensaver: ScreensaverView
    private val main = Handler(Looper.getMainLooper())
    private val startScreensaver = Runnable { enterScreensaver() }
    private var weatherLoading = false
    private var watchLoading = false
    private var watchCards: List<WatchNextCard> = emptyList()
    private var postersLoading = false
    private val posterFailedAt = java.util.concurrent.ConcurrentHashMap<String, Long>()
    /** Espera cada capa com prazo; o download roda em [posterIo], onde uma conexão presa não segura as próximas. */
    private val posterWait: ExecutorService = Executors.newSingleThreadExecutor()
    private val posterIo: ExecutorService = Executors.newCachedThreadPool()
    private var weatherAttemptMs = 0L
    private var weatherAttemptCity: City? = null
    /** Telas abertas pelo menu "Início" devolvem o foco ao Dock na volta (e não à barra). */
    private var returnFocusToDock = false
    /** A tecla que tirou do descanso de tela é consumida inteira (descer e subir), para não abrir nada por engano. */
    private val wakeKey = WakeKey()

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
        root.addView(clock, FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.START).apply {
            topMargin = Theme.si(TOP_ROW)
            leftMargin = Theme.si(SIDE)
        })
        // Clima no canto superior direito, espelhando o relógio: mesmo topo e mesma margem lateral.
        weatherView = WeatherView(this)
        root.addView(weatherView, FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.END).apply {
            topMargin = Theme.si(TOP_ROW)
            rightMargin = Theme.si(SIDE)
        })
        watchRow = WatchNextRow(this)
        root.addView(watchRow, FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.START).apply {
            topMargin = Theme.si(400f)
            leftMargin = Theme.si(SIDE)
        })
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
        screensaver = ScreensaverView(this, loader, io)
        root.addView(screensaver, FrameLayout.LayoutParams(match, match))
        setContentView(root)
        weather = WeatherRepository(prefs)
        watchNext = WatchNextRepository(this, loader)

        status = StatusMonitor(this) { wifi, bt -> menuBar.setStatus(wifi, bt) }
        // Ordem vertical do foco: barra, "Continuar assistindo" (se houver), Dock.
        dock.onUp = { if (watchRow.hasItems) watchRow.focus() else menuBar.focusFirst() }
        dock.onFocusItem = ::showTooltip
        menuBar.onDown = { if (watchRow.hasItems) watchRow.focus() else dock.focusLast() }
        watchRow.onUp = { menuBar.focusFirst() }
        watchRow.onDown = { dock.focusLast() }
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
        showWeather()
        refreshWatchNext()
        askTvListingsOnce()
        scheduleScreensaver()
        if (returnFocusToDock) {
            returnFocusToDock = false
            if (dockLoaded) dock.focusLast()
        }
    }

    override fun onStop() {
        unregisterReceiver(timeReceiver)
        unregisterReceiver(packageReceiver)
        status.stop()
        main.removeCallbacks(startScreensaver)
        screensaver.stop()
        super.onStop()
    }

    override fun onDestroy() {
        io.shutdownNow()
        net.shutdownNow()
        media.shutdownNow()
        posterWait.shutdownNow()
        posterIo.shutdownNow()
        super.onDestroy()
    }

    /** Botão Início com a tela já aberta: fecha o painel e volta o foco ao Dock. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (screensaver.isRunning) screensaver.stop()
        scheduleScreensaver()
        panels.close()
        dock.focusFirst()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val down = event.action == KeyEvent.ACTION_DOWN
        when (wakeKey.handle(down, event.keyCode, event.repeatCount, screensaver.isRunning)) {
            WakeKey.Decision.WAKE -> {
                val episode = screensaver.episode
                screensaver.stop()
                scheduleScreensaver()
                // OK no descanso abre o episódio do widget; as outras teclas só voltam para a tela inicial.
                if (episode != null && Screensaver.opensEpisode(event.keyCode)) episode.onOpen()
                return true
            }
            WakeKey.Decision.CONSUME -> return true // repetições e o UP da tecla que acordou
            WakeKey.Decision.PASS -> if (screensaver.isRunning) return true
        }
        if (event.action == KeyEvent.ACTION_DOWN) scheduleScreensaver() // qualquer tecla reinicia a contagem
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_DOWN && panels.isOpen) panels.close()
            return true // na tela inicial, Voltar nunca sai
        }
        if (event.keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_DOWN) {
            if (!panels.isOpen) openStartMenu(menuBar.startItem, opener = currentFocus ?: menuBar.startItem)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun tick() {
        val now = LocalDateTime.now()
        clock.update(now)
        menuBar.setTime(ClockFormat.menuBar(now))
        screensaver.tick(now)
        status.refresh() // Bluetooth relido a cada minuto
        if (::weather.isInitialized) showWeather()
        // Troca automática: se a foto da vez mudou, carrega com esmaecimento.
        if (dockLoaded && loader.effective(prefs.wallpaper, prefs.rotateMinutes, System.currentTimeMillis()) != loadedChoice) loadWallpaper()
    }

    private fun loadWallpaper() {
        val choice = loader.effective(prefs.wallpaper, prefs.rotateMinutes, System.currentTimeMillis())
        if (choice == loadedChoice && GlassSource.blurred != null) return
        val m = resources.displayMetrics
        io.execute {
            Guard.run({ Log.w(TAG, "falha ao carregar o papel de parede", it) }) {
                val result = loader.load(choice, m.widthPixels, m.heightPixels)
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    val current = wallpaperView.drawable
                    // Só a camada de cima de um esmaecimento anterior: senão cada troca guarda a foto anterior inteira.
                    val old = (current as? TransitionDrawable)?.getDrawable(1) ?: current
                    if (old != null && result.full != null) {
                        // Esmaece do fundo anterior para o novo e, terminado, fica só a foto nova.
                        val next = BitmapDrawable(resources, result.full)
                        val fade = TransitionDrawable(arrayOf(old, next)).apply { isCrossFadeEnabled = true }
                        wallpaperView.setImageDrawable(fade)
                        fade.startTransition(FADE_MS)
                        main.postDelayed({ if (wallpaperView.drawable === fade) wallpaperView.setImageDrawable(next) }, FADE_MS + 100L)
                    } else {
                        wallpaperView.setImageBitmap(result.full)
                    }
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
                prefs.updateNewApps { NewAppsTracker.ensureInitialized(it, System.currentTimeMillis()) }
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
        prefs.updateNewApps { NewAppsTracker.markOpened(it, e.pkg, System.currentTimeMillis()) }
        if (!repo.launch(e)) Toast.makeText(this, "Não foi possível abrir ${e.label}", Toast.LENGTH_SHORT).show()
    }

    private fun showTooltip(anchor: View, item: DockItem) {
        tooltip.text = item.label
        tooltip.visibility = View.VISIBLE
        tooltip.post {
            // Posição final do ícone (layout do Dock), sem a animação em curso: o ícone em foco volta ao centro do seu lugar.
            val loc = IntArray(2)
            dock.getLocationOnScreen(loc)
            tooltip.x = loc[0] + anchor.left + anchor.width / 2f - tooltip.width / 2f
            val grownTop = loc[1] + anchor.top - anchor.height * (DockView.FOCUS_SCALE - 1f) - Theme.s(16f)
            tooltip.y = grownTop - tooltip.height - Theme.s(18f)
            tooltip.invalidate() // o vidro depende da posição na tela
        }
    }

    private fun locked(): List<ViewGroup> = listOf(menuBar, watchRow, dock)

    private fun onBarItem(item: BarItem, anchor: View) {
        when (item) {
            BarItem.START -> openStartMenu(anchor)
            BarItem.BLUETOOTH -> shortcuts.open(Shortcuts.BLUETOOTH)
            BarItem.WIFI -> shortcuts.open(Shortcuts.WIFI)
            BarItem.CONTROL -> openControlCenter(anchor)
            BarItem.CLOCK -> openCalendar(anchor)
        }
    }

    private fun openStartMenu(anchor: View, opener: View = anchor) {
        val saver = prefs.screensaverMinutes
        val entries = mutableListOf(
            MenuEntry("Papel de parede…") { openFromMenu(WallpaperActivity::class.java) },
            MenuEntry("Organizar Dock…") { openFromMenu(DockEditorActivity::class.java) },
            MenuEntry("Cidade do clima…") { openFromMenu(CityActivity::class.java) },
            MenuEntry("Descanso de tela: ${if (saver == 0) "desligado" else "$saver min"}") { cycleScreensaver() },
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
        panels.show(MenuPanel(this, null, entries) { panels.close() }, PanelHost.below(start = loc[0]), opener, locked())
    }

    private fun openFromMenu(screen: Class<out Activity>) {
        returnFocusToDock = true
        startActivity(Intent(this, screen))
    }

    private fun cycleScreensaver() {
        val next = Options.next(Screensaver.OPTIONS, prefs.screensaverMinutes)
        prefs.screensaverMinutes = next
        Toast.makeText(this, if (next == 0) "Descanso de tela desligado" else "Descanso de tela depois de $next min parado", Toast.LENGTH_SHORT).show()
        scheduleScreensaver()
    }

    private fun scheduleScreensaver() {
        main.removeCallbacks(startScreensaver)
        val minutes = prefs.screensaverMinutes
        if (minutes > 0) main.postDelayed(startScreensaver, minutes * 60_000L)
    }

    /** Só entra com a tela inicial à vista e sem painel aberto; senão tenta de novo depois. */
    private fun enterScreensaver() {
        if (panels.isOpen || !hasWindowFocus()) {
            scheduleScreensaver()
            return
        }
        val photos = loader.userFiles().map { WallpaperChoice.UserFile(it) }
        val slides = photos.ifEmpty { (0 until WallpaperChoice.GRADIENT_COUNT).map { WallpaperChoice.Gradient(it) } }
        screensaver.start(slides, SaverInfo(weather.cached(), prefs.weatherCity?.name, watchCards.firstOrNull()))
    }

    /** A lista "Ver a seguir" pede uma permissão que o Android confirma na tela; perguntamos uma única vez. */
    private fun askTvListingsOnce() {
        if (checkSelfPermission(TV_LISTINGS) == PackageManager.PERMISSION_GRANTED || prefs.askedTvListings) return
        prefs.askedTvListings = true
        requestPermissions(arrayOf(TV_LISTINGS), REQ_TV_LISTINGS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_TV_LISTINGS && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) refreshWatchNext()
    }

    /** Lê a lista "Ver a seguir" e as capas fora da tela; a fileira só aparece se houver algo. */
    private fun refreshWatchNext() {
        if (watchLoading) return
        watchLoading = true
        // Capa decodificada já no tamanho do cartão em foco (12% maior), para não ficar borrada ao crescer.
        val w = Theme.si(WatchNextRow.CARD_W * WatchNextRow.FOCUS_SCALE)
        val h = Theme.si(WatchNextRow.CARD_H * WatchNextRow.FOCUS_SCALE)
        val badgeSize = Theme.si(WatchNextRow.BADGE)
        media.execute {
            val cards = Guard.run({ Log.w(TAG, "falha ao ler o Continuar assistindo", it); emptyList() }) {
                val started = System.currentTimeMillis()
                val names = HashMap<String, String>()
                val badges = HashMap<String, Bitmap?>()
                val items = watchNext.items()
                watchNext.prunePosters(items, w, h)
                val missing = ArrayList<WatchNextItem>()
                items.map { item ->
                    val app = names.getOrPut(item.pkg) { watchNext.appName(item.pkg) }
                    val poster = watchNext.cachedPoster(item, w, h)
                    if (poster == null && item.poster != null) missing += item
                    WatchNextCard(
                        title = item.title,
                        meta = WatchNextList.meta(item, app),
                        progress = WatchNextList.progress(item),
                        poster = poster,
                        badge = badges.getOrPut(item.pkg) { watchNext.badge(item.pkg, badgeSize) },
                    ) { watchNext.open(item) }
                }.also {
                    Log.i(TAG, "Continuar assistindo: ${it.size} itens em ${System.currentTimeMillis() - started} ms, ${missing.size} capas a baixar")
                    runOnUiThread { downloadPosters(missing, w, h) }
                }
            }
            runOnUiThread {
                watchLoading = false
                if (!isDestroyed) {
                    watchCards = cards
                    watchRow.setItems(cards)
                }
            }
        }
    }

    /**
     * Fase 2 da fileira: baixa as capas que faltam, cada uma com prazo total de [POSTER_DEADLINE_MS] (o tempo-limite
     * da conexão não cobre DNS nem um app que não responde). Se alguma chegou, a fileira é relida, já com ela.
     * Capa que falhou só é tentada de novo depois de 10 min.
     */
    private fun downloadPosters(missing: List<WatchNextItem>, w: Int, h: Int) {
        val now = System.currentTimeMillis()
        val todo = missing.filter { (posterFailedAt[it.poster] ?: 0L) + POSTER_RETRY_MS < now }
        if (postersLoading || todo.isEmpty() || isDestroyed) return
        postersLoading = true
        posterWait.execute {
            var any = false
            for (item in todo) {
                val job = posterIo.submit<Boolean> { watchNext.downloadPoster(item, w, h) }
                val ok = Guard.run({ job.cancel(true); false }) { job.get(POSTER_DEADLINE_MS, TimeUnit.MILLISECONDS) }
                if (ok) any = true else item.poster?.let { posterFailedAt[it] = System.currentTimeMillis() }
            }
            runOnUiThread {
                postersLoading = false
                if (any && !isDestroyed) refreshWatchNext()
            }
        }
    }

    /** Mostra o clima guardado na hora; se estiver velho, busca de novo em segundo plano (no máximo a cada 10 min). */
    private fun showWeather() {
        val now = System.currentTimeMillis()
        weatherView.show(weather.cached(), prefs.weatherCity, prefs.weatherAt, now)
        val city = prefs.weatherCity
        val lastAttempt = if (city == weatherAttemptCity) weatherAttemptMs else 0L // cidade nova não espera
        if (weatherLoading || !WeatherSchedule.shouldFetch(now, prefs.weatherAt, lastAttempt)) return
        weatherLoading = true
        weatherAttemptMs = now
        weatherAttemptCity = city
        net.execute {
            Guard.run({ Log.w(TAG, "falha ao buscar o clima", it) }) { weather.refresh() }
            runOnUiThread {
                weatherLoading = false
                if (!isDestroyed) weatherView.show(weather.cached(), prefs.weatherCity, prefs.weatherAt, System.currentTimeMillis())
            }
        }
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
        private const val FADE_MS = 1000
        /** Topo do relógio e do clima, e a margem lateral comum a relógio, clima e fileira (px de uma tela 1920). */
        private const val TOP_ROW = 110f
        private const val SIDE = 110f
        private const val POSTER_DEADLINE_MS = 15_000L
        private const val POSTER_RETRY_MS = 10 * 60_000L
        private const val TV_LISTINGS = "android.permission.READ_TV_LISTINGS"
        private const val REQ_TV_LISTINGS = 12
        const val BLUE_TOP = 0xFF3A7BFD.toInt()
        const val BLUE_BOTTOM = 0xFF1F4FD8.toInt()
        const val GRAY_TOP = 0xFF9A9CA3.toInt()
        const val GRAY_BOTTOM = 0xFF5D5F66.toInt()
        const val LIGHT_TOP = 0xFFC7C9D1.toInt()
        const val LIGHT_BOTTOM = 0xFF8E9099.toInt()
    }
}
