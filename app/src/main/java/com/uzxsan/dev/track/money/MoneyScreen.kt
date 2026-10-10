package com.uzxsan.dev.track.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.money.data.MoneyAccount
import com.uzxsan.dev.track.money.data.MoneyTransaction
import com.uzxsan.dev.track.todo.weekDays
import com.uzxsan.dev.track.ui.components.GlassExtendedFab
import com.uzxsan.dev.track.ui.components.GlassSurface
import com.uzxsan.dev.track.ui.components.GlassTopBar
import com.uzxsan.dev.track.ui.components.LocalBackdrop
import com.uzxsan.dev.track.ui.theme.accentSegmentedButtonColors
import com.uzxsan.dev.track.ui.theme.accentTrack
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Which sheet is open; kept as a kind + id pair so it survives rotation. */
private enum class Editor { NewTransaction, EditTransaction, NewAccount, EditAccount, EditTotal }

private fun MoneyPeriod.label() = when (this) {
    MoneyPeriod.Day -> R.string.budget_period_day
    MoneyPeriod.Week -> R.string.budget_period_week
    MoneyPeriod.Month -> R.string.budget_period_month
}

@Composable
fun MoneyScreen(
    onBack: () -> Unit,
    openAddOnStart: Boolean = false,
    viewModel: MoneyViewModel = viewModel(factory = MoneyViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val format = rememberMoneyFormat()
    val today = remember { LocalDate.now() }
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }

    var period by rememberSaveable { mutableStateOf(MoneyPeriod.Day) }
    var anchorEpoch by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var editor by rememberSaveable { mutableStateOf(if (openAddOnStart) Editor.NewTransaction else null) }
    var editorId by rememberSaveable { mutableLongStateOf(0L) }
    val anchor = LocalDate.ofEpochDay(anchorEpoch)

    val range = periodRange(anchor, period, firstDayOfWeek)
    val inPeriod = state.snapshot.transactions.inRange(range)
    val flow = flowTotals(inPeriod)
    val accountName: (Long?) -> String = { id -> state.account(id)?.name ?: "—" }

    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val contentBackdrop = rememberLayerBackdrop()
    val glassBackdrop = rememberCombinedBackdrop(LocalBackdrop.current, contentBackdrop)

    fun open(kind: Editor, id: Long = 0L) {
        editorId = id
        editor = kind
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { GlassTopBar(title = stringResource(R.string.budget_title), onBack = onBack, backdrop = glassBackdrop) },
        floatingActionButton = {
            GlassExtendedFab(
                text = stringResource(R.string.budget_add_transaction),
                icon = painterResource(R.drawable.ic_add),
                expanded = fabExpanded,
                onClick = { open(Editor.NewTransaction) },
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
            item(key = "totals") {
                GrandTotalsCard(state, format, onEdit = { open(Editor.EditTotal, it) })
            }
            item(key = "accounts") {
                AccountsRow(
                    accounts = state.snapshot.accounts,
                    balances = state.balances,
                    format = format,
                    onEdit = { open(Editor.EditAccount, it) },
                    onAdd = { open(Editor.NewAccount) },
                )
            }
            item(key = "period") {
                PeriodCard(
                    period = period,
                    onPeriodChange = { period = it },
                    title = periodTitle(anchor, period, firstDayOfWeek),
                    flow = flow,
                    format = format,
                    onPrev = { anchorEpoch = shiftPeriod(anchor, period, -1).toEpochDay() },
                    onNext = { anchorEpoch = shiftPeriod(anchor, period, 1).toEpochDay() },
                )
            }
            if (inPeriod.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.budget_empty_period),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                }
            } else {
                // Newest day first; Day view needs no per-day headers.
                inPeriod.groupBy { it.localDate }.toSortedMap(compareByDescending { it }).forEach { (date, dayItems) ->
                    if (period != MoneyPeriod.Day) {
                        item(key = "day-$date") { DayHeader(date, flowTotals(dayItems), format) }
                    }
                    items(dayItems, key = { "tx-${it.id}" }) { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            accountName = accountName,
                            format = format,
                            onClick = { open(Editor.EditTransaction, transaction.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    when (editor) {
        Editor.NewTransaction, Editor.EditTransaction -> {
            val initial = state.snapshot.transactions.firstOrNull { it.id == editorId }
                .takeIf { editor == Editor.EditTransaction }
            // Wait for data before opening, so a new transaction starts on a real account.
            if (state.snapshot.accounts.isNotEmpty()) {
                TransactionSheet(
                    initial = initial,
                    accounts = state.snapshot.accounts,
                    defaultDate = if (anchor > today) today else anchor,
                    format = format,
                    onSave = {
                        viewModel.saveTransaction(it)
                        editor = null
                    },
                    onDelete = { deleted ->
                        viewModel.deleteTransaction(deleted)
                        editor = null
                        scope.launch {
                            val result = snackbar.showSnackbar(
                                message = context.getString(R.string.budget_deleted),
                                actionLabel = context.getString(R.string.undo),
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) viewModel.saveTransaction(deleted)
                        }
                    },
                    onDismiss = { editor = null },
                    backdrop = glassBackdrop,
                )
            }
        }
        Editor.NewAccount, Editor.EditAccount -> {
            val account = state.account(editorId).takeIf { editor == Editor.EditAccount }
            AccountSheet(
                initial = account,
                totals = state.snapshot.totals,
                memberOf = account?.let { state.totalsOf(it.id) }.orEmpty(),
                transactionCount = account?.let { a ->
                    state.snapshot.transactions.count { it.accountId == a.id || it.toAccountId == a.id }
                } ?: 0,
                format = format,
                onSave = { saved, totalIds ->
                    viewModel.saveAccount(saved.copy(position = account?.position ?: state.snapshot.accounts.size), totalIds)
                    editor = null
                },
                onDelete = {
                    viewModel.deleteAccount(it)
                    editor = null
                },
                onDismiss = { editor = null },
                backdrop = glassBackdrop,
            )
        }
        Editor.EditTotal -> {
            val total = state.snapshot.totals.firstOrNull { it.id == editorId }
            if (total != null) {
                TotalSheet(
                    total = total,
                    accounts = state.snapshot.accounts,
                    members = state.membersOf(total.id),
                    balances = state.balances,
                    format = format,
                    onSave = { saved, ids ->
                        viewModel.saveTotal(saved, ids)
                        editor = null
                    },
                    onDismiss = { editor = null },
                    backdrop = glassBackdrop,
                )
            }
        }
        null -> Unit
    }
}

private fun periodTitle(anchor: LocalDate, period: MoneyPeriod, firstDayOfWeek: DayOfWeek): String {
    val locale = Locale.getDefault()
    return when (period) {
        MoneyPeriod.Day -> anchor.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
        MoneyPeriod.Week -> {
            val days = weekDays(anchor, firstDayOfWeek)
            val f = DateTimeFormatter.ofPattern("d MMM")
            "${days.first().format(f)} – ${days.last().format(f)}"
        }
        MoneyPeriod.Month -> "${anchor.month.getDisplayName(TextStyle.FULL, locale)} ${anchor.year}"
    }
}

/** The three user-defined grand totals; tap one to rename it or choose its accounts. */
@Composable
private fun GrandTotalsCard(state: MoneyUiState, format: MoneyFormat, onEdit: (Long) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.budget_grand_totals),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.grandTotals.forEachIndexed { index, (total, amount) ->
                    val members = state.membersOf(total.id).size
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (index == 0) scheme.primaryContainer.copy(alpha = 0.7f) else accentTrack())
                            .clickable { onEdit(total.id) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        Text(
                            total.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (index == 0) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        ThinAmount(format.formatWhole(amount), maxSize = 22.sp, minSize = 11.sp)
                        Text(
                            pluralStringResource(R.plurals.budget_accounts_count, members, members),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Horizontally scrolling account balances, plus a chip to add another account. */
@Composable
private fun AccountsRow(
    accounts: List<MoneyAccount>,
    balances: Map<Long, Long>,
    format: MoneyFormat,
    onEdit: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Text(
            stringResource(R.string.budget_accounts),
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            accounts.forEach { account ->
                GlassSurface(
                    modifier = Modifier.width(150.dp),
                    shape = RoundedCornerShape(22.dp),
                    onClick = { onEdit(account.id) },
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource(R.drawable.ic_wallet),
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                account.name,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        ThinAmount(format.format(balances[account.id] ?: 0L), maxSize = 20.sp)
                    }
                }
            }
            GlassSurface(
                modifier = Modifier.width(110.dp),
                shape = RoundedCornerShape(22.dp),
                tint = Color.White.copy(alpha = 0.2f),
                onClick = onAdd,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(scheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.budget_add_account), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** Day / Week / Month switch, period navigation and the period's income, expense and net. */
@Composable
private fun PeriodCard(
    period: MoneyPeriod,
    onPeriodChange: (MoneyPeriod) -> Unit,
    title: String,
    flow: FlowTotals,
    format: MoneyFormat,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp)) {
        Column(Modifier.padding(16.dp)) {
            val periods = MoneyPeriod.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                periods.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == period,
                        onClick = { onPeriodChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, periods.size),
                        colors = accentSegmentedButtonColors(),
                    ) { Text(stringResource(option.label())) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) {
                    Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.todo_prev))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNext) {
                    Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.todo_next))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowStat(stringResource(R.string.budget_income), format.format(flow.income), scheme.primary, Modifier.weight(1f))
                FlowStat(stringResource(R.string.budget_expense), format.format(flow.expense), scheme.onSurface, Modifier.weight(1f))
                FlowStat(stringResource(R.string.budget_net), format.formatSigned(flow.net), scheme.onSurface, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FlowStat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(accentTrack())
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ThinAmount(value, maxSize = 18.sp, minSize = 10.sp, color = color)
    }
}

@Composable
private fun DayHeader(date: LocalDate, flow: FlowTotals, format: MoneyFormat) {
    Row(
        modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            date.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
            modifier = Modifier.weight(1f),
        )
        Text(
            format.formatSigned(flow.net),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Light),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
