package com.letter.nastool.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smb_sync_tasks")
data class SmbSyncTaskEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "server") var serverAddress: String = "",
    @ColumnInfo(name = "username") var username: String = "",
    @ColumnInfo(name = "password") var password: String = "",
    @ColumnInfo(name = "remote_path") var remotePath: String = "",
    @ColumnInfo(name = "local_path") var localPath: String = "",
    @ColumnInfo(name = "file_type") var fileType: String = "*",
    @ColumnInfo(name = "last_sync") var lastSync: Long = 0L
)
