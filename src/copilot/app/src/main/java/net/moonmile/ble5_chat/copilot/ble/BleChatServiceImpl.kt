package net.moonmile.ble5_chat.copilot.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.moonmile.ble5_chat.copilot.model.AdvPacket
import net.moonmile.ble5_chat.copilot.model.ChatMessage
import net.moonmile.ble5_chat.copilot.model.ChatUiEffect
import net.moonmile.ble5_chat.copilot.util.AppLogger
import net.moonmile.ble5_chat.copilot.util.MessageCodec
import java.util.UUID

/**
 * BLE ファサード実装。
 * BroadcastReceiver を内部保持し、BLE ON/OFF を動的監視する。
 */
class BleChatServiceImpl(
    private val appContext: Context,
    private val advertiserManager: BleAdvertiserManager = BleAdvertiserManager(),
    private val scannerManager: BleScannerManager = BleScannerManager()
) : BleChatService {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val incomingMessagesFlow = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    private val bleEnabled = MutableStateFlow(false)
    private val effects = MutableSharedFlow<ChatUiEffect>(extraBufferCapacity = 8)

    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private var isStarted = false
    private var receiverRegistered = false

    private val bleStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            serviceScope.launch {
                onBleAdapterStateChanged(state)
            }
        }
    }

    override suspend fun start() {
        if (isStarted) return
        isStarted = true

        registerBleReceiverIfNeeded()
        refreshBleState()

        if (bleEnabled.value) {
            scannerManager.startScanning()
        }

        AppLogger.info("BleChatServiceImpl: started")
    }

    override suspend fun stop() {
        if (!isStarted) return
        isStarted = false

        advertiserManager.stopAdvertising()
        scannerManager.stopScanning()
        unregisterBleReceiverIfNeeded()

        AppLogger.info("BleChatServiceImpl: stopped")
    }

    override suspend fun send(chatMessage: ChatMessage) {
        val payload = MessageCodec.encode(chatMessage.text)
        val packet = AdvPacket(
            messageId = chatMessage.messageId.ifBlank { UUID.randomUUID().toString() },
            payload = payload,
            sentAt = chatMessage.timestamp
        )
        advertiserManager.broadcast(packet)
    }

    override fun incomingMessages(): Flow<ChatMessage> = incomingMessagesFlow.asSharedFlow()

    override suspend fun onBleAdapterStateChanged(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_ON -> {
                bleEnabled.value = true
                effects.emit(ChatUiEffect.ShowToast("Bluetooth がオンになりました"))
                if (isStarted) {
                    scannerManager.startScanning()
                }
                AppLogger.info("BleChatServiceImpl: STATE_ON")
            }

            BluetoothAdapter.STATE_TURNING_OFF,
            BluetoothAdapter.STATE_OFF -> {
                advertiserManager.stopAdvertising()
                scannerManager.stopScanning()
                bleEnabled.value = false
                effects.emit(ChatUiEffect.NotifyBleDisabled)
                AppLogger.info("BleChatServiceImpl: STATE_OFF")
            }
        }
    }

    override fun observeBleEnabled(): Flow<Boolean> = bleEnabled.asStateFlow()

    override fun observeEffects(): Flow<ChatUiEffect> = effects.asSharedFlow()

    override suspend fun refreshBleState() {
        val enabled = bluetoothManager?.adapter?.isEnabled == true
        bleEnabled.value = enabled
        AppLogger.debug("BleChatServiceImpl: refreshBleState enabled=$enabled")
    }

    private fun registerBleReceiverIfNeeded() {
        if (receiverRegistered) return
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        ContextCompat.registerReceiver(
            appContext,
            bleStateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
        AppLogger.debug("BleChatServiceImpl: receiver registered")
    }

    private fun unregisterBleReceiverIfNeeded() {
        if (!receiverRegistered) return
        appContext.unregisterReceiver(bleStateReceiver)
        receiverRegistered = false
        AppLogger.debug("BleChatServiceImpl: receiver unregistered")
    }
}
