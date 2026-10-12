package com.uzxsan.dev.track.todo

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.uzxsan.dev.track.data.TrackDatabase
import com.uzxsan.dev.track.todo.data.TodoRepository
import com.uzxsan.dev.track.todo.data.TodoTask
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Round-trips rollover and notes/links edits through Room. */
@RunWith(AndroidJUnit4::class)
class TodoRepositoryTest {
    private lateinit var db: TrackDatabase
    private lateinit var repository: TodoRepository
    private val today = LocalDate.of(2026, 10, 12)

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TrackDatabase::class.java).build()
        repository = TodoRepository(db.todoDao())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun only() = repository.observeAll().first().single()

    @Test
    fun rolloverMovesTaskAndKeepsStageProgress() = runBlocking {
        repository.add(TodoTask(title = "Write post", date = today.toEpochDay()), listOf("Draft", "Publish"))
        val added = only()
        repository.updateStages(added.advance(now = 1L))

        repository.update(only().task.rolledOver(today))
        repository.update(only().task.rolledOver(today))

        val rolled = only()
        assertEquals(today.plusDays(2).toEpochDay(), rolled.task.date)
        assertEquals(today.toEpochDay(), rolled.task.originalDate)
        assertEquals(2L, rolled.task.delayDays)
        assertEquals(listOf(true, false), rolled.stages.map { it.done })
    }

    @Test
    fun notesAndLinksAreSavedAndEditable() = runBlocking {
        repository.add(
            TodoTask(title = "Read", date = today.toEpochDay(), notes = "Ch. 3", links = "example.com"),
            emptyList(),
        )
        assertEquals("Ch. 3", only().task.notes)

        repository.update(only().task.copy(notes = "", links = "a.dev\nb.dev"))

        val edited = only().task
        assertTrue(edited.notes.isEmpty())
        assertEquals(listOf("a.dev", "b.dev"), parseLinks(edited.links))
    }
}
