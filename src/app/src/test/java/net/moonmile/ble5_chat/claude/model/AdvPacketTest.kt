package net.moonmile.ble5_chat.claude.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvPacketTest {

    private fun samplePayload() = byteArrayOf(0x01, 0x02, 0x03)

    private fun samplePacket() = AdvPacket(
        messageId = "msg-0001",
        payload   = samplePayload(),
        sentAt    = 1_700_000_000_000L
    )

    // ── フィールドが正しく格納される ─────────────────────────────
    @Test
    fun `fields are stored correctly`() {
        val pkt = samplePacket()
        assertEquals("msg-0001",            pkt.messageId)
        assertTrue(samplePayload().contentEquals(pkt.payload))
        assertEquals(1_700_000_000_000L,    pkt.sentAt)
    }

    // ── 等値性：同一内容は等しい ──────────────────────────────────
    @Test
    fun `equal packets are equal`() {
        assertEquals(samplePacket(), samplePacket())
    }

    // ── payload が異なると不等 ────────────────────────────────────
    @Test
    fun `packets with different payload are not equal`() {
        val a = samplePacket()
        val b = AdvPacket("msg-0001", byteArrayOf(0x04, 0x05), 1_700_000_000_000L)
        assertNotEquals(a, b)
    }

    // ── messageId が異なると不等 ──────────────────────────────────
    @Test
    fun `packets with different messageId are not equal`() {
        val a = samplePacket()
        val b = AdvPacket("msg-0002", samplePayload(), 1_700_000_000_000L)
        assertNotEquals(a, b)
    }

    // ── sentAt が異なると不等 ─────────────────────────────────────
    @Test
    fun `packets with different sentAt are not equal`() {
        val a = samplePacket()
        val b = AdvPacket("msg-0001", samplePayload(), 9_999L)
        assertNotEquals(a, b)
    }

    // ── hashCode は等しいオブジェクトで一致する ───────────────────
    @Test
    fun `equal packets have same hashCode`() {
        assertEquals(samplePacket().hashCode(), samplePacket().hashCode())
    }

    // ── 空の payload でも構築できる ──────────────────────────────
    @Test
    fun `empty payload is valid`() {
        val pkt = AdvPacket("id", ByteArray(0), 0L)
        assertTrue(pkt.payload.isEmpty())
    }
}
