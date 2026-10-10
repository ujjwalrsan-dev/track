package com.uzxsan.dev.track.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.TrackApplication
import com.uzxsan.dev.track.navigation.LaunchRequest
import com.uzxsan.dev.track.navigation.Routes
import com.uzxsan.dev.track.todo.advance
import com.uzxsan.dev.track.todo.currentStage
import com.uzxsan.dev.track.todo.data.TaskWithStages
import com.uzxsan.dev.track.todo.doneCount
import com.uzxsan.dev.track.todo.isDone
import com.uzxsan.dev.track.todo.timeLabel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Home-screen widget listing today's tasks; ✓ completes a task's current stage in place. */
class TodoWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as TrackApplication).container.todoRepository
        val today = LocalDate.now()
        val todays = repository.observeAll().map { all -> all.filter { it.task.localDate == today } }
        val initial = todays.first()
        provideContent {
            // Live while the widget session is active; WidgetUpdater re-renders it after app changes.
            val tasks by todays.collectAsState(initial)
            GlanceTheme { TodoWidgetContent(today, tasks) }
        }
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}

/** Completes the current stage of the task whose id is passed in [TaskIdKey]. */
class AdvanceStageAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        val repository = (context.applicationContext as TrackApplication).container.todoRepository
        val item = repository.get(taskId) ?: return
        repository.updateStages(item.advance(System.currentTimeMillis()))
        TodoWidget().update(context, glanceId)
    }
}

val TaskIdKey = ActionParameters.Key<Long>("taskId")

@Composable
private fun TodoWidgetContent(today: LocalDate, tasks: List<TaskWithStages>) {
    val context = LocalContext.current
    val openTodo = actionStartActivity(LaunchRequest(Routes.TODO, openAdd = false).toIntent(context))
    val addTask = actionStartActivity(LaunchRequest(Routes.TODO, openAdd = true).toIntent(context))

    GlassWidgetPanel(onClick = openTodo) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                WidgetText(
                    text = context.getString(R.string.widget_today),
                    size = 20.sp,
                    weight = FontWeight.Bold,
                )
                val done = tasks.count { it.isDone }
                WidgetText(
                    text = if (tasks.isEmpty()) {
                        today.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
                    } else {
                        context.getString(R.string.todo_progress, done, tasks.size)
                    },
                    size = 12.sp,
                    color = WidgetColors.secondaryText,
                )
            }
            AccentCircleButton(
                icon = R.drawable.ic_add,
                contentDescription = context.getString(R.string.todo_add_task),
                onClick = addTask,
            )
        }
        Spacer(GlanceModifier.height(12.dp))

        if (tasks.isEmpty()) {
            Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WidgetText(context.getString(R.string.widget_nothing_today), size = 14.sp)
                    WidgetText(
                        context.getString(R.string.widget_tap_to_add),
                        size = 12.sp,
                        color = WidgetColors.secondaryText,
                    )
                }
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(tasks, itemId = { it.task.id }) { item ->
                    Column {
                        TaskRow(item)
                        Spacer(GlanceModifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(item: TaskWithStages) {
    val context = LocalContext.current
    val done = item.isDone
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .glassTile()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckButton(item)
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = item.task.title,
                maxLines = 1,
                style = TextStyle(
                    color = if (done) WidgetColors.secondaryText else WidgetColors.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                ),
            )
            val stage = item.currentStage
            val status = if (stage == null) {
                context.getString(R.string.todo_done)
            } else {
                context.getString(R.string.todo_stage_progress, item.doneCount + 1, item.stages.size, stage.name)
            }
            WidgetText(
                text = listOfNotNull(item.timeLabel(), status).joinToString(" · "),
                size = 11.sp,
                color = WidgetColors.secondaryText,
            )
            if (item.stages.size > 1) {
                Spacer(GlanceModifier.height(6.dp))
                StageBar(item)
            }
        }
    }
}

/** ✓ ring that completes the current stage; a filled check once the task is done. */
@Composable
private fun CheckButton(item: TaskWithStages) {
    val context = LocalContext.current
    val primary = GlanceTheme.colors.primary
    if (item.isDone) {
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .background(ImageProvider(R.drawable.widget_circle), colorFilter = ColorFilter.tint(primary)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_check),
                contentDescription = context.getString(R.string.todo_done),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                modifier = GlanceModifier.size(18.dp),
            )
        }
    } else {
        // The ring is an Image, not a tinted background: background tinting fills outline-only shapes.
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .clickable(actionRunCallback<AdvanceStageAction>(actionParametersOf(TaskIdKey to item.task.id))),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.widget_ring),
                contentDescription = null,
                colorFilter = ColorFilter.tint(primary),
                modifier = GlanceModifier.fillMaxSize(),
            )
            Image(
                provider = ImageProvider(R.drawable.ic_check),
                contentDescription = context.getString(R.string.todo_complete_stage),
                colorFilter = ColorFilter.tint(primary),
                modifier = GlanceModifier.size(16.dp),
            )
        }
    }
}

/** One segment per stage: done = accent, current = soft accent, upcoming = faint. */
@Composable
private fun StageBar(item: TaskWithStages) {
    val primary = GlanceTheme.colors.primary
    val current = item.stages.indexOfFirst { !it.done }
    SegmentRow(count = item.stages.size, gap = 3.dp) { index ->
        val alpha = when {
            item.stages[index].done -> 1f
            index == current -> 0.4f
            else -> 0.15f
        }
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(4.dp)
                .background(ImageProvider(R.drawable.widget_pill), alpha = alpha, colorFilter = ColorFilter.tint(primary)),
        ) {}
    }
}
