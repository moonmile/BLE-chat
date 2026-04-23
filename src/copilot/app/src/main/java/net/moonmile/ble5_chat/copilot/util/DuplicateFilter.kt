package net.moonmile.ble5_chat.copilot.util

/**
 * MessageId による重複受信抑止フィルタ
 */
class DuplicateFilter {
    private val receivedMessageIds = mutableSetOf<String>()
    private val maxCacheSize = 1000

    /**
     * メッセージ ID の重複を確認
     *
     * @param messageId メッセージ ID
     * @return false なら重複メッセージ、true なら新規メッセージ
     */
    fun isDuplicate(messageId: String): Boolean {
        // キャッシュが満杯なら古いデータを削除
        if (receivedMessageIds.size >= maxCacheSize) {
            receivedMessageIds.clear()
        }
        return !receivedMessageIds.add(messageId)
    }

    /**
     * フィルタをリセット
     */
    fun reset() {
        receivedMessageIds.clear()
    }
}
