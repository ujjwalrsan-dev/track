package com.uzxsan.dev.track.dsa

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.dsa.data.Difficulty
import com.uzxsan.dev.track.dsa.data.DsaProblem
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.uzxsan.dev.track.ui.components.GlassExtendedFab
import com.uzxsan.dev.track.ui.components.GlassSurface
import com.uzxsan.dev.track.ui.components.GlassTopBar
import com.uzxsan.dev.track.ui.components.LocalBackdrop
import com.uzxsan.dev.track.ui.components.GlassTint
import com.uzxsan.dev.track.ui.components.HeatLegend
import com.uzxsan.dev.track.ui.components.MonthCalendar
import com.uzxsan.dev.track.ui.components.YearHeatmap
import com.uzxsan.dev.track.ui.theme.accentSegmentedButtonColors
import com.uzxsan.dev.track.ui.theme.accentTrack
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private enum class CalendarMode { Month, Year }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DsaScreen(
    onBack: () -> Unit,
    openAddOnStart: Boolean = false,
    viewModel: DsaViewModel = viewModel(factory = DsaViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }

    var selectedEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var monthIndex by rememberSaveable { mutableIntStateOf(today.year * 12 + today.monthValue - 1) }
    var mode by rememberSaveable { mutableStateOf(CalendarMode.Month) }
    var showSheet by rememberSaveable { mutableStateOf(openAddOnStart) }
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
    val dayProblems = state.problemsByDay[selected].orEmpty()
    // The top pill and FAB float over the list, so their glass refracts the list content as well as the background.
    val contentBackdrop = rememberLayerBackdrop()
    val glassBackdrop = rememberCombinedBackdrop(LocalBackdrop.current, contentBackdrop)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GlassTopBar(
                title = stringResource(R.string.dsa_title),
                onBack = onBack,
                backdrop = glassBackdrop,
            )
        },
        floatingActionButton = {
            GlassExtendedFab(
                text = stringResource(R.string.dsa_log_problem),
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
            item(key = "stats") { StatsRow(state.stats) }
            item(key = "difficulty") { DifficultyCard(state.stats) }
            item(key = "calendar") {
                CalendarCard(
                    mode = mode,
                    onModeChange = { mode = it },
                    month = month,
                    counts = state.countsByDay,
                    selected = selected,
                    today = today,
                    onSelect = ::select,
                    onMonthChange = { monthIndex = it.year * 12 + it.monthValue - 1 },
                )
            }
            item(key = "day-header") { DayHeader(selected, dayProblems.size) }
            if (dayProblems.isEmpty()) {
                item(key = "empty") { EmptyDay(onLog = { showSheet = true }) }
            } else {
                items(dayProblems, key = { it.id }) { problem ->
                    ProblemRow(
                        problem = problem,
                        modifier = Modifier.animateItem(),
                        onDelete = {
                            viewModel.delete(problem)
                            scope.launch {
                                val result = snackbar.showSnackbar(
                                    message = context.getString(R.string.dsa_deleted, problem.title),
                                    actionLabel = context.getString(R.string.undo),
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) viewModel.restore(problem)
                            }
                        },
                    )
                }
            }
        }
    }

    if (showSheet) {
        LogProblemSheet(
            initialDate = selected,
            today = today,
            onDismiss = { showSheet = false },
            onSave = { title, platform, difficulty, topic, date ->
                viewModel.log(title, platform, difficulty, topic, date)
                select(date)
                showSheet = false
            },
            backdrop = glassBackdrop,
        )
    }
}

@Composable
private fun StatsRow(stats: DsaStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            label = stringResource(R.string.dsa_total),
            value = stats.total.toString(),
            supporting = stringResource(R.string.dsa_all_time),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.dsa_streak),
            value = stats.currentStreak.toString(),
            supporting = stringResource(R.string.dsa_best_streak, stats.longestStreak),
            highlighted = true,
            icon = R.drawable.ic_fire,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.dsa_active_days),
            value = stats.activeDays.toString(),
            supporting = stringResource(R.string.dsa_all_time),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    icon: Int? = null,
) {
    val scheme = MaterialTheme.colorScheme
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        tint = if (highlighted) scheme.primaryContainer.copy(alpha = 0.6f) else GlassTint,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (highlighted) scheme.onPrimaryContainer else scheme.onSurface
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    if (icon != null) Icon(painterResource(icon), contentDescription = null, Modifier.size(18.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.headlineMedium)
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DifficultyCard(stats: DsaStats) {
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.dsa_by_difficulty), style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(accentTrack()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Difficulty.entries.forEach { difficulty ->
                    val count = stats.byDifficulty[difficulty] ?: 0
                    if (count > 0) {
                        Box(
                            Modifier
                                .weight(count.toFloat())
                                .height(12.dp)
                                .background(difficultyColor(difficulty))
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Difficulty.entries.forEach { difficulty ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(difficultyColor(difficulty)))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "${difficulty.label} ${stats.byDifficulty[difficulty] ?: 0}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarCard(
    mode: CalendarMode,
    onModeChange: (CalendarMode) -> Unit,
    month: YearMonth,
    counts: Map<LocalDate, Int>,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
) {
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp)) {
        Column(Modifier.padding(16.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                CalendarMode.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == mode,
                        onClick = { onModeChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, CalendarMode.entries.size),
                        colors = accentSegmentedButtonColors(),
                    ) {
                        Text(
                            stringResource(
                                if (option == CalendarMode.Month) R.string.dsa_view_month else R.string.dsa_view_year
                            )
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "calendar-mode",
            ) { shown ->
                when (shown) {
                    CalendarMode.Month -> MonthCalendar(
                        month = month,
                        counts = counts,
                        selected = selected,
                        today = today,
                        onSelect = onSelect,
                        onMonthChange = onMonthChange,
                    )
                    CalendarMode.Year -> YearHeatmap(
                        counts = counts,
                        selected = selected,
                        today = today,
                        onSelect = onSelect,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HeatLegend(Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, count: Int) {
    Row(
        modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pluralStringResource(R.plurals.submissions_count, count, count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmptyDay(onLog: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.dsa_empty_day),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onLog) { Text(stringResource(R.string.dsa_log_problem)) }
    }
}

@Composable
private fun ProblemRow(problem: DsaProblem, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(problem.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Text(listOf(problem.platform.label, problem.topic).filter { it.isNotBlank() }.joinToString(" · "))
            },
            leadingContent = { DifficultyBadge(problem.difficulty) },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.dsa_delete))
                }
            },
        )
    }
}

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    val color = difficultyColor(difficulty)
    Surface(color = color.copy(alpha = 0.16f), contentColor = color, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = difficulty.label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 64.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
