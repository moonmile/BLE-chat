package net.moonmile.ble5_chat.copilot.util

/**
 * BLE メッセージ / バイト列のエンコード・デコード
 */
object MessageCodec {
    private const val MAX_MESSAGE_LENGTH = 100
    private val CHARSET = Charsets.UTF_8

    /**
     * 文字列をバイト列に変換
     *
     * @param text メッセージテキスト（最大 100 文字）
     * @return エンコード済みバイト列
     * @throws IllegalArgumentException テキストが 100 文字を超える場合
     */
    fun encode(text: String): ByteArray {
        require(text.length <= MAX_MESSAGE_LENGTH) {
            "Message text must not exceed $MAX_MESSAGE_LENGTH characters"
        }
        return text.toByteArray(CHARSET)
    }

    /**
     * バイト列を文字列に変換
     *
     * @param bytes エンコード済みバイト列
     * @return デコード済みメッセージテキスト
     */
    fun decode(bytes: ByteArray): String {
        return String(bytes, CHARSET)
    }

    /**
     * テキストを最大 100 文字に制限
     *
     * @param text メッセージテキスト
     * @return 制限後のテキスト
     */
    fun truncate(text: String): String {
        return if (text.length > MAX_MESSAGE_LENGTH) {
            text.substring(0, MAX_MESSAGE_LENGTH)
        } else {
            text
        }
    }
}
