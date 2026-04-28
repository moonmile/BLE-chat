package net.moonmile.ble5_chat.claude.ble

import net.moonmile.ble5_chat.claude.model.AdvPacket
import net.moonmile.ble5_chat.claude.model.ChatMessage
import net.moonmile.ble5_chat.claude.util.AppLogger
import java.nio.ByteBuffer

object MessageCodec {
    private const val TAG = "MessageCodec"
    const val MAX_TEXT_LENGTH = 100

    // Packet format (fixed-length header):
    // [messageId: 36 bytes][senderId: 8 bytes][timestamp: 8 bytes][text: variable UTF-8]
    private const val MESSAGE_ID_LEN = 36
    private const val SENDER_ID_LEN = 8
    private const val TIMESTAMP_LEN = 8
    private const val HEADER_LEN = MESSAGE_ID_LEN + SENDER_ID_LEN + TIMESTAMP_LEN

    fun encode(message: ChatMessage): ByteArray {
        val text = message.text.take(MAX_TEXT_LENGTH)
        val textBytes = text.toByteArray(Charsets.UTF_8)

        val messageIdBytes = message.messageId.toByteArray(Charsets.UTF_8).copyOf(MESSAGE_ID_LEN)
        val senderIdBytes = message.senderId.toByteArray(Charsets.UTF_8).copyOf(SENDER_ID_LEN)

        val buffer = ByteBuffer.allocate(HEADER_LEN + textBytes.size)
        buffer.put(messageIdBytes)
        buffer.put(senderIdBytes)
        buffer.putLong(message.timestamp)
        buffer.put(textBytes)

        AppLogger.d(TAG, "Encoded: messageId=${message.messageId} textLen=${textBytes.size}")
        return buffer.array()
    }

    fun decode(raw: ByteArray): ChatMessage? {
        if (raw.size < HEADER_LEN) {
            AppLogger.w(TAG, "Packet too small: ${raw.size} bytes")
            return null
        }
        return try {
            val buf = ByteBuffer.wrap(raw)

            val messageIdBytes = ByteArray(MESSAGE_ID_LEN).also { buf.get(it) }
            val senderIdBytes = ByteArray(SENDER_ID_LEN).also { buf.get(it) }
            val timestamp = buf.long
            val textBytes = ByteArray(raw.size - HEADER_LEN).also { buf.get(it) }

            ChatMessage(
                messageId = String(messageIdBytes, Charsets.UTF_8).trimEnd('\u0000'),
                senderId = String(senderIdBytes, Charsets.UTF_8).trimEnd('\u0000'),
                timestamp = timestamp,
                text = String(textBytes, Charsets.UTF_8)
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Decode failed", e)
            null
        }
    }

    fun toAdvPacket(message: ChatMessage): AdvPacket = AdvPacket(
        messageId = message.messageId,
        payload = encode(message),
        sentAt = message.timestamp
    )
}
