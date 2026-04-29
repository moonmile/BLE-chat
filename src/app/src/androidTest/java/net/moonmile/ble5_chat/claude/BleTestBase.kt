package net.moonmile.ble5_chat.claude

import android.content.Intent
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Before

/**
 * BLE チャットテストの共通基底クラス。
 *
 * 起動モードが 2 種類ある:
 *   - setUp()              … アプリを完全に再起動（BLE 初期化からやり直す）
 *   - attachToRunningApp() … 既に起動しているアプリにアタッチ（BLE 状態を維持）
 *
 * attachToRunningApp() を使う場合は、サブクラスで setUp() をオーバーライドして呼ぶ。
 */
abstract class BleTestBase {

    protected lateinit var device: UiDevice

    companion object {
        const val PACKAGE        = "net.moonmile.ble5_chat.claude"
        const val LAUNCH_TIMEOUT = 8_000L   // アプリ起動待機 (ms)
        const val BLE_INIT_WAIT  = 2_000L   // BLE 初期化待機 (ms)  ※フル起動時のみ使用
        const val ATTACH_WAIT    = 500L     // アタッチ後の UI 安定待機 (ms)
        const val SEND_SETTLE    = 500L     // 送信後の UI 安定待機 (ms)
        const val TAG            = "BleChatTest"
    }

    // ── フル起動モード ────────────────────────────────────────────
    /**
     * アプリを完全に再起動する。
     * タスクをクリアして MainActivity を新規起動し、BLE 初期化まで待機する。
     */
    @Before
    open fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)

        // ホームに戻してから起動（前回の状態をリセット）
        device.pressHome()

        val intent = instrumentation.context.packageManager
            .getLaunchIntentForPackage(PACKAGE)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) }
            ?: error("パッケージが見つかりません: $PACKAGE")

        instrumentation.context.startActivity(intent)

        // アプリウィンドウが表示されるまで待機
        device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), LAUNCH_TIMEOUT)

        // BLE 初期化（スキャン開始）を待機
        Thread.sleep(BLE_INIT_WAIT)

        Log.i(TAG, "setUp: アプリ起動完了（フル起動）")
    }

    // ── アタッチモード ────────────────────────────────────────────
    /**
     * 既に起動しているアプリにアタッチする。
     *
     * FLAG_ACTIVITY_CLEAR_TASK を付けずに startActivity() を呼ぶことで、
     * 既存のタスクをそのままフォアグラウンドに戻す。
     * BLE はすでに初期化・スキャン中の状態が維持される。
     *
     * サブクラスで setUp() をオーバーライドして呼ぶ:
     *
     *   @Before
     *   override fun setUp() = attachToRunningApp()
     */
    protected fun attachToRunningApp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)

        // フラグなし = 既存タスクをフォアグラウンドに戻すだけ（Activity は再生成されない）
        val intent = instrumentation.context.packageManager
            .getLaunchIntentForPackage(PACKAGE)
            ?: error("パッケージが見つかりません: $PACKAGE")
        // FLAG_ACTIVITY_CLEAR_TASK を意図的に付けない

        instrumentation.context.startActivity(intent)

        // ウィンドウがフォアグラウンドに来るまで待機
        device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), LAUNCH_TIMEOUT)

        // UI 安定待機のみ（BLE 再初期化は不要）
        Thread.sleep(ATTACH_WAIT)

        Log.i(TAG, "attachToRunningApp: 既存アプリにアタッチ完了")
    }

    /**
     * メッセージを入力して送信する。
     *
     * @param text 送信するテキスト
     * @throws IllegalStateException 入力欄または送信ボタンが見つからない場合
     */
    protected fun sendMessage(text: String) {
        // テキスト入力欄を取得（Compose の OutlinedTextField は EditText として見える）
        val inputField = device.findObject(By.clazz("android.widget.EditText"))
            ?: error("入力フィールドが見つかりません。画面が正しく表示されているか確認してください。")

        inputField.clear()
        inputField.setText(text)

        // 送信ボタンをタップ（contentDescription = "送信"）
        val sendButton = device.findObject(By.desc("送信"))
            ?: error("送信ボタンが見つかりません。BLE が ON になっているか確認してください。")

        sendButton.click()

        // UI が安定するまで待機
        Thread.sleep(SEND_SETTLE)
    }
}
