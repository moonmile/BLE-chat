package net.moonmile.ble5_chat.copilot.domain

import kotlinx.coroutines.flow.Flow
import net.moonmile.ble5_chat.copilot.model.ChatMessage

/**
 * ドメイン層：チャットリポジトリインターフェース
 *
 * 送受信ユースケースの統合と履歴の仲介を行う。
 * テスト時には Fake 実装へ差し替え可能。
 */
interface ChatRepository {
    /**
     * メッセージを発行
     *
     * @param message 発行するメッセージ
     */
    suspend fun publishMessage(message: ChatMessage)

    /**
     * 受信メッセージを監視
     *
     * @return 受信メッセージの Flow
     */
    fun observeMessages(): Flow<ChatMessage>
}
