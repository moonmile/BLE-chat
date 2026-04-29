package net.moonmile.ble5_chat.claude

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BLE チャット 送信テスト（送信側端末で実行する）
 *
 * 【前提】端末でアプリを手動で起動し、チャット画面を表示した状態にしてから実行する。
 * テスト開始時にアプリを再起動せず、既存の BLE セッションにアタッチして送信する。
 *
 * 実行コマンド例（<SERIAL_A> は adb devices で確認）:
 *
 *   # 1回だけ送信
 *   adb -s <SERIAL_A> shell am instrument -w \
 *     -e class net.moonmile.ble5_chat.claude.BleChatSendTest#sendHelloOnce \
 *     net.moonmile.ble5_chat.claude.test/androidx.test.runner.AndroidJUnitRunner
 *
 *   # 1秒おきに10回送信
 *   adb -s <SERIAL_A> shell am instrument -w \
 *     -e class net.moonmile.ble5_chat.claude.BleChatSendTest#sendHelloTenTimesEverySecond \
 *     net.moonmile.ble5_chat.claude.test/androidx.test.runner.AndroidJUnitRunner
 *
 *   # 5秒おきに5回送信
 *   adb -s <SERIAL_A> shell am instrument -w \
 *     -e class net.moonmile.ble5_chat.claude.BleChatSendTest#sendHelloFiveTimesEveryFiveSeconds \
 *     net.moonmile.ble5_chat.claude.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class BleChatSendTest : BleTestBase() {

    /**
     * アプリを再起動せず、既に起動している BLE セッションにアタッチする。
     * BLE の初期化・スキャン状態は維持されるため、送信可能になるまでの待機が不要。
     */
    @Before
    override fun setUp() = attachToRunningApp()

    // ── テスト1: 「Hello」を1回送信 ────────────────────────────────
    @Test
    fun sendHelloOnce() {
        Log.i(TAG, "=== sendHelloOnce 開始 ===")

        sendMessage("Hello")
        Log.i(TAG, "[SEND] Hello")

        Log.i(TAG, "=== sendHelloOnce 完了 ===")
    }

    // ── テスト2: 「Hello + 通番」を1秒おきに10回送信 ───────────────
    @Test
    fun sendHelloTenTimesEverySecond() {
        val total    = 10
        val interval = 1_000L   // 1秒

        Log.i(TAG, "=== sendHelloTenTimesEverySecond 開始（${interval}ms 間隔 × $total 回）===")

        repeat(total) { index ->
            val seq = index + 1
            val msg = "Hello $seq"

            sendMessage(msg)
            Log.i(TAG, "[SEND][$seq/$total] $msg")

            if (seq < total) {
                Thread.sleep(interval)
            }
        }

        Log.i(TAG, "=== sendHelloTenTimesEverySecond 完了 ===")
    }

    // ── テスト3: 「Hello + 通番」を5秒おきに5回送信 ────────────────
    @Test
    fun sendHelloFiveTimesEveryFiveSeconds() {
        val total    = 5
        val interval = 5_000L   // 5秒

        Log.i(TAG, "=== sendHelloFiveTimesEveryFiveSeconds 開始（${interval}ms 間隔 × $total 回）===")

        repeat(total) { index ->
            val seq = index + 1
            val msg = "Hello $seq"

            sendMessage(msg)
            Log.i(TAG, "[SEND][$seq/$total] $msg")

            if (seq < total) {
                Thread.sleep(interval)
            }
        }

        Log.i(TAG, "=== sendHelloFiveTimesEveryFiveSeconds 完了 ===")
    }
}
