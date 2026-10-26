package br.gui.inicio.settings

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import br.gui.inicio.core.City
import br.gui.inicio.core.Guard
import br.gui.inicio.data.Prefs
import br.gui.inicio.data.WeatherRepository
import br.gui.inicio.ui.Backdrop
import br.gui.inicio.ui.Glyph
import br.gui.inicio.ui.GlyphView
import br.gui.inicio.ui.Theme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Escolha da cidade do clima: busca pelo nome (teclado na tela) ou volta a detectar pela internet. */
class CityActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var weather: WeatherRepository
    private lateinit var results: LinearLayout
    private lateinit var status: TextView
    private lateinit var input: EditText
    private val io: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.init(this)
        prefs = Prefs(this)
        weather = WeatherRepository(prefs)
        val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
        val match = LinearLayout.LayoutParams.MATCH_PARENT
        val root = FrameLayout(this)
        Backdrop.attach(this, root, prefs, io)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(Theme.text(this, 40f, 700, "Cidade do clima"))
        val current = prefs.weatherCity?.let { "Agora: ${label(it)}" } ?: "Agora: detectada pela internet"
        column.addView(Theme.text(this, 20f, 400, current).apply { alpha = 0.75f }, LinearLayout.LayoutParams(wrap, wrap).apply {
            topMargin = Theme.si(10f)
            bottomMargin = Theme.si(28f)
        })
        input = EditText(this).apply {
            hint = "Digite o nome da cidade e aperte OK"
            setHintTextColor(0x99FFFFFF.toInt())
            setTextColor(Color.WHITE)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Theme.s(26f))
            typeface = Theme.font(this@CityActivity, 500)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            background = Theme.rounded(Theme.FOCUS_FILL, 18f)
            setPadding(Theme.si(24f), Theme.si(18f), Theme.si(24f), Theme.si(18f))
            setOnEditorActionListener { v, action, ev ->
                val enter = action == EditorInfo.IME_ACTION_SEARCH ||
                    (ev?.keyCode == KeyEvent.KEYCODE_ENTER && ev.action == KeyEvent.ACTION_DOWN)
                if (enter) search(v.text.toString())
                enter
            }
        }
        column.addView(input, LinearLayout.LayoutParams(match, wrap))
        status = Theme.text(this, 18f, 400).apply { alpha = 0.75f }
        column.addView(status, LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = Theme.si(16f) })
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(results, LinearLayout.LayoutParams(match, wrap).apply { topMargin = Theme.si(12f) })
        column.addView(row("Detectar pela internet", Glyph.LOCATION) { choose(null) }, LinearLayout.LayoutParams(match, wrap).apply { topMargin = Theme.si(20f) })
        root.addView(column, FrameLayout.LayoutParams(Theme.si(900f), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL).apply { topMargin = Theme.si(110f) })
        setContentView(root)
        input.requestFocus()
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    private fun search(name: String) {
        if (name.isBlank()) return
        status.text = "Buscando…"
        io.execute {
            Guard.run({ Log.w("Inicio", "falha na busca de cidade", it) }) {
                val found = weather.search(name)
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    results.removeAllViews()
                    status.text = if (found.isEmpty()) "Nenhuma cidade encontrada (ou sem internet)." else "Escolha a cidade:"
                    found.forEach { c -> results.addView(row(label(c), Glyph.LOCATION) { choose(c) }) }
                    // Fecha o teclado: com ele aberto, as setas do controle vão para as teclas e não para a lista.
                    getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(input.windowToken, 0)
                    results.getChildAt(0)?.requestFocus()
                }
            }
        }
    }

    private fun choose(city: City?) {
        weather.setCity(city)
        finish()
    }

    private fun label(c: City) = if (c.region.isBlank()) c.name else "${c.name}, ${c.region}"

    private fun row(text: String, glyph: Glyph, onClick: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        setPadding(Theme.si(20f), Theme.si(14f), Theme.si(20f), Theme.si(14f))
        addView(GlyphView(context, glyph), LinearLayout.LayoutParams(Theme.si(28f), Theme.si(28f)))
        addView(Theme.text(context, 24f, 500, text), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { leftMargin = Theme.si(16f) })
        setOnFocusChangeListener { v, has -> v.background = if (has) Theme.rounded(Theme.SELECTION, 16f) else null }
        setOnClickListener { onClick() }
    }
}
