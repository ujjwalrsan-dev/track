package com.uzxsan.dev.track.todo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.uzxsan.dev.track.todo.data.TaskWithStages
import com.uzxsan.dev.track.todo.data.TodoStage
import com.uzxsan.dev.track.todo.data.TodoTask
import com.uzxsan.dev.track.ui.theme.TrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskCardTest {
    @get:Rule
    val rule = createComposeRule()

    private fun item(done: Boolean, originalDate: Long? = null) = TaskWithStages(
        TodoTask(
            id = 1,
            title = "Ship release",
            date = 20002,
            originalDate = originalDate,
            notes = "Check changelog",
            links = "example.com/notes",
        ),
        listOf(TodoStage(id = 1, taskId = 1, position = 0, name = "Done", done = done)),
    )

    private var rollovers = 0
    private var edits = 0

    private fun show(item: TaskWithStages) = rule.setContent {
        TrackTheme {
            TaskCard(
                item = item,
                onAdvance = {},
                onToggleStage = {},
                onRollover = { rollovers++ },
                onEdit = { edits++ },
                onDelete = {},
            )
        }
    }

    @Test
    fun rolledOverTaskShowsDelayTag() {
        show(item(done = false, originalDate = 20000))
        rule.onNodeWithText("Delayed by 2 days").assertIsDisplayed()
    }

    @Test
    fun taskThatWasNeverRolledOverHasNoTag() {
        show(item(done = false))
        rule.onNodeWithText("Delayed by", substring = true).assertDoesNotExist()
    }

    @Test
    fun expandedCardShowsDetailsAndActions() {
        show(item(done = false))
        rule.onNodeWithText("Ship release").performClick()

        rule.onNodeWithText("Check changelog").assertIsDisplayed()
        rule.onNodeWithText("example.com/notes").assertIsDisplayed()
        rule.onNodeWithText("Roll over").performClick()
        rule.onNodeWithText("Edit").performClick()
        rule.onNodeWithContentDescription("Delete task").assertIsDisplayed()
        rule.runOnIdle {
            assertEquals(1, rollovers)
            assertEquals(1, edits)
        }
    }

    @Test
    fun doneTaskCannotBeRolledOver() {
        show(item(done = true))
        rule.onNodeWithText("Ship release").performClick()
        rule.onNodeWithText("Edit").assertIsDisplayed()
        rule.onNodeWithText("Roll over").assertDoesNotExist()
    }
}
