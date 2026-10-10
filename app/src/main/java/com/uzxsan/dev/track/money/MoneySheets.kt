package com.uzxsan.dev.track.money

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.uzxsan.dev.track.R
import com.uzxsan.dev.track.money.data.GrandTotal
import com.uzxsan.dev.track.money.data.MoneyAccount
import com.uzxsan.dev.track.money.data.MoneyTransaction
import com.uzxsan.dev.track.money.data.TransactionType
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val DAY_MILLIS = 86_400_000L

private val expenseCategories =
    listOf("Food", "Groceries", "Transport", "Shopping", "Bills", "Rent", "Health", "Entertainment", "Travel")
private val incomeCategories = listOf("Salary", "Freelance", "Business", "Interest", "Gift", "Refund")

/** Scrollable sheet body shared by the Budget sheets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoneySheet(
    onDismiss: () -> Unit,
    backdrop: Backdrop,
    content: @Composable ColumnScope.(hide: (after: () -> Unit) -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hide: (() -> Unit) -> Unit = { after -> scope.launch { sheetState.hide() }.invokeOnCompletion { after() } }
    GlassBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, backdrop = backdrop) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { content(hide) }
    }
}

@Composable
private fun SheetTitle(text: String) {
    // Thin title per the Budget typography: no bold.
    Text(text, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light))
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun AccountChips(
    accounts: List<MoneyAccount>,
    selected: Long?,
    onSelect: (Long) -> Unit,
    exclude: Long? = null,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        accounts.filter { it.id != exclude }.forEach { account ->
            FilterChip(
                selected = account.id == selected,
                onClick = { onSelect(account.id) },
                label = { Text(account.name) },
                colors = accentFilterChipColors(),
                border = accentFilterChipBorder(account.id == selected),
            )
        }
    }
}

/**
 * Add or edit an income, expense or transfer. [initial] null means a new transaction on [defaultDate].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSheet(
    initial: MoneyTransaction?,
    accounts: List<MoneyAccount>,
    defaultDate: LocalDate,
    format: MoneyFormat,
    onSave: (MoneyTransaction) -> Unit,
    onDelete: (MoneyTransaction) -> Unit,
    onDismiss: () -> Unit,
    backdrop: Backdrop = LocalBackdrop.current,
) {
    val now = remember { LocalTime.now() }
    var type by rememberSaveable { mutableStateOf(initial?.type ?: TransactionType.Expense) }
    var amount by rememberSaveable { mutableStateOf(initial?.let { format.toInput(it.amount) } ?: "") }
    var accountId by rememberSaveable { mutableLongStateOf(initial?.accountId ?: accounts.firstOrNull()?.id ?: 0L) }
    var toAccountId by rememberSaveable {
        mutableLongStateOf(initial?.toAccountId ?: accounts.firstOrNull { it.id != accountId }?.id ?: 0L)
    }
    var category by rememberSaveable { mutableStateOf(initial?.category ?: "") }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var dateEpoch by rememberSaveable { mutableLongStateOf(initial?.date ?: defaultDate.toEpochDay()) }
    var minutes by rememberSaveable { mutableIntStateOf(initial?.timeMinutes ?: (now.hour * 60 + now.minute)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val parsed = format.parse(amount)?.takeIf { it > 0 }
    val isTransfer = type == TransactionType.Transfer
    val accountsValid = accounts.any { it.id == accountId } &&
        (!isTransfer || (toAccountId != accountId && accounts.any { it.id == toAccountId }))
    val date = LocalDate.ofEpochDay(dateEpoch)
    val time = LocalTime.of(minutes / 60, minutes % 60)

    MoneySheet(onDismiss = onDismiss, backdrop = backdrop) { hide ->
        SheetTitle(stringResource(if (initial == null) R.string.budget_new_transaction else R.string.budget_edit_transaction))

        val types = TransactionType.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            types.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == type,
                    onClick = { type = option },
                    shape = SegmentedButtonDefaults.itemShape(index, types.size),
                    colors = accentSegmentedButtonColors(),
                ) { Text(typeLabel(option)) }
            }
        }

        AmountField(value = amount, onValueChange = { amount = it }, format = format)

        if (isTransfer) {
            if (accounts.size < 2) {
                Text(
                    stringResource(R.string.budget_need_two_accounts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                SectionLabel(stringResource(R.string.budget_from))
                AccountChips(accounts, accountId, onSelect = {
                    accountId = it
                    if (toAccountId == it) toAccountId = accounts.first { a -> a.id != it }.id
                })
                SectionLabel(stringResource(R.string.budget_to))
                AccountChips(accounts, toAccountId, onSelect = { toAccountId = it }, exclude = accountId)
            }
        } else {
            SectionLabel(stringResource(R.string.budget_account))
            AccountChips(accounts, accountId, onSelect = { accountId = it })

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = stringResource(R.string.budget_category),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (if (type == TransactionType.Income) incomeCategories else expenseCategories).forEach {
                        SuggestionChip(onClick = { category = it }, label = { Text(it) }, border = accentSuggestionChipBorder())
                    }
                }
            }
        }

        PremiumTextField(
            value = note,
            onValueChange = { note = it },
            label = stringResource(R.string.budget_note),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel(stringResource(R.string.budget_when))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { showDatePicker = true },
                label = { Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_calendar), contentDescription = null) },
                colors = accentAssistChipColors(),
                border = accentAssistChipBorder(),
            )
            AssistChip(
                onClick = { showTimePicker = true },
                label = { Text(time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_schedule), contentDescription = null) },
                colors = accentAssistChipColors(),
                border = accentAssistChipBorder(),
            )
        }

        Button(
            onClick = {
                val value = parsed ?: return@Button
                val saved = MoneyTransaction(
                    id = initial?.id ?: 0,
                    type = type,
                    amount = value,
                    accountId = accountId,
                    toAccountId = if (isTransfer) toAccountId else null,
                    category = if (isTransfer) "" else category.trim(),
                    note = note.trim(),
                    date = dateEpoch,
                    timeMinutes = minutes,
                    createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                )
                hide { onSave(saved) }
            },
            enabled = parsed != null && accountsValid,
            colors = accentButtonColors(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.save)) }

        if (initial != null) {
            TextButton(onClick = { hide { onDelete(initial) } }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.budget_delete_transaction), color = MaterialTheme.colorScheme.error)
            }
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
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = pickerState) }
    }
    if (showTimePicker) {
        val pickerState = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60)
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.todo_pick_time)) },
            confirmButton = {
                TextButton(onClick = {
                    minutes = pickerState.hour * 60 + pickerState.minute
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { TimePicker(state = pickerState) }
    }
}

/** Checkbox row used to choose accounts for a total, or totals for an account. */
@Composable
private fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
        )
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (trailing != null) ThinAmount(trailing, maxSize = 16.sp, weight = FontWeight.Normal)
    }
}

/** Add or edit an account, its opening balance and which grand totals it counts towards. */
@Composable
fun AccountSheet(
    initial: MoneyAccount?,
    totals: List<GrandTotal>,
    memberOf: Set<Long>,
    transactionCount: Int,
    format: MoneyFormat,
    onSave: (MoneyAccount, Set<Long>) -> Unit,
    onDelete: (MoneyAccount) -> Unit,
    onDismiss: () -> Unit,
    backdrop: Backdrop = LocalBackdrop.current,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var opening by rememberSaveable { mutableStateOf(initial?.let { format.toInput(it.openingBalance) } ?: "") }
    // New accounts count towards the first total ("Net worth") by default.
    var selectedTotals by rememberSaveable {
        mutableStateOf((if (initial == null) totals.take(1).map { it.id } else memberOf.toList()).toLongArray())
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val openingValue = if (opening.isBlank()) 0L else format.parse(opening)

    MoneySheet(onDismiss = onDismiss, backdrop = backdrop) { hide ->
        SheetTitle(stringResource(if (initial == null) R.string.budget_new_account else R.string.budget_edit_account))
        PremiumTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.budget_account_name),
            placeholder = stringResource(R.string.budget_account_hint),
            leadingIcon = painterResource(R.drawable.ic_wallet),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel(stringResource(R.string.budget_opening_balance))
        AmountField(value = opening, onValueChange = { opening = it }, format = format, size = 32.sp)

        SectionLabel(stringResource(R.string.budget_counts_towards))
        Column {
            totals.forEach { total ->
                val checked = total.id in selectedTotals
                CheckRow(total.name, checked, onToggle = {
                    selectedTotals = if (checked) {
                        selectedTotals.filter { it != total.id }.toLongArray()
                    } else {
                        selectedTotals + total.id
                    }
                })
            }
        }

        Button(
            onClick = {
                val account = (initial ?: MoneyAccount(name = "")).copy(
                    name = name.trim(),
                    openingBalance = openingValue ?: return@Button,
                )
                hide { onSave(account, selectedTotals.toSet()) }
            },
            enabled = name.isNotBlank() && openingValue != null,
            colors = accentButtonColors(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.save)) }

        if (initial != null) {
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.budget_delete_account), color = MaterialTheme.colorScheme.error)
            }
        }

        if (confirmDelete && initial != null) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(stringResource(R.string.budget_delete_account_title, initial.name)) },
                text = { Text(stringResource(R.string.budget_delete_account_body, transactionCount)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        hide { onDelete(initial) }
                    }) { Text(stringResource(R.string.budget_delete), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}

/** Rename a grand total and choose which accounts it adds up. */
@Composable
fun TotalSheet(
    total: GrandTotal,
    accounts: List<MoneyAccount>,
    members: Set<Long>,
    balances: Map<Long, Long>,
    format: MoneyFormat,
    onSave: (GrandTotal, Set<Long>) -> Unit,
    onDismiss: () -> Unit,
    backdrop: Backdrop = LocalBackdrop.current,
) {
    var name by rememberSaveable { mutableStateOf(total.name) }
    var selected by rememberSaveable { mutableStateOf(members.toLongArray()) }
    val sum = selected.sumOf { balances[it] ?: 0L }

    MoneySheet(onDismiss = onDismiss, backdrop = backdrop) { hide ->
        SheetTitle(stringResource(R.string.budget_edit_total))
        ThinAmount(format.format(sum), maxSize = 40.sp, weight = FontWeight.ExtraLight)
        PremiumTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.budget_total_name),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel(stringResource(R.string.budget_included_accounts))
        Column {
            accounts.forEach { account ->
                val checked = account.id in selected
                CheckRow(
                    label = account.name,
                    checked = checked,
                    trailing = format.format(balances[account.id] ?: 0L),
                    onToggle = {
                        selected = if (checked) selected.filter { it != account.id }.toLongArray() else selected + account.id
                    },
                )
            }
        }
        Button(
            onClick = { hide { onSave(total.copy(name = name.trim()), selected.toSet()) } },
            enabled = name.isNotBlank(),
            colors = accentButtonColors(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.save)) }
    }
}
