package com.example.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.database.dao.AnalyticsDao
import com.example.database.dao.RunDao
import com.example.database.dao.RunPendingSyncDao
import com.example.database.entity.DeletedRunSyncEntity
import com.example.database.entity.RunEntity
import com.example.database.entity.RunPendingSyncEntity

@Database(
    entities = [
        RunEntity::class,
        RunPendingSyncEntity::class,
        DeletedRunSyncEntity::class
    ],
    version = 3
)
abstract class RunDatabase : RoomDatabase() {

    abstract val runDao: RunDao
    abstract val runPendingSyncDao: RunPendingSyncDao
    abstract val analyticsDao: AnalyticsDao
}