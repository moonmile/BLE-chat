package net.moonmile.ble5_chat.copilot.model

/**
 * BLE 広告パケット（1 広告単位データ）
 *
 * @property messageId メッセージ ID（重複排除用）
 * @property payload エンコード済みペイロード（最大 100 文字分のバイト列）
 * @property sentAt 送信時刻（Unix タイムスタンプ ms）
 */
data class AdvPacket(
    val messageId: String,
    val payload: ByteArray,
    val sentAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdvPacket

        if (messageId != other.messageId) return false
        if (!payload.contentEquals(other.payload)) return false
        if (sentAt != other.sentAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = messageId.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + sentAt.hashCode()
        return result
    }
}
