package com.letter.nastool.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.letter.nastool.manager.SmbSyncManager
import java.io.FileDescriptor
import java.io.PrintWriter

class SmbSyncService : Service() {

    private val smbSyncManager by lazy {
        SmbSyncManager.getInstance(this)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun dump(
        fd: FileDescriptor?,
        writer: PrintWriter?,
        args: Array<out String?>?
    ) {
        smbSyncManager.dump(fd, writer, args)
    }
}