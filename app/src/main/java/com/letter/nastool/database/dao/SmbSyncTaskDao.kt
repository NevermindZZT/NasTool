package com.letter.nastool.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.letter.nastool.database.entity.SmbSyncTaskEntity

@Dao
interface SmbSyncTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg tasks: SmbSyncTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insert(tasks: Collection<SmbSyncTaskEntity>)

    @Query("SELECT * FROM smb_sync_tasks")
    fun getAll(): List<SmbSyncTaskEntity>

    @Query("SELECT * FROM smb_sync_tasks WHERE id = :id")
    fun get(id: Int): SmbSyncTaskEntity?

    @Query("SELECT * FROM smb_sync_tasks WHERE name = :name")
    fun get(name: String): SmbSyncTaskEntity?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(task: SmbSyncTaskEntity)

    @Delete
    fun delete(task: SmbSyncTaskEntity)
}