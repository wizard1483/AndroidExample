package com.example.ble_test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ble_test.data.GattServiceData

class UUIDListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uuid_list)

        // Intent로 전달받은 GATT Service 데이터
        val gattServiceDataList = intent.getParcelableArrayListExtra<GattServiceData>("SERVICE_DATA") ?: arrayListOf()

        // RecyclerView 설정
        val recyclerView: RecyclerView = findViewById(R.id.uuidRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = GattServiceAdapter(gattServiceDataList)
    }
}
