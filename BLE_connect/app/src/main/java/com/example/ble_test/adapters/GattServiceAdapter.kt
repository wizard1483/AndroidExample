package com.example.ble_test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ble_test.data.GattServiceData

class GattServiceAdapter(private val serviceDataList: List<GattServiceData>) :
    RecyclerView.Adapter<GattServiceAdapter.ServiceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.service_item, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val serviceData = serviceDataList[position]
        holder.serviceUUID.text = serviceData.serviceUUID

        // 하위 Characteristic RecyclerView 설정
        val characteristicAdapter = CharacteristicAdapter(serviceData.characteristics)
        holder.characteristicRecyclerView.apply {
            layoutManager = LinearLayoutManager(holder.itemView.context)
            adapter = characteristicAdapter
        }
    }

    override fun getItemCount(): Int = serviceDataList.size

    class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val serviceUUID: TextView = view.findViewById(R.id.serviceUUID)
        val characteristicRecyclerView: RecyclerView = view.findViewById(R.id.characteristicRecyclerView)
    }
}
