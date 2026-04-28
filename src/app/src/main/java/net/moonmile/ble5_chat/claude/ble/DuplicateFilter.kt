package net.moonmile.ble5_chat.claude.ble

import net.moonmile.ble5_chat.claude.BuildConfig
import java.util.concurrent.ConcurrentHashMap

class DuplicateFilter(
    private val ttlMs: Long = BuildConfig.DUPLICATE_FILTER_TTL_SEC * 1_000L
) {
    private val seen = ConcurrentHashMap<String, Long>()

    fun isDuplicate(messageId: String): Boolean {
        val now = System.currentTimeMillis()
        evictExpired(now)
        return seen.putIfAbsent(messageId, now) != null
    }

    private fun evictExpired(now: Long) {
        val threshold = now - ttlMs
        seen.entries.removeIf { it.value < threshold }
    }

    fun clear() = seen.clear()
}
