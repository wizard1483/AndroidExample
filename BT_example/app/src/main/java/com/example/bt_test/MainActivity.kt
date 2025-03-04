package com.example.bt_test

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 1
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private val deviceList: MutableList<String> = mutableListOf()
    private val deviceMap: MutableMap<String, BluetoothDevice> = mutableMapOf()
    private var connectedThread: ConnectedThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val listView: ListView = findViewById(R.id.lvDevices)
        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        listView.adapter = arrayAdapter

        val btnDiscover: Button = findViewById(R.id.btnDiscover)
        btnDiscover.setOnClickListener {
            checkPermissions()
        }

        val btnMakeDiscoverable: Button = findViewById(R.id.btnMakeDiscoverable)
        btnMakeDiscoverable.setOnClickListener {
            makeDeviceDiscoverable()
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            Log.d("SON", "list touch position: $position")
            val deviceInfo = deviceList[position]
            Log.d("SON", "deviceInfo: $deviceInfo")
            val deviceAddress = deviceInfo.split("\n")[1]
            val device = deviceMap[deviceAddress]
            device?.let {
                connectToDevice(it)
            }
        }

        // 앱 시작 시 저장된 장치로 자동 재연결 시도
        val lastConnectedDeviceAddress = getLastConnectedDeviceAddress()
        lastConnectedDeviceAddress?.let {
            val device = bluetoothAdapter?.getRemoteDevice(it)
            device?.let {
                connectToDevice(it)
            }
        }
    }

    private fun startDiscovery() {
        deviceList.clear()
        arrayAdapter.notifyDataSetChanged()
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        bluetoothAdapter?.startDiscovery()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action!!
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                val deviceName = device.name
                val deviceAddress = device.address
                if (deviceName != null) {
                    deviceList.add("$deviceName\n$deviceAddress")
                    deviceMap[deviceAddress] = device
                    arrayAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Log.d("SON", "connectToDevice: $device")
        val uuid: UUID = device.uuids?.firstOrNull()?.uuid ?: MY_UUID
        var socket: BluetoothSocket? = null
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
            bluetoothAdapter?.cancelDiscovery()
            socket?.connect()
            // 연결 성공 처리
            Toast.makeText(this, "연결 성공: ${device.name}", Toast.LENGTH_SHORT).show()
            Log.d("SON", "연결 성공: ${device.name}")
            connectedThread = ConnectedThread(socket)
            connectedThread?.start()
            saveDeviceAddress(device.address)
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("SON", "연결 실패: ${device.name}, UUID: $uuid", e)
            try {
                socket?.close()
            } catch (closeException: IOException) {
                closeException.printStackTrace()
            }
            // 연결 실패 처리
            Toast.makeText(this, "연결 실패: ${device.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveDeviceAddress(address: String) {
        val sharedPref = getSharedPreferences("BT_PREFERENCES", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("LAST_CONNECTED_DEVICE", address)
            apply()
        }
    }

    private fun getLastConnectedDeviceAddress(): String? {
        Log.d("SON", "getLastConnectedDeviceAddress")
        val sharedPref = getSharedPreferences("BT_PREFERENCES", Context.MODE_PRIVATE)
        return sharedPref.getString("LAST_CONNECTED_DEVICE", null)
    }

    private fun makeDeviceDiscoverable() {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300) // 300초 동안 발견 가능
        }
        startActivity(discoverableIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 1
        val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // 예제 UUID
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            // 권한이 이미 부여되었을 때의 처리
            startDiscovery()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // 권한이 부여되었을 때의 처리
                startDiscovery()
            } else {
                // 권한이 거부되었을 때의 처리
                Toast.makeText(this, "권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 연결 끊김 시 팝업을 띄우는 메서드
    private fun showDisconnectDialog() {
        runOnUiThread {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("연결 끊김")
            builder.setMessage("블루투스 연결이 끊어졌습니다.")
            builder.setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }
    }

    // 데이터를 전송하고 수신하는 스레드 클래스
    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream
        private val buffer = ByteArray(1024) // 버퍼 크기 설정

        override fun run() {
            var numBytes: Int // 읽은 바이트 수
            try {
                while (true) {
                    // 입력 스트림에서 데이터를 읽음
                    numBytes = inputStream.read(buffer)
                    val readMessage = String(buffer, 0, numBytes)
                    Log.d("SON", "수신된 메시지: $readMessage")
                }
            } catch (e: IOException) {
                Log.e("SON", "입출력 오류", e)
                connectionLost()
                cancel() // 연결 끊김 시 소켓 닫기
            }
        }

        // 데이터 전송 메서드
        fun write(message: String) {
            try {
                outputStream.write(message.toByteArray())
                Log.d("SON", "보낸 메시지: $message")
            } catch (e: IOException) {
                Log.e("SON", "데이터 전송 오류", e)
            }
        }

        // 연결 해제 메서드
        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e("SON", "소켓 닫기 오류", e)
            }
        }

        // 연결 끊김 시 호출되는 메서드
        private fun connectionLost() {
            showDisconnectDialog()
        }
    }
}
