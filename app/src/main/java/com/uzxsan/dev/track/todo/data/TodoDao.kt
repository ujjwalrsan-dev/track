package com.uzxsan.dev.track.todo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Transaction
    @Query("SELECT * FROM todo_tasks ORDER BY date, timeMinutes IS NULL, timeMinutes, createdAt")
    fun observeAll(): Flow<List<TaskWithStages>>

    @Transaction
    @Query("SELECT * FROM todo_tasks WHERE id = :id")
    suspend fun get(id: Long): TaskWithStages?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TodoTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStages(stages: List<TodoStage>)

    @Update
    suspend fun updateStages(stages: List<TodoStage>)

    @Delete
    suspend fun deleteTask(task: TodoTask)

    @Transaction
    suspend fun insertWithStages(task: TodoTask, stages: List<TodoStage>) {
        val taskId = insertTask(task)
        insertStages(stages.map { it.copy(taskId = taskId) })
    }
}
