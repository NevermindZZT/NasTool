package com.letter.nastool.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.letter.nastool.R
import com.letter.nastool.database.entity.SmbSyncTaskEntity
import com.letter.nastool.manager.SmbSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmbSyncTaskListsViewModel(application: Application): AndroidViewModel(application) {

    val tasks: MutableLiveData<ObservableList<SmbSyncTaskEntity>> = MutableLiveData(
        ObservableArrayList()
    )

    val smbSyncManager by lazy {
        SmbSyncManager.getInstance(application)
    }

    fun loadTasks() {
        viewModelScope.launch {
            tasks.value?.clear()
            val syncTasks: List<SmbSyncTaskEntity>
            withContext(Dispatchers.IO) {
                syncTasks = smbSyncManager.getAllTasks()
            }
            tasks.value?.addAll(syncTasks)
        }
    }

    fun run(position: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val added = smbSyncManager.runTask(tasks.value!![position])
            withContext(Dispatchers.Main) {
                val context = getApplication<Application>()
                val message = if (added > 0) {
                    context.getString(R.string.fragment_smb_sync_tasks_start_success).format(added)
                } else {
                    context.getString(R.string.fragment_smb_sync_tasks_start_no_new_task)
                }
                Toast.makeText(
                    context,
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun getTaskInfo(position: Int, onCompleted: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val stringBuilder = StringBuilder()
            val task = tasks.value!![position]
            val info = smbSyncManager.getSyncTaskInfo(task)
            val context = getApplication<Application>()
            stringBuilder.append(
                context.getString(R.string.fragment_smb_sync_tasks_task_info_total).format(info.total)
            )
            stringBuilder.append("\n")
            stringBuilder.append(
                context.getString(R.string.fragment_smb_sync_tasks_task_info_synced).format(info.synced)
            )
            withContext(Dispatchers.Main) {
                onCompleted.invoke(stringBuilder.toString())
            }
        }
    }
}