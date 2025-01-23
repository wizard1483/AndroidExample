package com.example.ble_test

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Intent
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
import com.example.ble_test.data.GattServiceData

// Device 데이터 클래스
data class Device(
    val name: String,
    val address: String,
    var isConnected: Boolean = false
)

// DeviceAdapter 클래스
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

        // 연결 상태에 따라 배경색 변경
        if (device.isConnected) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.sky_blue))
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.transparent))
        }

        // 항목 클릭 이벤트 처리
        holder.itemView.setOnClickListener {
            onDeviceClick(device)
        }
    }

    override fun getItemCount(): Int = deviceList.size

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.deviceName)
        val deviceAddress: TextView = view.findViewById(R.id.deviceAddress)
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val deviceList = mutableListOf<Device>()
    private lateinit var deviceAdapter: DeviceAdapter
    private var bluetoothGatt: BluetoothGatt? = null

    // BLE 스캔 결과를 처리하는 ScanCallback
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let {
                val deviceName = it.name ?: "Unknown Device"
                val deviceAddress = it.address

                if (!deviceList.any { device -> device.address == deviceAddress }) {
                    deviceList.add(Device(deviceName, deviceAddress))
                    runOnUiThread {
                        deviceAdapter.notifyDataSetChanged()
                    }
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            results.forEach { result ->
                val deviceName = result.device.name ?: "Unknown Device"
                val deviceAddress = result.device.address

                if (!deviceList.any { device -> device.address == deviceAddress }) {
                    deviceList.add(Device(deviceName, deviceAddress))
                }
            }

            runOnUiThread {
                deviceAdapter.notifyDataSetChanged()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scan failed with error: $errorCode")
            runOnUiThread {
                Toast.makeText(applicationContext, "Scan failed: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // BLE GATT 연결 및 서비스 탐색 처리하는 GattCallback
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val deviceAddress = gatt.device.address // 현재 연결된 장치 주소

            runOnUiThread {
                val device = deviceList.find { it.address == deviceAddress } // 해당 장치를 리스트에서 찾음
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to ${gatt.device.name}")
                    Toast.makeText(applicationContext, "Connected to ${gatt.device.name}", Toast.LENGTH_SHORT).show()

                    // 연결 상태를 true로 변경
                    device?.isConnected = true
                    // 서비스 탐색 시작
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from ${gatt.device.name}")
                    Toast.makeText(applicationContext, "Disconnected from ${gatt.device.name}", Toast.LENGTH_SHORT).show()

                    // 연결 상태를 false로 변경
                    device?.isConnected = false

                    // GATT 리소스 해제
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                }

                // RecyclerView 새로고침
                deviceAdapter.notifyDataSetChanged()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                val services = gatt.services
                val gattServiceDataList = services.map { service ->
                    val characteristicUUIDs = service.characteristics.map { it.uuid.toString() }
                    GattServiceData(service.uuid.toString(), characteristicUUIDs)
                }

                // 데이터 전달 및 화면 전환
                runOnUiThread {
                    val intent = Intent(this@MainActivity, UUIDListActivity::class.java)
                    intent.putParcelableArrayListExtra("SERVICE_DATA", ArrayList(gattServiceDataList))
                    Log.d("MainActivity", "Navigating to UUIDListActivity with data: $gattServiceDataList")
                    startActivity(intent)
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        deviceAdapter = DeviceAdapter(deviceList) { device ->
            connectToDevice(device)
        }

        findViewById<RecyclerView>(R.id.deviceList).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
        }

        findViewById<Button>(R.id.scanButton).setOnClickListener {
            startBleScan()
        }

        if (!hasPermissions()) {
            requestPermissions()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startBleScan() {
        bluetoothLeScanner.startScan(null, ScanSettings.Builder().build(), scanCallback)
        Handler(Looper.getMainLooper()).postDelayed({
            bluetoothLeScanner.stopScan(scanCallback)
        }, 10000)
    }

    private fun connectToDevice(device: Device) {
        if (device.isConnected) {
            // 이미 연결된 장치를 다시 선택한 경우 연결 해제
            bluetoothGatt?.disconnect()
            device.isConnected = false
            deviceAdapter.notifyDataSetChanged()
            Log.d(TAG, "Disconnected from device: ${device.name}")
            Toast.makeText(this, "Disconnected from ${device.name}", Toast.LENGTH_SHORT).show()
        } else {
            // 새로운 장치와 연결
            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)
            bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback)
            device.isConnected = true
            deviceAdapter.notifyDataSetChanged()
            Log.d(TAG, "Connecting to device: ${device.name}")
            Toast.makeText(this, "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
    }

    companion object {
        private const val TAG = "BLEScanApp"
        private const val PERMISSION_REQUEST_CODE = 1
    }
}
