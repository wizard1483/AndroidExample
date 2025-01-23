package com.example.ble_test.data

import android.os.Parcel
import android.os.Parcelable

data class GattServiceData(
    val serviceUUID: String, // GATT 서비스 UUID
    val characteristics: List<String> // 이 서비스에 포함된 Characteristic UUID 리스트
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.createStringArrayList() ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(serviceUUID)
        parcel.writeStringList(characteristics)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<GattServiceData> {
        override fun createFromParcel(parcel: Parcel): GattServiceData {
            return GattServiceData(parcel)
        }

        override fun newArray(size: Int): Array<GattServiceData?> {
            return arrayOfNulls(size)
        }
    }
}
