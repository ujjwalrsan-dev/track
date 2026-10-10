package com.uzxsan.dev.track.money.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyDao {
    @Query("SELECT * FROM money_accounts ORDER BY position, id")
    fun observeAccounts(): Flow<List<MoneyAccount>>

    @Query("SELECT * FROM money_transactions ORDER BY date DESC, timeMinutes DESC, createdAt DESC")
    fun observeTransactions(): Flow<List<MoneyTransaction>>

    @Query("SELECT * FROM money_totals ORDER BY id")
    fun observeTotals(): Flow<List<GrandTotal>>

    @Query("SELECT * FROM money_total_accounts")
    fun observeMemberships(): Flow<List<GrandTotalAccount>>

    @Query("SELECT COUNT(*) FROM money_totals")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM money_accounts")
    suspend fun accountCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: MoneyAccount): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: MoneyTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTotals(totals: List<GrandTotal>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMemberships(memberships: List<GrandTotalAccount>)

    @Query("DELETE FROM money_total_accounts WHERE accountId = :accountId")
    suspend fun clearMembershipsOfAccount(accountId: Long)

    @Query("DELETE FROM money_total_accounts WHERE totalId = :totalId")
    suspend fun clearMembershipsOfTotal(totalId: Long)

    @Delete
    suspend fun deleteTransaction(transaction: MoneyTransaction)

    /** Also deletes the account's transactions and memberships (foreign-key cascade). */
    @Delete
    suspend fun deleteAccount(account: MoneyAccount)

    @Transaction
    suspend fun saveAccount(account: MoneyAccount, totalIds: Set<Long>) {
        val rowId = upsertAccount(account)
        val id = if (account.id == 0L) rowId else account.id
        clearMembershipsOfAccount(id)
        insertMemberships(totalIds.map { GrandTotalAccount(it, id) })
    }

    @Transaction
    suspend fun saveTotal(total: GrandTotal, accountIds: Set<Long>) {
        upsertTotals(listOf(total))
        clearMembershipsOfTotal(total.id)
        insertMemberships(accountIds.map { GrandTotalAccount(total.id, it) })
    }
}
