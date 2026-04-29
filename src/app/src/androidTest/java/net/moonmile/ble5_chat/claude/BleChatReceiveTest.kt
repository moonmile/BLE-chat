package net.moonmile.ble5_chat.claude

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BLE チャット 受信確認テスト（受信側端末で実行する）
 *
 * 受信メッセージを logcat に出力する。以下の adb コマンドでリアルタイム確認できる:
 *
 *   adb -s <SERIAL_B> logcat -s BleChatTest:I "*:S"
 *
 * テスト実行コマンド例:
 *
 *   adb -s <SERIAL_B> shell am instrument -w \
 *     -e class net.moonmile.ble5_chat.claude.BleChatReceiveTest#dumpReceivedMessages \
 *     net.moonmile.ble5_chat.claude.test/androidx.test.runner.AndroidJUnitRunner
 *
 * 送信テストと同時に実行する場合は scripts/run_ble_e2e.ps1 を参照。
 */
@RunWith(AndroidJUnit4::class)
class BleChatReceiveTest : BleTestBase() {

    companion object {
        /** 受信待機時間（送信テストの所要時間に合わせて調整） */
        private const val RECEIVE_WAIT_MS = 15_000L

        /** スクロール1回あたりの最大待機 (ms) */
        private const val SCROLL_WAIT_MS = 500L

        // UI の固定ラベルテキスト（受信メッセージの判定から除外する）
        private val EXCLUDE_STRINGS = setOf(
            "Bluetooth ON", "Bluetooth OFF",
            "メッセージがありません",
            "お気に入りはまだありません",
            "メッセージを入力",
            "Bluetoothをオンにしてください",
            "設定", "戻る", "お気に入り", "著作権情報",
            "送信者ID", "BLE 設定", "アプリ情報",
            "端末設定", "バージョン", "保存", "キャンセル",
        )
        private val EXCLUDE_PATTERNS = listOf(
            Regex("""残り\s*\d+\s*文字"""),      // "残り N 文字"
            Regex("""👥\s*\d+人"""),              // "👥 N人"
            Regex("""^[●⟳]"""),                  // BLE 状態インジケータ
            Regex("""^\d+件"""),                  // "N件"
            Regex("""\w{1,8}\s{2,}\d{2}:\d{2}"""), // "senderId  HH:mm"（メッセージラベル行）
            Regex("""^\d{2}:\d{2}$"""),           // "HH:mm"（自分のメッセージラベル）
        )
    }

    // ── テスト4: 受信メッセージを adb logcat に出力 ────────────────
    @Test
    fun dumpReceivedMessages() {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        Log.i(TAG, "=== 受信確認開始 ($timestamp) ===")
        Log.i(TAG, "[INFO] ${RECEIVE_WAIT_MS / 1000} 秒間 BLE 受信を待機します...")

        // 送信側のテスト実行に合わせて待機
        Thread.sleep(RECEIVE_WAIT_MS)

        // リストの先頭に戻してからメッセージを収集
        val messages = collectMessagesFromList()

        // 結果を logcat に出力
        val endTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        Log.i(TAG, "=== 受信メッセージ一覧 ($endTime) ===")

        if (messages.isEmpty()) {
            Log.i(TAG, "[RECV] メッセージなし（BLE が届いていないか、送信前に実行した可能性があります）")
        } else {
            messages.forEachIndexed { index, text ->
                Log.i(TAG, "[RECV][${index + 1}/${messages.size}] $text")
            }
        }

        Log.i(TAG, "=== 合計 ${messages.size} 件受信 ===")
    }

    // ── メッセージリストのテキストを収集 ─────────────────────────────
    private fun collectMessagesFromList(): List<String> {
        val collected = mutableListOf<String>()
        val seen      = mutableSetOf<String>()

        val scrollable = UiScrollable(
            UiSelector().className("androidx.recyclerview.widget.RecyclerView")
        ).also { it.setMaxSearchSwipes(20) }

        // スクロールが効かない場合（RecyclerView が見つからない）は
        // LazyColumn の scrollable コンテナを別途探す
        val hasScrollable = scrollable.exists()

        if (hasScrollable) {
            // 先頭に戻る
            scrollable.flingToBeginning(5)
            Thread.sleep(SCROLL_WAIT_MS)

            // 末尾までスクロールしながらテキストを収集
            var canScroll = true
            while (canScroll) {
                harvestVisibleTexts(seen, collected)
                canScroll = scrollable.scrollForward()
                Thread.sleep(SCROLL_WAIT_MS)
            }
            // 最後のページも収集
            harvestVisibleTexts(seen, collected)

        } else {
            // スクロールなし（件数が少なく画面内に収まっている場合）
            harvestVisibleTexts(seen, collected)
        }

        return collected
    }

    /** 現在画面に表示されているテキストノードから受信メッセージを抽出する */
    private fun harvestVisibleTexts(
        seen: MutableSet<String>,
        results: MutableList<String>
    ) {
        device.findObjects(By.clazz("android.widget.TextView"))
            .mapNotNull { it.text }
            .filter { it.isMessageCandidate() }
            .forEach { text ->
                if (seen.add(text)) {   // 重複排除
                    results.add(text)
                }
            }
    }

    /** UI の固定ラベルを除外し、メッセージ本文らしいテキストかどうかを判定する */
    private fun String.isMessageCandidate(): Boolean {
        if (isBlank()) return false
        if (EXCLUDE_STRINGS.any { this.contains(it) }) return false
        if (EXCLUDE_PATTERNS.any { it.containsMatchIn(this) }) return false
        return true
    }
}
