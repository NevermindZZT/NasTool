package com.letter.nastool.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.letter.nastool.R
import com.letter.nastool.manager.SmbSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application): AndroidViewModel(application) {

    private val smbSyncManager by lazy {
        SmbSyncManager.getInstance(application)
    }

    fun clearFileSyncInfos() {
        viewModelScope.launch(Dispatchers.IO) {
            smbSyncManager.clearSyncFileInfos()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    getApplication<Application>(),
                    R.string.fragment_settings_dialog_clear_file_sync_infos_completed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}