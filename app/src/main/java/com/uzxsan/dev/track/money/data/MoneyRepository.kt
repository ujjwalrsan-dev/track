package com.uzxsan.dev.track.money.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Everything the Budget tracker shows, observed together. */
data class MoneySnapshot(
    val accounts: List<MoneyAccount> = emptyList(),
    val transactions: List<MoneyTransaction> = emptyList(),
    val totals: List<GrandTotal> = emptyList(),
    val memberships: List<GrandTotalAccount> = emptyList(),
)

class MoneyRepository(private val dao: MoneyDao) {

    fun observe(): Flow<MoneySnapshot> = combine(
        dao.observeAccounts(),
        dao.observeTransactions(),
        dao.observeTotals(),
        dao.observeMemberships(),
    ) { accounts, transactions, totals, memberships ->
        MoneySnapshot(accounts, transactions, totals, memberships)
    }

    fun observeTransactions(): Flow<List<MoneyTransaction>> = dao.observeTransactions()

    /** First run: three named grand totals and a "Cash" account counted in the first one. */
    suspend fun ensureDefaults() {
        if (dao.totalCount() == 0) {
            dao.upsertTotals(DEFAULT_TOTALS.mapIndexed { i, name -> GrandTotal(id = i + 1L, name = name) })
        }
        if (dao.accountCount() == 0) {
            dao.saveAccount(MoneyAccount(name = DEFAULT_ACCOUNT), totalIds = setOf(1L))
        }
    }

    suspend fun saveTransaction(transaction: MoneyTransaction) {
        dao.upsertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: MoneyTransaction) = dao.deleteTransaction(transaction)

    suspend fun saveAccount(account: MoneyAccount, totalIds: Set<Long>) = dao.saveAccount(account, totalIds)

    suspend fun deleteAccount(account: MoneyAccount) = dao.deleteAccount(account)

    suspend fun saveTotal(total: GrandTotal, accountIds: Set<Long>) = dao.saveTotal(total, accountIds)

    companion object {
        val DEFAULT_TOTALS = listOf("Net worth", "Spendable", "Savings")
        const val DEFAULT_ACCOUNT = "Cash"
    }
}
