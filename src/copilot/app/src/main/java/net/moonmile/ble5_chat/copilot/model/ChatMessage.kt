package net.moonmile.ble5_chat.copilot.model

import java.util.UUID

/**
 * ドメインモデル：チャットメッセージ
 *
 * @property messageId メッセージの一意識別子
 * @property senderId 送信者の ID（端末 ID など）
 * @property timestamp メッセージの送信時刻（Unix タイムスタンプ ms）
 * @property text メッセージ本文（最大 100 文字）
 */
data class ChatMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val text: String
) {
    init {
        require(text.length <= 100) { "Message text must not exceed 100 characters" }
    }
}
