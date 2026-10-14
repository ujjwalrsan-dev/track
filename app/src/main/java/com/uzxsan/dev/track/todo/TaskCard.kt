package com.uzxsan.dev.track.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.todo.data.TaskWithStages
import com.uzxsan.dev.track.todo.data.TodoStage
import com.uzxsan.dev.track.ui.components.GlassButtonStyle
import com.uzxsan.dev.track.ui.components.GlassSurface
import com.uzxsan.dev.track.ui.components.GlassTintedButton
import com.uzxsan.dev.track.ui.theme.accentStrong
import com.uzxsan.dev.track.ui.theme.accentTrack
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/**
 * Glass card for one task: title, current stage, a segmented stage-progress bar and a button that
 * completes the current stage. Tapping the card expands the stage checklist, notes, links and actions.
 */
@Composable
fun TaskCard(
    item: TaskWithStages,
    onAdvance: () -> Unit,
    onToggleStage: (TodoStage) -> Unit,
    onRollover: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showTime: Boolean = false,
) {
    var expanded by rememberSaveable(item.task.id) { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val done = item.isDone

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        onClick = { expanded = !expanded },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        color = if (done) scheme.onSurfaceVariant else scheme.onSurface,
                    )
                    val current = item.currentStage
                    val status = if (current == null) {
                        stringResource(R.string.todo_done)
                    } else {
                        stringResource(R.string.todo_stage_progress, item.doneCount + 1, item.stages.size, current.name)
                    }
                    val time = item.task.localTime?.takeIf { showTime }?.format(timeFormatter)
                    Text(
                        text = listOfNotNull(time, status).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    val delay = item.task.delayDays
                    if (delay > 0) {
                        Spacer(Modifier.height(6.dp))
                        DelayTag(delay.toInt())
                    }
                }
                Spacer(Modifier.size(12.dp))
                if (done) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentStrong()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_check), stringResource(R.string.todo_done), tint = Color.White)
                    }
                } else {
                    GlassTintedButton(
                        icon = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.todo_complete_stage),
                        onClick = onAdvance,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            StageBar(item.stages)

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(Modifier.padding(top = 8.dp)) {
                    item.stages.forEachIndexed { index, stage ->
                        StageRow(index + 1, stage, onClick = { onToggleStage(stage) })
                    }
                    TaskDetails(item.task.notes, parseLinks(item.task.links))
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!done) {
                            GlassTintedButton(
                                icon = painterResource(R.drawable.ic_rollover),
                                text = stringResource(R.string.todo_rollover),
                                onClick = onRollover,
                                style = GlassButtonStyle.Secondary,
                            )
                        }
                        GlassTintedButton(
                            icon = painterResource(R.drawable.ic_edit),
                            text = stringResource(R.string.todo_edit),
                            contentDescription = stringResource(R.string.todo_edit_details),
                            onClick = onEdit,
                            style = GlassButtonStyle.Secondary,
                        )
                        GlassTintedButton(
                            icon = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.todo_delete),
                            onClick = onDelete,
                            style = GlassButtonStyle.Destructive,
                        )
                    }
                }
            }
        }
    }
}

/** Pill shown on rolled-over tasks: "Delayed by N days". */
@Composable
private fun DelayTag(days: Int) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = pluralStringResource(R.plurals.todo_delayed_by, days, days),
        style = MaterialTheme.typography.labelSmall,
        color = scheme.onErrorContainer,
        modifier = Modifier
            .clip(CircleShape)
            .background(scheme.errorContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Notes text and tappable links, shown in the expanded card when present. */
@Composable
private fun TaskDetails(notes: String, links: List<String>) {
    if (notes.isBlank() && links.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    Column(Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)) {
        if (notes.isNotBlank()) {
            Row {
                Icon(
                    painterResource(R.drawable.ic_notes),
                    contentDescription = stringResource(R.string.todo_notes),
                    modifier = Modifier.padding(top = 2.dp).size(16.dp),
                    tint = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(8.dp))
                Text(notes, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
        }
        links.forEach { link ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { runCatching { uriHandler.openUri(linkUri(link)) } }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_link),
                    contentDescription = stringResource(R.string.todo_links),
                    modifier = Modifier.size(16.dp),
                    tint = scheme.primary,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = link,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.primary,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** One segment per stage: done = primary, current = soft primary, upcoming = light track. */
@Composable
private fun StageBar(stages: List<TodoStage>) {
    val scheme = MaterialTheme.colorScheme
    val current = stages.indexOfFirst { !it.done }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        stages.forEachIndexed { index, stage ->
            val color = when {
                stage.done -> scheme.primary
                index == current -> scheme.primary.copy(alpha = 0.35f)
                else -> accentTrack()
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun StageRow(number: Int, stage: TodoStage, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .then(
                    if (stage.done) Modifier.background(scheme.primary)
                    else Modifier.border(1.5.dp, scheme.primary.copy(alpha = 0.5f), CircleShape)
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (stage.done) {
                Icon(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp), tint = scheme.onPrimary)
            } else {
                Text(number.toString(), style = MaterialTheme.typography.labelMedium, color = scheme.primary)
            }
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = stage.name,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (stage.done) TextDecoration.LineThrough else null,
            color = if (stage.done) scheme.onSurfaceVariant else scheme.onSurface,
        )
    }
}

/** Formats a task's time for timeline labels. */
fun TaskWithStages.timeLabel(): String? = task.localTime?.format(timeFormatter)
