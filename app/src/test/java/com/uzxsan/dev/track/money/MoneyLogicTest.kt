package com.uzxsan.dev.track.money

import com.uzxsan.dev.track.money.data.GrandTotal
import com.uzxsan.dev.track.money.data.GrandTotalAccount
import com.uzxsan.dev.track.money.data.MoneyAccount
import com.uzxsan.dev.track.money.data.MoneySnapshot
import com.uzxsan.dev.track.money.data.MoneyTransaction
import com.uzxsan.dev.track.money.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

class MoneyLogicTest {
    private val today = LocalDate.of(2026, 10, 10)

    private fun tx(type: TransactionType, amount: Long, from: Long, to: Long? = null, date: LocalDate = today) =
        MoneyTransaction(type = type, amount = amount, accountId = from, toAccountId = to, date = date.toEpochDay(), timeMinutes = 0)

    private val snapshot = MoneySnapshot(
        accounts = listOf(
            MoneyAccount(id = 1, name = "Cash", openingBalance = 1_000),
            MoneyAccount(id = 2, name = "Bank", openingBalance = 50_000),
            MoneyAccount(id = 3, name = "Savings", openingBalance = 0),
        ),
        transactions = listOf(
            tx(TransactionType.Income, 20_000, from = 2),
            tx(TransactionType.Expense, 300, from = 1),
            tx(TransactionType.Transfer, 10_000, from = 2, to = 3),
        ),
        totals = listOf(GrandTotal(1, "Net worth"), GrandTotal(2, "Spendable"), GrandTotal(3, "Savings")),
        memberships = listOf(
            GrandTotalAccount(1, 1), GrandTotalAccount(1, 2), GrandTotalAccount(1, 3),
            GrandTotalAccount(2, 1), GrandTotalAccount(2, 2),
            GrandTotalAccount(3, 3),
        ),
    )

    @Test
    fun balancesApplyIncomeExpenseAndTransfers() {
        assertEquals(mapOf(1L to 700L, 2L to 60_000L, 3L to 10_000L), balances(snapshot))
    }

    @Test
    fun grandTotalsSumOnlyChosenAccounts() {
        val totals = grandTotals(snapshot).associate { it.first.id to it.second }
        assertEquals(70_700L, totals[1])
        assertEquals(60_700L, totals[2])
        assertEquals(10_000L, totals[3])
    }

    @Test
    fun accountFilterKeepsTransfersOnEitherSide() {
        val all = snapshot.transactions
        assertEquals(all, all.forAccounts(emptySet()))
        assertEquals(listOf(TransactionType.Expense), all.forAccounts(setOf(1L)).map { it.type })
        assertEquals(listOf(TransactionType.Transfer), all.forAccounts(setOf(3L)).map { it.type })
        assertEquals(listOf(TransactionType.Income, TransactionType.Transfer), all.forAccounts(setOf(2L)).map { it.type })
        assertEquals(all, all.forAccounts(setOf(1L, 2L)))
    }

    @Test
    fun transfersAreNotIncomeOrExpense() {
        val flow = flowTotals(snapshot.transactions)
        assertEquals(20_000L, flow.income)
        assertEquals(300L, flow.expense)
        assertEquals(19_700L, flow.net)
    }

    @Test
    fun periodRanges() {
        assertEquals(today..today, periodRange(today, MoneyPeriod.Day, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 10, 5)..LocalDate.of(2026, 10, 11), periodRange(today, MoneyPeriod.Week, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 10, 1)..LocalDate.of(2026, 10, 31), periodRange(today, MoneyPeriod.Month, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 9, 10), shiftPeriod(today, MoneyPeriod.Month, -1))
    }

    @Test
    fun parsesAmountsIntoMinorUnits() {
        val fmt = MoneyFormat(Locale.US)
        assertEquals(123_450L, fmt.parse("1234.5"))
        assertEquals(123_450L, fmt.parse("1,234.50"))
        assertEquals(100_000L, fmt.parse("1,000"))
        assertEquals(1_99L, fmt.parse("1.99"))
        assertNull(fmt.parse(""))
        assertNull(fmt.parse("abc"))
        assertNull(fmt.parse("-5"))
        // Three digits after the last separator means it groups thousands (e.g. "1.999" in many locales).
        assertEquals(199_900L, fmt.parse("1.999"))
    }

    @Test
    fun formatsSignedAmounts() {
        val fmt = MoneyFormat(Locale.US)
        assertEquals("+$5.00", fmt.formatSigned(500))
        assertEquals("−$1.25", fmt.formatSigned(-125))
        assertEquals("1234.50", fmt.toInput(123_450))
    }
}
