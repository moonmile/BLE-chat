package net.moonmile.ble5_chat.claude.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.moonmile.ble5_chat.claude.BuildConfig
import net.moonmile.ble5_chat.claude.model.AdvPacket
import net.moonmile.ble5_chat.claude.util.AppLogger
import net.moonmile.ble5_chat.claude.util.DispatcherProvider

class BleAdvertiserManager(
    private val context: Context,
    private val dispatchers: DispatcherProvider
) {
    private val TAG = "BleAdvertiserManager"
    private val MANUFACTURER_ID = 0x4D4E  // "MN" = moonmile
    private val ADVERTISE_DURATION_MS = BuildConfig.ADVERTISE_DURATION_MS

    private val scope = CoroutineScope(dispatchers.io() + SupervisorJob())
    private var timeoutJob: Job? = null
    private var currentAdvertisingSet: AdvertisingSet? = null
    private var isAdvertising = false

    private val bluetoothLeAdvertiser: BluetoothLeAdvertiser? by lazy {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mgr.adapter?.bluetoothLeAdvertiser
    }

    private val advertisingCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(adSet: AdvertisingSet?, txPower: Int, status: Int) {
            if (status == ADVERTISE_SUCCESS) {
                currentAdvertisingSet = adSet
                isAdvertising = true
                AppLogger.d(TAG, "Advertising started (txPower=$txPower)")
            } else {
                isAdvertising = false
                AppLogger.e(TAG, "Advertising start failed: status=$status")
            }
        }

        override fun onAdvertisingSetStopped(adSet: AdvertisingSet?) {
            currentAdvertisingSet = null
            isAdvertising = false
            AppLogger.d(TAG, "Advertising stopped")
        }
    }

    fun broadcast(packet: AdvPacket) {
        AppLogger.d(TAG, "broadcast: messageId=${packet.messageId}")
        startAdvertising(packet.payload)
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(ADVERTISE_DURATION_MS)
            stopAdvertising()
        }
    }

    fun startAdvertising(payload: ByteArray) {
        val advertiser = bluetoothLeAdvertiser
        if (advertiser == null) {
            AppLogger.e(TAG, "BluetoothLeAdvertiser not available")
            return
        }
        if (isAdvertising) stopAdvertising()

        val parameters = AdvertisingSetParameters.Builder()
            .setLegacyMode(false)
            .setConnectable(false)
            .setScannable(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .addManufacturerData(MANUFACTURER_ID, payload)
            .build()

        try {
            advertiser.startAdvertisingSet(parameters, data, null, null, null, advertisingCallback)
            AppLogger.d(TAG, "startAdvertisingSet called (payload=${payload.size} bytes)")
        } catch (e: Exception) {
            AppLogger.e(TAG, "startAdvertisingSet failed", e)
        }
    }

    fun stopAdvertising() {
        timeoutJob?.cancel()
        timeoutJob = null
        if (!isAdvertising) return
        try {
            bluetoothLeAdvertiser?.stopAdvertisingSet(advertisingCallback)
        } catch (e: Exception) {
            AppLogger.e(TAG, "stopAdvertisingSet failed", e)
        }
    }

    fun release() {
        stopAdvertising()
        scope.coroutineContext[Job]?.cancel()
    }
}
