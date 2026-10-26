package br.gui.inicio.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import br.gui.inicio.core.ClockFormat
import br.gui.inicio.core.Guard
import br.gui.inicio.core.WallpaperChoice
import br.gui.inicio.ui.Theme
import br.gui.inicio.wallpaper.WallpaperLoader
import java.time.LocalDateTime
import java.util.concurrent.Executor
import kotlin.random.Random

/**
 * Descanso de tela estilo Apple TV: fotos em tela cheia, cada uma por [SLIDE_MS] com zoom e deslocamento lentos
 * (animação da placa de vídeo), esmaecendo de uma para a outra. Relógio grande no canto inferior esquerdo.
 * No máximo duas fotos na memória; tudo é liberado em [stop].
 */
class ScreensaverView(ctx: Context, private val loader: WallpaperLoader, private val io: Executor) : FrameLayout(ctx) {
    private val main = Handler(Looper.getMainLooper())
    private val layers = listOf(ImageView(ctx), ImageView(ctx))
    private val time = Theme.text(ctx, 140f, 700).apply { letterSpacing = -0.03f }
    private val date = Theme.text(ctx, 30f, 600).apply { alpha = 0.9f }
    private val extra = Theme.text(ctx, 24f, 500).apply { alpha = 0.85f }
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
        val corner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(date)
            addView(time)
            addView(extra)
        }
        addView(corner, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.START).apply {
            leftMargin = Theme.si(90f)
            bottomMargin = Theme.si(80f)
        })
        visibility = View.GONE
    }

    /** [line] = texto extra embaixo do relógio (ex.: o clima); vazio esconde. */
    fun start(choices: List<WallpaperChoice>, line: String) {
        if (running || choices.isEmpty()) return
        running = true
        slides = choices
        index = Random.nextInt(choices.size)
        extra.text = line
        extra.visibility = if (line.isEmpty()) View.GONE else View.VISIBLE
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
        const val SLIDE_MS = 20_000L
        const val FADE_MS = 1_500L
    }
}
