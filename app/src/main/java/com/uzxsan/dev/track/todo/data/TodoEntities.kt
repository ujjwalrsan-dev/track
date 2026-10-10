package com.uzxsan.dev.track.todo.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate
import java.time.LocalTime

/** A task scheduled on [date] (epoch day), optionally at [timeMinutes] past midnight. */
@Entity(tableName = "todo_tasks", indices = [Index("date")])
data class TodoTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: Long,
    val timeMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val localDate: LocalDate get() = LocalDate.ofEpochDay(date)
    val localTime: LocalTime? get() = timeMinutes?.let { LocalTime.of(it / 60, it % 60) }
}

/** One ordered step of a task. A task is done once all of its stages are done. */
@Entity(
    tableName = "todo_stages",
    foreignKeys = [
        ForeignKey(
            entity = TodoTask::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class TodoStage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val position: Int,
    val name: String,
    val done: Boolean = false,
    val completedAt: Long? = null,
)

data class TaskWithStages(
    @Embedded val task: TodoTask,
    @Relation(parentColumn = "id", entityColumn = "taskId")
    val stageRows: List<TodoStage>,
) {
    val stages: List<TodoStage> get() = stageRows.sortedBy { it.position }
}
