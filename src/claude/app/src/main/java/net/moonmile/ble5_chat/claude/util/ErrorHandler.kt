package net.moonmile.ble5_chat.claude.util

sealed class ChatError {
    object BleDisabled : ChatError()
    object PermissionDenied : ChatError()
    data class AdvertiseError(val errorCode: Int) : ChatError()
    data class ScanError(val errorCode: Int) : ChatError()
    data class Unknown(val throwable: Throwable) : ChatError()
}

class ErrorHandler {
    fun onBleStateOff(): ChatError = ChatError.BleDisabled
    fun onPermissionDenied(): ChatError = ChatError.PermissionDenied
    fun onAdvertiseError(errorCode: Int): ChatError = ChatError.AdvertiseError(errorCode)
    fun onScanError(errorCode: Int): ChatError = ChatError.ScanError(errorCode)

    fun shouldRetry(error: ChatError): Boolean = when (error) {
        is ChatError.Unknown -> true
        else -> false
    }

    fun toUserMessage(error: ChatError): String = when (error) {
        is ChatError.BleDisabled -> "Bluetoothがオフになりました"
        is ChatError.PermissionDenied -> "Bluetoothの権限を許可してください"
        is ChatError.AdvertiseError -> "広告送信に失敗しました (エラー: ${error.errorCode})"
        is ChatError.ScanError -> "スキャンに失敗しました (エラー: ${error.errorCode})"
        is ChatError.Unknown -> "エラーが発生しました: ${error.throwable.message}"
    }
}
