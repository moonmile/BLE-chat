package net.moonmile.ble5_chat.claude.ble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

class PeerRegistry(private val timeoutMs: Long = 60_000L) {
    private val peers = ConcurrentHashMap<String, Long>()

    private val _peerCount = MutableStateFlow(0)
    val peerCount: StateFlow<Int> = _peerCount

    fun update(senderId: String) {
        val now = System.currentTimeMillis()
        peers[senderId] = now
        evictExpired(now)
        _peerCount.value = peers.size
    }

    private fun evictExpired(now: Long) {
        val threshold = now - timeoutMs
        peers.entries.removeIf { it.value < threshold }
    }

    fun clear() {
        peers.clear()
        _peerCount.value = 0
    }
}
