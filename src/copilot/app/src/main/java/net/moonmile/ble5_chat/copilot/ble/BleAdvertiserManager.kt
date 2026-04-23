package net.moonmile.ble5_chat.copilot.ble

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.moonmile.ble5_chat.copilot.model.AdvPacket
import net.moonmile.ble5_chat.copilot.util.AppLogger

/**
 * BLE 送信管理（暫定実装）
 */
class BleAdvertiserManager {
    private val lock = Mutex()
    private var isAdvertising = false

    suspend fun startAdvertising() {
        lock.withLock {
            if (isAdvertising) return
            isAdvertising = true
            AppLogger.info("BleAdvertiserManager: startAdvertising")
        }
    }

    suspend fun stopAdvertising() {
        lock.withLock {
            if (!isAdvertising) return
            isAdvertising = false
            AppLogger.info("BleAdvertiserManager: stopAdvertising")
        }
    }

    suspend fun broadcast(packet: AdvPacket) {
        startAdvertising()
        AppLogger.debug("BleAdvertiserManager: broadcast messageId=${packet.messageId}")
        // TODO: Extended Advertising 実送信

        // 設計書どおり 10 秒で自動停止
        delay(10_000)
        stopAdvertising()
    }
}
