package net.moonmile.ble5_chat.copilot.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import net.moonmile.ble5_chat.copilot.util.AppLogger

/**
 * BLE スキャン管理（Extended Advertising 対応）
 */
class BleScannerManager(private val context: Context) {

    private val incomingRaw = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    @Volatile private var isScanning = false

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            extractAndEmit(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            results.forEach { extractAndEmit(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            AppLogger.warn("BleScannerManager: scan failed errorCode=$errorCode")
            isScanning = false
        }

        private fun extractAndEmit(result: ScanResult) {
            val record = result.scanRecord
            // 全パケットをダンプ（デバッグ用）
            AppLogger.debug("BleScannerManager: onScanResult addr=${result.device?.address} rssi=${result.rssi} record=${record != null}")
            if (record != null) {
                val allServiceData = record.serviceData
                if (allServiceData.isNotEmpty()) {
                    allServiceData.forEach { (uuid, data) ->
                        AppLogger.debug("BleScannerManager:   serviceData uuid=$uuid bytes=${data.size} raw=${data.decodeToString().take(60)}")
                    }
                } else {
                    AppLogger.debug("BleScannerManager:   no serviceData in record")
                }
                val manufacturerData = record.manufacturerSpecificData
                if (manufacturerData != null && manufacturerData.size() > 0) {
                    for (index in 0 until manufacturerData.size()) {
                        val manufacturerId = manufacturerData.keyAt(index)
                        val data = manufacturerData.valueAt(index)
                        AppLogger.debug(
                            "BleScannerManager:   manufacturerData id=$manufacturerId bytes=${data.size} raw=${data.decodeToString().take(60)}"
                        )
                    }
                } else {
                    AppLogger.debug("BleScannerManager:   no manufacturerData in record")
                }
                val serviceUuids = record.serviceUuids
                AppLogger.debug("BleScannerManager:   serviceUuids=$serviceUuids")
            }
            val serviceData = record
                ?.getServiceData(ParcelUuid(BleAdvertiserManager.SERVICE_UUID))
            val fallbackManufacturerData = record
                ?.manufacturerSpecificData
                ?.get(0xFFFE)
            val payload = serviceData ?: fallbackManufacturerData
            if (payload != null) {
                val source = if (serviceData != null) "serviceData" else "manufacturerData"
                AppLogger.info("BleScannerManager: payload received via $source ${payload.size} bytes: ${payload.decodeToString().take(80)}")
                val emitted = incomingRaw.tryEmit(payload)
                if (!emitted) AppLogger.warn("BleScannerManager: tryEmit dropped (buffer full)")
            }
        }
    }

    /**
     * スキャンを開始する
     */
    fun startScanning() {
        if (isScanning) return
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: run {
            AppLogger.warn("BleScannerManager: scanner not available")
            return
        }
        isScanning = true

        // フィルタなし: 全パケットを受信してログで確認する（デバッグ用）
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
        AppLogger.info("BleScannerManager: startScanning (no filter) SERVICE_UUID=${BleAdvertiserManager.SERVICE_UUID} scanMode=LOW_LATENCY")
    }

    /**
     * スキャンを停止する
     */
    fun stopScanning() {
        if (!isScanning) return
        isScanning = false
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            AppLogger.warn("BleScannerManager: stopScan error: ${e.message}")
        }
        AppLogger.info("BleScannerManager: stopScanning")
    }

    /**
     * 受信した raw バイト列の Flow
     */
    fun rawPackets(): Flow<ByteArray> = incomingRaw
}
