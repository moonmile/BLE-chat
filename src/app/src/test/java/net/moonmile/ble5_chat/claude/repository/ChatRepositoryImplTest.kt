package net.moonmile.ble5_chat.claude.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.moonmile.ble5_chat.claude.ble.BleChatService
import net.moonmile.ble5_chat.claude.model.ChatMessage
import net.moonmile.ble5_chat.claude.util.ChatError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * ChatRepositoryImpl のユニットテスト
 *
 * BleChatService を mock() に差し替え、
 * - 各メソッドが bleService に正しく委譲されること
 * - observe 系が bleService の Flow をそのまま返すこと
 * を検証する。
 */
class ChatRepositoryImplTest {

    // ── BleChatService を mock に差し替える ──────────────────────
    private lateinit var bleService: BleChatService
    private lateinit var repository: ChatRepositoryImpl

    // テスト用の Fake Flow（replay=1 で collect 前に emit できる）
    private val fakeMessages = MutableSharedFlow<ChatMessage>(replay = 1)
    private val fakeErrors   = MutableSharedFlow<ChatError>(replay = 1)

    @Before
    fun setUp() {
        bleService = mock()
        whenever(bleService.incomingMessages).thenReturn(fakeMessages)
        whenever(bleService.errors).thenReturn(fakeErrors)

        repository = ChatRepositoryImpl(bleService)
    }

    // ── start() ──────────────────────────────────────────────────
    @Test
    fun `start delegates to bleService start`() {
        repository.start()
        verify(bleService).start()
    }

    // ── stop() ───────────────────────────────────────────────────
    @Test
    fun `stop delegates to bleService stop`() {
        repository.stop()
        verify(bleService).stop()
    }

    // ── publishMessage() ─────────────────────────────────────────
    @Test
    fun `publishMessage delegates send to bleService`() {
        val msg = ChatMessage("id-001", "Alice", 1_000L, "Hello")
        repository.publishMessage(msg)
        verify(bleService).send(msg)
    }

    @Test
    fun `publishMessage passes exact message object`() {
        val msg = ChatMessage("id-002", "Bob", 2_000L, "こんにちは")
        repository.publishMessage(msg)
        verify(bleService).send(msg)
    }

    // ── observeMessages() ────────────────────────────────────────
    @Test
    fun `observeMessages returns bleService incomingMessages flow`() {
        assertSame(fakeMessages, repository.observeMessages())
    }

    @Test
    fun `observeMessages emits message from bleService`() = runTest {
        val msg = ChatMessage("id-003", "Carol", 3_000L, "Hi")
        fakeMessages.emit(msg)

        val received = repository.observeMessages().first()
        assertEquals(msg, received)
    }

    // ── observeErrors() ──────────────────────────────────────────
    @Test
    fun `observeErrors returns bleService errors flow`() {
        assertSame(fakeErrors, repository.observeErrors())
    }

    @Test
    fun `observeErrors emits BleDisabled error`() = runTest {
        fakeErrors.emit(ChatError.BleDisabled)

        val error = repository.observeErrors().first()
        assertEquals(ChatError.BleDisabled, error)
    }

    @Test
    fun `observeErrors emits AdvertiseError with code`() = runTest {
        fakeErrors.emit(ChatError.AdvertiseError(3))

        val error = repository.observeErrors().first()
        assertEquals(ChatError.AdvertiseError(3), error)
    }

    // ── 複数メッセージが順番通りに届く ────────────────────────────
    @Test
    fun `publishMessage can be called multiple times`() {
        val msg1 = ChatMessage("id-004", "Alice", 4_000L, "Hello 1")
        val msg2 = ChatMessage("id-005", "Alice", 5_000L, "Hello 2")
        val msg3 = ChatMessage("id-006", "Alice", 6_000L, "Hello 3")

        repository.publishMessage(msg1)
        repository.publishMessage(msg2)
        repository.publishMessage(msg3)

        verify(bleService).send(msg1)
        verify(bleService).send(msg2)
        verify(bleService).send(msg3)
    }
}
