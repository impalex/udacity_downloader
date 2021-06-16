package com.udacity.downloader

import android.animation.*
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.udacity.downloader.data.ObjectInfo
import com.udacity.downloader.databinding.ActivityMainBinding
import com.udacity.downloader.viewmodels.MainViewModel
import com.udacity.downloader.viewmodels.ProgressState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


private const val CHANNEL_ID = "udacity_downloader"
private const val NOTIFICATION_MAIN_ID = 0
private const val NOTIFICATION_DETAIL_ID = 1

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }
    private val progressText by lazy { getString(R.string.button_loading) }
    private val idleText by lazy { getString(R.string.button_name) }
    private val notificationBuilder by lazy { createNotificationBuilder() }
    private val mainNotificationIntent by lazy { createMainNotificationIntent() }
    private val buttonAnimator by lazy { initButtonAnimator() }
    private var stopAnimatorNextCycle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        createNotificationChannel()
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        binding.mainAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.mainMotionLayout.progress = -verticalOffset / binding.mainAppBar.totalScrollRange.toFloat()
        })

        initListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationManagerCompat.from(this).cancelAll()
    }

    private fun initListeners() {
        lifecycleScope.launch {
            viewModel.downloadProgress.collect {
                when (it.state) {
                    ProgressState.State.IDLE -> showIdleButton()
                    ProgressState.State.START -> initStartProgress(it)
                    ProgressState.State.END -> downloadComplete(it)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.toastEvent.collect {
                Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = getString(R.string.notif_channel_description)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun createNotificationBuilder() = NotificationCompat.Builder(this, CHANNEL_ID).apply {
        setSmallIcon(R.drawable.ic_assistant_black_24dp)
        priority = NotificationCompat.PRIORITY_DEFAULT
        setContentTitle(getString(R.string.app_name))
        setContentText(getString(R.string.action_settings))
        setContentIntent(mainNotificationIntent)
        setOnlyAlertOnce(true)
        setOngoing(true)
    }

    private fun createMainNotificationIntent() = PendingIntent.getActivity(
        applicationContext,
        NOTIFICATION_MAIN_ID,
        Intent(applicationContext, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun createDetailNotificationIntent(objectInfo: ObjectInfo?) = PendingIntent.getActivity(
        applicationContext,
        NOTIFICATION_DETAIL_ID,
        Intent(applicationContext, DetailActivity::class.java).apply {
            objectInfo?.let { putExtra(DOWNLOAD_INFO_EXTRA, it) }
        },
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun showIdleButton() {
        binding.customButton.apply {
            setShowCircle(false)
            setCircleProgress(0f)
            setSecondaryColorStart(0f)
            setSecondaryColorEnd(0f)
            setText(idleText)
        }
    }

    private fun initStartProgress(progressState: ProgressState) {
        binding.customButton.apply {
            setShowCircle(true)
            setCircleProgress(0f)
            setSecondaryColorStart(0f)
            setSecondaryColorEnd(0f)
            setText(progressText)
        }
        notificationBuilder.apply {
            setProgress(0, 0, true)
            setContentText(getString(R.string.notif_text_downloading))
            setContentTitle(progressState.objectInfo?.name)
            setOngoing(true)
            setAutoCancel(false)
            clearActions()
        }
        buttonAnimator.start()
        NotificationManagerCompat.from(this@MainActivity).notify(NOTIFICATION_MAIN_ID, notificationBuilder.build())
    }

    private fun downloadComplete(progressState: ProgressState) {
        stopAnimatorNextCycle = true
        notificationBuilder.apply {
            setProgress(0, 0, false)
            setContentTitle(progressState.objectInfo?.name)
            setContentText(getString(R.string.notif_text_complete))
            setOngoing(false)
            setAutoCancel(true)
            addAction(R.drawable.ic_download, getString(R.string.open), createDetailNotificationIntent(progressState.objectInfo))
        }
        NotificationManagerCompat.from(this@MainActivity).notify(NOTIFICATION_MAIN_ID, notificationBuilder.build())
    }

    private fun initButtonAnimator(): ObjectAnimator {
        val startKeyFrames = arrayOf(
            Keyframe.ofFloat(0f, 0f),
            Keyframe.ofFloat(0.5f, 0f),
            Keyframe.ofFloat(0.7f, 1f)
        )
        val endKeyFrames = arrayOf(
            Keyframe.ofFloat(0f, 0f),
            Keyframe.ofFloat(0.6f, 1f)
        )
        val start = PropertyValuesHolder.ofKeyframe("secondaryColorStart", *startKeyFrames)
        val end = PropertyValuesHolder.ofKeyframe("secondaryColorEnd", *endKeyFrames)
        val circle = PropertyValuesHolder.ofFloat("circleProgress", 0f, 1f)
        return ObjectAnimator.ofPropertyValuesHolder(binding.customButton, start, end, circle).apply {
            duration = 3000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART

            addListener(object : AnimatorListenerAdapter(){
                override fun onAnimationRepeat(animation: Animator) {
                    if (stopAnimatorNextCycle) {
                        animation.cancel()
                        stopAnimatorNextCycle = false
                        viewModel.readyToDownload()
                    }
                }
            })
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
            viewModel.downloadComplete(id)
        }
    }

}
