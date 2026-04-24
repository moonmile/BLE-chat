package net.moonmile.ble5_chat.claude.model

data class ChatMessage(
    val messageId: String,
    val senderId: String,
    val timestamp: Long,
    val text: String
)
