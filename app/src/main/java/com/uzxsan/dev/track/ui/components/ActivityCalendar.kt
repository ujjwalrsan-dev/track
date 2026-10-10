package com.uzxsan.dev.track.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.ui.theme.accentTrack
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Buckets a daily count into 0..4 intensity levels. */
fun heatLevel(count: Int): Int = when {
    count <= 0 -> 0
    count == 1 -> 1
    count <= 3 -> 2
    count <= 5 -> 3
    else -> 4
}

private val levelFractions = floatArrayOf(0f, 0.3f, 0.55f, 0.8f, 1f)

@Composable
fun heatColor(count: Int): Color {
    val scheme = MaterialTheme.colorScheme
    val level = heatLevel(count)
    val empty = accentTrack()
    return if (level == 0) empty else lerp(empty, scheme.primary, levelFractions[level])
}

@Composable
fun heatContentColor(count: Int): Color =
    if (heatLevel(count) >= 3) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface

@Composable
private fun rememberFirstDayOfWeek(): DayOfWeek =
    remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }

@Composable
fun MonthCalendar(
    month: YearMonth,
    counts: Map<LocalDate, Int>,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    allowFuture: Boolean = false,
    subtitle: String? = null,
) {
    val firstDayOfWeek = rememberFirstDayOfWeek()
    val locale = Locale.getDefault()
    val monthCounts = counts.filterKeys { YearMonth.from(it) == month }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                Icon(
                    painterResource(R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.dsa_prev_month),
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${month.month.getDisplayName(TextStyle.FULL, locale)} ${month.year}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = subtitle ?: pluralStringResource(
                        R.plurals.submissions_count,
                        monthCounts.values.sum(),
                        monthCounts.values.sum(),
                    ) + " · " + pluralStringResource(
                        R.plurals.active_days_count,
                        monthCounts.size,
                        monthCounts.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { onMonthChange(month.plusMonths(1)) },
                enabled = allowFuture || month < YearMonth.from(today),
            ) {
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.dsa_next_month),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row {
            repeat(7) { i ->
                Text(
                    text = firstDayOfWeek.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it * direction } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it * direction } + fadeOut()) using
                    SizeTransform(clip = true)
            },
            label = "month",
        ) { shownMonth ->
            val offset = (shownMonth.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
            val length = shownMonth.lengthOfMonth()
            val rows = (offset + length + 6) / 7
            Column(Modifier.fillMaxWidth()) {
                repeat(rows) { row ->
                    Row {
                        repeat(7) { column ->
                            val dayOfMonth = row * 7 + column - offset + 1
                            if (dayOfMonth in 1..length) {
                                val date = shownMonth.atDay(dayOfMonth)
                                DayCell(
                                    date = date,
                                    count = counts[date] ?: 0,
                                    isToday = date == today,
                                    isSelected = date == selected,
                                    isFuture = !allowFuture && date > today,
                                    onClick = { onSelect(date) },
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    count: Int,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val content = heatContentColor(count)
    val ring = when {
        isSelected -> Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, shape)
        isToday -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape)
        else -> Modifier
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(shape)
            .background(if (isFuture) Color.Transparent else heatColor(count))
            .then(ring)
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else null,
                color = if (isFuture) content.copy(alpha = 0.38f) else content,
            )
            if (count > 0) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 10.sp),
                    color = content.copy(alpha = 0.8f),
                )
            }
        }
    }
}

/** GitHub-style contribution grid for the trailing 53 weeks. */
@Composable
fun YearHeatmap(
    counts: Map<LocalDate, Int>,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstDayOfWeek = rememberFirstDayOfWeek()
    val locale = Locale.getDefault()
    val weekStart = today.minusDays(((today.dayOfWeek.value - firstDayOfWeek.value + 7) % 7).toLong())
    val start = weekStart.minusWeeks(52)
    val lastYearTotal = counts.filterKeys { it >= start }.values.sum()
    val scroll = rememberScrollState()
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }

    val cellShape = RoundedCornerShape(3.dp)
    Column(modifier) {
        Text(
            text = pluralStringResource(R.plurals.submissions_last_year, lastYearTotal, lastYearTotal),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            // End padding leaves room for the last month label, which may overflow its column.
            modifier = Modifier.horizontalScroll(scroll).padding(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            repeat(53) { week ->
                val columnStart = start.plusWeeks(week.toLong())
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.size(width = 14.dp, height = 16.dp)) {
                        if (columnStart.dayOfMonth <= 7) {
                            Text(
                                text = columnStart.month.getDisplayName(TextStyle.SHORT, locale),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = false,
                                modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
                            )
                        }
                    }
                    repeat(7) { day ->
                        val date = columnStart.plusDays(day.toLong())
                        if (date > today) {
                            Spacer(Modifier.size(14.dp))
                        } else {
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .clip(cellShape)
                                    .background(heatColor(counts[date] ?: 0))
                                    .then(
                                        if (date == selected) {
                                            Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, cellShape)
                                        } else Modifier
                                    )
                                    .clickable { onSelect(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeatLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.dsa_less),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        listOf(0, 1, 2, 4, 6).forEach { sample ->
            Box(
                Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(heatColor(sample))
            )
        }
        Text(
            stringResource(R.string.dsa_more),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
