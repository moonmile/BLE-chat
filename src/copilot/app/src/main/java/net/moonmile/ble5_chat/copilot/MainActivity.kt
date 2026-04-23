package net.moonmile.ble5_chat.copilot

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.moonmile.ble5_chat.copilot.model.ChatMessage
import net.moonmile.ble5_chat.copilot.model.ChatUiEffect
import net.moonmile.ble5_chat.copilot.model.ChatUiState
import net.moonmile.ble5_chat.copilot.ui.ChatScreen
import net.moonmile.ble5_chat.copilot.ui.theme.BLE5chatTheme
import net.moonmile.ble5_chat.copilot.util.AppLogger
import net.moonmile.ble5_chat.copilot.util.MessageCodec
import java.util.UUID

/**
 * メインアクティビティ
 *
 * - Compose エントリポイント
 * - ChatRepository の Flow を collectAsStateWithLifecycle() で収集し ChatUiState を保持
 * - ライフサイクル連携
 */
class MainActivity : ComponentActivity() {
    private val selfId = UUID.randomUUID().toString().substring(0, 8)

    // ViewModel に相当する状態管理
    private val _uiState = MutableStateFlow(ChatUiState(canSend = false, peerCount = 0))
    private val uiState = _uiState.asStateFlow()

    private val _uiEffects = MutableSharedFlow<ChatUiEffect>()
    private val uiEffects = _uiEffects.asSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BLE5chatTheme {
                val state = uiState.collectAsState().value

                // Effect を監視
                LaunchedEffect(Unit) {
                    uiEffects.collect { effect ->
                        handleEffect(effect)
                    }
                }

                ChatScreenContainer(
                    state = state,
                    onSend = ::handleSendMessage,
                    onInputChange = ::handleInputChange,
                    onStart = ::handleStartChat,
                    onStop = ::handleStopChat,
                    selfId = selfId,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 初期化処理
        lifecycleScope.launch {
            AppLogger.info("MainActivity created with selfId=$selfId")
            // TODO: BLE 初期化処理
        }
    }

    private fun handleSendMessage() {
        val text = _uiState.value.inputText
        if (text.isNotBlank()) {
            lifecycleScope.launch {
                try {
                    val message = ChatMessage(
                        senderId = selfId,
                        text = MessageCodec.truncate(text)
                    )
                    AppLogger.debug("Sending message: ${message.text}")
                    // TODO: ChatRepository.publishMessage(message)

                    // UI を更新
                    _uiState.value = _uiState.value.copy(inputText = "")

                    // メッセージをローカル表示
                    val messages = _uiState.value.messages.toMutableList()
                    messages.add(message)
                    _uiState.value = _uiState.value.copy(messages = messages)
                } catch (e: Exception) {
                    AppLogger.error("Failed to send message", e)
                    _uiEffects.emit(ChatUiEffect.ShowToast("メッセージ送信に失敗しました"))
                }
            }
        }
    }

    private fun handleInputChange(text: String) {
        val truncated = MessageCodec.truncate(text)
        _uiState.value = _uiState.value.copy(inputText = truncated)
    }

    private fun handleStartChat() {
        lifecycleScope.launch {
            try {
                AppLogger.info("Starting chat")
                _uiState.value = _uiState.value.copy(canSend = true, isScanning = true)
                // TODO: BleChatService.start()
            } catch (e: Exception) {
                AppLogger.error("Failed to start chat", e)
                _uiEffects.emit(ChatUiEffect.ShowToast("チャット開始に失敗しました"))
            }
        }
    }

    private fun handleStopChat() {
        lifecycleScope.launch {
            try {
                AppLogger.info("Stopping chat")
                _uiState.value = _uiState.value.copy(canSend = false, isScanning = false)
                // TODO: BleChatService.stop()
            } catch (e: Exception) {
                AppLogger.error("Failed to stop chat", e)
                _uiEffects.emit(ChatUiEffect.ShowToast("チャット停止に失敗しました"))
            }
        }
    }

    private fun handleEffect(effect: ChatUiEffect) {
        when (effect) {
            is ChatUiEffect.ShowToast -> {
                Toast.makeText(this, effect.message, Toast.LENGTH_SHORT).show()
            }

            is ChatUiEffect.NotifyBleDisabled -> {
                _uiState.value = _uiState.value.copy(
                    canSend = false,
                    isScanning = false,
                    errorMessage = "Bluetooth がオフになりました"
                )
                Toast.makeText(this, "Bluetooth がオフになりました", Toast.LENGTH_SHORT).show()
            }

            is ChatUiEffect.RequestBluetoothPermission -> {
                Toast.makeText(this, "Bluetooth の権限を許可してください", Toast.LENGTH_SHORT).show()
                // TODO: 権限リクエスト
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.info("MainActivity destroyed")
        // TODO: BLE クリーンアップ
    }
}

/**
 * Compose 画面コンテナ
 */
@Composable
private fun ChatScreenContainer(
    state: ChatUiState,
    onSend: () -> Unit,
    onInputChange: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    selfId: String,
    modifier: Modifier = Modifier
) {
    ChatScreen(
        state = state,
        onSend = onSend,
        onStart = onStart,
        onStop = onStop,
        onInputChange = onInputChange,
        selfId = selfId,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    BLE5chatTheme {
        ChatScreenContainer(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(senderId = "peer001", text = "Hello!"),
                    ChatMessage(senderId = "self123", text = "Hi there!")
                ),
                inputText = "",
                canSend = true,
                isScanning = true,
                peerCount = 2
            ),
            onSend = {},
            onInputChange = {},
            onStart = {},
            onStop = {},
            selfId = "self123",
            modifier = Modifier.fillMaxSize()
        )
    }
}