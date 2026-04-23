package net.moonmile.ble5_chat.copilot.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.content.Context
import android.os.ParcelUuid
import net.moonmile.ble5_chat.copilot.util.AppLogger
import java.util.UUID

/**
 * BLE5 Extended Advertising 送信管理
 */
class BleAdvertiserManager(private val context: Context) {

    companion object {
        /** アプリ識別用サービス UUID */
        val SERVICE_UUID: UUID = UUID.fromString("0000feea-0000-1000-8000-00805f9b34fb")
        private const val MANUFACTURER_ID = 0xFFFE
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    @Volatile private var isAdvertising = false

    private val advertiserCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(
            advertisingSet: AdvertisingSet?,
            txPower: Int,
            status: Int
        ) {
            if (status == ADVERTISE_SUCCESS) {
                isAdvertising = true
                AppLogger.info("BleAdvertiserManager: started txPower=$txPower")
            } else {
                isAdvertising = false
                AppLogger.warn("BleAdvertiserManager: start failed status=$status")
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            isAdvertising = false
            AppLogger.info("BleAdvertiserManager: set stopped")
        }
    }

    /**
     * サービスデータを Extended Advertising で送出する
     *
     * @param serviceData 送信するバイト列（最大 255 バイト）
     */
    fun broadcast(serviceData: ByteArray) {
        AppLogger.debug("BleAdvertiserManager: broadcast() called, ${serviceData.size} bytes: ${serviceData.decodeToString().take(80)}")
        val adapter = bluetoothAdapter ?: run {
            AppLogger.warn("BleAdvertiserManager: adapter unavailable")
            return
        }
        AppLogger.debug("BleAdvertiserManager: adapter.isEnabled=${adapter.isEnabled} isLeExtAdv=${adapter.isLeExtendedAdvertisingSupported} isLe2M=${adapter.isLe2MPhySupported}")
        if (!adapter.isEnabled) {
            AppLogger.warn("BleAdvertiserManager: Bluetooth disabled")
            return
        }
        if (!adapter.isLeExtendedAdvertisingSupported) {
            AppLogger.warn("BleAdvertiserManager: Extended Advertising not supported on this device")
            return
        }
        val advertiser = adapter.bluetoothLeAdvertiser ?: run {
            AppLogger.warn("BleAdvertiserManager: no advertiser")
            return
        }

        // 既存の Advertising set を停止してから新規開始
        if (isAdvertising) {
            try {
                advertiser.stopAdvertisingSet(advertiserCallback)
            } catch (e: Exception) {
                AppLogger.warn("BleAdvertiserManager: stopAdvertisingSet error: ${e.message}")
            }
        }

        val secondaryPhy = if (adapter.isLe2MPhySupported) {
            BluetoothDevice.PHY_LE_2M
        } else {
            BluetoothDevice.PHY_LE_1M
        }

        val params = AdvertisingSetParameters.Builder()
            .setLegacyMode(false)
            .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
            .setSecondaryPhy(secondaryPhy)
            .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addServiceData(ParcelUuid(SERVICE_UUID), serviceData)
            .addManufacturerData(MANUFACTURER_ID, serviceData)
            .build()

        advertiser.startAdvertisingSet(params, data, null, null, null, advertiserCallback)
        AppLogger.info(
            "BleAdvertiserManager: startAdvertisingSet called, ${serviceData.size} bytes, " +
                "secondaryPhy=$secondaryPhy manufacturerId=$MANUFACTURER_ID serviceUuid=$SERVICE_UUID"
        )
    }

    /**
     * Advertising を停止する
     */
    fun stopAdvertising() {
        if (!isAdvertising) return
        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: return
        try {
            advertiser.stopAdvertisingSet(advertiserCallback)
        } catch (e: Exception) {
            AppLogger.warn("BleAdvertiserManager: stopAdvertising error: ${e.message}")
        }
        AppLogger.info("BleAdvertiserManager: stopAdvertising")
    }
}
