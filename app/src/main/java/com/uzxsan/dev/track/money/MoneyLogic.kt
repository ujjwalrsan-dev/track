package com.uzxsan.dev.track.money

import com.uzxsan.dev.track.money.data.GrandTotal
import com.uzxsan.dev.track.money.data.MoneySnapshot
import com.uzxsan.dev.track.money.data.MoneyTransaction
import com.uzxsan.dev.track.money.data.TransactionType
import com.uzxsan.dev.track.todo.weekStart
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency
import java.util.Locale

enum class MoneyPeriod { Day, Week, Month }

/** Current balance of every account: opening balance plus all money in, minus all money out. */
fun balances(snapshot: MoneySnapshot): Map<Long, Long> {
    val result = snapshot.accounts.associate { it.id to it.openingBalance }.toMutableMap()
    fun add(accountId: Long?, delta: Long) {
        if (accountId != null && accountId in result) result[accountId] = result.getValue(accountId) + delta
    }
    snapshot.transactions.forEach { t ->
        when (t.type) {
            TransactionType.Income -> add(t.accountId, t.amount)
            TransactionType.Expense -> add(t.accountId, -t.amount)
            TransactionType.Transfer -> {
                add(t.accountId, -t.amount)
                add(t.toAccountId, t.amount)
            }
        }
    }
    return result
}

/** Each grand total with the sum of the balances of the accounts chosen for it. */
fun grandTotals(snapshot: MoneySnapshot, balances: Map<Long, Long> = balances(snapshot)): List<Pair<GrandTotal, Long>> =
    snapshot.totals.map { total ->
        val members = snapshot.memberships.filter { it.totalId == total.id }.map { it.accountId }
        total to members.sumOf { balances[it] ?: 0L }
    }

/** The date range of the [period] containing [anchor]. */
fun periodRange(anchor: LocalDate, period: MoneyPeriod, firstDayOfWeek: DayOfWeek): ClosedRange<LocalDate> =
    when (period) {
        MoneyPeriod.Day -> anchor..anchor
        MoneyPeriod.Week -> weekStart(anchor, firstDayOfWeek).let { it..it.plusDays(6) }
        MoneyPeriod.Month -> YearMonth.from(anchor).let { it.atDay(1)..it.atEndOfMonth() }
    }

fun shiftPeriod(anchor: LocalDate, period: MoneyPeriod, steps: Long): LocalDate = when (period) {
    MoneyPeriod.Day -> anchor.plusDays(steps)
    MoneyPeriod.Week -> anchor.plusWeeks(steps)
    MoneyPeriod.Month -> anchor.plusMonths(steps)
}

data class FlowTotals(val income: Long = 0, val expense: Long = 0) {
    val net: Long get() = income - expense
}

/** Income and expense over [transactions]; transfers only move money, so they're left out. */
fun flowTotals(transactions: List<MoneyTransaction>): FlowTotals = FlowTotals(
    income = transactions.filter { it.type == TransactionType.Income }.sumOf { it.amount },
    expense = transactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount },
)

fun List<MoneyTransaction>.inRange(range: ClosedRange<LocalDate>): List<MoneyTransaction> =
    filter { it.localDate in range }

/** Transactions touching any of [accountIds], including transfers to or from them; all of them when empty. */
fun List<MoneyTransaction>.forAccounts(accountIds: Set<Long>): List<MoneyTransaction> =
    if (accountIds.isEmpty()) this else filter { it.accountId in accountIds || it.toAccountId in accountIds }

/** Formats and parses amounts stored in minor units, in the device's currency. */
class MoneyFormat(locale: Locale = Locale.getDefault()) {
    private val currency: Currency = runCatching { Currency.getInstance(locale) }.getOrElse { Currency.getInstance("USD") }
    val fractionDigits: Int = currency.defaultFractionDigits.coerceAtLeast(0)
    val symbol: String = currency.getSymbol(locale)

    private val full = NumberFormat.getCurrencyInstance(locale).apply { currency = this@MoneyFormat.currency }
    private val compact = NumberFormat.getCurrencyInstance(locale).apply {
        currency = this@MoneyFormat.currency
        maximumFractionDigits = 0
    }

    private fun major(minor: Long): BigDecimal = BigDecimal.valueOf(minor, fractionDigits)

    /** e.g. ₹1,234.50 (negative values keep the locale's minus style). */
    fun format(minor: Long): String = full.format(major(minor))

    /** Whole units only, for tiles and widgets: ₹1,235. */
    fun formatWhole(minor: Long): String = compact.format(major(minor))

    /** "+₹500.00" / "−₹120.00" for transaction rows. */
    fun formatSigned(minor: Long): String = when {
        minor > 0 -> "+" + format(minor)
        minor < 0 -> "−" + format(-minor)
        else -> format(0)
    }

    /** Parses user input like "1234.5" or "1,234.50" into minor units; null if invalid or negative. */
    fun parse(text: String): Long? {
        val cleaned = text.trim().replace(symbol, "").replace(" ", "")
        if (cleaned.isEmpty()) return null
        // Treat the last '.' or ',' as the decimal separator when followed by 1..fractionDigits digits.
        val lastSep = cleaned.lastIndexOfAny(charArrayOf('.', ','))
        val normalized = if (lastSep >= 0 && cleaned.length - lastSep - 1 in 1..fractionDigits) {
            cleaned.substring(0, lastSep).replace(",", "").replace(".", "") + "." + cleaned.substring(lastSep + 1)
        } else {
            cleaned.replace(",", "").replace(".", "")
        }
        val value = normalized.toBigDecimalOrNull() ?: return null
        if (value.signum() < 0) return null
        return runCatching { value.movePointRight(fractionDigits).longValueExact() }.getOrNull()
    }

    /** Minor units back to an editable string, e.g. 123450 -> "1234.50". */
    fun toInput(minor: Long): String = major(minor).toPlainString()
}
