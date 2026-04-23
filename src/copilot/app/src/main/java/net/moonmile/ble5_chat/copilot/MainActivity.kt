package net.moonmile.ble5_chat.copilot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.moonmile.ble5_chat.copilot.ble.BleChatService
import net.moonmile.ble5_chat.copilot.ble.BleChatServiceImpl
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
    private lateinit var bleChatService: BleChatService

    // ViewModel に相当する状態管理
    private val _uiState = MutableStateFlow(ChatUiState(canSend = false, peerCount = 0))
    private val uiState = _uiState.asStateFlow()

    private val _uiEffects = MutableSharedFlow<ChatUiEffect>()
    private val uiEffects = _uiEffects.asSharedFlow()

    // Android 12 以降の BLE 権限リクエスト
    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            AppLogger.info("BLE permissions granted")
            lifecycleScope.launch {
                bleChatService.start()
                bleChatService.refreshBleState()
            }
        } else {
            AppLogger.warn("BLE permissions denied: $results")
            lifecycleScope.launch {
                _uiEffects.emit(ChatUiEffect.RequestBluetoothPermission)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bleChatService = BleChatServiceImpl(applicationContext, selfId)
        observeBleService()

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

        // 権限確認 → BLE 初期状態を反映
        checkAndRequestBlePermissions()
    }

    private fun observeBleService() {
        lifecycleScope.launch {
            bleChatService.observeBleEnabled().collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(
                    canSend = enabled,
                    isScanning = enabled,
                    errorMessage = if (enabled) null else "Bluetooth をオンにしてください"
                )
            }
        }

        lifecycleScope.launch {
            bleChatService.observeEffects().collectLatest { effect ->
                _uiEffects.emit(effect)
            }
        }

        lifecycleScope.launch {
            bleChatService.observePeerCount().collectLatest { count ->
                _uiState.value = _uiState.value.copy(peerCount = count)
            }
        }

        lifecycleScope.launch {
            bleChatService.incomingMessages().collect { message ->
                val messages = _uiState.value.messages.toMutableList()
                messages.add(message)
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
    }

    /**
     * BLE 利用に必要な権限を確認してリクエスト
     */
    private fun checkAndRequestBlePermissions() {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            AppLogger.info("BLE permissions already granted")
            lifecycleScope.launch {
                bleChatService.start()
                bleChatService.refreshBleState()
            }
        } else {
            AppLogger.info("Requesting BLE permissions: $missing")
            blePermissionLauncher.launch(missing.toTypedArray())
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
                    bleChatService.send(message)
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
                bleChatService.start()
                bleChatService.refreshBleState()
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
                bleChatService.stop()
                _uiState.value = _uiState.value.copy(canSend = false, isScanning = false)
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
                Toast.makeText(this, "Bluetooth の権限を許可してください", Toast.LENGTH_LONG).show()
                _uiState.value = _uiState.value.copy(
                    canSend = false,
                    errorMessage = "Bluetooth の権限が必要です"
                )
            }
        }
    }

    override fun onDestroy() {
        if (::bleChatService.isInitialized) {
            lifecycleScope.launch {
                bleChatService.stop()
            }
        }
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