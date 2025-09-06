package com.letter.nastool.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.letter.nastool.database.dao.SmbSyncFileInfoDao
import com.letter.nastool.database.dao.SmbSyncTaskDao
import com.letter.nastool.database.entity.SmbSyncFileInfoEntity
import com.letter.nastool.database.entity.SmbSyncTaskEntity

@Database(entities = [SmbSyncTaskEntity::class, SmbSyncFileInfoEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {

    abstract fun smbSyncTaskDao(): SmbSyncTaskDao

    abstract fun smbSyncFileInfoDao(): SmbSyncFileInfoDao

    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun getInstance(context: Context): AppDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nastool_db"
                ).build()
            }
            return instance!!
        }
    }
}