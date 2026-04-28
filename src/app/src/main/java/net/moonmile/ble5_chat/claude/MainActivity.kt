package net.moonmile.ble5_chat.claude

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.moonmile.ble5_chat.claude.ble.BleAdvertiserManager
import net.moonmile.ble5_chat.claude.ble.BleChatService
import net.moonmile.ble5_chat.claude.ble.BleScannerManager
import net.moonmile.ble5_chat.claude.ble.DuplicateFilter
import net.moonmile.ble5_chat.claude.ble.MessageCodec
import net.moonmile.ble5_chat.claude.ble.PeerRegistry
import net.moonmile.ble5_chat.claude.model.ChatMessage
import net.moonmile.ble5_chat.claude.model.ChatUiEffect
import net.moonmile.ble5_chat.claude.model.ChatUiState
import net.moonmile.ble5_chat.claude.repository.ChatRepository
import net.moonmile.ble5_chat.claude.repository.ChatRepositoryImpl
import net.moonmile.ble5_chat.claude.ui.ChatScreen
import net.moonmile.ble5_chat.claude.ui.CopyrightScreen
import net.moonmile.ble5_chat.claude.ui.SettingsScreen
import net.moonmile.ble5_chat.claude.ui.theme.BLE5ChatClaudeTheme
import net.moonmile.ble5_chat.claude.util.AppLogger
import net.moonmile.ble5_chat.claude.util.ChatError
import net.moonmile.ble5_chat.claude.util.DefaultDispatcherProvider
import net.moonmile.ble5_chat.claude.util.ErrorHandler
import java.util.UUID

private enum class AppScreen {
    CHAT,
    SETTINGS,
    COPYRIGHT
}

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    private val preferences by lazy { getPreferences(Context.MODE_PRIVATE) }
    private val selfIdState = MutableStateFlow("")

    private val dispatchers = DefaultDispatcherProvider()
    private val duplicateFilter = DuplicateFilter()
    private val peerRegistry = PeerRegistry()
    private val errorHandler = ErrorHandler()

    private val advertiser by lazy { BleAdvertiserManager(this, dispatchers) }
    private val scanner by lazy { BleScannerManager(this, duplicateFilter) }
    private val bleService by lazy {
        BleChatService(this, advertiser, scanner, peerRegistry, errorHandler)
    }
    private val repository: ChatRepository by lazy { ChatRepositoryImpl(bleService) }

    private val uiState = MutableStateFlow(ChatUiState())
    private val effects = MutableSharedFlow<ChatUiEffect>(extraBufferCapacity = 16)

    // Permission request launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            initializeBle()
        } else {
            uiState.update { it.copy(canSend = false, isScanning = false) }
            lifecycleScope.launch {
                effects.emit(ChatUiEffect.RequestBluetoothPermission)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ensureSelfIdInitialized()

        collectEffects()
        requestBlePermissionsOrInit()

        setContent {
            BLE5ChatClaudeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by uiState.collectAsState()
                    val selfId by selfIdState.collectAsState()
                    var currentScreen by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(AppScreen.CHAT)
                    }

                    when (currentScreen) {
                        AppScreen.CHAT -> {
                            ChatScreen(
                                state = state,
                                selfId = selfId,
                                onSend = { text -> sendMessage(text) },
                                onInputChanged = { text ->
                                    uiState.update { it.copy(inputText = text) }
                                },
                                onStart = { repository.start() },
                                onStop = { repository.stop() },
                                onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                                modifier = Modifier.statusBarsPadding()
                            )
                        }

                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                selfId = selfId,
                                onSelfIdChange = { updateSelfId(it) },
                                onNavigateToCopyright = { currentScreen = AppScreen.COPYRIGHT },
                                onNavigateBack = { currentScreen = AppScreen.CHAT },
                                modifier = Modifier.statusBarsPadding()
                            )
                        }

                        AppScreen.COPYRIGHT -> {
                            CopyrightScreen(
                                onNavigateBack = { currentScreen = AppScreen.SETTINGS },
                                modifier = Modifier.statusBarsPadding()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleService.release()
    }

    private fun requestBlePermissionsOrInit() {
        val required = requiredPermissions()
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            initializeBle()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun requiredPermissions(): List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun initializeBle() {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bleEnabled = btManager.adapter?.isEnabled == true

        uiState.update { it.copy(canSend = bleEnabled, isScanning = bleEnabled) }

        if (bleEnabled) {
            repository.start()
        } else {
            lifecycleScope.launch { effects.emit(ChatUiEffect.NotifyBleDisabled) }
        }

        // Observe incoming messages
        lifecycleScope.launch {
            repository.observeMessages().collect { message ->
                peerRegistry.update(message.senderId)
                uiState.update { state ->
                    state.copy(messages = state.messages + message)
                }
                AppLogger.d(TAG, "Received: ${message.messageId} from ${message.senderId}")
            }
        }

        // Observe BLE errors
        lifecycleScope.launch {
            repository.observeErrors().collect { error ->
                handleBleError(error)
            }
        }

        // Observe peer count
        lifecycleScope.launch {
            peerRegistry.peerCount.collect { count ->
                uiState.update { it.copy(peerCount = count) }
            }
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return
        val selfId = selfIdState.value
        val message = ChatMessage(
            messageId = UUID.randomUUID().toString(),
            senderId = selfId,
            timestamp = System.currentTimeMillis(),
            text = text.take(MessageCodec.MAX_TEXT_LENGTH)
        )
        // Add to local list immediately so sender sees their own message
        uiState.update { state ->
            state.copy(
                messages = state.messages + message,
                inputText = ""
            )
        }
        repository.publishMessage(message)
    }

    private fun ensureSelfIdInitialized() {
        val savedId = preferences.getString("selfId", null)
        val initialId = if (!savedId.isNullOrBlank()) {
            savedId
        } else {
            UUID.randomUUID().toString().take(8).also { generatedId ->
                preferences.edit().putString("selfId", generatedId).apply()
            }
        }
        selfIdState.value = initialId
    }

    private fun updateSelfId(newId: String) {
        val normalizedId = newId.trim().take(8)
        if (normalizedId.isBlank()) return
        selfIdState.value = normalizedId
        preferences.edit().putString("selfId", normalizedId).apply()
    }

    private fun handleBleError(error: ChatError) {
        AppLogger.e(TAG, "BLE error: $error")
        when (error) {
            is ChatError.BleDisabled -> {
                uiState.update { it.copy(canSend = false, isScanning = false) }
                lifecycleScope.launch { effects.emit(ChatUiEffect.NotifyBleDisabled) }
            }
            is ChatError.PermissionDenied -> {
                uiState.update { it.copy(canSend = false, isScanning = false) }
                lifecycleScope.launch { effects.emit(ChatUiEffect.RequestBluetoothPermission) }
            }
            else -> {
                val msg = errorHandler.toUserMessage(error)
                lifecycleScope.launch { effects.emit(ChatUiEffect.ShowToast(msg)) }
            }
        }
    }

    private fun collectEffects() {
        lifecycleScope.launch {
            effects.collect { effect ->
                when (effect) {
                    is ChatUiEffect.ShowToast ->
                        Toast.makeText(this@MainActivity, effect.message, Toast.LENGTH_SHORT).show()
                    is ChatUiEffect.NotifyBleDisabled ->
                        Toast.makeText(this@MainActivity, "Bluetoothがオフになりました", Toast.LENGTH_SHORT).show()
                    is ChatUiEffect.RequestBluetoothPermission ->
                        Toast.makeText(this@MainActivity, "Bluetoothの権限を許可してください", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
