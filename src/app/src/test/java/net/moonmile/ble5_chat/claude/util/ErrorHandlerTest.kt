package net.moonmile.ble5_chat.claude.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ErrorHandlerTest {

    private lateinit var handler: ErrorHandler

    @Before
    fun setUp() {
        handler = ErrorHandler()
    }

    // ── ファクトリメソッドが正しい型を返す ────────────────────────
    @Test
    fun `onBleStateOff returns BleDisabled`() {
        assertTrue(handler.onBleStateOff() is ChatError.BleDisabled)
    }

    @Test
    fun `onPermissionDenied returns PermissionDenied`() {
        assertTrue(handler.onPermissionDenied() is ChatError.PermissionDenied)
    }

    @Test
    fun `onAdvertiseError returns AdvertiseError with code`() {
        val error = handler.onAdvertiseError(3)
        assertTrue(error is ChatError.AdvertiseError)
        assertEquals(3, (error as ChatError.AdvertiseError).errorCode)
    }

    @Test
    fun `onScanError returns ScanError with code`() {
        val error = handler.onScanError(7)
        assertTrue(error is ChatError.ScanError)
        assertEquals(7, (error as ChatError.ScanError).errorCode)
    }

    // ── shouldRetry は Unknown のみ true ─────────────────────────
    @Test
    fun `shouldRetry is true for Unknown`() {
        val error = ChatError.Unknown(RuntimeException("oops"))
        assertTrue(handler.shouldRetry(error))
    }

    @Test
    fun `shouldRetry is false for BleDisabled`() {
        assertFalse(handler.shouldRetry(ChatError.BleDisabled))
    }

    @Test
    fun `shouldRetry is false for PermissionDenied`() {
        assertFalse(handler.shouldRetry(ChatError.PermissionDenied))
    }

    @Test
    fun `shouldRetry is false for AdvertiseError`() {
        assertFalse(handler.shouldRetry(ChatError.AdvertiseError(1)))
    }

    @Test
    fun `shouldRetry is false for ScanError`() {
        assertFalse(handler.shouldRetry(ChatError.ScanError(2)))
    }

    // ── toUserMessage が各エラーに適切なメッセージを返す ──────────
    @Test
    fun `toUserMessage for BleDisabled`() {
        val msg = handler.toUserMessage(ChatError.BleDisabled)
        assertEquals("Bluetoothがオフになりました", msg)
    }

    @Test
    fun `toUserMessage for PermissionDenied`() {
        val msg = handler.toUserMessage(ChatError.PermissionDenied)
        assertEquals("Bluetoothの権限を許可してください", msg)
    }

    @Test
    fun `toUserMessage for AdvertiseError contains error code`() {
        val msg = handler.toUserMessage(ChatError.AdvertiseError(5))
        assertTrue(msg.contains("5"))
    }

    @Test
    fun `toUserMessage for ScanError contains error code`() {
        val msg = handler.toUserMessage(ChatError.ScanError(9))
        assertTrue(msg.contains("9"))
    }

    @Test
    fun `toUserMessage for Unknown contains throwable message`() {
        val error = ChatError.Unknown(RuntimeException("connection reset"))
        val msg   = handler.toUserMessage(error)
        assertTrue(msg.contains("connection reset"))
    }

    // ── ChatError の等値性 ────────────────────────────────────────
    @Test
    fun `AdvertiseError equality based on errorCode`() {
        assertEquals(ChatError.AdvertiseError(10), ChatError.AdvertiseError(10))
    }

    @Test
    fun `ScanError equality based on errorCode`() {
        assertEquals(ChatError.ScanError(2), ChatError.ScanError(2))
    }
}
