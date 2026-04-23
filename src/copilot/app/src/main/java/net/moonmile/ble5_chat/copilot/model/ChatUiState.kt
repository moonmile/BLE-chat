package net.moonmile.ble5_chat.copilot.model

/**
 * Compose 描画用状態
 *
 * @property messages 受信メッセージの一覧（時系列）
 * @property inputText 入力フィールドのテキスト
 * @property canSend 送信可能かどうか（BLE ON / 権限あり）
 * @property isScanning BLE スキャン中かどうか
 * @property errorMessage エラーメッセージ（null で エラーなし）
 * @property peerCount 参加中の端末数
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val canSend: Boolean = false,
    val isScanning: Boolean = false,
    val errorMessage: String? = null,
    val peerCount: Int = 0
)
