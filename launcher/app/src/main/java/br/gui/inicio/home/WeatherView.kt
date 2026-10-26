package br.gui.inicio.home

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import br.gui.inicio.core.City
import br.gui.inicio.core.Weather
import br.gui.inicio.core.WeatherCodes
import br.gui.inicio.ui.GlassDrawable
import br.gui.inicio.ui.Theme
import br.gui.inicio.ui.WeatherIconView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Cartão de vidro do clima, abaixo do relógio: ícone, temperatura, condição, cidade, máxima e mínima. */
class WeatherView(ctx: Context) : LinearLayout(ctx) {
    private val icon = WeatherIconView(ctx)
    private val temp = Theme.text(ctx, 60f, 600)
    private val condition = Theme.text(ctx, 22f, 500)
    private val details = Theme.text(ctx, 18f, 400).apply { alpha = 0.8f }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = GlassDrawable(this, Theme.s(28f))
        setPadding(Theme.si(26f), Theme.si(16f), Theme.si(32f), Theme.si(16f))
        val wrap = LayoutParams.WRAP_CONTENT
        addView(icon, LayoutParams(Theme.si(72f), Theme.si(72f)))
        addView(temp, LayoutParams(wrap, wrap).apply { leftMargin = Theme.si(14f) })
        val texts = LinearLayout(ctx).apply {
            orientation = VERTICAL
            addView(condition)
            addView(details, LayoutParams(wrap, wrap).apply { topMargin = Theme.si(6f) })
        }
        addView(texts, LayoutParams(wrap, wrap).apply { leftMargin = Theme.si(20f) })
        visibility = View.GONE
    }

    /** [updatedAt] só aparece quando o dado tem mais de 2 h (sem internet há algum tempo). */
    fun show(weather: Weather?, city: City?, updatedAt: Long, nowMs: Long) {
        if (weather == null || city == null) {
            visibility = View.GONE
            return
        }
        icon.sky = WeatherCodes.sky(weather.code)
        icon.isDay = weather.isDay
        temp.text = "${weather.temp}°"
        condition.text = WeatherCodes.describe(weather.code)
        val old = nowMs - updatedAt > 2 * 60 * 60_000L
        val suffix = if (old) "  ·  atualizado às ${HOUR.format(Date(updatedAt))}" else ""
        details.text = "${city.name}  ·  máx ${weather.max}°  mín ${weather.min}°$suffix"
        visibility = View.VISIBLE
        invalidate()
    }

    private companion object {
        val HOUR = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR"))
    }
}
