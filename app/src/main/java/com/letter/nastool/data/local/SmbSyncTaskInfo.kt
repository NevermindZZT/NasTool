package com.letter.nastool.data.local

data class SmbSyncTaskInfo(
    val taskId: Int,
    val total: Int,
    val synced: Int,
    val state: Int = STATE_NONE
) {
    companion object {
        const val STATE_NONE = 0
        const val STATE_SYNCING = 1
    }
}
