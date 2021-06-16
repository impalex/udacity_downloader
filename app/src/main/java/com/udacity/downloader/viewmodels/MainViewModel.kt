package com.udacity.downloader.viewmodels

import android.app.Activity
import android.app.Application
import android.app.DownloadManager
import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.udacity.downloader.data.ObjectInfo
import com.udacity.downloader.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProgressState(val state: State, val objectInfo: ObjectInfo? = null) {
    enum class State { IDLE, START, END }

    companion object {
        fun idle() = ProgressState(State.IDLE)
        fun start(objectInfo: ObjectInfo) = ProgressState(State.START, objectInfo)
        fun end(objectInfo: ObjectInfo) = ProgressState(State.END, objectInfo)
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val buttonIds = arrayOf(R.id.glide_radio, R.id.loadapp_radio, R.id.retrofit_radio, R.id.custom_radio)
    private val downloadNames by lazy { application.resources.getStringArray(R.array.short_name) }
    private val downloadUrls by lazy { application.resources.getStringArray(R.array.url_list) }
    private val downloadReadmes by lazy { application.resources.getStringArray(R.array.readme_list) }

    private var isDownloading = false
    private val _downloadProgress = MutableStateFlow(ProgressState.idle())
    val downloadProgress: StateFlow<ProgressState> = _downloadProgress
    val checkedRadiobuttonId = MutableLiveData(0)
    val customUrl = MutableLiveData("")
    private val _toastEvent = MutableSharedFlow<Int>()
    val toastEvent: SharedFlow<Int> = _toastEvent
    private var downloads = mutableMapOf<Long, ObjectInfo>()

    fun downloadClick() {
        if (isDownloading)
            return

        val option = buttonIds.indexOf(checkedRadiobuttonId.value)
        if (option < 0) {
            viewModelScope.launch { _toastEvent.emit(R.string.select_file_toast) }
            return
        }

        val name = if (downloadNames[option].isNullOrEmpty()) customUrl.value ?: "" else downloadNames[option]
        val url = if (downloadUrls[option].isNullOrEmpty()) customUrl.value ?: "" else downloadUrls[option]
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            viewModelScope.launch { _toastEvent.emit(R.string.invalid_url) }
            return
        }

        val readme = if (downloadReadmes[option].isNullOrEmpty()) "" else downloadReadmes[option]
        val downloadObject = ObjectInfo(name, url, readme, false, 0)
        download(downloadObject)
    }

    private fun download(objectInfo: ObjectInfo) {
        isDownloading = true
        val request = DownloadManager
            .Request(Uri.parse(objectInfo.url))
            .setTitle(getApplication<Application>().resources.getString(R.string.app_name))
            .setDescription(getApplication<Application>().resources.getString(R.string.app_description))
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = getApplication<Application>().getSystemService(Activity.DOWNLOAD_SERVICE) as DownloadManager

        downloads[downloadManager.enqueue(request)] = objectInfo

        _downloadProgress.value = ProgressState.start(objectInfo)

    }

    fun downloadComplete(id: Long) {
        val objectInfo = downloads[id] ?: return
        val downloadManager = getApplication<Application>().getSystemService(Activity.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().apply { setFilterById(id) }
        val cursor = downloadManager.query(query).apply { moveToFirst() }
        val status = cursor.run { getInt(getColumnIndex(DownloadManager.COLUMN_STATUS)) }
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            objectInfo.apply {
                size = cursor.run { getInt(getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)) }
                success = true
            }
        }
        downloads.remove(id)
        _downloadProgress.value = ProgressState.end(objectInfo)
    }

    fun readyToDownload() {
        _downloadProgress.value = ProgressState.idle()
        isDownloading = false
    }

}