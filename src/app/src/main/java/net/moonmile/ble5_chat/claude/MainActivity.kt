package net.moonmile.ble5_chat.claude

import android.Manifest
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
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import net.moonmile.ble5_chat.claude.repository.FavoriteRepository
import net.moonmile.ble5_chat.claude.ui.ChatScreen
import net.moonmile.ble5_chat.claude.ui.CopyrightScreen
import net.moonmile.ble5_chat.claude.ui.FavoritesScreen
import net.moonmile.ble5_chat.claude.ui.SettingsScreen
import net.moonmile.ble5_chat.claude.ui.theme.BLE5ChatClaudeTheme
import net.moonmile.ble5_chat.claude.util.AppLogger
import net.moonmile.ble5_chat.claude.util.ChatError
import net.moonmile.ble5_chat.claude.util.DefaultDispatcherProvider
import net.moonmile.ble5_chat.claude.util.ErrorHandler
import java.util.UUID

// ── ルート定義 ────────────────────────────────────────────────────
private object Route {
    const val CHAT      = "chat"
    const val SETTINGS  = "settings"
    const val COPYRIGHT = "copyright"
    const val FAVORITES = "favorites"
}

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private val prefs by lazy { getPreferences(Context.MODE_PRIVATE) }

    /** selfId を Flow で保持して設定画面の変更をチャット画面にも即反映 */
    private val selfIdFlow: MutableStateFlow<String> by lazy {
        val stored = prefs.getString("selfId", null)
            ?: UUID.randomUUID().toString().take(8).also { saveSelfId(it) }
        MutableStateFlow(stored)
    }

    private val dispatchers    = DefaultDispatcherProvider()
    private val duplicateFilter = DuplicateFilter()
    private val peerRegistry   = PeerRegistry()
    private val errorHandler   = ErrorHandler()

    private val advertiser by lazy { BleAdvertiserManager(this, dispatchers) }
    private val scanner    by lazy { BleScannerManager(this, duplicateFilter) }
    private val bleService by lazy {
        BleChatService(this, advertiser, scanner, peerRegistry, errorHandler)
    }
    private val repository: ChatRepository by lazy { ChatRepositoryImpl(bleService) }
    private val favoriteRepository by lazy { FavoriteRepository(this) }

    private val uiState = MutableStateFlow(ChatUiState())
    private val effects = MutableSharedFlow<ChatUiEffect>(extraBufferCapacity = 16)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            initializeBle()
        } else {
            uiState.update { it.copy(canSend = false, isScanning = false) }
            lifecycleScope.launch { effects.emit(ChatUiEffect.RequestBluetoothPermission) }
        }
    }

    // ── ライフサイクル ────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        collectEffects()
        requestBlePermissionsOrInit()

        setContent {
            BLE5ChatClaudeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val state  by uiState.collectAsState()
                    val selfId by selfIdFlow.collectAsState()
                    val favorites by favoriteRepository.favorites.collectAsState()

                    NavHost(
                        navController    = navController,
                        startDestination = Route.CHAT,
                        modifier         = Modifier.statusBarsPadding()
                    ) {
                        // チャット画面
                        composable(Route.CHAT) {
                            ChatScreen(
                                state                = state,
                                selfId               = selfId,
                                favoriteIds          = favorites.map { it.messageId }.toSet(),
                                onSend               = { text -> sendMessage(text, selfId) },
                                onInputChanged       = { text ->
                                    uiState.update { it.copy(inputText = text) }
                                },
                                onStart              = { repository.start() },
                                onStop               = { repository.stop() },
                                onToggleFavorite     = { message -> favoriteRepository.toggle(message) },
                                onNavigateToFavorites = { navController.navigate(Route.FAVORITES) },
                                onNavigateToSettings = { navController.navigate(Route.SETTINGS) }
                            )
                        }
                        // 設定画面
                        composable(Route.SETTINGS) {
                            SettingsScreen(
                                selfId                = selfId,
                                onSelfIdChange        = { newId -> updateSelfId(newId) },
                                onNavigateToCopyright = { navController.navigate(Route.COPYRIGHT) },
                                onNavigateBack        = { navController.popBackStack() }
                            )
                        }
                        // 著作権情報画面
                        composable(Route.COPYRIGHT) {
                            CopyrightScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Route.FAVORITES) {
                            FavoritesScreen(
                                favorites = favorites,
                                onNavigateBack = { navController.popBackStack() },
                                onRemoveFavorite = { messageId -> favoriteRepository.remove(messageId) }
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

    // ── selfId 管理 ────────────────────────────────────────────────
    private fun updateSelfId(newId: String) {
        saveSelfId(newId)
        selfIdFlow.value = newId
        AppLogger.d(TAG, "selfId updated: $newId")
    }

    private fun saveSelfId(id: String) = prefs.edit().putString("selfId", id).apply()

    // ── 権限 / BLE 初期化 ──────────────────────────────────────────
    private fun requestBlePermissionsOrInit() {
        val missing = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) initializeBle() else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun requiredPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
        ) else listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    private fun initializeBle() {
        val btManager  = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bleEnabled = btManager.adapter?.isEnabled == true
        uiState.update { it.copy(canSend = bleEnabled, isScanning = bleEnabled) }
        if (bleEnabled) repository.start()
        else lifecycleScope.launch { effects.emit(ChatUiEffect.NotifyBleDisabled) }

        lifecycleScope.launch {
            repository.observeMessages().collect { message ->
                peerRegistry.update(message.senderId)
                uiState.update { it.copy(messages = it.messages + message) }
                AppLogger.d(TAG, "Received: ${message.messageId} from ${message.senderId}")
            }
        }
        lifecycleScope.launch { repository.observeErrors().collect { handleBleError(it) } }
        lifecycleScope.launch {
            peerRegistry.peerCount.collect { count -> uiState.update { it.copy(peerCount = count) } }
        }
    }

    // ── メッセージ送信 ──────────────────────────────────────────────
    private fun sendMessage(text: String, selfId: String) {
        if (text.isBlank()) return
        val msg = ChatMessage(
            messageId = UUID.randomUUID().toString(),
            senderId  = selfId,
            timestamp = System.currentTimeMillis(),
            text      = text.take(MessageCodec.MAX_TEXT_LENGTH)
        )
        uiState.update { it.copy(messages = it.messages + msg, inputText = "") }
        repository.publishMessage(msg)
    }

    // ── エラーハンドリング ──────────────────────────────────────────
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
            else -> lifecycleScope.launch {
                effects.emit(ChatUiEffect.ShowToast(errorHandler.toUserMessage(error)))
            }
        }
    }

    // ── Effect 購読 ────────────────────────────────────────────────
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
