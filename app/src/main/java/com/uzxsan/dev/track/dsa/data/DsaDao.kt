package com.uzxsan.dev.track.dsa.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DsaDao {
    @Query("SELECT * FROM dsa_problems ORDER BY solvedOn DESC, createdAt DESC")
    fun observeAll(): Flow<List<DsaProblem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(problem: DsaProblem): Long

    @Delete
    suspend fun delete(problem: DsaProblem)
}
