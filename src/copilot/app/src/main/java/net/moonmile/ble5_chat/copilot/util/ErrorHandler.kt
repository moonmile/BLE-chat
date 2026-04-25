package net.moonmile.ble5_chat.copilot.util

import net.moonmile.ble5_chat.copilot.model.ChatUiEffect

/**
 * ドメインエラーとその対応する UI Effect
 */
sealed class ChatError(override val message: String) : Exception(message) {
    object BleDisabled : ChatError("Bluetooth がオフになりました")
    object BleNotAvailable : ChatError("このデバイスは Bluetooth をサポートしていません")
    object PermissionDenied : ChatError("Bluetooth の権限を許可してください")
    object BleAdapterError : ChatError("Bluetooth アダプタエラーが発生しました")
    data class UnknownError(override val message: String) : ChatError(message)
}

/**
 * エラーハンドリング・復旧判定
 */
object ErrorHandler {
    /**
     * BLE エラーを UI Effect に変換
     *
     * @param error ChatError
     * @return 対応する ChatUiEffect
     */
    fun toChatUiEffect(error: ChatError): ChatUiEffect {
        return when (error) {
            is ChatError.BleDisabled -> ChatUiEffect.NotifyBleDisabled
            is ChatError.PermissionDenied -> ChatUiEffect.RequestBluetoothPermission
            else -> ChatUiEffect.ShowToast(error.message)
        }
    }

    /**
     * エラーがリトライ可能かどうかを判定
     *
     * @param error ChatError
     * @return true ならリトライ可能
     */
    fun isRetryable(error: ChatError): Boolean {
        return when (error) {
            is ChatError.BleDisabled -> true
            is ChatError.BleAdapterError -> true
            else -> false
        }
    }
}
