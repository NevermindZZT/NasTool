package com.letter.nastool.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smb_sync_file_info")
data class SmbSyncFileInfoEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "task_id") var taskId: Int = 0,
    @ColumnInfo(name = "path") var path: String = "",
    @ColumnInfo(name = "file_size") var fileSize: Long = 0L,
    @ColumnInfo(name = "timestamp") var timestamp: Long = 0L
)
