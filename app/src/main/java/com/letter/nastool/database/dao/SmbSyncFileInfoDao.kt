package com.letter.nastool.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.letter.nastool.database.entity.SmbSyncFileInfoEntity

@Dao
interface SmbSyncFileInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg infos: SmbSyncFileInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(infos: Collection<SmbSyncFileInfoEntity>)

    @Query("SELECT * FROM smb_sync_file_info WHERE task_id = :taskId")
    fun getAllByTaskId(taskId: Int): List<SmbSyncFileInfoEntity>

    @Query("SELECT * FROM smb_sync_file_info WHERE task_id = :taskId AND path = :path")
    fun get(taskId: Int, path: String): SmbSyncFileInfoEntity?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(info: SmbSyncFileInfoEntity)

    @Delete
    fun delete(info: SmbSyncFileInfoEntity)

    @Query("DELETE FROM smb_sync_file_info")
    fun deleteAll()
}