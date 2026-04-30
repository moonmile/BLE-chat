package net.moonmile.ble5_chat.claude.ble

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DuplicateFilterTest {

    // TTL を 200ms に短く設定してテストを高速化
    private lateinit var filter: DuplicateFilter

    @Before
    fun setUp() {
        filter = DuplicateFilter(ttlMs = 200L)
    }

    // ── 初回は重複ではない ───────────────────────────────────────────
    @Test
    fun `first occurrence is not duplicate`() {
        assertFalse(filter.isDuplicate("msg-001"))
    }

    // ── 同じ ID を 2 回渡すと重複扱いになる ────────────────────────
    @Test
    fun `second occurrence is duplicate`() {
        filter.isDuplicate("msg-002")
        assertTrue(filter.isDuplicate("msg-002"))
    }

    // ── 異なる ID は重複ではない ────────────────────────────────────
    @Test
    fun `different ids are not duplicates`() {
        filter.isDuplicate("msg-003")
        assertFalse(filter.isDuplicate("msg-004"))
    }

    // ── TTL を過ぎると重複扱いにならなくなる ───────────────────────
    @Test
    fun `entry expires after ttl`() {
        filter.isDuplicate("msg-005")
        Thread.sleep(250L)          // TTL(200ms) を超えて待機
        assertFalse(filter.isDuplicate("msg-005"))
    }

    // ── TTL 内であれば依然として重複扱い ───────────────────────────
    @Test
    fun `entry is still duplicate within ttl`() {
        filter.isDuplicate("msg-006")
        Thread.sleep(100L)          // TTL(200ms) の半分
        assertTrue(filter.isDuplicate("msg-006"))
    }

    // ── clear() 後は重複なし ────────────────────────────────────────
    @Test
    fun `clear resets all entries`() {
        filter.isDuplicate("msg-007")
        assertTrue(filter.isDuplicate("msg-007"))
        filter.clear()
        assertFalse(filter.isDuplicate("msg-007"))
    }

    // ── 複数エントリが独立して管理される ───────────────────────────
    @Test
    fun `multiple entries managed independently`() {
        repeat(5) { i -> filter.isDuplicate("msg-${100 + i}") }
        // 全て2回目は重複
        repeat(5) { i -> assertTrue(filter.isDuplicate("msg-${100 + i}")) }
    }
}
