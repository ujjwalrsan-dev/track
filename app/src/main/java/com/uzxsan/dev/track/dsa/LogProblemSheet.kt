package com.uzxsan.dev.track.dsa

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.dsa.data.Difficulty
import com.uzxsan.dev.track.dsa.data.Platform
import com.uzxsan.dev.track.ui.components.GlassBottomSheet
import com.uzxsan.dev.track.ui.components.LocalBackdrop
import com.uzxsan.dev.track.ui.components.PremiumTextField
import com.uzxsan.dev.track.ui.theme.accentAssistChipBorder
import com.uzxsan.dev.track.ui.theme.accentAssistChipColors
import com.uzxsan.dev.track.ui.theme.accentButtonColors
import com.uzxsan.dev.track.ui.theme.accentFilterChipBorder
import com.uzxsan.dev.track.ui.theme.accentFilterChipColors
import com.uzxsan.dev.track.ui.theme.accentSegmentedButtonColors
import com.uzxsan.dev.track.ui.theme.accentSuggestionChipBorder
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val DAY_MILLIS = 86_400_000L

private val commonTopics = listOf(
    "Array", "String", "Hashing", "Two Pointers", "Sliding Window", "Binary Search",
    "Stack", "Linked List", "Tree", "Graph", "DP", "Greedy", "Backtracking",
    "Heap", "Trie", "Math", "Bit Manipulation",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogProblemSheet(
    initialDate: LocalDate,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (title: String, platform: Platform, difficulty: Difficulty, topic: String, date: LocalDate) -> Unit,
    backdrop: Backdrop = LocalBackdrop.current,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var title by rememberSaveable { mutableStateOf("") }
    var platform by rememberSaveable { mutableStateOf(Platform.LeetCode) }
    var difficulty by rememberSaveable { mutableStateOf(Difficulty.Medium) }
    var topic by rememberSaveable { mutableStateOf("") }
    var dateEpoch by rememberSaveable { mutableLongStateOf(minOf(initialDate, today).toEpochDay()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(dateEpoch)

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
            Text(stringResource(R.string.log_title), style = MaterialTheme.typography.headlineSmall)

            PremiumTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.log_problem),
                placeholder = stringResource(R.string.log_problem_hint),
                leadingIcon = painterResource(R.drawable.ic_code),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel(stringResource(R.string.log_platform))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Platform.entries.forEach {
                    FilterChip(
                        selected = it == platform,
                        onClick = { platform = it },
                        label = { Text(it.label) },
                        colors = accentFilterChipColors(),
                        border = accentFilterChipBorder(selected = it == platform),
                    )
                }
            }

            SectionLabel(stringResource(R.string.log_difficulty))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Difficulty.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == difficulty,
                        onClick = { difficulty = option },
                        shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size),
                        colors = accentSegmentedButtonColors(),
                    ) { Text(option.label) }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = stringResource(R.string.log_topic),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    commonTopics.forEach {
                        SuggestionChip(
                            onClick = { topic = it },
                            label = { Text(it) },
                            border = accentSuggestionChipBorder(),
                        )
                    }
                }
            }

            SectionLabel(stringResource(R.string.log_solved_on))
            AssistChip(
                onClick = { showDatePicker = true },
                label = { Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_calendar), contentDescription = null) },
                colors = accentAssistChipColors(),
                border = accentAssistChipBorder(),
            )

            Button(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onSave(title, platform, difficulty, topic, date)
                    }
                },
                enabled = title.isNotBlank(),
                colors = accentButtonColors(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }
        }
    }

    if (showDatePicker) {
        val todayEpoch = today.toEpochDay()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateEpoch * DAY_MILLIS,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / DAY_MILLIS <= todayEpoch
                override fun isSelectableYear(year: Int) = year <= today.year
            },
        )
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
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
