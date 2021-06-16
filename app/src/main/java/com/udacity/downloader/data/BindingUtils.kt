package com.udacity.downloader.data

import android.text.format.Formatter
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.udacity.downloader.R

@BindingAdapter("filesize")
fun TextView.bindFileSizeText(size: Int) {
    text = if (size > 0)
        Formatter.formatFileSize(context, size.toLong())
    else
        context.getString(R.string.unknown_size)
}

@BindingAdapter("is_success")
fun TextView.bindResultText(isSuccess: Boolean) {
    if (isSuccess) {
        setText(R.string.status_success)
        setTextColor(context.getColor(R.color.colorSuccess))
    } else {
        setText(R.string.status_fail)
        setTextColor(context.getColor(R.color.colorFailure))
    }

}