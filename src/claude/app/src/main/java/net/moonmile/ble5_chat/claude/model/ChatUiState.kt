package net.moonmile.ble5_chat.claude.model

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val canSend: Boolean = false,
    val isScanning: Boolean = false,
    val peerCount: Int = 0,
    val errorMessage: String? = null
)
