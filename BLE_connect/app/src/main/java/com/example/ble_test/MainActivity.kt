package com.example.ble_test

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Device data class
data class Device(val name: String, val address: String)

// Device Adapter for RecyclerView
class DeviceAdapter(
    private val deviceList: List<Device>,
    private val onDeviceClick: (Device) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]
        holder.deviceName.text = device.name
        holder.deviceAddress.text = device.address

        holder.itemView.setOnClickListener {
            onDeviceClick(device)
        }
    }

    override fun getItemCount(): Int = deviceList.size

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.deviceName)
        val deviceAddress: TextView = view.findViewById(R.id.deviceAddress)
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val scanResults = mutableMapOf<String, ScanResult>()
    private lateinit var deviceAdapter: DeviceAdapter
    private val deviceList = mutableListOf<Device>()
    private var bluetoothGatt: BluetoothGatt? = null

    // BluetoothGattCallback for handling connection and service discovery
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to ${gatt.device.name}")
                runOnUiThread {
                    Toast.makeText(applicationContext, "Connected to ${gatt.device.name}", Toast.LENGTH_SHORT).show()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from ${gatt.device.name}")
                runOnUiThread {
                    Toast.makeText(applicationContext, "Disconnected from ${gatt.device.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
            }
        }
    }

    // ScanCallback for handling BLE scan results
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                val deviceName = it.device.name
                if (deviceName != null && deviceName != "Unknown Device") {
                    val deviceAddress = it.device.address
                    Log.d("BLE_SCAN", "Device found: $deviceName ($deviceAddress)")
                    deviceList.add(Device(deviceName, deviceAddress))
                    deviceAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            for (result in results) {
                val deviceName = result.device.name
                val deviceAddress = result.device.address

                // 이름이 null이거나 Unknown Device라면 건너뜀
                if (deviceName.isNullOrEmpty() || deviceName == "Unknown Device") continue

                // 동일한 deviceAddress가 이미 리스트에 있다면 추가하지 않음
                if (deviceList.any {it.address == deviceAddress}) {
                    Log.d(TAG, "Device already in list: $deviceName ($deviceAddress)")
                    continue
                }

                // 리스트에 새로운 장치 추가
                deviceList.add(Device(deviceName, deviceAddress))
                scanResults[result.device.address] = result
                deviceAdapter.notifyDataSetChanged()
                Log.d(TAG, "Found device: ${result.device.name} - ${result.device.address}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scan failed with error: $errorCode")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize Bluetooth adapter and scanner
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        // Setup RecyclerView with DeviceAdapter
        deviceAdapter = DeviceAdapter(deviceList) { device ->
            connectToDevice(device)
        }
        val recyclerView: RecyclerView = findViewById(R.id.deviceList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = deviceAdapter

        // Scan button setup
        val scanButton: Button = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            startBleScan()
        }

        // Check permissions
        if (!hasPermissions()) {
            requestPermissions()
        }

        // Handle window insets (for edge-to-edge UI)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Start BLE scan
    private fun startBleScan() {
        //val scanSettings = ScanSettings.Builder()
        //    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        //    .build()

        val scanSettings = ScanSettings.Builder()
            .setReportDelay(1000)
            .build()

        val scanFilters = listOf(ScanFilter.Builder().build())
        bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
        Log.d(TAG, "BLE scan started.")

        // Stop scan after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            bluetoothLeScanner.stopScan(scanCallback)
            Log.d(TAG, "BLE scan stopped.")
        }, SCAN_PERIOD)
    }

    // Connect to selected device
    private fun connectToDevice(device: Device) {
        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)
        bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback)
        Log.d(TAG, "Connecting to device: ${device.name}")
    }

    // Check permissions for Bluetooth and Location
    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Request permissions if not granted
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
    }

    // Handle permissions result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBleScan()
            } else {
                Log.e(TAG, "Permission denied.")
            }
        }
    }

    companion object {
        private const val TAG = "BLEScanApp"
        private const val SCAN_PERIOD: Long = 10000 // 10 seconds
        private const val PERMISSION_REQUEST_CODE = 1
    }
}
