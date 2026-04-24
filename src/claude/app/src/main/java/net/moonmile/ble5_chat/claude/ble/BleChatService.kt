package net.moonmile.ble5_chat.claude.ble

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import net.moonmile.ble5_chat.claude.model.AdvPacket
import net.moonmile.ble5_chat.claude.model.ChatMessage
import net.moonmile.ble5_chat.claude.util.AppLogger
import net.moonmile.ble5_chat.claude.util.ChatError
import net.moonmile.ble5_chat.claude.util.ErrorHandler

class BleChatService(
    private val context: Context,
    private val advertiser: BleAdvertiserManager,
    private val scanner: BleScannerManager,
    private val peerRegistry: PeerRegistry,
    private val errorHandler: ErrorHandler
) {
    private val TAG = "BleChatService"

    private val _errors = MutableSharedFlow<ChatError>(extraBufferCapacity = 16)
    val errors: SharedFlow<ChatError> = _errors

    val incomingMessages: Flow<ChatMessage> = scanner.incomingMessages
    val peerCount = peerRegistry.peerCount

    private val bleStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            onBleAdapterStateChanged(state)
        }
    }

    fun start() {
        AppLogger.d(TAG, "start()")
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bleStateReceiver, filter)
        scanner.startScanning()
    }

    fun stop() {
        AppLogger.d(TAG, "stop()")
        advertiser.stopAdvertising()
        scanner.stopScanning()
        peerRegistry.clear()
        try { context.unregisterReceiver(bleStateReceiver) } catch (_: Exception) {}
    }

    fun send(chatMessage: ChatMessage) {
        AppLogger.d(TAG, "send: ${chatMessage.messageId}")
        val packet: AdvPacket = MessageCodec.toAdvPacket(chatMessage)
        advertiser.broadcast(packet)
    }

    fun onBleAdapterStateChanged(state: Int) {
        AppLogger.d(TAG, "BLE state changed: $state")
        when (state) {
            BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                advertiser.stopAdvertising()
                scanner.stopScanning()
                val error = errorHandler.onBleStateOff()
                _errors.tryEmit(error)
            }
            BluetoothAdapter.STATE_ON -> {
                scanner.startScanning()
            }
        }
    }

    fun release() {
        stop()
        advertiser.release()
    }
}
