package net.moonmile.ble5_chat.claude.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import net.moonmile.ble5_chat.claude.model.ChatMessage
import net.moonmile.ble5_chat.claude.util.AppLogger

class BleScannerManager(
    private val context: Context,
    private val duplicateFilter: DuplicateFilter
) {
    private val TAG = "BleScannerManager"
    private val MANUFACTURER_ID = 0x4D4E  // must match BleAdvertiserManager

    private val _incomingMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<ChatMessage> = _incomingMessages

    private var isScanning = false

    private val bluetoothLeScanner: BluetoothLeScanner? by lazy {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mgr.adapter?.bluetoothLeScanner
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val raw = result.scanRecord?.getManufacturerSpecificData(MANUFACTURER_ID) ?: return
            onAdvertisementReceived(raw)
        }

        override fun onScanFailed(errorCode: Int) {
            AppLogger.e(TAG, "Scan failed: errorCode=$errorCode")
            isScanning = false
        }
    }

    fun startScanning() {
        if (isScanning) return
        val scanner = bluetoothLeScanner
        if (scanner == null) {
            AppLogger.e(TAG, "BluetoothLeScanner not available")
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setLegacy(false)  // enable extended advertising scan (API 26+)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
            isScanning = true
            AppLogger.d(TAG, "Scanning started")
        } catch (e: Exception) {
            AppLogger.e(TAG, "startScan failed", e)
        }
    }

    fun stopScanning() {
        if (!isScanning) return
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            AppLogger.d(TAG, "Scanning stopped")
        } catch (e: Exception) {
            AppLogger.e(TAG, "stopScan failed", e)
        } finally {
            isScanning = false
        }
    }

    fun onAdvertisementReceived(raw: ByteArray) {
        val message = MessageCodec.decode(raw) ?: return
        if (duplicateFilter.isDuplicate(message.messageId)) {
            AppLogger.d(TAG, "Duplicate filtered: ${message.messageId}")
            return
        }
        AppLogger.d(TAG, "Received: ${message.messageId} from ${message.senderId}")
        _incomingMessages.tryEmit(message)
    }
}
