package net.moonmile.ble5_chat.copilot.model

/**
 * UI 操作イベント
 */
sealed class ChatUiEvent {
    data class OnInputChanged(val text: String) : ChatUiEvent()
    object OnSendClicked : ChatUiEvent()
    object OnStartChat : ChatUiEvent()
    object OnStopChat : ChatUiEvent()
}
