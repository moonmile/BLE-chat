package net.moonmile.ble5_chat.claude.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChatMessageTest {

    private fun sampleMessage() = ChatMessage(
        messageId = "msg-0001",
        senderId  = "Alice",
        timestamp = 1_700_000_000_000L,
        text      = "Hello"
    )

    // ── 基本的なフィールドアクセス ────────────────────────────────
    @Test
    fun `fields are stored correctly`() {
        val msg = sampleMessage()
        assertEquals("msg-0001",           msg.messageId)
        assertEquals("Alice",              msg.senderId)
        assertEquals(1_700_000_000_000L,   msg.timestamp)
        assertEquals("Hello",              msg.text)
    }

    // ── 等値性：同じ内容なら等しい ───────────────────────────────
    @Test
    fun `equal messages are equal`() {
        assertEquals(sampleMessage(), sampleMessage())
    }

    // ── 等値性：異なるフィールドなら不等 ─────────────────────────
    @Test
    fun `messages with different text are not equal`() {
        assertNotEquals(sampleMessage(), sampleMessage().copy(text = "Bye"))
    }

    // ── copy で特定フィールドだけ変えられる ──────────────────────
    @Test
    fun `copy changes only specified field`() {
        val updated = sampleMessage().copy(senderId = "Bob")
        assertEquals("Bob",      updated.senderId)
        assertEquals("Hello",    updated.text)
        assertEquals("msg-0001", updated.messageId)
    }

    // ── hashCode は等しいオブジェクトで一致する ───────────────────
    @Test
    fun `equal messages have same hashCode`() {
        assertEquals(sampleMessage().hashCode(), sampleMessage().hashCode())
    }

    // ── toString には各フィールドが含まれる ──────────────────────
    @Test
    fun `toString contains all fields`() {
        val str = sampleMessage().toString()
        assert(str.contains("msg-0001"))
        assert(str.contains("Alice"))
        assert(str.contains("Hello"))
    }
}
