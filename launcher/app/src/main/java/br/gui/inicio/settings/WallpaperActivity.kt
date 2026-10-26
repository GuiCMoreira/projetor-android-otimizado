package br.gui.inicio.settings

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import br.gui.inicio.core.Guard
import br.gui.inicio.core.Rotation
import br.gui.inicio.core.WallpaperChoice
import br.gui.inicio.data.Prefs
import br.gui.inicio.ui.Backdrop
import br.gui.inicio.ui.Theme
import br.gui.inicio.wallpaper.Gradients
import br.gui.inicio.wallpaper.WallpaperLoader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Escolha do fundo: 4 degradês desenhados, fotos incluídas e fotos enviadas pelo computador. */
class WallpaperActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var loader: WallpaperLoader
    private lateinit var gradientRow: LinearLayout
    private lateinit var photoRow: LinearLayout
    private lateinit var rotationRow: LinearLayout
    private val io: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.init(this)
        prefs = Prefs(this)
        loader = WallpaperLoader(this)
        val wrap = LinearLayout.LayoutParams.WRAP_CONTENT
        val root = FrameLayout(this)
        Backdrop.attach(this, root, prefs, io)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(Theme.text(this, 40f, 700, "Papel de parede"), LinearLayout.LayoutParams(wrap, wrap).apply { bottomMargin = Theme.si(30f) })
        // Tamanhos explícitos: sem eles o LinearLayout mede a coluna pelo texto de dica e corta as fileiras.
        column.addView(section("DEGRADÊS"), LinearLayout.LayoutParams(wrap, wrap))
        gradientRow = row()
        column.addView(gradientRow, LinearLayout.LayoutParams(wrap, wrap))
        column.addView(section("FOTOS"), LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = Theme.si(30f) })
        photoRow = row()
        // Largura da coluna (4 miniaturas); com mais fotos a fileira rola para o lado em vez de alargar a tela.
        column.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            addView(photoRow)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, wrap))
        column.addView(section("TROCAR SOZINHO ENTRE AS FOTOS"), LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = Theme.si(30f) })
        rotationRow = row()
        column.addView(rotationRow, LinearLayout.LayoutParams(wrap, wrap))
        renderRotation()
        column.addView(
            Theme.text(this, 18f, 400, "Para usar uma foto sua, envie pelo computador: scripts/enviar-wallpaper.sh foto.jpg").apply { alpha = 0.7f },
            LinearLayout.LayoutParams(wrap, wrap).apply { topMargin = Theme.si(24f) },
        )
        root.addView(column, FrameLayout.LayoutParams(Theme.si(COLUMN_WIDTH), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        val tw = Theme.si(320f)
        val th = Theme.si(180f)
        io.execute {
            Guard.run({ Log.w("Inicio", "falha ao montar as miniaturas", it) }) {
                val gradients = (0 until Gradients.COUNT).map { WallpaperChoice.Gradient(it) to Gradients.render(it, tw, th) }
                val choices = loader.assetNames().map { WallpaperChoice.Asset(it) } + loader.userFiles().map { WallpaperChoice.UserFile(it) }
                val photos = choices.mapNotNull { c -> loader.render(c, tw, th)?.let { c to it } }
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    fill(gradientRow, gradients)
                    fill(photoRow, photos)
                    focusSelected()
                }
            }
        }
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    private fun section(title: String) = Theme.text(this, 18f, 600, title).apply { alpha = 0.7f; setPadding(0, 0, 0, Theme.si(12f)) }

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        clipChildren = false
        clipToPadding = false
        setPadding(Theme.si(10f), Theme.si(10f), Theme.si(10f), Theme.si(10f))
    }

    /** Opções da troca automática como pílulas: a ativa fica azul, a focada fica branca. OK escolhe sem sair da tela. */
    private fun renderRotation() {
        val focusedIndex = (0 until rotationRow.childCount).firstOrNull { rotationRow.getChildAt(it).isFocused }
        rotationRow.removeAllViews()
        Rotation.OPTIONS.forEachIndexed { i, minutes ->
            val active = prefs.rotateMinutes == minutes
            val label = when (minutes) {
                0 -> "Desligado"
                60 -> "A cada 1 hora"
                1440 -> "1 vez por dia"
                else -> "A cada ${minutes / 60} horas"
            }
            val pill = Theme.text(this, 20f, 500, label).apply {
                isFocusable = true
                setPadding(Theme.si(22f), Theme.si(12f), Theme.si(22f), Theme.si(12f))
                fun paint(has: Boolean) {
                    background = Theme.rounded(if (has) Color.WHITE else if (active) Theme.SELECTION else Theme.TILE_FILL, 24f)
                    setTextColor(if (has) 0xFF111111.toInt() else Color.WHITE)
                }
                paint(false)
                setOnFocusChangeListener { _, has -> paint(has) }
                setOnClickListener {
                    prefs.rotateMinutes = minutes
                    renderRotation()
                }
            }
            rotationRow.addView(pill, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { rightMargin = Theme.si(14f) })
        }
        focusedIndex?.let { rotationRow.getChildAt(it).requestFocus() }
    }

    private fun fill(target: LinearLayout, items: List<Pair<WallpaperChoice, Bitmap>>) {
        target.removeAllViews()
        items.forEach { (choice, bmp) -> target.addView(thumb(choice, bmp)) }
    }

    private fun thumb(choice: WallpaperChoice, bmp: Bitmap): View {
        val tw = Theme.si(320f)
        val th = Theme.si(180f)
        val radius = Theme.s(16f)
        fun ring(color: Int) = GradientDrawable().apply {
            cornerRadius = radius
            setStroke(Theme.si(4f), color)
        }
        val selected = choice == prefs.wallpaper
        val image = ImageView(this).apply {
            setImageBitmap(bmp)
            scaleType = ImageView.ScaleType.CENTER_CROP
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) = outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
            clipToOutline = true
        }
        return FrameLayout(this).apply {
            tag = choice
            isFocusable = true
            addView(image, FrameLayout.LayoutParams(tw, th))
            foreground = if (selected) ring(Theme.SELECTION) else null
            layoutParams = LinearLayout.LayoutParams(tw, th).apply { rightMargin = Theme.si(24f) }
            setOnFocusChangeListener { v, has ->
                v.animate().scaleX(if (has) 1.08f else 1f).scaleY(if (has) 1.08f else 1f).setDuration(150).start()
                (v as FrameLayout).foreground = when {
                    has -> ring(Color.WHITE)
                    selected -> ring(Theme.SELECTION)
                    else -> null
                }
            }
            setOnClickListener {
                prefs.wallpaper = choice
                prefs.rotateMinutes = 0 // escolher um fundo específico desliga a troca automática
                finish()
            }
        }
    }

    private fun focusSelected() {
        val current = prefs.wallpaper
        val all = (0 until gradientRow.childCount).map { gradientRow.getChildAt(it) } +
            (0 until photoRow.childCount).map { photoRow.getChildAt(it) }
        (all.firstOrNull { it.tag == current } ?: all.firstOrNull())?.requestFocus()
    }

    private companion object {
        /** 4 miniaturas de 320 px + 24 px de espaço + 20 px de margem da fileira. */
        const val COLUMN_WIDTH = 1420f
    }
}
