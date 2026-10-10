package com.uzxsan.dev.track.todo

import com.uzxsan.dev.track.todo.data.TaskWithStages
import com.uzxsan.dev.track.todo.data.TodoStage
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val TaskWithStages.doneCount: Int get() = stages.count { it.done }

val TaskWithStages.isDone: Boolean get() = stages.isNotEmpty() && stages.all { it.done }

/** The stage to work on next, or null when the task is done. */
val TaskWithStages.currentStage: TodoStage? get() = stages.firstOrNull { !it.done }

/** Day the last stage was completed, or null while the task is still open. */
fun TaskWithStages.completedOn(zone: ZoneId = ZoneId.systemDefault()): LocalDate? {
    if (!isDone) return null
    val last = stages.mapNotNull { it.completedAt }.maxOrNull() ?: return task.localDate
    return Instant.ofEpochMilli(last).atZone(zone).toLocalDate()
}

/** Marks the current stage done; returns the stages to persist (empty if already done). */
fun TaskWithStages.advance(now: Long): List<TodoStage> =
    listOfNotNull(currentStage?.copy(done = true, completedAt = now))

/** Toggles one stage, keeping completion time only while it is done. */
fun TodoStage.toggled(now: Long): TodoStage =
    if (done) copy(done = false, completedAt = null) else copy(done = true, completedAt = now)

fun weekStart(date: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate =
    date.minusDays(((date.dayOfWeek.value - firstDayOfWeek.value + 7) % 7).toLong())

fun weekDays(date: LocalDate, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val start = weekStart(date, firstDayOfWeek)
    return (0L until 7L).map { start.plusDays(it) }
}

data class TodoSummary(
    val todayDone: Int = 0,
    val todayTotal: Int = 0,
    val doneThisWeek: Int = 0,
    val open: Int = 0,
    /** Tasks completed per day, keyed by completion date. */
    val completionsByDay: Map<LocalDate, Int> = emptyMap(),
)

fun summarize(
    tasks: List<TaskWithStages>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    zone: ZoneId = ZoneId.systemDefault(),
): TodoSummary {
    val todays = tasks.filter { it.task.localDate == today }
    val completions = tasks.mapNotNull { it.completedOn(zone) }.groupingBy { it }.eachCount()
    val week = weekDays(today, firstDayOfWeek).toSet()
    return TodoSummary(
        todayDone = todays.count { it.isDone },
        todayTotal = todays.size,
        doneThisWeek = completions.filterKeys { it in week }.values.sum(),
        open = tasks.count { !it.isDone },
        completionsByDay = completions,
    )
}
