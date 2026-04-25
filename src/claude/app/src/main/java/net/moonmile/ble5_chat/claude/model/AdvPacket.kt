package net.moonmile.ble5_chat.claude.model

data class AdvPacket(
    val messageId: String,
    val payload: ByteArray,
    val sentAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AdvPacket
        return messageId == other.messageId && payload.contentEquals(other.payload) && sentAt == other.sentAt
    }

    override fun hashCode(): Int {
        var result = messageId.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + sentAt.hashCode()
        return result
    }
}
