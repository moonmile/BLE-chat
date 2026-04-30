package net.moonmile.ble5_chat.claude.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatUiStateTest {

    // ── デフォルト値のチェック ────────────────────────────────────
    @Test
    fun `default state has empty messages`() {
        val state = ChatUiState()
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun `default state has empty inputText`() {
        assertEquals("", ChatUiState().inputText)
    }

    @Test
    fun `default state has canSend false`() {
        assertFalse(ChatUiState().canSend)
    }

    @Test
    fun `default state has isScanning false`() {
        assertFalse(ChatUiState().isScanning)
    }

    @Test
    fun `default state has peerCount zero`() {
        assertEquals(0, ChatUiState().peerCount)
    }

    @Test
    fun `default state has null errorMessage`() {
        assertNull(ChatUiState().errorMessage)
    }

    // ── copy でフィールドを更新できる ────────────────────────────
    @Test
    fun `copy updates inputText`() {
        val state = ChatUiState().copy(inputText = "Hello")
        assertEquals("Hello", state.inputText)
    }

    @Test
    fun `copy updates canSend to true`() {
        val state = ChatUiState().copy(canSend = true)
        assertTrue(state.canSend)
    }

    @Test
    fun `copy adds message to list`() {
        val msg   = ChatMessage("id", "sender", 0L, "text")
        val state = ChatUiState().copy(messages = listOf(msg))
        assertEquals(1, state.messages.size)
        assertEquals("text", state.messages.first().text)
    }

    @Test
    fun `copy updates peerCount`() {
        val state = ChatUiState().copy(peerCount = 3)
        assertEquals(3, state.peerCount)
    }

    @Test
    fun `copy updates errorMessage`() {
        val state = ChatUiState().copy(errorMessage = "Something went wrong")
        assertEquals("Something went wrong", state.errorMessage)
    }

    // ── 等値性 ───────────────────────────────────────────────────
    @Test
    fun `two default states are equal`() {
        assertEquals(ChatUiState(), ChatUiState())
    }
}
