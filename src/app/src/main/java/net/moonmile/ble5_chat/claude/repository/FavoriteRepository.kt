package net.moonmile.ble5_chat.claude.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.moonmile.ble5_chat.claude.model.ChatMessage
import org.json.JSONArray
import org.json.JSONObject

class FavoriteRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val _favorites = MutableStateFlow(loadFavorites())
    val favorites: StateFlow<List<ChatMessage>> = _favorites.asStateFlow()

    fun toggle(message: ChatMessage) {
        val current = _favorites.value
        val updated = if (current.any { it.messageId == message.messageId }) {
            current.filterNot { it.messageId == message.messageId }
        } else {
            current + message
        }
        persist(updated)
    }

    fun remove(messageId: String) {
        persist(_favorites.value.filterNot { it.messageId == messageId })
    }

    fun isFavorite(messageId: String): Boolean {
        return _favorites.value.any { it.messageId == messageId }
    }

    private fun persist(messages: List<ChatMessage>) {
        _favorites.value = messages
        val array = JSONArray()
        messages.forEach { message ->
            array.put(
                JSONObject()
                    .put("messageId", message.messageId)
                    .put("senderId", message.senderId)
                    .put("timestamp", message.timestamp)
                    .put("text", message.text)
            )
        }
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply()
    }

    private fun loadFavorites(): List<ChatMessage> {
        val raw = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        ChatMessage(
                            messageId = item.getString("messageId"),
                            senderId = item.getString("senderId"),
                            timestamp = item.getLong("timestamp"),
                            text = item.getString("text")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREF_NAME = "favorites"
        const val KEY_FAVORITES = "favorite_messages"
    }
}
