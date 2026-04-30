package net.moonmile.ble5_chat.claude.ble

import net.moonmile.ble5_chat.claude.model.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageCodecTest {

    private fun makeMessage(
        messageId: String = "test-msg-id-1234-5678-abcd-efgh",
        senderId: String  = "Alice",
        timestamp: Long   = 1_700_000_000_000L,
        text: String      = "Hello"
    ) = ChatMessage(messageId, senderId, timestamp, text)

    // ── エンコード後にデコードすると元のメッセージに戻る ──────────────
    @Test
    fun `encode then decode roundtrip`() {
        val original = makeMessage()
        val raw      = MessageCodec.encode(original)
        val decoded  = MessageCodec.decode(raw)

        assertNotNull(decoded)
        assertEquals(original.messageId, decoded!!.messageId)
        assertEquals(original.senderId,  decoded.senderId)
        assertEquals(original.timestamp, decoded.timestamp)
        assertEquals(original.text,      decoded.text)
    }

    // ── テキストが 100 文字を超えるとき truncate される ───────────────
    @Test
    fun `encode truncates text to MAX_TEXT_LENGTH`() {
        val longText = "A".repeat(150)
        val msg      = makeMessage(text = longText)
        val raw      = MessageCodec.encode(msg)
        val decoded  = MessageCodec.decode(raw)

        assertNotNull(decoded)
        assertEquals(MessageCodec.MAX_TEXT_LENGTH, decoded!!.text.length)
    }

    // ── senderId が 8 文字未満のとき null バイトがパディングされ trimEnd で除去 ──
    @Test
    fun `short senderId is padded then trimmed on decode`() {
        val msg     = makeMessage(senderId = "Bob")
        val decoded = MessageCodec.decode(MessageCodec.encode(msg))
        assertEquals("Bob", decoded!!.senderId)
    }

    // ── messageId が 36 文字未満のとき null バイトがパディングされ trimEnd で除去 ──
    @Test
    fun `short messageId is padded then trimmed on decode`() {
        val msg     = makeMessage(messageId = "short-id")
        val decoded = MessageCodec.decode(MessageCodec.encode(msg))
        assertEquals("short-id", decoded!!.messageId)
    }

    // ── 日本語テキストのエンコード/デコード ────────────────────────────
    @Test
    fun `Japanese text roundtrip`() {
        val msg     = makeMessage(text = "こんにちは世界")
        val decoded = MessageCodec.decode(MessageCodec.encode(msg))
        assertEquals("こんにちは世界", decoded!!.text)
    }

    // ── ヘッダーより短いバイト列は decode で null を返す ──────────────
    @Test
    fun `decode returns null for payload smaller than header`() {
        val tooSmall = ByteArray(10)
        assertNull(MessageCodec.decode(tooSmall))
    }

    // ── ちょうどヘッダーサイズ（テキスト空）でも decode できる ─────────
    @Test
    fun `decode succeeds with empty text payload`() {
        val msg     = makeMessage(text = "")
        val decoded = MessageCodec.decode(MessageCodec.encode(msg))
        assertNotNull(decoded)
        assertEquals("", decoded!!.text)
    }

    // ── toAdvPacket で AdvPacket の各フィールドが正しい ─────────────────
    @Test
    fun `toAdvPacket has correct fields`() {
        val msg    = makeMessage()
        val packet = MessageCodec.toAdvPacket(msg)

        assertEquals(msg.messageId, packet.messageId)
        assertEquals(msg.timestamp, packet.sentAt)
        assertTrue(packet.payload.isNotEmpty())
    }

    // ── encode された長さが HEADER_LEN + text UTF-8 byte 数になる ──────
    @Test
    fun `encoded length equals header plus text bytes`() {
        val text     = "Hello!"
        val msg      = makeMessage(text = text)
        val raw      = MessageCodec.encode(msg)
        val expected = 52 + text.toByteArray(Charsets.UTF_8).size   // HEADER_LEN = 52
        assertEquals(expected, raw.size)
    }

    // ── MAX_TEXT_LENGTH の確認 ──────────────────────────────────────────
    @Test
    fun `MAX_TEXT_LENGTH is 100`() {
        assertEquals(100, MessageCodec.MAX_TEXT_LENGTH)
    }
}
