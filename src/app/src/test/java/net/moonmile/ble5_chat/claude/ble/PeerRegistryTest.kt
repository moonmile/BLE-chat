package net.moonmile.ble5_chat.claude.ble

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PeerRegistryTest {

    // タイムアウトを 200ms に短く設定してテストを高速化
    private lateinit var registry: PeerRegistry

    @Before
    fun setUp() {
        registry = PeerRegistry(timeoutMs = 200L)
    }

    // ── 初期状態では peerCount が 0 ─────────────────────────────────
    @Test
    fun `initial peerCount is zero`() {
        assertEquals(0, registry.peerCount.value)
    }

    // ── 1 件の Peer を追加すると peerCount が 1 になる ──────────────
    @Test
    fun `update single peer increments count to one`() {
        registry.update("Alice")
        assertEquals(1, registry.peerCount.value)
    }

    // ── 同じ Peer を 2 回更新しても peerCount は 1 ──────────────────
    @Test
    fun `update same peer twice keeps count at one`() {
        registry.update("Alice")
        registry.update("Alice")
        assertEquals(1, registry.peerCount.value)
    }

    // ── 異なる Peer を 3 件追加すると peerCount が 3 ──────────────
    @Test
    fun `update three different peers gives count three`() {
        registry.update("Alice")
        registry.update("Bob")
        registry.update("Carol")
        assertEquals(3, registry.peerCount.value)
    }

    // ── clear() 後は peerCount が 0 に戻る ─────────────────────────
    @Test
    fun `clear resets peerCount to zero`() {
        registry.update("Alice")
        registry.update("Bob")
        registry.clear()
        assertEquals(0, registry.peerCount.value)
    }

    // ── タイムアウト後に update を呼ぶと古いエントリが排除される ────
    @Test
    fun `expired peers are evicted on next update`() {
        registry.update("Alice")
        assertEquals(1, registry.peerCount.value)

        Thread.sleep(250L)          // タイムアウト(200ms)を超えて待機

        // 別の Peer を update することで evict が走る
        registry.update("Bob")
        assertEquals(1, registry.peerCount.value)   // Alice は消え Bob だけ残る
    }

    // ── タイムアウト内では Peer は残る ─────────────────────────────
    @Test
    fun `peer is retained within timeout window`() {
        registry.update("Alice")
        Thread.sleep(100L)          // タイムアウト(200ms)の半分

        registry.update("Trigger")  // evict トリガー
        assertEquals(2, registry.peerCount.value)   // Alice と Trigger が両方残る
    }
}
