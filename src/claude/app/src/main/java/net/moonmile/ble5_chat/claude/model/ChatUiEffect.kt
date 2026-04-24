package net.moonmile.ble5_chat.claude.model

sealed class ChatUiEffect {
    data class ShowToast(val message: String) : ChatUiEffect()
    object RequestBluetoothPermission : ChatUiEffect()
    object NotifyBleDisabled : ChatUiEffect()
}
