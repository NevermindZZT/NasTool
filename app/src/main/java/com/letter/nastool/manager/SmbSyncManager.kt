package com.letter.nastool.manager

import android.content.Context
import android.media.MediaScannerConnection
import android.os.ConditionVariable
import android.util.Log
import android.webkit.MimeTypeMap
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

    private val stopCompleteCondition by lazy {
        ConditionVariable()
    }

    private var threadPaused = true

    private val taskInfos = mutableMapOf<Int, SmbSyncTaskInfo>()

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
        val syncing = hasUnCompletedTask(task.id) && !threadPaused
        val state = if (syncing) {
            SmbSyncTaskInfo.STATE_SYNCING
        } else {
            SmbSyncTaskInfo.STATE_NONE
        }
        var info = taskInfos[task.id]
        if (info == null) {
            info = SmbSyncTaskInfo(task.id, totalFiles, syncFiles.size, state)
        } else {
            info.let {
                it.total = totalFiles
                it.synced = syncFiles.size
                it.state = state
            }
        }
        syncFiles.forEach {
            val syncFileInfo = database.smbSyncFileInfoDao().get(task.id, it.path)
            if (syncFileInfo != null) {
                if (syncFileInfo.fileSize != it.fileSize || syncFileInfo.timestamp != it.timestamp) {
                    info.updates++
                }
            }
        }
        taskInfos.put(task.id, info)
        smbRepo.close()
        return info
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
                if (downloadInfos.value!!.firstOrNull { item -> item.task.id == task.id && item.srcFile.path == it.path } == null) {
                    downloadInfos.value!!.add(
                        SmbSyncDownloadInfo(task, smbRepo, it, dest)
                    )
                    Log.i(TAG, "addDownloadInfo: add download info: ${it.path} to $dest")
                    added += 1
                } else {
                    Log.i(TAG, "addDownloadInfo: download info already exists, skip: ${it.path} to $dest")
                }
            }
        }
        return added
    }

    private fun hasUnCompletedTask(taskId: Int): Boolean {
        return downloadInfos.value!!.firstOrNull {
            it.task.id == taskId && (it.state == SmbSyncDownloadInfo.STATE_PENDING || it.state == SmbSyncDownloadInfo.STATE_IN_PROGRESS)
        } != null
    }

    fun runTask(task: SmbSyncTaskEntity): Int {
        val smbRepo = SmbRepo(task.serverAddress, task.username, task.password)
        val added = addDownloadInfo(task, smbRepo, task.remotePath, task.localPath)
        if (hasUnCompletedTask(task.id)) {
            taskInfos[task.id]?.let {
                it.state = SmbSyncTaskInfo.STATE_SYNCING
                it.onChanged()
            }
        }
        startSync()
        return added
    }

    fun stopTask(task: SmbSyncTaskEntity) {
        stopSync()
    }

    fun isRunning(task: SmbSyncTaskEntity): Boolean {
        return taskInfos[task.id]?.state == SmbSyncTaskInfo.STATE_SYNCING
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
        stopCompleteCondition.close()
        if (threadCondition.block(1)) {
            threadCondition.close()
            stopCompleteCondition.block()
        }
    }

    private fun syncProcess() {
        while (!Thread.interrupted()) {
            threadCondition.block()
            SmbSyncService.start(contextWeakReference.get()!!, SmbSyncService.START_TYPE_START_SYNC)
            threadPaused = false
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
                            taskInfos[it.task.id]?.let {
                                if (it.updates > 0) {
                                    it.updates--
                                }
                                it.onChanged()
                            }
                        } else {
                            database.smbSyncFileInfoDao().insert(
                                SmbSyncFileInfoEntity(
                                    taskId = it.task.id,
                                    path = it.srcFile.path,
                                    fileSize = it.srcFile.length(),
                                    timestamp = it.srcFile.date
                                )
                            )
                            taskInfos[it.task.id]?.let {
                                it.synced++
                                it.onChanged()
                            }
                            Log.i(TAG, "syncProcess: insert sync file info: ${it.srcFile.path}")
                        }
                        MediaScannerConnection.scanFile(
                            contextWeakReference.get()!!,
                            arrayOf(dest),
                            arrayOf(
                                MimeTypeMap.getSingleton()
                                    .getMimeTypeFromExtension(
                                        dest.substringAfterLast('.', "")
                                    )
                                    ?.lowercase()
                            ),
                            null
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "syncProcess: download exception: ${it.srcFile.path} to ${it.destPath}", e)
                }
                taskInfos[it.task.id]?.let { taskInfo ->
                    val syncing = hasUnCompletedTask(it.task.id)
                    if (!syncing) {
                        taskInfo.state = SmbSyncTaskInfo.STATE_NONE
                        taskInfo.onChanged()
                    }
                }
                if (!threadCondition.block(1)) {
                    Log.i(TAG, "syncProcess: thread paused")
                    break
                }
            }
            SmbSyncService.start(contextWeakReference.get()!!, SmbSyncService.START_TYPE_STOP_SYNC)
            threadPaused = true
            stopCompleteCondition.open()
            threadCondition.close()
            taskInfos.forEach {
                it.value.state = SmbSyncTaskInfo.STATE_NONE
                it.value.onChanged()
            }
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