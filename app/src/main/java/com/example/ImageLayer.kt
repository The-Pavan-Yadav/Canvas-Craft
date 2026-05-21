package com.example

import android.os.Parcel
import android.os.Parcelable

data class ImageLayer(
    override val id: String,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    override val zIndex: Int = 0,
    override val isSelected: Boolean = false
) : Layer {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeFloat(offsetX)
        parcel.writeFloat(offsetY)
        parcel.writeFloat(scale)
        parcel.writeFloat(rotation)
        parcel.writeInt(zIndex)
        parcel.writeByte(if (isSelected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ImageLayer> = object : Parcelable.Creator<ImageLayer> {
            override fun createFromParcel(parcel: Parcel): ImageLayer {
                return ImageLayer(parcel)
            }

            override fun newArray(size: Int): Array<ImageLayer?> {
                return arrayOfNulls(size)
            }
        }
    }
}
