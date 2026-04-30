package net.moonmile.ble5_chat.claude.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatUiEffectTest {

    // ── ShowToast にメッセージが格納される ────────────────────────
    @Test
    fun `ShowToast stores message`() {
        val effect = ChatUiEffect.ShowToast("Test message")
        assertEquals("Test message", effect.message)
    }

    // ── ShowToast の等値性 ────────────────────────────────────────
    @Test
    fun `ShowToast equality based on message`() {
        assertEquals(
            ChatUiEffect.ShowToast("hello"),
            ChatUiEffect.ShowToast("hello")
        )
    }

    // ── RequestBluetoothPermission は同じオブジェクト ─────────────
    @Test
    fun `RequestBluetoothPermission is singleton object`() {
        val a = ChatUiEffect.RequestBluetoothPermission
        val b = ChatUiEffect.RequestBluetoothPermission
        assertEquals(a, b)
    }

    // ── NotifyBleDisabled は同じオブジェクト ──────────────────────
    @Test
    fun `NotifyBleDisabled is singleton object`() {
        val a = ChatUiEffect.NotifyBleDisabled
        val b = ChatUiEffect.NotifyBleDisabled
        assertEquals(a, b)
    }

    // ── 型チェック：when 分岐 ─────────────────────────────────────
    @Test
    fun `sealed class type check works`() {
        val effects: List<ChatUiEffect> = listOf(
            ChatUiEffect.ShowToast("msg"),
            ChatUiEffect.RequestBluetoothPermission,
            ChatUiEffect.NotifyBleDisabled
        )

        var showToastCount   = 0
        var permissionCount  = 0
        var bleDisabledCount = 0

        effects.forEach { effect ->
            when (effect) {
                is ChatUiEffect.ShowToast                  -> showToastCount++
                is ChatUiEffect.RequestBluetoothPermission -> permissionCount++
                is ChatUiEffect.NotifyBleDisabled          -> bleDisabledCount++
            }
        }

        assertEquals(1, showToastCount)
        assertEquals(1, permissionCount)
        assertEquals(1, bleDisabledCount)
    }

    // ── ShowToast は ChatUiEffect のサブタイプ ────────────────────
    @Test
    fun `ShowToast is ChatUiEffect`() {
        val effect: ChatUiEffect = ChatUiEffect.ShowToast("x")
        assertTrue(effect is ChatUiEffect.ShowToast)
    }
}
