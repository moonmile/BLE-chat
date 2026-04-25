package net.moonmile.ble5_chat.claude.repository

import kotlinx.coroutines.flow.Flow
import net.moonmile.ble5_chat.claude.model.ChatMessage
import net.moonmile.ble5_chat.claude.util.ChatError

interface ChatRepository {
    fun publishMessage(message: ChatMessage)
    fun observeMessages(): Flow<ChatMessage>
    fun observeErrors(): Flow<ChatError>
    fun start()
    fun stop()
}
