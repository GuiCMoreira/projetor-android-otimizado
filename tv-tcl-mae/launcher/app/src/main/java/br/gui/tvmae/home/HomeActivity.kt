package br.gui.tvmae.home

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.database.ContentObserver
import android.media.tv.TvContract
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import br.gui.tvmae.R
import br.gui.tvmae.apps.AllAppsActivity
import br.gui.tvmae.core.ClockFormat
import br.gui.tvmae.core.Dir
import br.gui.tvmae.core.Greeting
import br.gui.tvmae.core.Guard
import br.gui.tvmae.core.HomeNav
import br.gui.tvmae.core.Pins
import br.gui.tvmae.core.WatchNextItem
import br.gui.tvmae.core.WatchNextList
import br.gui.tvmae.core.WeatherCodes
import br.gui.tvmae.core.WeatherPolicy
import br.gui.tvmae.data.AppEntry
import br.gui.tvmae.data.Apps
import br.gui.tvmae.data.Prefs
import br.gui.tvmae.data.Shortcut
import br.gui.tvmae.data.Shortcuts
import br.gui.tvmae.data.Wallpapers
import br.gui.tvmae.data.WatchNextRepository
import br.gui.tvmae.data.WeatherRepository
import br.gui.tvmae.rescue.CrashLog
import br.gui.tvmae.rescue.RescueActivity
import br.gui.tvmae.ui.GlyphView
import br.gui.tvmae.ui.GridDots
import br.gui.tvmae.ui.Theme
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Tela inicial da TV da mãe (opção B do protótipo): foto de fundo que troca a cada hora, saudação, relógio grande,
 * data e clima; atalhos no canto; Continuar assistindo no meio; faixa de apps embaixo. Nada roda com ela escondida.
 */
class HomeActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var apps: Apps
    private lateinit var shortcuts: Shortcuts
    private lateinit var wallpapers: Wallpapers
    private lateinit var watchNext: WatchNextRepository
    private lateinit var weather: WeatherRepository
    private lateinit var root: FrameLayout
    private lateinit var wallpaperView: ImageView
    private lateinit var greeting: TextView
    private lateinit var clock: TextView
    private lateinit var dateWeather: TextView
    private lateinit var shortcutLabel: TextView
    private lateinit var watchLabel: TextView
    private lateinit var watchBox: FrameLayout
    private lateinit var appsBox: FrameLayout
    private lateinit var appLabel: TextView
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    /** Rede do clima em fila própria: internet sem resposta não segura o resto. */
    private val net: ExecutorService = Executors.newSingleThreadExecutor()
    /** Continuar assistindo e as capas em fila própria. */
    private val media: ExecutorService = Executors.newSingleThreadExecutor()
    private val nav = HomeNav()
    private var pos = HomeNav.Pos(HomeNav.APPS, 0)
    private var shortcutViews: List<View> = emptyList()
    private var cardViews: List<View> = emptyList()
    private var appViews: List<View> = emptyList()
    private var appNames: List<String> = emptyList()
    private var shownSignature: List<String>? = null
    private var shownWatch: List<String>? = null
    private var loadedWallpaper: File? = null
    private var weatherLoading = false
    private var weatherAttemptMs = 0L
    private var watchLoading = false
    /** Mudança na lista chegou durante uma leitura: lê de novo ao terminar, senão ela se perderia. */
    private var watchPending = false
    /** Entre onStart e onStop: com a tela à vista, o aviso de pouca memória não solta nada. */
    private var visible = false
    private var firstFocus = true
    /**
     * Só na primeira vez que a tela abre: se o Continuar assistindo chegar depois dos apps, o foco vai para o primeiro
     * cartão. Qualquer tecla (inclusive OK) cancela; ao voltar de um app, o foco fica onde o Android o devolveu.
     */
    private var jumpToWatch = true
    /** Capas já tentadas desde que a tela apareceu: cada uma é baixada no máximo uma vez por volta (sem laço). */
    private val triedPosters = HashSet<String>()
    /** Pacote, título e linha de cada cartão na tela: se só a capa mudou, troca a imagem sem remontar a fileira. */
    private var shownWatchText: List<String>? = null

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = tick()
    }
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = refreshApps()
    }
    private val handler = Handler(Looper.getMainLooper())
    private val watchChanged = Runnable { if (visible) refreshWatchNext() }
    /**
     * O app pode gravar na lista segundos depois de ela voltar para a tela inicial; sem isto, a mudança só apareceria
     * na próxima volta. Espera a lista assentar (os apps gravam vários itens seguidos) e lê uma vez.
     */
    private val watchObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            handler.removeCallbacks(watchChanged)
            handler.postDelayed(watchChanged, WATCH_SETTLE_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Caiu 3 vezes em 5 minutos: não tenta de novo, abre a tela de emergência (que não usa nada daqui).
        if (CrashLog.shouldRescue(this)) {
            val rescue = if (CrashLog.rescueFailed(this)) Intent(android.provider.Settings.ACTION_SETTINGS) else Intent(this, RescueActivity::class.java)
            startActivity(rescue.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
            return
        }
        CrashLog.homeOpened(this)
        Theme.init(this)
        prefs = Prefs(this)
        apps = Apps(this)
        shortcuts = Shortcuts(this)
        wallpapers = Wallpapers(this)
        watchNext = WatchNextRepository(this, apps)
        weather = WeatherRepository(prefs)
        val match = FrameLayout.LayoutParams.MATCH_PARENT

        root = FrameLayout(this).apply {
            background = Theme.background()
            clipChildren = false
        }
        wallpaperView = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        root.addView(wallpaperView, FrameLayout.LayoutParams(match, match))
        // Véu: a foto aparece bem em cima, atrás do relógio, e escurece embaixo, onde ficam os cartões e os apps.
        root.addView(View(this).apply {
            background = GradientDrawable().apply {
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
                setColors(intArrayOf(0x26000000, 0x59000000, 0xD9000000.toInt(), 0xD9000000.toInt()), floatArrayOf(0f, 0.35f, 0.72f, 1f))
            }
        }, FrameLayout.LayoutParams(match, match))

        greeting = Theme.text(this, 22f, 600, 0xFFEEF1F6.toInt())
        root.addView(greeting, at(68f, 30f))
        clock = Theme.text(this, 96f, 600).apply { letterSpacing = -0.03f }
        root.addView(clock, at(64f, 62f))
        dateWeather = Theme.text(this, 20f, 500, 0xFFDFE4EE.toInt())
        root.addView(dateWeather, at(68f, 166f))

        shortcutViews = Shortcut.values().mapIndexed { i, s ->
            shortcutButton(s).also { root.addView(it, box(SHORTCUT_RIGHT - (Shortcut.values().size - i) * SHORTCUT_STEP, 46f, SHORTCUT, SHORTCUT)) }
        }
        shortcutLabel = Theme.text(this, 15f, 500, 0xFFEEF1F6.toInt()).apply { visibility = View.INVISIBLE }
        root.addView(shortcutLabel, at(0f, 108f))

        watchLabel = Theme.text(this, 15f, 600, 0xFFE7EAF1.toInt(), "Continuar assistindo").apply { visibility = View.GONE }
        root.addView(watchLabel, at(64f, 278f))
        watchBox = FrameLayout(this).apply { clipChildren = false }
        root.addView(watchBox, FrameLayout.LayoutParams(match, match))
        appsBox = FrameLayout(this).apply { clipChildren = false }
        root.addView(appsBox, FrameLayout.LayoutParams(match, match))
        appLabel = Theme.text(this, 16f, 600).apply { visibility = View.INVISIBLE }
        root.addView(appLabel, at(0f, 676f))
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        visible = true
        triedPosters.clear()
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
        Guard.run({ Log.w(TAG, "sem aviso de mudança no Continuar assistindo", it) }) {
            contentResolver.registerContentObserver(TvContract.WatchNextPrograms.CONTENT_URI, true, watchObserver)
        }
        tick()
        refreshApps()
        refreshWatchNext()
    }

    override fun onStop() {
        visible = false
        unregisterReceiver(timeReceiver)
        unregisterReceiver(packageReceiver)
        contentResolver.unregisterContentObserver(watchObserver)
        handler.removeCallbacks(watchChanged)
        super.onStop()
    }

    override fun onDestroy() {
        io.shutdownNow()
        net.shutdownNow()
        media.shutdownNow()
        super.onDestroy()
    }

    /** Pouca memória com a tela escondida: solta a foto e as capas (recarregadas no onStart), como no projetor. */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (!::root.isInitialized || visible) return
        if (level < TRIM_MEMORY_BACKGROUND && level != TRIM_MEMORY_RUNNING_LOW && level != TRIM_MEMORY_RUNNING_CRITICAL) return
        wallpaperView.setImageDrawable(null)
        loadedWallpaper = null
        shownWatch = null
        cardViews.forEach { (it.getTag(R.id.poster) as? ImageView)?.setImageDrawable(null) }
        Log.i(TAG, "pouca memória (nível $level): foto e capas liberadas")
    }

    /** Botão Início com a tela já aberta: volta para o começo. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        focus(HomeNav.start(sizes()))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val dir = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> Dir.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> Dir.RIGHT
            KeyEvent.KEYCODE_DPAD_UP -> Dir.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> Dir.DOWN
            KeyEvent.KEYCODE_BACK -> return true // na tela inicial, Voltar nunca sai
            else -> {
                if (event.action == KeyEvent.ACTION_DOWN) jumpToWatch = false
                return super.dispatchKeyEvent(event)
            }
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            jumpToWatch = false
            // Sem nada em foco (não deveria acontecer), qualquer seta devolve o foco.
            if (currentFocus == null) {
                focus(HomeNav.start(sizes()))
                return true
            }
            focus(nav.move(pos, dir, sizes()))
        }
        return true
    }

    private fun sizes() = intArrayOf(shortcutViews.size, cardViews.size, appViews.size)

    private fun focus(p: HomeNav.Pos) {
        val row = when (p.row) {
            HomeNav.SHORTCUTS -> shortcutViews
            HomeNav.WATCH -> cardViews
            else -> appViews
        }
        row.getOrNull(p.col)?.requestFocus()
    }

    private fun tick() {
        val now = LocalDateTime.now()
        clock.text = ClockFormat.time(now)
        greeting.text = Greeting.at(now.hour, getString(R.string.greeting_name))
        showWeather(now)
        loadWallpaper()
    }

    private fun showWeather(now: LocalDateTime) {
        val date = ClockFormat.longDate(now)
        val nowMs = System.currentTimeMillis()
        // Clima guardado só vale por algumas horas; mais velho que isso, fica só a data.
        val cached = if (WeatherPolicy.showCached(nowMs, prefs.weatherAt)) weather.cached() else null
        dateWeather.text = date + (cached?.let { " · " + weatherLine(it.temp, it.code, it.isDay) } ?: "")
        if (weatherLoading || !WeatherPolicy.shouldFetch(nowMs, prefs.weatherAt, weatherAttemptMs)) return
        weatherLoading = true
        weatherAttemptMs = nowMs
        net.execute {
            val fresh = Guard.run({ null }) { weather.refresh() }
            runOnUiThread {
                weatherLoading = false
                if (!isDestroyed && fresh != null) dateWeather.text = "$date · " + weatherLine(fresh.temp, fresh.code, fresh.isDay)
            }
        }
    }

    private fun weatherLine(temp: Int, code: Int, isDay: Boolean): String =
        "$temp° " + WeatherCodes.describe(code, isDay).replaceFirstChar { it.lowercase() }

    /** Foto da hora; só decodifica quando a hora muda de foto (ou depois de ser solta por falta de memória). */
    private fun loadWallpaper() {
        val m = resources.displayMetrics
        io.execute {
            Guard.run({ Log.w(TAG, "falha ao carregar a foto", it) }) {
                val file = wallpapers.current(System.currentTimeMillis())
                if (file == null || file == loadedWallpaper) return@run
                val bmp = wallpapers.decode(file, m.widthPixels, m.heightPixels) ?: return@run
                runOnUiThread {
                    // Dois pedidos juntos (onStart e o minuto virando) decodificam duas vezes; só o primeiro é aplicado.
                    if (isDestroyed || !visible || file == loadedWallpaper) return@runOnUiThread
                    wallpaperView.setImageBitmap(bmp)
                    loadedWallpaper = file
                }
            }
        }
    }

    private fun refreshApps() {
        io.execute {
            Guard.run({ Log.w(TAG, "falha ao montar a faixa de apps", it) }) {
                val all = apps.launchable()
                val byPkg = all.associateBy { it.pkg }
                val pins = Pins.visible(prefs.pins, byPkg.keys).mapNotNull { byPkg[it] }
                // O caminho do APK muda a cada atualização: um ícone novo com o mesmo nome também redesenha a faixa.
                val signature = pins.map { "${it.pkg}|${it.label}|${it.info.applicationInfo.sourceDir}" }
                if (signature == shownSignature) return@run
                val icons = pins.map { apps.icon(it, Theme.si(APP)) }
                runOnUiThread {
                    if (isDestroyed || signature == shownSignature) return@runOnUiThread
                    shownSignature = signature
                    showApps(pins, icons)
                }
            }
        }
    }

    private fun showApps(pins: List<AppEntry>, icons: List<Bitmap>) {
        val focusedPkg = if (pos.row == HomeNav.APPS && appViews.any { it.isFocused }) appNames.getOrNull(pos.col) else null
        rebuild(appsBox) { appsBox.removeAllViews() }
        val views = pins.mapIndexed { i, e -> appTile(e, icons[i], i) } + allAppsTile(pins.size)
        views.forEachIndexed { i, v -> appsBox.addView(v, box(64f + i * APP_STEP, 548f, APP, APP)) }
        appViews = views
        appNames = pins.map { it.label } + "Todos os apps"
        when {
            focusedPkg != null -> focus(HomeNav.Pos(HomeNav.APPS, appNames.indexOf(focusedPkg).coerceAtLeast(0)))
            firstFocus -> {
                firstFocus = false
                focus(HomeNav.start(sizes()))
            }
        }
    }

    private fun appTile(e: AppEntry, icon: Bitmap, i: Int): View = ImageView(this).apply {
        setImageBitmap(icon)
        contentDescription = e.label
        isFocusable = true
        isClickable = true
        setOnClickListener { if (!apps.launch(e)) Toast.makeText(context, "Não foi possível abrir ${e.label}", Toast.LENGTH_SHORT).show() }
        onFocusChangeListener = View.OnFocusChangeListener { v, has ->
            highlight(v, has, RADIUS_APP)
            if (has) {
                pos = HomeNav.Pos(HomeNav.APPS, i)
                showLabel(appLabel, e.label, 64f + i * APP_STEP + APP / 2)
            } else if (appViews.none { it.isFocused }) appLabel.visibility = View.INVISIBLE
        }
    }

    private fun allAppsTile(i: Int): View = FrameLayout(this).apply {
        background = Theme.rounded(0x33FFFFFF, RADIUS_APP)
        contentDescription = "Todos os apps"
        isFocusable = true
        isClickable = true
        addView(GridDots(context), FrameLayout.LayoutParams(Theme.si(44f), Theme.si(44f), Gravity.CENTER))
        setOnClickListener { startActivity(Intent(context, AllAppsActivity::class.java)) }
        onFocusChangeListener = View.OnFocusChangeListener { v, has ->
            highlight(v, has, RADIUS_APP)
            if (has) {
                pos = HomeNav.Pos(HomeNav.APPS, i)
                showLabel(appLabel, "Todos os apps", 64f + i * APP_STEP + APP / 2)
            } else if (appViews.none { it.isFocused }) appLabel.visibility = View.INVISIBLE
        }
    }

    private fun shortcutButton(s: Shortcut): View = FrameLayout(this).apply {
        background = Theme.rounded(0x24FFFFFF, RADIUS_SHORTCUT)
        contentDescription = s.label
        isFocusable = true
        isClickable = true
        addView(GlyphView(context, s.glyph), FrameLayout.LayoutParams(Theme.si(24f), Theme.si(24f), Gravity.CENTER))
        setOnClickListener { shortcuts.open(s) }
        onFocusChangeListener = View.OnFocusChangeListener { v, has ->
            highlight(v, has, RADIUS_SHORTCUT)
            if (has) {
                val i = s.ordinal
                pos = HomeNav.Pos(HomeNav.SHORTCUTS, i)
                showLabel(shortcutLabel, s.label, SHORTCUT_RIGHT - (Shortcut.values().size - i) * SHORTCUT_STEP + SHORTCUT / 2)
            } else if (shortcutViews.none { it.isFocused }) shortcutLabel.visibility = View.INVISIBLE
        }
    }

    /** Nome do item em foco, centralizado embaixo dele. */
    private fun showLabel(label: TextView, text: String, centerX: Float) {
        label.text = text
        label.visibility = View.VISIBLE
        label.post { label.translationX = Theme.s(centerX) - label.width / 2f }
    }

    private fun refreshWatchNext() {
        if (watchLoading) {
            watchPending = true
            return
        }
        watchLoading = true
        val w = Theme.si(CARD_W * FOCUS_SCALE)
        val h = Theme.si(CARD_H * FOCUS_SCALE)
        val badgeSize = Theme.si(BADGE)
        media.execute {
            val loaded = Guard.run({ Log.w(TAG, "falha ao ler o Continuar assistindo", it); null }) {
                val items = watchNext.items()
                watchNext.prunePosters(items, w, h)
                val names = HashMap<String, String>()
                val badges = HashMap<String, Bitmap?>()
                val cards = items.map { item ->
                    Card(
                        item = item,
                        meta = WatchNextList.meta(item, names.getOrPut(item.pkg) { watchNext.appName(item.pkg) }),
                        poster = watchNext.cachedPoster(item, w, h),
                        badge = badges.getOrPut(item.pkg) { watchNext.badge(item.pkg, badgeSize) },
                    )
                }
                // Capas que faltam: baixa uma por vez, com prazo total; na próxima volta já aparecem do disco.
                val missing = items.filter { it.poster != null && it.poster !in triedPosters && watchNext.cachedPoster(it, w, h) == null }
                missing.forEach { triedPosters += it.poster!! }
                cards to missing
            }
            runOnUiThread {
                watchLoading = false
                if (!isDestroyed && visible && loaded != null) showWatch(loaded.first)
                if (watchPending) {
                    watchPending = false
                    if (!isDestroyed && visible) refreshWatchNext()
                }
            }
            val missing = loaded?.second.orEmpty()
            if (missing.isNotEmpty()) {
                val deadline = System.currentTimeMillis() + POSTER_DEADLINE_MS
                var got = false
                for (item in missing) {
                    if (System.currentTimeMillis() > deadline) break
                    if (watchNext.downloadPoster(item, w, h)) got = true
                }
                if (got) runOnUiThread { if (!isDestroyed && visible) refreshWatchNext() }
            }
        }
    }

    private class Card(val item: WatchNextItem, val meta: String, val poster: Bitmap?, val badge: Bitmap?)

    private fun showWatch(cards: List<Card>) {
        val signature = cards.map { "${it.item.pkg}|${it.item.title}|${it.meta}|${it.poster != null}" }
        if (signature == shownWatch) return
        shownWatch = signature
        val text = cards.map { "${it.item.pkg}|${it.item.title}|${it.meta}" }
        // Só chegaram capas: troca a imagem em cada cartão, sem remontar (o foco nem pisca).
        if (text == shownWatchText && cardViews.size == cards.size) {
            cards.forEachIndexed { i, c -> c.poster?.let { (cardViews[i].getTag(R.id.poster) as? ImageView)?.setImageBitmap(it) } }
            return
        }
        shownWatchText = text
        val focusedCol = cardViews.indexOfFirst { it.isFocused }
        rebuild(watchBox) { watchBox.removeAllViews() }
        cardViews = cards.mapIndexed { i, c -> card(c, i).also { watchBox.addView(it, box(64f + i * CARD_STEP, 322f, CARD_W, CARD_H)) } }
        watchLabel.visibility = if (cards.isEmpty()) View.GONE else View.VISIBLE
        when {
            // Estava num cartão: fica no mesmo; se a fileira sumiu (acabou o último episódio), vai para os apps.
            focusedCol >= 0 -> focus(if (cards.isEmpty()) HomeNav.start(sizes()) else HomeNav.Pos(HomeNav.WATCH, focusedCol.coerceAtMost(cards.size - 1)))
            jumpToWatch && cards.isNotEmpty() -> {
                jumpToWatch = false
                focus(HomeNav.start(sizes()))
            }
            currentFocus == null && !firstFocus -> focus(HomeNav.start(sizes()))
        }
    }

    /**
     * Remonta [box] sem o foco escapar: com o foco lá dentro, tirar as views faz o Android entregar o foco à primeira
     * view focável da tela (o atalho "Entrada"), que piscava no canto. Bloqueado durante a troca, o foco só sai.
     */
    private fun rebuild(box: FrameLayout, change: () -> Unit) {
        box.descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
        change()
        box.descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
    }

    private fun card(c: Card, i: Int): View {
        val frame = FrameLayout(this).apply {
            background = Theme.rounded(0xFF2A2F3B.toInt(), RADIUS_CARD)
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) = outline.setRoundRect(0, 0, view.width, view.height, Theme.s(RADIUS_CARD))
            }
            clipToOutline = true
            isFocusable = true
            isClickable = true
            contentDescription = c.item.title
            setOnClickListener {
                if (!watchNext.open(c.item)) Toast.makeText(context, "Não foi possível abrir ${c.item.title}", Toast.LENGTH_SHORT).show()
            }
            onFocusChangeListener = View.OnFocusChangeListener { v, has ->
                highlight(v, has, RADIUS_CARD)
                if (has) pos = HomeNav.Pos(HomeNav.WATCH, i)
            }
        }
        val match = FrameLayout.LayoutParams.MATCH_PARENT
        val poster = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            c.poster?.let { setImageBitmap(it) }
        }
        frame.setTag(R.id.poster, poster)
        frame.addView(poster, FrameLayout.LayoutParams(match, match))
        frame.addView(View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.TRANSPARENT, 0xD9000000.toInt()))
        }, FrameLayout.LayoutParams(match, Theme.si(CARD_H * 0.6f), Gravity.BOTTOM))
        c.badge?.let { frame.addView(ImageView(this).apply { setImageBitmap(it) }, box(8f, 8f, BADGE, BADGE)) }
        frame.addView(Theme.text(this, 17f, 600, value = c.item.title).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }, FrameLayout.LayoutParams(match, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
            leftMargin = Theme.si(12f)
            rightMargin = Theme.si(12f)
            bottomMargin = Theme.si(28f)
        })
        frame.addView(Theme.text(this, 12f, 500, 0xFFCFD3DC.toInt(), c.meta).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }, FrameLayout.LayoutParams(match, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
            leftMargin = Theme.si(12f)
            rightMargin = Theme.si(12f)
            bottomMargin = Theme.si(10f)
        })
        WatchNextList.progress(c.item)?.let { p ->
            frame.addView(View(this).apply { setBackgroundColor(0x40FFFFFF) }, FrameLayout.LayoutParams(match, Theme.si(4f), Gravity.BOTTOM))
            frame.addView(View(this).apply { setBackgroundColor(Color.WHITE) },
                FrameLayout.LayoutParams(Theme.si(CARD_W * p), Theme.si(4f), Gravity.BOTTOM or Gravity.START))
        }
        return frame
    }

    /** Foco: cresce e ganha a borda branca do protótipo. */
    private fun highlight(v: View, has: Boolean, radius: Float) {
        val scale = if (has) FOCUS_SCALE else 1f
        v.animate().scaleX(scale).scaleY(scale).setDuration(140).start()
        v.foreground = if (has) Theme.ring(radius, RING) else null
        v.elevation = if (has) Theme.s(16f) else 0f
    }

    private fun at(left: Float, top: Float) = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
        leftMargin = Theme.si(left)
        topMargin = Theme.si(top)
    }

    private fun box(left: Float, top: Float, w: Float, h: Float) = FrameLayout.LayoutParams(Theme.si(w), Theme.si(h)).apply {
        leftMargin = Theme.si(left)
        topMargin = Theme.si(top)
    }

    private companion object {
        const val TAG = "TvMae"
        const val FOCUS_SCALE = 1.07f
        const val RING = 4f
        const val SHORTCUT = 52f
        const val SHORTCUT_STEP = 62f
        const val SHORTCUT_RIGHT = 1216f
        const val RADIUS_SHORTCUT = 14f
        const val CARD_W = 270f
        const val CARD_H = 170f
        const val CARD_STEP = 290f
        const val RADIUS_CARD = 10f
        const val BADGE = 26f
        const val APP = 116f
        const val APP_STEP = 128f
        const val RADIUS_APP = 20f
        const val POSTER_DEADLINE_MS = 15_000L
        /** Os apps gravam vários itens seguidos: espera a lista parar de mudar antes de ler. */
        const val WATCH_SETTLE_MS = 1_500L
    }
}
