package com.example.ble_test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CharacteristicAdapter(private val characteristicList: List<String>) :
    RecyclerView.Adapter<CharacteristicAdapter.CharacteristicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacteristicViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.characteristic_item, parent, false)
        return CharacteristicViewHolder(view)
    }

    override fun onBindViewHolder(holder: CharacteristicViewHolder, position: Int) {
        holder.characteristicUUID.text = characteristicList[position]
    }

    override fun getItemCount(): Int = characteristicList.size

    class CharacteristicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val characteristicUUID: TextView = view.findViewById(R.id.characteristicUUID)
    }
}
