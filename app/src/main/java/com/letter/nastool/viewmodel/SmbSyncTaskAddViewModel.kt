package com.letter.nastool.viewmodel

import android.app.Application
import android.content.Context
import android.os.ConditionVariable
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.letter.nastool.database.entity.SmbSyncTaskEntity
import com.letter.nastool.manager.SmbSyncManager
import com.letter.nastool.R
import com.letter.nastool.data.local.FileChooseInfo
import com.letter.nastool.repository.SmbRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "SmbSyncTaskAddViewModel"

class SmbSyncTaskAddViewModel(application: Application): AndroidViewModel(application) {

    val taskName = MutableLiveData("")
    val serviceAddress = MutableLiveData("")
    val username = MutableLiveData("")
    val password = MutableLiveData("")
    val remotePath = MutableLiveData("")
    val localPath = MutableLiveData("")
    val fileType = MutableLiveData("*")
    val includeSubDirs = MutableLiveData(true)

    var task: SmbSyncTaskEntity? = null

    val smbSyncManager by lazy {
        SmbSyncManager.getInstance(application)
    }

    fun load(id: Int) {
        if (id < 0) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            task = smbSyncManager.getAllTasks().find { it.id == id }
            task?.let {
                taskName.postValue(it.name)
                serviceAddress.postValue(it.serverAddress)
                username.postValue(it.username)
                password.postValue(it.password)
                remotePath.postValue(it.remotePath)
                localPath.postValue(it.localPath)
                fileType.postValue(it.fileType)
                includeSubDirs.postValue(it.includeSubDirs)
            }
        }
    }

    fun save(onResult: (Boolean) -> Unit) {
        Log.i(TAG, "save: ${taskName.value}, ${serviceAddress.value}, ${username.value}, ${password.value}, ${remotePath.value}, ${localPath.value}")
        var result = false
        viewModelScope.launch(Dispatchers.IO) {
            if (task != null) {
                task!!.let {
                    it.name = taskName.value ?: ""
                    it.serverAddress = serviceAddress.value ?: ""
                    it.username = username.value ?: ""
                    it.password = password.value ?: ""
                    it.remotePath = remotePath.value ?: ""
                    it.localPath = localPath.value ?: ""
                    it.fileType = fileType.value ?: ""
                    it.includeSubDirs = includeSubDirs.value == true
                    if (smbSyncManager.check(it)) {
                        smbSyncManager.updateTask(it)
                        result = true
                    }
                }
            } else {
                val task = SmbSyncTaskEntity(
                    name = taskName.value ?: "",
                    serverAddress = serviceAddress.value ?: "",
                    username = username.value ?: "",
                    password = password.value ?: "",
                    remotePath = remotePath.value ?: "",
                    localPath = localPath.value ?: "",
                    fileType = fileType.value ?: "",
                    includeSubDirs = includeSubDirs.value == true

                )
                if (smbSyncManager.check(task)) {
                    smbSyncManager.addTask(task)
                    result = true
                }
            }
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun delete() {
        if (task != null) {
            viewModelScope.launch(Dispatchers.IO) {
                smbSyncManager.deleteTask(task!!)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication<Application>(),
                        R.string.activity_smb_sync_task_add_toast_delete_success,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun listSmbFiles(context: Context, path: String): List<FileChooseInfo> {
        var list = emptyList<FileChooseInfo>()
        val condition = ConditionVariable(false)
        viewModelScope.launch(Dispatchers.IO) {
            val smbRepo = SmbRepo(
                serviceAddress.value ?: "",
                username.value ?: "",
                password.value ?: ""
            )
            if (smbRepo.checkConnection(path)) {
                val smbFiles = smbRepo.listFiles(path)
                list = smbFiles.map {
                    FileChooseInfo(
                        it.name,
                        it.isDirectory
                    )
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        R.string.activity_smb_sync_task_add_toast_check_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            smbRepo.close()
            condition.open()
        }
        condition.block()
        return list.sortedWith(compareBy(
            { !it.isDirectory },
            { it.name.lowercase() }
        ))
    }

    fun listLocalFiles(path: String): List<FileChooseInfo> {
        val list = mutableListOf<FileChooseInfo>()
        File(path).apply {
            listFiles {
                list.add(
                    FileChooseInfo(
                        it.name,
                        it.isDirectory
                    )
                )
            }
        }
        return list.sortedWith(compareBy(
            { !it.isDirectory },
            { it.name.lowercase() }
        ))
    }

}