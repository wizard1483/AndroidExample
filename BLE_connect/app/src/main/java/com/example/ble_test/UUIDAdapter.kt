package com.example.ble_test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// UUID 리스트를 표시하는 RecyclerView Adapter
class UUIDAdapter(private val uuidList: List<String>) :
    RecyclerView.Adapter<UUIDAdapter.UUIDViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UUIDViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.uuid_item, parent, false)
        return UUIDViewHolder(view)
    }

    override fun onBindViewHolder(holder: UUIDViewHolder, position: Int) {
        holder.uuidText.text = uuidList[position]
    }

    override fun getItemCount(): Int = uuidList.size

    // ViewHolder 클래스
    class UUIDViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val uuidText: TextView = view.findViewById(R.id.uuidTextView)
    }
}
