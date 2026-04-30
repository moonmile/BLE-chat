package net.moonmile.ble5_chat.claude.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatUiEventTest {

    // ── OnInputChanged にテキストが格納される ─────────────────────
    @Test
    fun `OnInputChanged stores text`() {
        val event = ChatUiEvent.OnInputChanged("Hello")
        assertEquals("Hello", event.text)
    }

    // ── OnInputChanged の等値性 ───────────────────────────────────
    @Test
    fun `OnInputChanged equality based on text`() {
        assertEquals(
            ChatUiEvent.OnInputChanged("abc"),
            ChatUiEvent.OnInputChanged("abc")
        )
    }

    // ── OnSendClicked は同じオブジェクト ─────────────────────────
    @Test
    fun `OnSendClicked is singleton object`() {
        assertEquals(ChatUiEvent.OnSendClicked, ChatUiEvent.OnSendClicked)
    }

    // ── OnStartChat は同じオブジェクト ───────────────────────────
    @Test
    fun `OnStartChat is singleton object`() {
        assertEquals(ChatUiEvent.OnStartChat, ChatUiEvent.OnStartChat)
    }

    // ── OnStopChat は同じオブジェクト ────────────────────────────
    @Test
    fun `OnStopChat is singleton object`() {
        assertEquals(ChatUiEvent.OnStopChat, ChatUiEvent.OnStopChat)
    }

    // ── 型チェック：when 分岐 ─────────────────────────────────────
    @Test
    fun `sealed class type check works`() {
        val events: List<ChatUiEvent> = listOf(
            ChatUiEvent.OnInputChanged("x"),
            ChatUiEvent.OnSendClicked,
            ChatUiEvent.OnStartChat,
            ChatUiEvent.OnStopChat
        )

        var inputCount  = 0
        var sendCount   = 0
        var startCount  = 0
        var stopCount   = 0

        events.forEach { event ->
            when (event) {
                is ChatUiEvent.OnInputChanged -> inputCount++
                is ChatUiEvent.OnSendClicked  -> sendCount++
                is ChatUiEvent.OnStartChat    -> startCount++
                is ChatUiEvent.OnStopChat     -> stopCount++
            }
        }

        assertEquals(1, inputCount)
        assertEquals(1, sendCount)
        assertEquals(1, startCount)
        assertEquals(1, stopCount)
    }

    // ── OnInputChanged は ChatUiEvent のサブタイプ ────────────────
    @Test
    fun `OnInputChanged is ChatUiEvent`() {
        val event: ChatUiEvent = ChatUiEvent.OnInputChanged("test")
        assertTrue(event is ChatUiEvent.OnInputChanged)
    }

    // ── OnInputChanged の copy ────────────────────────────────────
    @Test
    fun `OnInputChanged copy changes text`() {
        val original = ChatUiEvent.OnInputChanged("original")
        val copied   = original.copy(text = "changed")
        assertEquals("changed",  copied.text)
        assertEquals("original", original.text)
    }
}
