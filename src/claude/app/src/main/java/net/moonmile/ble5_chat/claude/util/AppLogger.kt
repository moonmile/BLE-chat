package net.moonmile.ble5_chat.claude.util

import android.util.Log

object AppLogger {
    private const val ROOT_TAG = "BLE5Chat"

    fun d(tag: String, message: String) = Log.d("$ROOT_TAG/$tag", message)
    fun i(tag: String, message: String) = Log.i("$ROOT_TAG/$tag", message)
    fun w(tag: String, message: String) = Log.w("$ROOT_TAG/$tag", message)
    fun e(tag: String, message: String, throwable: Throwable? = null) =
        Log.e("$ROOT_TAG/$tag", message, throwable)
}
