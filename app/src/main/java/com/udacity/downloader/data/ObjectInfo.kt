package com.udacity.downloader.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ObjectInfo(val name: String, val url: String, val readme: String, var success: Boolean, var size: Int): Parcelable
