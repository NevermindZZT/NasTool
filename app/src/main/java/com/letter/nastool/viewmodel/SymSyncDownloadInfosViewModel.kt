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

class SymSyncDownloadInfosViewModel(application: Application): AndroidViewModel(application) {

    val smbSyncManager = SmbSyncManager.getInstance(application)

    val infos = smbSyncManager.downloadInfos
}