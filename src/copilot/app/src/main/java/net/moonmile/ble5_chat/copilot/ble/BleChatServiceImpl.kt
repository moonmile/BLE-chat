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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.moonmile.ble5_chat.copilot.model.ChatMessage
import net.moonmile.ble5_chat.copilot.model.ChatUiEffect
import net.moonmile.ble5_chat.copilot.util.AppLogger
import net.moonmile.ble5_chat.copilot.util.DuplicateFilter
import net.moonmile.ble5_chat.copilot.util.MessageCodec
import net.moonmile.ble5_chat.copilot.util.PeerRegistry

/**
 * BLE ファサード実装。
 * BroadcastReceiver を内部保持し、BLE ON/OFF を動的監視する。
 *
 * @param appContext アプリケーションコンテキスト
 * @param selfId     自端末 ID（送信パケットに埋め込む／自己ループ排除に使用）
 */
class BleChatServiceImpl(
    private val appContext: Context,
    private val selfId: String
) : BleChatService {

    private val advertiserManager = BleAdvertiserManager(appContext)
    private val scannerManager = BleScannerManager(appContext)

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val incomingMessagesFlow = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    private val bleEnabled = MutableStateFlow(false)
    private val effects = MutableSharedFlow<ChatUiEffect>(extraBufferCapacity = 8)
    private val peerCount = MutableStateFlow(0)

    private val peerRegistry = PeerRegistry()
    private val duplicateFilter = DuplicateFilter()

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
            // 存在通知（メッセージなし）を送出
            advertiserManager.broadcast(MessageCodec.encodePacket("", selfId, ""))
        }

        // スキャン受信パケットをデコードして peerRegistry と incomingMessages に反映
        serviceScope.launch {
            scannerManager.rawPackets().collect { raw ->
                handleRawPacket(raw)
            }
        }

        // 30 秒タイムアウトを反映するため定期的に peerCount を更新
        serviceScope.launch {
            while (isStarted) {
                delay(10_000)
                peerCount.value = peerRegistry.getActivePeerCount()
            }
        }

        AppLogger.info("BleChatServiceImpl: started selfId=$selfId")
    }

    override suspend fun stop() {
        if (!isStarted) return
        isStarted = false

        advertiserManager.stopAdvertising()
        scannerManager.stopScanning()
        unregisterBleReceiverIfNeeded()
        peerRegistry.reset()
        peerCount.value = 0

        AppLogger.info("BleChatServiceImpl: stopped")
    }

    override suspend fun send(chatMessage: ChatMessage) {
        val serviceData = MessageCodec.encodePacket(
            chatMessage.messageId,
            selfId,
            chatMessage.text
        )
        advertiserManager.broadcast(serviceData)
    }

    override fun incomingMessages(): Flow<ChatMessage> = incomingMessagesFlow.asSharedFlow()

    override suspend fun onBleAdapterStateChanged(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_ON -> {
                bleEnabled.value = true
                effects.emit(ChatUiEffect.ShowToast("Bluetooth がオンになりました"))
                if (isStarted) {
                    scannerManager.startScanning()
                    advertiserManager.broadcast(MessageCodec.encodePacket("", selfId, ""))
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

    override fun observePeerCount(): Flow<Int> = peerCount.asStateFlow()

    override suspend fun refreshBleState() {
        val enabled = bluetoothManager?.adapter?.isEnabled == true
        bleEnabled.value = enabled
        AppLogger.debug("BleChatServiceImpl: refreshBleState enabled=$enabled")
    }

    // -----------------------------------------------------------------------
    // private helpers
    // -----------------------------------------------------------------------

    private suspend fun handleRawPacket(raw: ByteArray) {
        AppLogger.debug("BleChatServiceImpl: handleRawPacket ${raw.size} bytes raw=${raw.decodeToString().take(80)}")
        val decoded = MessageCodec.decodePacket(raw)
        if (decoded == null) {
            AppLogger.warn("BleChatServiceImpl: decodePacket failed, dropping packet")
            return
        }
        val (msgId, senderId, text) = decoded
        AppLogger.debug("BleChatServiceImpl: decoded msgId=$msgId senderId=$senderId text='${text.take(40)}'")

        if (senderId == selfId) {
            AppLogger.debug("BleChatServiceImpl: self-loop dropped senderId=$senderId")
            return
        }
        if (senderId.isBlank()) {
            AppLogger.warn("BleChatServiceImpl: blank senderId, dropping")
            return
        }

        // ピア登録とカウント更新
        peerRegistry.registerPeer(senderId)
        val count = peerRegistry.getActivePeerCount()
        peerCount.value = count
        AppLogger.info("BleChatServiceImpl: peer registered senderId=$senderId activePeers=$count")

        // テキストありの場合だけメッセージとして通知（重複排除）
        if (text.isNotBlank()) {
            val effectiveMsgId = msgId.ifBlank { "$senderId:$text" }
            if (!duplicateFilter.isDuplicate(effectiveMsgId)) {
                val message = ChatMessage(
                    messageId = effectiveMsgId,
                    senderId = senderId,
                    text = MessageCodec.truncate(text)
                )
                AppLogger.info("BleChatServiceImpl: incoming message from=$senderId text='${text.take(40)}'")
                incomingMessagesFlow.emit(message)
            } else {
                AppLogger.debug("BleChatServiceImpl: duplicate msgId=$effectiveMsgId dropped")
            }
        }
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
