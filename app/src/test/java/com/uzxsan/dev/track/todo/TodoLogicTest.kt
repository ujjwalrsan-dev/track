package com.uzxsan.dev.track.todo

import com.uzxsan.dev.track.todo.data.TaskWithStages
import com.uzxsan.dev.track.todo.data.TodoStage
import com.uzxsan.dev.track.todo.data.TodoTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class TodoLogicTest {
    private val today = LocalDate.of(2026, 10, 10) // a Saturday
    private val utc = ZoneOffset.UTC

    private fun millis(date: LocalDate) = date.atStartOfDay(utc).toInstant().toEpochMilli() + 3_600_000

    private fun task(id: Long, date: LocalDate, vararg stagesDone: LocalDate?) = TaskWithStages(
        TodoTask(id = id, title = "t$id", date = date.toEpochDay()),
        stagesDone.mapIndexed { i, doneOn ->
            TodoStage(id = id * 10 + i, taskId = id, position = stagesDone.size - 1 - i, name = "s$i",
                done = doneOn != null, completedAt = doneOn?.let(::millis))
        },
    )

    @Test
    fun stagesAreOrderedByPosition() {
        val t = task(1, today, null, null, null)
        assertEquals(listOf(0, 1, 2), t.stages.map { it.position })
    }

    @Test
    fun advanceCompletesStagesInOrder() {
        val t = task(1, today, null, null)
        val updated = t.advance(now = 5L).single()
        assertEquals(0, updated.position)
        assertTrue(updated.done)
        assertEquals(5L, updated.completedAt)
    }

    @Test
    fun doneTaskHasNoCurrentStageAndCannotAdvance() {
        val t = task(1, today, today, today)
        assertTrue(t.isDone)
        assertNull(t.currentStage)
        assertTrue(t.advance(now = 1L).isEmpty())
    }

    @Test
    fun completedOnIsTheLastStageCompletion() {
        val t = task(1, today, today.minusDays(2), today.minusDays(1))
        assertEquals(today.minusDays(1), t.completedOn(utc))
        assertNull(task(2, today, today, null).completedOn(utc))
    }

    @Test
    fun toggleClearsCompletionTime() {
        val stage = TodoStage(taskId = 1, position = 0, name = "a", done = true, completedAt = 9L)
        val undone = stage.toggled(now = 20L)
        assertFalse(undone.done)
        assertNull(undone.completedAt)
        assertEquals(20L, undone.toggled(now = 20L).completedAt)
    }

    @Test
    fun weekStartsOnLocaleFirstDay() {
        assertEquals(LocalDate.of(2026, 10, 5), weekStart(today, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 10, 4), weekStart(today, DayOfWeek.SUNDAY))
        assertEquals(7, weekDays(today, DayOfWeek.MONDAY).size)
    }

    @Test
    fun summaryCountsTodayAndWeek() {
        val tasks = listOf(
            task(1, today, today),                  // today, done today
            task(2, today, null),                   // today, open
            task(3, today.minusDays(3), today.minusDays(2)), // done Thursday (same Monday-week)
            task(4, today.minusDays(9), today.minusDays(9)), // done last week
        )
        val s = summarize(tasks, today, DayOfWeek.MONDAY, utc)
        assertEquals(1, s.todayDone)
        assertEquals(2, s.todayTotal)
        assertEquals(2, s.doneThisWeek)
        assertEquals(1, s.open)
    }
}
