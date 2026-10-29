package br.gui.inicio.rescue

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import br.gui.inicio.home.HomeActivity

/**
 * Tela de emergência, aberta quando o Início caiu 3 vezes em 5 minutos. Usa só componentes do Android,
 * nada do resto do Início, para continuar abrindo mesmo que o defeito esteja no código da tela inicial.
 */
class RescueActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLog.rescueStarted(this)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(48), dp(40), dp(48), dp(40))
        }
        column.addView(text("A tela inicial teve um problema", 30f, Color.WHITE))
        column.addView(text("O Início fechou sozinho 3 vezes seguidas. Escolha um app abaixo ou tente abrir a tela inicial de novo.", 18f, 0xFFB8BAC2.toInt()))
        val retry = button("Tentar a tela inicial de novo") {
            CrashLog.clear(this)
            startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
        column.addView(retry)
        column.addView(button("Ajustes do Android") { open(Intent(Settings.ACTION_SETTINGS)) })
        launchableApps().forEach { (label, intent) -> column.addView(button(label) { open(intent) }) }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(0xFF15161A.toInt())
            addView(column)
        })
        retry.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        CrashLog.rescueShown(this)
    }

    /** Saiu para um app ou para os Ajustes: fecha, para não ficar uma emergência esquecida na memória. */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) finish()
    }

    /** Voltar não sai daqui: atrás desta tela não há tela inicial. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event)

    private fun launchableApps(): List<Pair<String, Intent>> {
        val seen = HashSet<String>()
        return listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER).flatMap { category ->
            packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)
        }.filter { it.activityInfo.packageName != packageName && seen.add(it.activityInfo.packageName) }
            .map { info ->
                info.loadLabel(packageManager).toString() to
                    Intent(Intent.ACTION_MAIN).setClassName(info.activityInfo.packageName, info.activityInfo.name)
            }
            .sortedBy { it.first.lowercase() }
    }

    private fun open(intent: Intent) {
        runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun text(value: String, sizeSp: Float, color: Int) = TextView(this).apply {
        text = value
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, dp(20))
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), pill(0xFF3A7BFD.toInt()))
            addState(intArrayOf(android.R.attr.state_hovered), pill(0xFF3A7BFD.toInt()))
            addState(intArrayOf(), pill(0xFF2A2C33.toInt()))
        }
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(dp(640), LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) }
    }

    private fun pill(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(14).toFloat()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
