package com.uzxsan.dev.track.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.ui.components.GlassBottomSheet
import com.uzxsan.dev.track.ui.components.LocalBackdrop
import com.uzxsan.dev.track.ui.components.PremiumTextField
import com.uzxsan.dev.track.ui.theme.accentAssistChipBorder
import com.uzxsan.dev.track.ui.theme.accentAssistChipColors
import com.uzxsan.dev.track.ui.theme.accentButtonColors
import com.uzxsan.dev.track.ui.theme.accentSuggestionChipBorder
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val DAY_MILLIS = 86_400_000L

private val stageSuggestions = listOf("Plan", "Research", "Draft", "Build", "Review", "Test", "Ship")

private val stageListSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

private fun List<String>.toMutableStateList() = mutableStateListOf<String>().also { it.addAll(this) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (title: String, date: LocalDate, time: LocalTime?, stages: List<String>) -> Unit,
    backdrop: Backdrop = LocalBackdrop.current,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var title by rememberSaveable { mutableStateOf("") }
    var dateEpoch by rememberSaveable { mutableLongStateOf(initialDate.toEpochDay()) }
    // Minutes past midnight, or -1 for "anytime".
    var timeMinutes by rememberSaveable { mutableStateOf(-1) }
    val stages = rememberSaveable(saver = stageListSaver) { mutableStateListOf() }
    var newStage by rememberSaveable { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val date = LocalDate.ofEpochDay(dateEpoch)
    val time = timeMinutes.takeIf { it >= 0 }?.let { LocalTime.of(it / 60, it % 60) }

    fun addStage(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) stages.add(trimmed)
        newStage = ""
    }

    GlassBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, backdrop = backdrop) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.todo_new_task), style = MaterialTheme.typography.headlineSmall)

            PremiumTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.todo_task_name),
                placeholder = stringResource(R.string.todo_task_hint),
                leadingIcon = painterResource(R.drawable.ic_task),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel(stringResource(R.string.todo_when))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_calendar), contentDescription = null) },
                    colors = accentAssistChipColors(),
                    border = accentAssistChipBorder(),
                )
                AssistChip(
                    onClick = { showTimePicker = true },
                    label = {
                        Text(time?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                            ?: stringResource(R.string.todo_add_time))
                    },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_schedule), contentDescription = null) },
                    colors = accentAssistChipColors(),
                    border = accentAssistChipBorder(),
                )
                if (time != null) {
                    IconButton(onClick = { timeMinutes = -1 }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.todo_remove_time),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            SectionLabel(stringResource(R.string.todo_stages))
            if (stages.isEmpty()) {
                Text(
                    stringResource(R.string.todo_stages_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column {
                    stages.forEachIndexed { index, name ->
                        StageEditorRow(number = index + 1, name = name, onRemove = { stages.removeAt(index) })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PremiumTextField(
                    value = newStage,
                    onValueChange = { newStage = it },
                    label = stringResource(R.string.todo_stage_name),
                    placeholder = stringResource(R.string.todo_stage_hint),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { addStage(newStage) }),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(8.dp))
                FilledTonalIconButton(
                    onClick = { addStage(newStage) },
                    enabled = newStage.isNotBlank(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(painterResource(R.drawable.ic_add), stringResource(R.string.todo_add_stage))
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                stageSuggestions.forEach {
                    SuggestionChip(onClick = { addStage(it) }, label = { Text(it) }, border = accentSuggestionChipBorder())
                }
            }

            Button(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onSave(title, date, time, stages.toList())
                    }
                },
                enabled = title.isNotBlank(),
                colors = accentButtonColors(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateEpoch * DAY_MILLIS)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateEpoch = it / DAY_MILLIS }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }

    if (showTimePicker) {
        val now = LocalTime.now()
        val pickerState = rememberTimePickerState(
            initialHour = time?.hour ?: now.hour,
            initialMinute = time?.minute ?: 0,
        )
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.todo_pick_time)) },
            confirmButton = {
                TextButton(onClick = {
                    timeMinutes = pickerState.hour * 60 + pickerState.minute
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { TimePicker(state = pickerState) }
    }
}

@Composable
private fun StageEditorRow(number: Int, name: String, onRemove: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), style = MaterialTheme.typography.labelMedium, color = scheme.onPrimaryContainer)
        }
        Spacer(Modifier.size(12.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove) {
            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.todo_remove_stage))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
