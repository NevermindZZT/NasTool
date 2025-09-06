package com.letter.nastool.data.local

import com.letter.nastool.database.entity.SmbSyncTaskEntity
import com.letter.nastool.repository.SmbRepo
import jcifs.smb.SmbFile

data class SmbSyncDownloadInfo(
    val task: SmbSyncTaskEntity,
    val repo: SmbRepo,
    val srcFile: SmbFile,
    val destPath: String,
    var isDownloaded: Boolean = false,
    var state: Int = STATE_PENDING,
    var onInfoChanged: (() -> Unit)? = null,
    var info: String? = null
) {
    companion object {
        const val STATE_PENDING = 0
        const val STATE_IN_PROGRESS = 1
        const val STATE_COMPLETED = 2
        const val STATE_FAILED = 3
    }
}
