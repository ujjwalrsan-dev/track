package com.uzxsan.dev.track.todo.data

import kotlinx.coroutines.flow.Flow

class TodoRepository(private val dao: TodoDao) {
    fun observeAll(): Flow<List<TaskWithStages>> = dao.observeAll()

    suspend fun get(id: Long): TaskWithStages? = dao.get(id)

    suspend fun add(task: TodoTask, stageNames: List<String>) {
        val names = stageNames.ifEmpty { listOf(DEFAULT_STAGE) }
        dao.insertWithStages(task, names.mapIndexed { i, name -> TodoStage(taskId = 0, position = i, name = name) })
    }

    suspend fun update(task: TodoTask) = dao.updateTask(task)

    suspend fun updateStages(stages: List<TodoStage>) = dao.updateStages(stages)

    suspend fun delete(task: TodoTask) = dao.deleteTask(task)

    /** Re-inserts a deleted task with its original ids, for undo. */
    suspend fun restore(item: TaskWithStages) = dao.insertWithStages(item.task, item.stages)

    companion object {
        const val DEFAULT_STAGE = "Done"
    }
}
