package net.moonmile.ble5_chat.claude.model

sealed class ChatUiEvent {
    data class OnInputChanged(val text: String) : ChatUiEvent()
    object OnSendClicked : ChatUiEvent()
    object OnStartChat : ChatUiEvent()
    object OnStopChat : ChatUiEvent()
}
