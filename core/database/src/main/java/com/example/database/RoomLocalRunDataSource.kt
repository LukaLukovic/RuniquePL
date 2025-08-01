package com.example.database

import android.database.sqlite.SQLiteFullException
import com.example.core.domain.run.LocalRunDataSource
import com.example.core.domain.run.Run
import com.example.core.domain.run.RunId
import com.example.core.domain.util.DataError
import com.example.core.domain.util.Result
import com.example.database.dao.RunDao
import com.example.database.mappers.toRun
import com.example.database.mappers.toRunEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomLocalRunDataSource(
    private val runDao: RunDao
): LocalRunDataSource {
    override fun getRuns(): Flow<List<Run>> {
        return runDao.getRuns().map {
            it.map {
                it.toRun()
            }
        }
    }

    override suspend fun upsertRun(run: Run): Result<RunId, DataError.Local> {
        return try {
            val entity = run.toRunEntity()
            runDao.upsertRun(entity)
            Result.Success(entity.id)
        } catch (e: SQLiteFullException) {
            Result.Error(DataError.Local.DISK_FULL)
        }
    }

    override suspend fun upsertRuns(run: List<Run>): Result<List<RunId>, DataError.Local> {
        return try {
            val entities = run.map { it.toRunEntity() }
            runDao.upsertRuns(entities)
            Result.Success(entities.map { it.id })
        } catch (e: SQLiteFullException) {
            Result.Error(DataError.Local.DISK_FULL)
        }
    }

    override suspend fun deleteRun(id: String) {
        runDao.deleteRun(id)
    }

    override suspend fun deleteAllRuns() {
        runDao.deleteAllRuns()
    }
}