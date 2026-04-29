package net.moonmile.ble5_chat.claude.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.moonmile.ble5_chat.claude.ble.MessageCodec
import net.moonmile.ble5_chat.claude.model.ChatMessage
import net.moonmile.ble5_chat.claude.model.ChatUiState

@Composable
fun ChatScreen(
    state: ChatUiState,
    selfId: String,
    favoriteIds: Set<String>,
    onSend: (String) -> Unit,
    onInputChanged: (String) -> Unit,
    onToggleFavorite: (ChatMessage) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        ChatHeader(
            state                 = state,
            onNavigateToSettings  = onNavigateToSettings,
            onNavigateToFavorites = onNavigateToFavorites,
            modifier              = Modifier.fillMaxWidth()
        )
        MessageList(
            messages         = state.messages,
            selfId           = selfId,
            favoriteIds      = favoriteIds,
            onToggleFavorite = onToggleFavorite,
            modifier         = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        MessageInputBar(
            inputText    = state.inputText,
            canSend      = state.canSend,
            onTextChange = { text -> onInputChanged(text.take(MessageCodec.MAX_TEXT_LENGTH)) },
            onSendClick  = { onSend(state.inputText) },
            modifier     = Modifier.fillMaxWidth()
        )
    }
}
