package net.moonmile.ble5_chat.copilot.util

import android.util.Log

/**
 * アプリケーション全体のログ出力
 */
object AppLogger {
    private const val TAG = "BLE5Chat"

    fun debug(message: String) {
        Log.d(TAG, message)
    }

    fun info(message: String) {
        Log.i(TAG, message)
    }

    fun warn(message: String, throwable: Throwable? = null) {
        Log.w(TAG, message, throwable)
    }

    fun error(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
