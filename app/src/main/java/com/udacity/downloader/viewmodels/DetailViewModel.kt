package com.udacity.downloader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udacity.downloader.data.ObjectInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.net.URL

class DetailViewModel: ViewModel() {

    private val _objectInfo = MutableLiveData<ObjectInfo?>().apply { value = null }
    val objectInfo: LiveData<ObjectInfo?> = _objectInfo

    private val _readme = MutableLiveData<String>().apply { value = "" }
    val readme: LiveData<String> = _readme

    fun setObjectInfo(objectInfo: ObjectInfo) {
        _objectInfo.value = objectInfo
        if (objectInfo.readme.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                _readme.postValue(loadText(objectInfo.readme))
            }
        }
    }

    private fun loadText(url: String): String =
        try {
            URL(url).openConnection().getInputStream().bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            ""
        }

}