package br.gui.tvavo.home

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import br.gui.tvavo.R
import br.gui.tvavo.apps.AllAppsActivity
import br.gui.tvavo.core.ClockFormat
import br.gui.tvavo.core.Dir
import br.gui.tvavo.core.Greeting
import br.gui.tvavo.core.Guard
import br.gui.tvavo.core.HomeGrid
import br.gui.tvavo.core.Pins
import br.gui.tvavo.core.WeatherCodes
import br.gui.tvavo.core.WeatherPolicy
import br.gui.tvavo.data.AppEntry
import br.gui.tvavo.data.Apps
import br.gui.tvavo.data.Prefs
import br.gui.tvavo.data.WeatherRepository
import br.gui.tvavo.rescue.CrashLog
import br.gui.tvavo.rescue.RescueActivity
import br.gui.tvavo.ui.GridDots
import br.gui.tvavo.ui.Theme
import java.time.LocalDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Tela inicial da TV da avó: saudação, data, relógio e clima em cima; até 6 apps em botões grandes; "Todos os apps"
 * embaixo. Feita para o controle: setas, OK e Voltar. Nada roda com ela escondida.
 */
class HomeActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var apps: Apps
    private lateinit var weather: WeatherRepository
    private lateinit var root: FrameLayout
    private lateinit var greeting: TextView
    private lateinit var date: TextView
    private lateinit var clock: TextView
    private lateinit var weatherText: TextView
    private lateinit var grid: FrameLayout
    private lateinit var allApps: View
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    /** Rede do clima em fila própria: internet sem resposta não segura os botões. */
    private val net: ExecutorService = Executors.newSingleThreadExecutor()
    private var tiles: List<View> = emptyList()
    private var shownPins: List<String> = emptyList()
    /** App de onde se desceu para o "Todos os apps": subindo, o foco volta para ele. */
    private var lastTile = 0
    private var weatherLoading = false
    /** Última tentativa de buscar o clima, deu certo ou não: sem internet, espera antes de tentar de novo. */
    private var weatherAttemptMs = 0L
    /** Pacotes e nomes dos botões na tela: se nada mudou, a volta de um app não remonta a tela (um OK não se perde). */
    private var shownSignature: List<String>? = null
    /** Primeira montagem: o foco vai para o primeiro app, mesmo que o Android já o tenha dado ao "Todos os apps". */
    private var firstTiles = true

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = tick()
    }
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = refreshApps()
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
        weather = WeatherRepository(prefs)
        val wrap = FrameLayout.LayoutParams.WRAP_CONTENT
        val match = FrameLayout.LayoutParams.MATCH_PARENT

        root = FrameLayout(this).apply { background = Theme.background() }
        greeting = Theme.text(this, 40f, 600)
        root.addView(greeting, at(80f, 54f))
        date = Theme.text(this, 30f, 500, Theme.MUTED)
        root.addView(date, at(80f, 104f))
        clock = Theme.text(this, 120f, 600).apply { letterSpacing = -0.03f }
        root.addView(clock, FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.END).apply {
            topMargin = Theme.si(44f)
            rightMargin = Theme.si(80f)
        })
        weatherText = Theme.text(this, 26f, 500, Theme.MUTED)
        root.addView(weatherText, FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.END).apply {
            topMargin = Theme.si(178f)
            rightMargin = Theme.si(84f)
        })
        grid = FrameLayout(this).apply { clipChildren = false }
        root.addView(grid, FrameLayout.LayoutParams(match, match))
        allApps = allAppsButton()
        // Largura pelo conteúdo (ícone, texto e folgas): com largura fixa, o "apps" saía cortado na TV.
        root.addView(allApps, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, Theme.si(ALL_H), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = Theme.si(ALL_TOP)
        })
        root.clipChildren = false
        setContentView(root)
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
        tick()
        refreshApps()
    }

    override fun onStop() {
        unregisterReceiver(timeReceiver)
        unregisterReceiver(packageReceiver)
        super.onStop()
    }

    override fun onDestroy() {
        io.shutdownNow()
        net.shutdownNow()
        super.onDestroy()
    }

    /** Botão Início com a tela já aberta: volta o foco para o primeiro app. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        focus(0)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val dir = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> Dir.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> Dir.RIGHT
            KeyEvent.KEYCODE_DPAD_UP -> Dir.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> Dir.DOWN
            KeyEvent.KEYCODE_BACK -> return true // na tela inicial, Voltar nunca sai
            else -> return super.dispatchKeyEvent(event)
        }
        // As setas seguem a grade do protótipo, não a busca de foco do Android (que erra entre fileiras de tamanhos diferentes).
        if (event.action == KeyEvent.ACTION_DOWN) {
            // Nada em foco (não deveria acontecer, mas sem destaque ela fica perdida): qualquer seta devolve o foco.
            if (currentFocus == null) {
                focus(lastTile)
                return true
            }
            val current = currentIndex()
            val next = HomeGrid.move(current, dir, tiles.size, lastTile)
            if (next != current) focus(next)
        }
        return true
    }

    private fun currentIndex(): Int {
        val i = tiles.indexOfFirst { it.isFocused }
        return if (i >= 0) i else tiles.size // nada em foco nos apps: está no "Todos os apps"
    }

    /** [i] de 0 até tiles.size-1 é um app; tiles.size é o "Todos os apps". */
    private fun focus(i: Int) {
        if (i < tiles.size) {
            lastTile = i
            tiles[i].requestFocus()
        } else {
            allApps.requestFocus()
        }
    }

    private fun tick() {
        val now = LocalDateTime.now()
        clock.text = ClockFormat.time(now)
        date.text = ClockFormat.longDate(now)
        greeting.text = Greeting.at(now.hour, getString(R.string.greeting_name))
        showWeather()
    }

    private fun showWeather() {
        val now = System.currentTimeMillis()
        // Clima guardado só vale por algumas horas; mais velho que isso, a linha some em vez de enganar.
        val cached = if (WeatherPolicy.showCached(now, prefs.weatherAt)) weather.cached() else null
        weatherText.text = cached?.let { weatherLine(it.temp, it.code, it.isDay) } ?: ""
        if (weatherLoading || !WeatherPolicy.shouldFetch(now, prefs.weatherAt, weatherAttemptMs)) return
        weatherLoading = true
        weatherAttemptMs = now
        net.execute {
            val fresh = Guard.run({ null }) { weather.refresh() }
            runOnUiThread {
                weatherLoading = false
                if (!isDestroyed && fresh != null) weatherText.text = weatherLine(fresh.temp, fresh.code, fresh.isDay)
            }
        }
    }

    private fun weatherLine(temp: Int, code: Int, isDay: Boolean): String =
        "$temp° · " + WeatherCodes.describe(code, isDay).replaceFirstChar { it.lowercase() }

    private fun refreshApps() {
        io.execute {
            Guard.run({ Log.w(TAG, "falha ao montar os botões", it) }) {
                val all = apps.launchable()
                val byPkg = all.associateBy { it.pkg }
                val pins = Pins.visible(prefs.pins, byPkg.keys).mapNotNull { byPkg[it] }
                val signature = pins.map { "${it.pkg}|${it.label}" }
                if (signature == shownSignature) return@run
                val cells = HomeGrid.cells(pins.size)
                val icons = pins.mapIndexed { i, e -> apps.icon(e, Theme.si(iconSize(cells[i], pins.size))) }
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    shownSignature = signature
                    showTiles(pins, cells, icons)
                }
            }
        }
    }

    private fun showTiles(pins: List<AppEntry>, cells: List<HomeGrid.Cell>, icons: List<Bitmap>) {
        val wasOnAllApps = allApps.isFocused && !firstTiles
        firstTiles = false
        val focusedPkg = tiles.indexOfFirst { it.isFocused }.takeIf { it >= 0 }?.let { shownPins.getOrNull(it) }
        grid.removeAllViews()
        tiles = pins.mapIndexed { i, e -> tile(e, cells[i], icons[i], pins.size) }
        tiles.forEach { grid.addView(it) }
        shownPins = pins.map { it.pkg }
        when {
            wasOnAllApps -> allApps.requestFocus()
            focusedPkg != null && focusedPkg in shownPins -> focus(shownPins.indexOf(focusedPkg))
            else -> focus(0)
        }
    }

    private fun iconSize(cell: HomeGrid.Cell, n: Int): Float = when {
        cell.compact -> 84f
        n == 4 -> 120f
        else -> 150f
    }

    private fun tile(e: AppEntry, cell: HomeGrid.Cell, icon: Bitmap, n: Int): View {
        val iconPx = Theme.si(iconSize(cell, n))
        val box = LinearLayout(this).apply {
            background = Theme.rounded(Theme.TILE, RADIUS)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            if (cell.compact) {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(Theme.si(28f), 0, Theme.si(20f), 0)
            } else {
                orientation = LinearLayout.VERTICAL
            }
            contentDescription = e.label
            setOnClickListener { open(e) }
            onFocusChangeListener = View.OnFocusChangeListener { v, has -> highlight(v, has) }
        }
        box.addView(ImageView(this).apply { setImageBitmap(icon) }, LinearLayout.LayoutParams(iconPx, iconPx))
        val nameSize = when {
            cell.compact -> 36f
            n == 4 -> 40f
            else -> 46f
        }
        val name = Theme.text(this, nameSize, 600, value = e.label).apply {
            maxLines = 2
            if (!cell.compact) gravity = Gravity.CENTER_HORIZONTAL
        }
        box.addView(name, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            if (cell.compact) leftMargin = Theme.si(22f) else topMargin = Theme.si(18f)
        })
        box.layoutParams = FrameLayout.LayoutParams(Theme.si(cell.width.toFloat()), Theme.si(cell.height.toFloat())).apply {
            leftMargin = Theme.si(cell.left.toFloat())
            topMargin = Theme.si(cell.top.toFloat())
        }
        return box
    }

    private fun allAppsButton(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        background = Theme.rounded(Theme.TILE, 20f)
        setPadding(Theme.si(14f), 0, Theme.si(22f), 0)
        isFocusable = true
        isClickable = true
        contentDescription = "Todos os apps"
        addView(GridDots(context), LinearLayout.LayoutParams(Theme.si(34f), Theme.si(34f)).apply { rightMargin = Theme.si(14f) })
        addView(Theme.text(context, 24f, 500, Theme.MUTED, "Todos os apps"))
        setOnClickListener { startActivity(Intent(context, AllAppsActivity::class.java)) }
        onFocusChangeListener = View.OnFocusChangeListener { v, has -> highlight(v, has, small = true) }
    }

    /** Foco: o botão cresce e ganha a borda branca grossa do protótipo. */
    private fun highlight(v: View, has: Boolean, small: Boolean = false) {
        val scale = if (has) FOCUS_SCALE else 1f
        v.animate().scaleX(scale).scaleY(scale).setDuration(140).start()
        v.foreground = if (has) Theme.ring(if (small) 20f else RADIUS, RING) else null
        v.elevation = if (has) Theme.s(18f) else 0f
    }

    private fun open(e: AppEntry) {
        if (!apps.launch(e)) Toast.makeText(this, "Não foi possível abrir ${e.label}", Toast.LENGTH_SHORT).show()
    }

    private fun at(left: Float, top: Float) = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
        leftMargin = Theme.si(left)
        topMargin = Theme.si(top)
    }

    private companion object {
        const val TAG = "TvAvo"
        const val RADIUS = 34f
        const val RING = 7f
        const val FOCUS_SCALE = 1.06f
        const val ALL_H = 58f
        const val ALL_TOP = 610f
    }
}
