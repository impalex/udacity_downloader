package com.udacity.downloader

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.AppBarLayout
import com.udacity.downloader.data.ObjectInfo
import com.udacity.downloader.databinding.ActivityDetailBinding
import com.udacity.downloader.viewmodels.DetailViewModel
import io.noties.markwon.Markwon

const val DOWNLOAD_INFO_EXTRA = "DOWNLOAD_INFO"

class DetailActivity : AppCompatActivity() {

    private val binding by lazy { ActivityDetailBinding.inflate(layoutInflater) }
    private val viewmodel by lazy { ViewModelProvider(this).get(DetailViewModel::class.java) }
    private val markwon by lazy { Markwon.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.viewmodel = viewmodel

        binding.detailAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.detailMotionLayout.progress = -verticalOffset / binding.detailAppBar.totalScrollRange.toFloat()
        })

        intent?.getParcelableExtra<ObjectInfo>(DOWNLOAD_INFO_EXTRA)?.let {
            viewmodel.setObjectInfo(it)
        }

        viewmodel.readme.observe(this) {
            if (it.isNullOrEmpty()) {
                binding.contentLayout.infoText.text = ""
            } else {
                markwon.setMarkdown(binding.contentLayout.infoText, it)
            }
        }

        binding.detailOkButton.setOnClickListener {
            startActivity(Intent(applicationContext, MainActivity::class.java))
        }

    }

}
