package net.moonmile.ble5_chat.copilot.model

/**
 * 1 回性イベント（副作用）
 */
sealed class ChatUiEffect {
    data class ShowToast(val message: String) : ChatUiEffect()
    object RequestBluetoothPermission : ChatUiEffect()
    object NotifyBleDisabled : ChatUiEffect()
}
