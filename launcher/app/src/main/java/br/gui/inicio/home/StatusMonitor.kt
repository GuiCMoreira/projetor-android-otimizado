package br.gui.inicio.home

import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper

/** Avisa mudanças de rede e lê o Bluetooth, só entre [start] e [stop] (tela visível). */
class StatusMonitor(private val ctx: Context, private val onChange: (wifi: Boolean, bluetooth: Boolean?) -> Unit) {
    private val cm = ctx.getSystemService(ConnectivityManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var connected = false
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            post(true)
        }

        override fun onLost(network: Network) {
            post(false)
        }
    }

    fun start() {
        connected = cm.activeNetwork != null
        emit()
        cm.registerDefaultNetworkCallback(callback)
    }

    fun stop() {
        runCatching { cm.unregisterNetworkCallback(callback) }
    }

    private fun post(value: Boolean) {
        main.post {
            connected = value
            emit()
        }
    }

    private fun emit() = onChange(connected, bluetoothEnabled())

    /** No Android 12 ler o estado pode exigir BLUETOOTH_CONNECT; sem ela, null esconde o ícone. */
    private fun bluetoothEnabled(): Boolean? = try {
        ctx.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled
    } catch (e: SecurityException) {
        null
    }
}
