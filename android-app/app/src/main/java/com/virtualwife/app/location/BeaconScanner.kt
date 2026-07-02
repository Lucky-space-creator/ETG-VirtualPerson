package com.virtualwife.app.location

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BeaconScanner(private val context: Context) {

    data class BeaconInfo(
        val macAddress: String,
        val rssi: Int,
        val txPower: Int = -59,
        val spotId: String? = null
    ) {
        val estimatedDistanceMeters: Double
            get() {
                val ratio = rssi.toDouble() / txPower.toDouble()
                return if (ratio < 1.0) {
                    Math.pow(ratio, 10.0)
                } else {
                    0.89976 * Math.pow(ratio, 7.7095) + 0.111
                }
            }
    }

    private var bleScanner: BluetoothLeScanner? = null
    private var isScanning = false

    fun startScan(): Flow<List<BeaconInfo>> = callbackFlow {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if (adapter == null || !adapter.isEnabled) {
            close(IllegalStateException("蓝牙未开启"))
            return@callbackFlow
        }

        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            close(SecurityException("缺少蓝牙扫描权限"))
            return@callbackFlow
        }

        bleScanner = adapter.bluetoothLeScanner
        val beacons = mutableMapOf<String, BeaconInfo>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val beacon = BeaconInfo(
                    macAddress = result.device.address,
                    rssi = result.rssi
                )
                beacons[beacon.macAddress] = beacon
                trySend(beacons.values.toList())
            }
        }

        bleScanner?.startScan(callback)
        isScanning = true

        awaitClose {
            stopScanInternal()
        }
    }

    fun stopScan() {
        stopScanInternal()
    }

    private fun stopScanInternal() {
        if (isScanning) {
            try {
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bleScanner?.stopScan(object : ScanCallback() {})
                }
            } catch (_: Exception) { }
            isScanning = false
        }
    }

    fun isScanning(): Boolean = isScanning
}
