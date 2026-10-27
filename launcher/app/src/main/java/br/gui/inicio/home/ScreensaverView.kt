package br.gui.inicio.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import br.gui.inicio.core.ClockFormat
import br.gui.inicio.core.Guard
import br.gui.inicio.core.WallpaperChoice
import br.gui.inicio.core.Weather
import br.gui.inicio.core.WeatherCodes
import br.gui.inicio.ui.Glyph
import br.gui.inicio.ui.GlyphView
import br.gui.inicio.ui.WeatherIconView
import br.gui.inicio.ui.Theme
import br.gui.inicio.wallpaper.WallpaperLoader
import java.time.LocalDateTime
import java.util.concurrent.Executor
import kotlin.random.Random

/** O que o descanso mostra além das fotos. Tudo opcional: o que faltar some da tela. */
class SaverInfo(
    val weather: Weather? = null,
    val city: String? = null,
    val episode: WatchNextCard? = null,
)

/**
 * Descanso de tela no estilo da tela de bloqueio do macOS: fotos em tela cheia, cada uma por [SLIDE_MS] com zoom
 * e deslocamento lentos (animação da placa de vídeo), esmaecendo de uma para a outra. Data e hora grandes no alto,
 * widgets de vidro com o clima e o último episódio, e o nome do aparelho no rodapé.
 * No máximo duas fotos na memória; tudo é liberado em [stop].
 */
class ScreensaverView(ctx: Context, private val loader: WallpaperLoader, private val io: Executor) : FrameLayout(ctx) {
    private val main = Handler(Looper.getMainLooper())
    private val layers = listOf(ImageView(ctx), ImageView(ctx))
    private val time = Theme.text(ctx, 300f, 700).apply {
        letterSpacing = -0.035f
        alpha = 0.86f
    }
    private val date = Theme.text(ctx, 40f, 600).apply { alpha = 0.88f }
    private val widgets = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_HORIZONTAL
    }
    /** Episódio do widget; o OK no descanso abre ele. */
    var episode: WatchNextCard? = null
        private set
    private var slides: List<WallpaperChoice> = emptyList()
    private var index = 0
    private var front = 0
    private var running = false
    private val next = Runnable { showNext() }
    private val kenBurns = arrayOfNulls<AnimatorSet>(2)

    val isRunning: Boolean get() = running

    init {
        setBackgroundColor(Color.BLACK)
        val match = LayoutParams.MATCH_PARENT
        layers.forEach {
            it.scaleType = ImageView.ScaleType.CENTER_CROP
            it.alpha = 0f
            addView(it, LayoutParams(match, match))
        }
        // Véu escuro em cima e embaixo: a hora e o rodapé leem bem sobre qualquer foto (o macOS faz o mesmo).
        addView(View(ctx).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x47000000, 0, 0, 0x40000000))
        }, LayoutParams(match, match))
        val wrap = LayoutParams.WRAP_CONTENT
        addView(date, LayoutParams(wrap, wrap, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = Theme.si(92f) })
        addView(time, LayoutParams(wrap, wrap, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = Theme.si(150f) })
        addView(widgets, LayoutParams(match, wrap, Gravity.TOP).apply { topMargin = Theme.si(486f) })
        addView(footer(ctx), LayoutParams(wrap, wrap, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = Theme.si(48f) })
        visibility = View.GONE
    }

    fun start(choices: List<WallpaperChoice>, info: SaverInfo) {
        if (running || choices.isEmpty()) return
        running = true
        slides = choices
        index = Random.nextInt(choices.size)
        episode = info.episode
        buildWidgets(info)
        tick(LocalDateTime.now())
        visibility = View.VISIBLE
        alpha = 0f
        animate().alpha(1f).setDuration(800).start()
        showNext()
    }

    fun stop() {
        if (!running) return
        running = false
        main.removeCallbacks(next)
        animate().cancel()
        visibility = View.GONE
        episode = null
        widgets.removeAllViews() // solta a capa do episódio
        kenBurns.forEach { it?.cancel() }
        layers.forEach {
            it.animate().cancel()
            it.setImageDrawable(null) // libera as fotos
            it.alpha = 0f
        }
    }

    fun tick(now: LocalDateTime) {
        time.text = ClockFormat.time(now)
        date.text = ClockFormat.longDate(now)
    }

    private fun buildWidgets(info: SaverInfo) {
        widgets.removeAllViews()
        val gap = Theme.si(14f)
        if (info.weather != null && info.city != null) {
            widgets.addView(weatherWidget(info.weather, info.city), LinearLayout.LayoutParams(Theme.si(280f), Theme.si(280f)).apply { setMargins(gap, 0, gap, 0) })
        }
        info.episode?.let { ep ->
            widgets.addView(episodeWidget(ep), LinearLayout.LayoutParams(Theme.si(640f), Theme.si(280f)).apply { setMargins(gap, 0, gap, 0) })
        }
    }

    private fun weatherWidget(w: Weather, city: String) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = widgetGlass()
        setPadding(Theme.si(30f), Theme.si(28f), Theme.si(30f), Theme.si(28f))
        addView(Theme.text(context, 20f, 600, city).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END })
        addView(Theme.text(context, 72f, 500, "${w.temp}°").apply { letterSpacing = -0.03f }, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = Theme.si(8f) })
        addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))
        addView(WeatherIconView(context).apply { sky = WeatherCodes.sky(w.code); isDay = w.isDay }, LinearLayout.LayoutParams(Theme.si(40f), Theme.si(40f)))
        addView(Theme.text(context, 20f, 600, WeatherCodes.describe(w.code, w.isDay)).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = Theme.si(6f) })
        addView(Theme.text(context, 19f, 500, "máx ${w.max}°  mín ${w.min}°").apply { alpha = 0.75f }, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = Theme.si(4f) })
    }

    private fun episodeWidget(ep: WatchNextCard) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        background = widgetGlass()
        val pad = Theme.si(20f)
        setPadding(pad, pad, Theme.si(28f), pad)
        val thumb = FrameLayout(context).apply {
            background = Theme.rounded(Theme.TILE_FILL, 18f)
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) = outline.setRoundRect(0, 0, view.width, view.height, Theme.s(18f))
            }
            clipToOutline = true
            ep.poster?.let { addView(ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setImageBitmap(it) }, FrameLayout.LayoutParams(MATCH, MATCH)) }
            ep.progress?.let { p ->
                addView(ProgressLine(context, p), FrameLayout.LayoutParams(MATCH, Theme.si(6f), Gravity.BOTTOM).apply { setMargins(Theme.si(14f), 0, Theme.si(14f), Theme.si(14f)) })
            }
        }
        addView(thumb, LinearLayout.LayoutParams(Theme.si(250f), MATCH))
        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val kick = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                ep.badge?.let { addView(ImageView(context).apply { setImageBitmap(it) }, LinearLayout.LayoutParams(Theme.si(30f), Theme.si(30f)).apply { rightMargin = Theme.si(10f) }) }
                addView(Theme.text(context, 16f, 700, "CONTINUAR").apply { letterSpacing = 0.08f; alpha = 0.7f })
            }
            addView(kick)
            addView(Theme.text(context, 26f, 700, ep.title).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END }, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = Theme.si(14f) })
            addView(Theme.text(context, 18f, 500, ep.meta).apply { alpha = 0.75f; maxLines = 2 }, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = Theme.si(8f) })
            addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))
            val go = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(Theme.text(context, 15f, 700, "OK").apply {
                    background = Theme.rounded(0x33FFFFFF, 8f)
                    setPadding(Theme.si(10f), Theme.si(4f), Theme.si(10f), Theme.si(4f))
                })
                addView(Theme.text(context, 18f, 600, "volta e abre o episódio").apply { alpha = 0.85f }, LinearLayout.LayoutParams(WRAP, WRAP).apply { leftMargin = Theme.si(10f) })
            }
            addView(go)
        }
        addView(texts, LinearLayout.LayoutParams(0, MATCH, 1f).apply { leftMargin = Theme.si(24f) })
    }

    /** Rodapé no lugar do avatar do Mac: ícone do projetor, o nome dele e o aviso para voltar. */
    private fun footer(ctx: Context) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val size = Theme.si(64f)
        val avatar = FrameLayout(ctx).apply {
            background = Theme.oval(0x38FFFFFF)
            addView(GlyphView(ctx, Glyph.PROJECTION), LayoutParams(Theme.si(32f), Theme.si(32f), Gravity.CENTER))
        }
        addView(avatar, LinearLayout.LayoutParams(size, size))
        addView(Theme.text(ctx, 22f, 600, DEVICE_NAME).apply { alpha = 0.9f }, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = Theme.si(12f) })
        addView(Theme.text(ctx, 18f, 500, "Aperte qualquer tecla para voltar").apply {
            alpha = 0.8f
            background = Theme.rounded(0x24FFFFFF, 999f)
            setPadding(Theme.si(22f), Theme.si(10f), Theme.si(22f), Theme.si(10f))
        }, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = Theme.si(12f) })
    }

    private fun widgetGlass() = GradientDrawable().apply {
        setColor(0x4D1E1E28)
        setStroke(Theme.si(1.5f).coerceAtLeast(1), 0x2EFFFFFF)
        cornerRadius = Theme.s(36f)
    }

    private fun showNext() {
        if (!running) return
        val choice = slides[index % slides.size]
        index++
        val m = resources.displayMetrics
        io.execute {
            val bmp: Bitmap? = Guard.run({ null }) { loader.render(choice, m.widthPixels, m.heightPixels, fullScreen = true) }
            main.post { if (running) crossfade(bmp) }
        }
    }

    private fun crossfade(bmp: Bitmap?) {
        val incomingIndex = 1 - front
        val incoming = layers[incomingIndex]
        val outgoing = layers[front]
        if (bmp != null) {
            incoming.setImageBitmap(bmp)
            incoming.alpha = 0f
            incoming.animate().alpha(1f).setDuration(FADE_MS).start()
            // Ken Burns: zoom e deslocamento lentos durante todo o tempo da foto (a GPU anima, a CPU fica parada).
            // Animador separado do esmaecimento: um ViewPropertyAnimator só tem uma duração por vez.
            val dx = (if (Random.nextBoolean()) 1 else -1) * Theme.s(60f)
            kenBurns[incomingIndex]?.cancel()
            kenBurns[incomingIndex] = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(incoming, View.SCALE_X, 1f, 1.12f),
                    ObjectAnimator.ofFloat(incoming, View.SCALE_Y, 1f, 1.12f),
                    ObjectAnimator.ofFloat(incoming, View.TRANSLATION_X, 0f, dx),
                )
                duration = SLIDE_MS + FADE_MS
                interpolator = LinearInterpolator()
                start()
            }
            outgoing.animate().alpha(0f).setDuration(FADE_MS).withEndAction { if (running) outgoing.setImageDrawable(null) }.start()
            front = incomingIndex
        }
        main.postDelayed(next, SLIDE_MS)
    }

    private companion object {
        const val DEVICE_NAME = "Projetor do Gui"
        const val MATCH = LayoutParams.MATCH_PARENT
        const val WRAP = LayoutParams.WRAP_CONTENT
        const val SLIDE_MS = 20_000L
        const val FADE_MS = 1_500L
    }
}
