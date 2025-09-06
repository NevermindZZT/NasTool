package com.letter.nastool.manager

import android.content.Context
import android.os.ConditionVariable
import android.util.Log
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.lifecycle.MutableLiveData
import com.letter.nastool.data.local.SmbSyncDownloadInfo
import com.letter.nastool.data.local.SmbSyncTaskInfo
import com.letter.nastool.database.AppDatabase
import com.letter.nastool.database.entity.SmbSyncFileInfoEntity
import com.letter.nastool.database.entity.SmbSyncTaskEntity
import com.letter.nastool.repository.SmbRepo
import com.letter.nastool.services.SmbSyncService
import com.letter.nastool.utils.ext.joinPath
import java.io.File
import java.io.FileDescriptor
import java.io.PrintWriter
import java.lang.ref.WeakReference

private const val TAG = "SmbSyncManager"

class SmbSyncManager private constructor(context: Context) {

    private val contextWeakReference = WeakReference(context)

    val downloadInfos: MutableLiveData<ObservableList<SmbSyncDownloadInfo>> = MutableLiveData(
        ObservableArrayList()
    )

    private val database by lazy {
        AppDatabase.getInstance(contextWeakReference.get()!!)
    }

    private var syncThread: Thread? = null

    private val threadCondition by lazy {
        ConditionVariable(false)
    }

    fun getAllTasks(): List<SmbSyncTaskEntity> {
        return database.smbSyncTaskDao().getAll()
    }

    fun addTask(task: SmbSyncTaskEntity) {
        database.smbSyncTaskDao().insert(task)
    }

    fun updateTask(task: SmbSyncTaskEntity) {
        database.smbSyncTaskDao().update(task)
    }

    fun deleteTask(task: SmbSyncTaskEntity) {
        database.smbSyncTaskDao().delete(task)
    }

    fun getSyncFileInfos(taskId: Int): List<SmbSyncFileInfoEntity> {
        return database.smbSyncFileInfoDao().getAllByTaskId(taskId)
    }

    fun getSyncFileInfo(taskId: Int, path: String): SmbSyncFileInfoEntity? {
        return database.smbSyncFileInfoDao().get(taskId, path)
    }

    fun addSyncFileInfo(info: SmbSyncFileInfoEntity) {
        database.smbSyncFileInfoDao().insert(info)
    }

    fun clearSyncFileInfos() {
        database.smbSyncFileInfoDao().deleteAll()
    }

    fun check(task: SmbSyncTaskEntity): Boolean {
        val smbRepo = SmbRepo(task.serverAddress, task.username, task.password)
        val result = smbRepo.checkConnection(task.remotePath)
        smbRepo.close()
        return result
    }

    private fun isFileFiltered(task: SmbSyncTaskEntity, fileName: String): Boolean {
        if (task.fileType.isEmpty() || task.fileType == "*") {
            return true
        }
        val filters = task.fileType.split(",").map { it.trim().lowercase() }
        val lowerFileName = fileName.lowercase()
        filters.forEach {
            if (it.isNotEmpty() && lowerFileName.endsWith(it)) {
                return true
            }
        }
        return false
    }

    private fun getSmbFilesCount(task: SmbSyncTaskEntity, smbRepo: SmbRepo, srcPath: String): Int {
        var count = 0
        val smbFiles = smbRepo.listFiles(srcPath)
        smbFiles.forEach {
            count += if (it.isDirectory) {
                if (task.includeSubDirs) {
                    getSmbFilesCount(task, smbRepo, it.path)
                } else {
                    0
                }
            } else {
                if (isFileFiltered(task, it.name)) 1 else 0
            }
        }
        return count
    }

    fun getSyncTaskInfo(task: SmbSyncTaskEntity): SmbSyncTaskInfo {
        val smbRepo = SmbRepo(task.serverAddress, task.username, task.password)
        val totalFiles = getSmbFilesCount(task, smbRepo, task.remotePath)
        val syncFiles = database.smbSyncFileInfoDao().getAllByTaskId(task.id)
        val result = SmbSyncTaskInfo(task.id, totalFiles, syncFiles.size)
        smbRepo.close()
        return result
    }

    private fun addDownloadInfo(task: SmbSyncTaskEntity, smbRepo:  SmbRepo, srcPath: String,  destPath: String): Int {
        var added = 0

        Log.i(TAG, "addDownloadInfo: list files: $srcPath")
        val smbFiles = smbRepo.listFiles(srcPath)
        smbFiles.forEach {
            if (it.isDirectory) {
                if (task.includeSubDirs) {
                    added += addDownloadInfo(
                        task,
                        smbRepo,
                        it.path,
                        destPath.joinPath(it.name)
                    )
                }
            } else {
                val syncFileInfo = getSyncFileInfo(task.id, it.path)
                if (syncFileInfo?.fileSize == it.length() && syncFileInfo.timestamp == it.date) {
                    Log.i(TAG, "runTask: file unchanged, skip: ${it.path}")
                    return@forEach
                }
                if (!isFileFiltered(task, it.name)) {
                    Log.i(TAG, "runTask: file filtered, skip: ${it.path}")
                    return@forEach
                }

                val dest = destPath.joinPath(it.name)
                downloadInfos.value!!.add(
                    SmbSyncDownloadInfo(task, smbRepo, it, dest)
                )
                Log.i(TAG, "addDownloadInfo: add download info: ${it.path} to $dest")
                added += 1
            }
        }
        return added
    }

    fun runTask(task: SmbSyncTaskEntity): Int {
        val smbRepo = SmbRepo(task.serverAddress, task.username, task.password)
        val added = addDownloadInfo(task, smbRepo, task.remotePath, task.localPath)
        startSync()
        return added
    }

    private fun startSync() {
        if (syncThread == null) {
            syncThread = Thread {
                syncProcess()
            }.apply {
                start()
            }
        }
        threadCondition.open()
    }

    private fun stopSync() {
        threadCondition.close()
    }

    private fun syncProcess() {
        while (!Thread.interrupted()) {
            threadCondition.block()
            SmbSyncService.start(contextWeakReference.get()!!, SmbSyncService.START_TYPE_START_SYNC)
            for (it in downloadInfos.value!!.iterator()) {
                if (it.isDownloaded) {
                    continue
                }
                try {
                    it.state = SmbSyncDownloadInfo.STATE_IN_PROGRESS
                    it.onInfoChanged?.invoke()
                    val dest = try {
                        it.repo.download(it.srcFile, it.destPath)
                    } catch (e: Exception) {
                        Log.e(TAG, "", e)
                        it.info = e.stackTraceToString()
                        null
                    }
                    if (dest == null) {
                        it.state = SmbSyncDownloadInfo.STATE_FAILED
                        it.onInfoChanged?.invoke()
                        Log.e(TAG, "syncProcess: download failed: ${it.srcFile.path} to ${it.destPath}")
                    } else {
                        it.isDownloaded = true
                        it.state = SmbSyncDownloadInfo.STATE_COMPLETED
                        it.onInfoChanged?.invoke()
                        Log.i(TAG, "syncProcess: download success: ${it.srcFile.path} to $dest")
                        val syncFileInfo = database.smbSyncFileInfoDao().get(it.task.id, it.srcFile.path)
                        if (syncFileInfo != null) {
                            syncFileInfo.timestamp = it.srcFile.date
                            syncFileInfo.fileSize = it.srcFile.length()
                            database.smbSyncFileInfoDao().update(syncFileInfo)
                            Log.i(TAG, "syncProcess: update sync file info: $syncFileInfo")
                        } else {
                            database.smbSyncFileInfoDao().insert(
                                SmbSyncFileInfoEntity(
                                    taskId = it.task.id,
                                    path = it.srcFile.path,
                                    fileSize = it.srcFile.length(),
                                    timestamp = it.srcFile.date
                                )
                            )
                            Log.i(TAG, "syncProcess: insert sync file info: ${it.srcFile.path}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "syncProcess: download exception: ${it.srcFile.path} to ${it.destPath}", e)
                }
                if (!threadCondition.block(0)) {
                    Log.i(TAG, "syncProcess: thread paused")
                    break
                }
            }
            SmbSyncService.start(contextWeakReference.get()!!, SmbSyncService.START_TYPE_STOP_SYNC)
            threadCondition.close()
        }
        Log.e(TAG, "syncProcess: thread interrupted")
        syncThread = null
    }

    fun dump(
        fd: FileDescriptor?,
        writer: PrintWriter?,
        args: Array<out String?>?
    ) {
        if (args.isNullOrEmpty()) {
            return
        }
        when (args[0]) {
            "--clearSyncFileInfos"-> clearSyncFileInfos()
        }
    }

    companion object {
        private var instance: SmbSyncManager? = null

        @Synchronized
        fun getInstance(context: Context): SmbSyncManager {
            if (instance == null) {
                instance = SmbSyncManager(context.applicationContext)
            }
            return instance!!
        }
    }
}