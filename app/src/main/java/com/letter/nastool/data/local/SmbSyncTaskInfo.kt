package com.letter.nastool.data.local

data class SmbSyncTaskInfo(
    val taskId: Int,
    var total: Int,
    var synced: Int,
    var state: Int = STATE_NONE,
    var updates: Int = 0,
    var onInfoChanged: ((SmbSyncTaskInfo) -> Unit)? = null
) {
    fun onChanged() {
        try {
            onInfoChanged?.invoke(this)
        } catch (e: Exception) {
            onInfoChanged = null
        }
    }

    companion object {
        const val STATE_NONE = 0
        const val STATE_SYNCING = 1
    }
}
