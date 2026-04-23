package net.moonmile.ble5_chat.copilot.ble

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import net.moonmile.ble5_chat.copilot.util.AppLogger

/**
 * BLE 受信管理（暫定実装）
 */
class BleScannerManager {
    private val incomingRaw = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    private var isScanning = false

    suspend fun startScanning() {
        if (isScanning) return
        isScanning = true
        AppLogger.info("BleScannerManager: startScanning")
        // TODO: Extended Advertising スキャン開始
    }

    suspend fun stopScanning() {
        if (!isScanning) return
        isScanning = false
        AppLogger.info("BleScannerManager: stopScanning")
        // TODO: スキャン停止
    }

    fun rawPackets(): Flow<ByteArray> = incomingRaw

    suspend fun onAdvertisementReceived(raw: ByteArray) {
        incomingRaw.emit(raw)
    }
}
