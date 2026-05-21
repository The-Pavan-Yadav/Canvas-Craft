package com.example

import android.os.Parcelable

interface Layer : Parcelable {
    val id: String
    val isSelected: Boolean
    val zIndex: Int
}
