package net.moonmile.ble5_chat.copilot.ble

import kotlinx.coroutines.flow.Flow
import net.moonmile.ble5_chat.copilot.model.ChatMessage
import net.moonmile.ble5_chat.copilot.model.ChatUiEffect

/**
 * BLE ファサード：BLE 初期化、Advertiser/Scanner の開始停止管理
 *
 * - BLE アダプタ状態を監視
 * - アプリの開始/停止に追従して送受信を制御
 * - BroadcastReceiver で BLE OFF を検知して送受信を中断
 */
interface BleChatService {
    /**
     * BLE チャットを開始
     */
    suspend fun start()

    /**
     * BLE チャットを停止
     */
    suspend fun stop()

    /**
     * メッセージを送信
     *
     * @param chatMessage 送信するメッセージ
     */
    suspend fun send(chatMessage: ChatMessage)

    /**
     * 受信メッセージを監視
     *
     * @return 受信メッセージの Flow
     */
    fun incomingMessages(): Flow<ChatMessage>

    /**
     * BLE アダプタ状態変更を通知（BroadcastReceiver 用）
     *
     * @param state BLE アダプタの状態（BluetoothAdapter.STATE_* ）
     */
    suspend fun onBleAdapterStateChanged(state: Int)

    /**
     * 現在の BLE 有効状態を監視
     */
    fun observeBleEnabled(): Flow<Boolean>

    /**
     * BLE 関連の 1 回性イベントを監視
     */
    fun observeEffects(): Flow<ChatUiEffect>

    /**
     * Bluetooth アダプタ現在状態を即時反映
     */
    suspend fun refreshBleState()
}
