package net.moonmile.ble5_chat.claude.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.moonmile.ble5_chat.claude.model.ChatMessage
import org.json.JSONArray
import org.json.JSONObject

/**
 * お気に入りメッセージを SharedPreferences に JSON 形式で永続化する。
 * StateFlow でリアルタイムに変化を通知する。
 */
class FavoriteRepository(private val prefs: SharedPreferences) {

    private val KEY = "favorites"

    private val _favorites = MutableStateFlow(load())
    val favorites: StateFlow<List<ChatMessage>> = _favorites

    /** お気に入りの追加／解除をトグルする */
    fun toggle(message: ChatMessage) {
        val current = _favorites.value.toMutableList()
        if (current.any { it.messageId == message.messageId }) {
            current.removeIf { it.messageId == message.messageId }
        } else {
            current.add(message)
        }
        _favorites.value = current
        save(current)
    }

    /** お気に入り画面からの個別削除 */
    fun remove(messageId: String) {
        val updated = _favorites.value.filter { it.messageId != messageId }
        _favorites.value = updated
        save(updated)
    }

    /** 現在お気に入り済みかどうか */
    fun isFavorite(messageId: String): Boolean =
        _favorites.value.any { it.messageId == messageId }

    // ── 永続化 ─────────────────────────────────────────────────────

    private fun load(): List<ChatMessage> = runCatching {
        val json = prefs.getString(KEY, "[]") ?: "[]"
        val arr  = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            ChatMessage(
                messageId = obj.getString("messageId"),
                senderId  = obj.getString("senderId"),
                timestamp = obj.getLong("timestamp"),
                text      = obj.getString("text")
            )
        }
    }.getOrDefault(emptyList())

    private fun save(list: List<ChatMessage>) {
        val arr = JSONArray()
        list.forEach { msg ->
            arr.put(JSONObject().apply {
                put("messageId", msg.messageId)
                put("senderId",  msg.senderId)
                put("timestamp", msg.timestamp)
                put("text",      msg.text)
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }
}
