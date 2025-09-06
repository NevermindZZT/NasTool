package com.letter.nastool.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import com.letter.nastool.R
import com.letter.nastool.manager.SmbSyncManager
import com.letter.nastool.ui.activity.MainActivity
import java.io.FileDescriptor
import java.io.PrintWriter

class SmbSyncService : Service() {

    private val smbSyncManager by lazy {
        SmbSyncManager.getInstance(this)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val type = intent?.getIntExtra(KEY_START_TYPE, START_TYPE_NONE)
        when (type) {
            START_TYPE_START_SYNC  -> startSync()
            START_TYPE_STOP_SYNC -> stopSync()
        }

        return START_STICKY
    }

    override fun dump(
        fd: FileDescriptor?,
        writer: PrintWriter?,
        args: Array<out String?>?
    ) {
        smbSyncManager.dump(fd, writer, args)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "sync",
            getString(R.string.notification_channel_smb_sync_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_smb_sync_description)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
            createNotificationChannel(channel)
        }
    }

    private fun startSync() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, "sync")
            .setContentTitle(getString(R.string.notification_smb_sync_running_title))
            .setContentText(getString(R.string.notification_smb_sync_running_message))
            .setSmallIcon(R.drawable.ic_notify_small)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round))
            .setOngoing(true)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(pi)
            .build()
        startForeground(1, notification)
    }

    private fun stopSync() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val KEY_START_TYPE = "type"

        const val START_TYPE_NONE = 0
        const val START_TYPE_START_SYNC = 1
        const val START_TYPE_STOP_SYNC = 2

        fun start(context: Context, type: Int) {
            context.startService(
                Intent(context, SmbSyncService::class.java).apply {
                    putExtra(KEY_START_TYPE, type)
                }
            )
        }
    }
}