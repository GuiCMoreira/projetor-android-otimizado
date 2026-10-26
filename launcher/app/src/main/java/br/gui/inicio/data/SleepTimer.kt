package br.gui.inicio.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import br.gui.inicio.core.Guard

/**
 * Timer para desligar. Agenda com AlarmManager.setWindow (janela de 1 min, não exige permissão de alarme exato)
 * e um aviso 1 min antes. Desligar o projetor ou reiniciar cancela tudo (alarmes não sobrevivem).
 */
class SleepTimerScheduler(private val ctx: Context) {
    private val prefs = Prefs(ctx)
    private val alarms = ctx.getSystemService(AlarmManager::class.java)

    /** Instante do desligamento agendado, ou 0. */
    fun activeAt(nowMs: Long = System.currentTimeMillis()): Long = prefs.sleepAt.takeIf { it > nowMs } ?: 0

    /** Minutos da opção ativa, ou 0. */
    fun activeMinutes(nowMs: Long = System.currentTimeMillis()): Int = if (activeAt(nowMs) > 0) prefs.sleepMinutes else 0

    fun set(minutes: Int, nowMs: Long = System.currentTimeMillis()) {
        alarms.cancel(pending(SleepTimerReceiver.ACTION_WARN))
        alarms.cancel(pending(SleepTimerReceiver.ACTION_OFF))
        if (minutes <= 0) {
            prefs.sleepAt = 0
            prefs.sleepMinutes = 0
            return
        }
        val at = nowMs + minutes * 60_000L
        prefs.sleepAt = at
        prefs.sleepMinutes = minutes
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at - 60_000L, 30_000L, pending(SleepTimerReceiver.ACTION_WARN))
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at, 60_000L, pending(SleepTimerReceiver.ACTION_OFF))
    }

    private fun pending(action: String): PendingIntent = PendingIntent.getBroadcast(
        ctx, action.hashCode(),
        Intent(ctx, SleepTimerReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/**
 * Recebe os alarmes do timer. Para desligar usa o serviço de energia do fabricante: ele grava sys.powerctl=shutdown
 * ao receber "shutdown.cast" e exige a permissão "TODO", que o launcher declara (ver especificação 1.1, seção 5).
 */
class SleepTimerReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WARN -> Toast.makeText(ctx, "O projetor vai desligar em 1 minuto", Toast.LENGTH_LONG).show()
            ACTION_OFF -> {
                Prefs(ctx).run {
                    sleepAt = 0
                    sleepMinutes = 0
                }
                Guard.run({ Log.w("Inicio", "falha ao pedir o desligamento", it) }) {
                    ctx.sendBroadcast(Intent("shutdown.cast").setComponent(POWER_RECEIVER))
                }
            }
        }
    }

    companion object {
        const val ACTION_WARN = "br.gui.inicio.SLEEP_WARN"
        const val ACTION_OFF = "br.gui.inicio.SLEEP_OFF"
        val POWER_RECEIVER = ComponentName("com.zhiying.powerservice", "com.zhiying.powerservice.ShutdownBroadcastReceiver")
    }
}
