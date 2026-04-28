package net.moonmile.ble5_chat.claude.repository

import kotlinx.coroutines.flow.Flow
import net.moonmile.ble5_chat.claude.ble.BleChatService
import net.moonmile.ble5_chat.claude.model.ChatMessage
import net.moonmile.ble5_chat.claude.util.AppLogger
import net.moonmile.ble5_chat.claude.util.ChatError

class ChatRepositoryImpl(
    private val bleService: BleChatService
) : ChatRepository {

    private val TAG = "ChatRepositoryImpl"

    override fun start() {
        AppLogger.d(TAG, "start()")
        bleService.start()
    }

    override fun stop() {
        AppLogger.d(TAG, "stop()")
        bleService.stop()
    }

    override fun publishMessage(message: ChatMessage) {
        AppLogger.d(TAG, "publishMessage: ${message.messageId}")
        bleService.send(message)
    }

    override fun observeMessages(): Flow<ChatMessage> = bleService.incomingMessages

    override fun observeErrors(): Flow<ChatError> = bleService.errors
}
