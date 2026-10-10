package com.uzxsan.dev.track.todo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.todo.data.TaskWithStages
import com.uzxsan.dev.track.todo.data.TodoStage
import com.uzxsan.dev.track.ui.components.GlassExtendedFab
import com.uzxsan.dev.track.ui.components.GlassIconButton
import com.uzxsan.dev.track.ui.components.GlassSurface
import com.uzxsan.dev.track.ui.components.GlassTopBar
import com.uzxsan.dev.track.ui.components.LocalBackdrop
import com.uzxsan.dev.track.ui.components.MonthCalendar
import com.uzxsan.dev.track.ui.theme.accentSegmentedButtonColors
import com.uzxsan.dev.track.ui.theme.accentTrack
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Day and Week are the primary timelines (toggle); Month and Year live in the overflow menu. */
enum class TodoView { Day, Week, Month, Year }

private val primaryViews = listOf(TodoView.Day, TodoView.Week)
private val secondaryViews = listOf(TodoView.Month, TodoView.Year)

private fun TodoView.label() = when (this) {
    TodoView.Day -> R.string.todo_view_day
    TodoView.Week -> R.string.todo_view_week
    TodoView.Month -> R.string.todo_view_month
    TodoView.Year -> R.string.todo_view_year
}

/** Callbacks a task row needs, bundled so they can be passed through list builders. */
private class TaskActions(
    val advance: (TaskWithStages) -> Unit,
    val toggle: (TodoStage) -> Unit,
    val delete: (TaskWithStages) -> Unit,
)

@Composable
fun TodoScreen(
    onBack: () -> Unit,
    openAddOnStart: Boolean = false,
    viewModel: TodoViewModel = viewModel(factory = TodoViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }

    var view by rememberSaveable { mutableStateOf(TodoView.Day) }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var monthIndex by rememberSaveable { mutableIntStateOf(today.year * 12 + today.monthValue - 1) }
    var showSheet by rememberSaveable { mutableStateOf(openAddOnStart) }
    var menuOpen by remember { mutableStateOf(false) }
    val selected = LocalDate.ofEpochDay(selectedEpochDay)
    val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)

    fun select(date: LocalDate) {
        selectedEpochDay = date.toEpochDay()
        monthIndex = date.year * 12 + date.monthValue - 1
    }

    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    // The top bar, menu button and FAB float over the list, so their glass blurs the list too.
    val contentBackdrop = rememberLayerBackdrop()
    val glassBackdrop = rememberCombinedBackdrop(LocalBackdrop.current, contentBackdrop)

    val actions = remember(viewModel) {
        TaskActions(
            advance = viewModel::advance,
            toggle = viewModel::toggle,
            delete = { item ->
                viewModel.delete(item)
                scope.launch {
                    val result = snackbar.showSnackbar(
                        message = context.getString(R.string.todo_deleted, item.task.title),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restore(item)
                }
            },
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GlassTopBar(
                title = stringResource(R.string.todo_title),
                onBack = onBack,
                backdrop = glassBackdrop,
                actions = {
                    Box {
                        GlassIconButton(
                            icon = painterResource(R.drawable.ic_more_vert),
                            contentDescription = stringResource(R.string.todo_more_views),
                            onClick = { menuOpen = true },
                            backdrop = glassBackdrop,
                        )
                        ViewMenu(
                            expanded = menuOpen,
                            current = view,
                            onDismiss = { menuOpen = false },
                            onSelect = {
                                view = it
                                menuOpen = false
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            GlassExtendedFab(
                text = stringResource(R.string.todo_add_task),
                icon = painterResource(R.drawable.ic_add),
                expanded = fabExpanded,
                onClick = { showSheet = true },
                backdrop = glassBackdrop,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(contentBackdrop),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                HeaderCard(
                    view = view,
                    onViewChange = { view = it },
                    selected = selected,
                    today = today,
                    month = month,
                    firstDayOfWeek = firstDayOfWeek,
                    tasksByDate = state.tasksByDate,
                    onSelect = ::select,
                    onOpenDay = {
                        select(it)
                        view = TodoView.Day
                    },
                    onMonthChange = { monthIndex = it.year * 12 + it.monthValue - 1 },
                    onOpenMonth = {
                        monthIndex = it.year * 12 + it.monthValue - 1
                        view = TodoView.Month
                    },
                )
            }
            when (view) {
                TodoView.Day -> dayTimeline(selected, state.tasksByDate[selected].orEmpty(), actions)
                TodoView.Week -> weekList(selected, today, firstDayOfWeek, state.tasksByDate, actions) {
                    select(it)
                    view = TodoView.Day
                }
                // Month and Year are overviews; tapping a day or month drills into Day or Month.
                TodoView.Month, TodoView.Year -> Unit
            }
        }
    }

    if (showSheet) {
        AddTaskSheet(
            initialDate = selected,
            onDismiss = { showSheet = false },
            onSave = { title, date, time, stages ->
                viewModel.add(title, date, time, stages)
                select(date)
                showSheet = false
            },
            backdrop = glassBackdrop,
        )
    }
}

@Composable
private fun ViewMenu(
    expanded: Boolean,
    current: TodoView,
    onDismiss: () -> Unit,
    onSelect: (TodoView) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    ) {
        secondaryViews.forEach { option ->
            DropdownMenuItem(
                text = { Text(stringResource(option.label())) },
                onClick = { onSelect(option) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_calendar), contentDescription = null) },
                trailingIcon = if (option == current) {
                    { Icon(painterResource(R.drawable.ic_check), contentDescription = null) }
                } else null,
            )
        }
    }
}

@Composable
private fun HeaderCard(
    view: TodoView,
    onViewChange: (TodoView) -> Unit,
    selected: LocalDate,
    today: LocalDate,
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    tasksByDate: Map<LocalDate, List<TaskWithStages>>,
    onSelect: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onOpenMonth: (YearMonth) -> Unit,
) {
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp)) {
        Column(Modifier.padding(16.dp)) {
            // Only Day/Week are toggles; in Month/Year neither is selected and tapping one returns.
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                primaryViews.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == view,
                        onClick = { onViewChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, primaryViews.size),
                        colors = accentSegmentedButtonColors(),
                    ) { Text(stringResource(option.label())) }
                }
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                targetState = view,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "todo-view",
            ) { shown ->
                when (shown) {
                    TodoView.Day -> DayStrip(selected, today, firstDayOfWeek, tasksByDate, onSelect)
                    TodoView.Week -> WeekNavigator(selected, firstDayOfWeek, tasksByDate, onSelect)
                    TodoView.Month -> {
                        val inMonth = tasksByDate.filterKeys { YearMonth.from(it) == month }.values.flatten()
                        MonthCalendar(
                            month = month,
                            counts = tasksByDate.mapValues { it.value.size },
                            selected = selected,
                            today = today,
                            onSelect = onOpenDay,
                            onMonthChange = onMonthChange,
                            allowFuture = true,
                            subtitle = stringResource(
                                R.string.todo_progress,
                                inMonth.count { it.isDone },
                                inMonth.size,
                            ),
                        )
                    }
                    TodoView.Year -> YearGrid(month.year, today, tasksByDate, onMonthChange, onOpenMonth)
                }
            }
        }
    }
}

@Composable
private fun NavRow(title: String, subtitle: String?, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) {
            Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.todo_prev))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.todo_next))
        }
    }
}

/** Week strip for the Day view: pick a day; a dot shows whether that day has open or finished tasks. */
@Composable
private fun DayStrip(
    selected: LocalDate,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    tasksByDate: Map<LocalDate, List<TaskWithStages>>,
    onSelect: (LocalDate) -> Unit,
) {
    val locale = Locale.getDefault()
    val scheme = MaterialTheme.colorScheme
    Column {
        NavRow(
            title = "${selected.month.getDisplayName(TextStyle.FULL, locale)} ${selected.year}",
            subtitle = null,
            onPrev = { onSelect(selected.minusWeeks(1)) },
            onNext = { onSelect(selected.plusWeeks(1)) },
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            weekDays(selected, firstDayOfWeek).forEach { day ->
                val isSelected = day == selected
                val tasks = tasksByDate[day].orEmpty()
                val content = if (isSelected) scheme.onPrimary else scheme.onSurface
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) scheme.primary else Color.Transparent)
                        .clickable { onSelect(day) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = content.copy(alpha = 0.7f),
                    )
                    Text(
                        day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (day == today) FontWeight.Bold else null,
                        color = if (day == today && !isSelected) scheme.primary else content,
                    )
                    val dot = when {
                        tasks.isEmpty() -> Color.Transparent
                        tasks.all { it.isDone } -> if (isSelected) scheme.onPrimary else scheme.primary
                        else -> (if (isSelected) scheme.onPrimary else scheme.primary).copy(alpha = 0.4f)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dot)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekNavigator(
    selected: LocalDate,
    firstDayOfWeek: DayOfWeek,
    tasksByDate: Map<LocalDate, List<TaskWithStages>>,
    onSelect: (LocalDate) -> Unit,
) {
    val days = weekDays(selected, firstDayOfWeek)
    val tasks = days.flatMap { tasksByDate[it].orEmpty() }
    val dayMonth = DateTimeFormatter.ofPattern("d MMM")
    NavRow(
        title = "${days.first().format(dayMonth)} – ${days.last().format(dayMonth)}",
        subtitle = stringResource(R.string.todo_progress, tasks.count { it.isDone }, tasks.size),
        onPrev = { onSelect(selected.minusWeeks(1)) },
        onNext = { onSelect(selected.plusWeeks(1)) },
    )
}

/** Twelve month tiles with completion progress; tapping one opens it in the Month view. */
@Composable
private fun YearGrid(
    year: Int,
    today: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskWithStages>>,
    onYearChange: (YearMonth) -> Unit,
    onOpenMonth: (YearMonth) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val locale = Locale.getDefault()
    val inYear = tasksByDate.filterKeys { it.year == year }
    val all = inYear.values.flatten()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NavRow(
            title = year.toString(),
            subtitle = stringResource(R.string.todo_progress, all.count { it.isDone }, all.size),
            onPrev = { onYearChange(YearMonth.of(year - 1, 1)) },
            onNext = { onYearChange(YearMonth.of(year + 1, 1)) },
        )
        Month.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { m ->
                    val ym = YearMonth.of(year, m)
                    val tasks = inYear.filterKeys { it.month == m }.values.flatten()
                    val done = tasks.count { it.isDone }
                    val isCurrent = ym == YearMonth.from(today)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isCurrent) scheme.primaryContainer else accentTrack())
                            .clickable { onOpenMonth(ym) }
                            .padding(12.dp),
                    ) {
                        Text(
                            m.getDisplayName(TextStyle.SHORT, locale),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isCurrent) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                        )
                        Text(
                            if (tasks.isEmpty()) "–" else "$done/${tasks.size}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(scheme.primary.copy(alpha = 0.15f))
                        ) {
                            if (tasks.isNotEmpty() && done > 0) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(done.toFloat() / tasks.size)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(scheme.primary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.dayTimeline(day: LocalDate, tasks: List<TaskWithStages>, actions: TaskActions) {
    item(key = "day-title") {
        SectionTitle(
            title = day.format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
            trailing = tasks.takeIf { it.isNotEmpty() }?.let {
                stringResource(R.string.todo_progress, it.count { t -> t.isDone }, it.size)
            },
        )
    }
    if (tasks.isEmpty()) {
        item(key = "day-empty") { EmptyHint(stringResource(R.string.todo_empty_day)) }
    } else {
        itemsIndexed(tasks, key = { _, it -> "task-${it.task.id}" }) { index, item ->
            TimelineRow(
                item = item,
                isFirst = index == 0,
                isLast = index == tasks.lastIndex,
                actions = actions,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

private fun LazyListScope.weekList(
    selected: LocalDate,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    tasksByDate: Map<LocalDate, List<TaskWithStages>>,
    actions: TaskActions,
    onOpenDay: (LocalDate) -> Unit,
) {
    weekDays(selected, firstDayOfWeek).forEach { day ->
        val tasks = tasksByDate[day].orEmpty()
        item(key = "week-$day") {
            WeekDayHeader(day, day == today, tasks, onClick = { onOpenDay(day) })
        }
        tasks.forEach { item ->
            item(key = "task-${item.task.id}") {
                TaskCard(
                    item = item,
                    onAdvance = { actions.advance(item) },
                    onToggleStage = actions.toggle,
                    onDelete = { actions.delete(item) },
                    showTime = true,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

/**
 * One task on the Day timeline: time label, a rail with a node (filled when done), then the card.
 * The rail is drawn behind the row and extends through the list spacing so it reads as one line.
 */
@Composable
private fun TimelineRow(
    item: TaskWithStages,
    isFirst: Boolean,
    isLast: Boolean,
    actions: TaskActions,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val done = item.isDone
    val surface = scheme.surface
    Row(
        modifier = modifier.drawBehind {
            val x = 56.dp.toPx() + 10.dp.toPx()
            val nodeY = 30.dp.toPx()
            val gap = 12.dp.toPx()
            drawLine(
                color = scheme.primary.copy(alpha = 0.3f),
                start = Offset(x, if (isFirst) nodeY else 0f),
                end = Offset(x, if (isLast) nodeY else size.height + gap),
                strokeWidth = 2.dp.toPx(),
            )
            val radius = 6.dp.toPx()
            if (done) {
                drawCircle(scheme.primary, radius, Offset(x, nodeY))
            } else {
                drawCircle(surface, radius, Offset(x, nodeY))
                drawCircle(scheme.primary, radius, Offset(x, nodeY), style = Stroke(2.dp.toPx()))
            }
        },
    ) {
        Text(
            text = item.timeLabel() ?: stringResource(R.string.todo_anytime),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier
                .width(56.dp)
                .padding(top = 22.dp, end = 4.dp),
        )
        Spacer(Modifier.width(28.dp))
        TaskCard(
            item = item,
            onAdvance = { actions.advance(item) },
            onToggleStage = actions.toggle,
            onDelete = { actions.delete(item) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WeekDayHeader(day: LocalDate, isToday: Boolean, tasks: List<TaskWithStages>, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = day.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
            style = MaterialTheme.typography.titleMedium,
            color = if (isToday) scheme.primary else scheme.onSurface,
            fontWeight = if (isToday) FontWeight.Bold else null,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (tasks.isEmpty()) {
                stringResource(R.string.todo_empty_week_day)
            } else {
                stringResource(R.string.todo_progress, tasks.count { it.isDone }, tasks.size)
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (tasks.isEmpty()) scheme.onSurfaceVariant else scheme.primary,
        )
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String?) {
    Row(
        modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
    )
}
